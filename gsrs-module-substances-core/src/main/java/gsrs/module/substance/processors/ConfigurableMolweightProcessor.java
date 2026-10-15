package gsrs.module.substance.processors;

import com.fasterxml.jackson.annotation.JsonCreator;
import gov.nih.ncats.molwitch.Atom;
import gov.nih.ncats.molwitch.Chemical;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.Amount;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Property;
import ix.ginas.models.v1.QualifiedAtom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * Updated for GSRS 3.0
 * Changed 'inherent' to 'intrinsic'
 *
 * The atomic weights used for the calculation are supplied through this entity processor's
 * "parameters" block rather than through an external file.  Each entry is a map with a "symbol"
 * and a "value", plus an optional "massNumber" identifying a specific isotope, for example:
 *
 * <pre>
 * gsrs.entityProcessors.list.ConfigurableMolweightProcessor = {
 *   "entityClassName" : "ix.ginas.models.v1.ChemicalSubstance",
 *   "processor" : "gsrs.module.substance.processors.ConfigurableMolweightProcessor",
 *   "parameters" : {
 *     "persistenceMode" : "intrinsic",
 *     "atomicWeights" : [
 *       { "symbol" : "C", "value" : 12.511 },
 *       { "symbol" : "Br", "massNumber" : 81, "value" : 280.916290563 }
 *     ]
 *   }
 * }
 * </pre>
 *
 * @author mitch
 */
@Slf4j
public class ConfigurableMolweightProcessor implements EntityProcessor<ChemicalSubstance> {

    private Map<QualifiedAtom, Double> atomicWeights = new HashMap<>();
    private String persistenceMode;
    private String propertyName;
    private Integer decimalDigits = 0;
    private String oldPropertyName;

    private final String PROPERTY_TYPE = "PHYSICAL";

    /**
     * The delegating {@link JsonCreator} is required because entity processors declared with
     * "parameters" are built by Jackson ({@code ObjectMapper.convertValue}) rather than by a direct
     * constructor call.  A delegating creator hands the whole parameter map to this constructor and
     * bypasses Jackson's unknown-property handling.
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public ConfigurableMolweightProcessor(Map initialValues) {
        log.trace("starting in ConfigurableMolweightProcessor");
        Object configuredAtomicWeights = initialValues.get("atomicWeights");
        if (configuredAtomicWeights instanceof List) {
            initAtomicWeights((List<?>) configuredAtomicWeights);
        } else {
            log.trace("no atomic weights supplied; internal exact masses will be used");
        }
        persistenceMode = (String) initialValues.get("persistenceMode");
        propertyName = (String) initialValues.get("propertyName");
        if (initialValues.get("decimalDigits") != null) {
            decimalDigits = (Integer) initialValues.get("decimalDigits");
        }
        if (initialValues.get("oldPropertyName") != null) {
            oldPropertyName = (String) initialValues.get("oldPropertyName");
        }
    }

    public String getPersistenceMode() {
        return persistenceMode;
    }

    public void setPersistenceMode(String persistenceMode) {
        this.persistenceMode = persistenceMode;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getOldPropertyName() {
        return oldPropertyName;
    }

    public void setOldPropertyName(String oldPropertyName) {
        this.oldPropertyName = oldPropertyName;
    }

    private void initAtomicWeights(List<?> entries) {
        log.trace("starting in initAtomicWeights. entries: {}", entries.size());
        atomicWeights = new HashMap<>();
        for (Object entryObject : entries) {
            if (!(entryObject instanceof Map)) {
                log.warn("skipping atomic weight entry that is not a map: {}", entryObject);
                continue;
            }
            Map<?, ?> entry = (Map<?, ?>) entryObject;
            Object symbolObject = entry.get("symbol");
            Object valueObject = entry.get("value");
            if (symbolObject == null || valueObject == null) {
                log.warn("skipping atomic weight entry without a symbol or value: {}", entry);
                continue;
            }
            String symbol = symbolObject.toString();
            Optional<Double> parsedWt = safelyParseDouble(valueObject.toString());
            if (!parsedWt.isPresent()) {
                log.warn("skipping atomic weight entry with an invalid value: {}", entry);
                continue;
            }
            int massIndication = 0;
            if (entry.get("massNumber") != null) {
                Optional<Integer> parsedMassIndication = safelyParseInteger(entry.get("massNumber").toString());
                if (parsedMassIndication.isPresent()) {
                    massIndication = parsedMassIndication.get();
                }
            }
            QualifiedAtom qa = new QualifiedAtom(symbol, massIndication);
            log.trace("adding atomic weight for symbol {}, massIndication: {}, value: {}",
                    symbol, massIndication, parsedWt.get());
            atomicWeights.put(qa, parsedWt.get());
        }
        log.trace("initAtomicWeights completed");
    }

    @Override
    public void prePersist(final ChemicalSubstance s) {
        calculateMw(s);
    }

    public void calculateMw(ChemicalSubstance s) {
        log.trace("ConfigurableMolweightProcessor.calculateMw");
        Double calculatedMw = computeMolWt(s);
        if (persistenceMode.equalsIgnoreCase("intrinsic")) {
            log.debug("setting intrinsic mw value to " + calculatedMw);
            s.getStructure().mwt = calculatedMw;
            //s.getStructure().forceUpdate();
        }
        else {
            setMwProperty(s, calculatedMw);
        }
    }

    @Override
    public void preUpdate(ChemicalSubstance chem) {
        //log.debug("ConfigurableMolweightProcessor.preUpdate");
        calculateMw(chem);
    }

    private void setMwProperty(ChemicalSubstance chem, Double mw) {
        log.trace("setMwProperty. total properties: " + chem.properties.size());
        Property mwProperty = null;
        for (Property p : chem.properties) {
            if (p.getName().equalsIgnoreCase(propertyName)) {
                mwProperty = p;
                log.trace("found property");
                break;
            }
        }
        if (mwProperty == null) {
            mwProperty = new Property();
            mwProperty.setName(propertyName);
            mwProperty.setPropertyType(PROPERTY_TYPE);
            chem.addProperty(mwProperty);
            log.trace("created property named " + propertyName);
        }
        Amount propertyAmount = new Amount();
        propertyAmount.average = decimalDigits > 0 ? roundToDecimals(mw, decimalDigits) : mw;
        mwProperty.setPropertyType(PROPERTY_TYPE);
        mwProperty.setValue(propertyAmount);
        mwProperty.forceUpdate();
        if (oldPropertyName != null && oldPropertyName.length() > 0) {
            //Property propertyToRemove = null;
            int indexToRemove = -1;
            for (int p = 0; p < chem.properties.size(); p++) {

                if (chem.properties.get(p).getName().equalsIgnoreCase(oldPropertyName)) {
                    indexToRemove = p;
                    chem.properties.get(p).deprecated = true;
                    break;
                }
            }
            if (indexToRemove > -1) {
                Property propertyToRemove = chem.properties.get(indexToRemove);

                log.trace("found and removed old property at index " + indexToRemove);
                chem.properties.remove(indexToRemove);
                propertyToRemove.deprecate(true);
            }
        }
        log.trace("end of setMwProperty. total properties: " + chem.properties.size());
    }

    private Double computeMolWt(ChemicalSubstance chemical) {

        java.util.concurrent.atomic.DoubleAccumulator mw = new DoubleAccumulator(Double::sum, 0L);
        Chemical chem = chemical.getStructure().toChemical().copy();
        if (chem.hasImplicitHs()) {
            chem.makeHydrogensExplicit();
        }
        AtomicInteger implicitHydrogenCount = new AtomicInteger(0);
        List<Atom> atoms = chemical.getStructure().toChemical().atoms().collect(Collectors.toList());
        atoms.forEach(a -> {
            implicitHydrogenCount.addAndGet(a.getImplicitHCount());
            //todo: reduce logging when this gets tedious:
            log.debug(String.format("atom; symbol: %s; mass number: %d", a.getSymbol(), a.getMassNumber()));
            QualifiedAtom qa = new QualifiedAtom(a.getSymbol(), a.getMassNumber());
            if (atomicWeights.containsKey(qa)) {
                log.debug("from supplied atomic weights: " + atomicWeights.get(qa));
                mw.accumulate(atomicWeights.get(qa));
            }
            else {
                log.debug("Using internal exact mass " + a.getExactMass() + " for atom " + a.getSymbol() + " calc "
                        + getAtomicMass(a));
                mw.accumulate(getAtomicMass(a));
            }
        });
        double mass = chem.getMass();
        QualifiedAtom hydrogen = new QualifiedAtom("H", 0, 1.008);
        double calculated = mw.doubleValue() + (atomicWeights.containsKey(hydrogen) ? atomicWeights.get(hydrogen) : hydrogen.getAtomicMass()) * implicitHydrogenCount.get();
        log.debug(String.format("getMass: %.4f; computed: %.4f.  implicitHydrogenCount.get(): %d", mass, calculated, implicitHydrogenCount.get()));
        return calculated;
    }

    private Optional<Double> safelyParseDouble(String input) {
        Optional<Double> parsed = Optional.empty();
        try {
            double d = Double.parseDouble(input);
            parsed = Optional.of(d);
        } catch (NumberFormatException ignored) {

        }
        return parsed;
    }

    private Optional<Integer> safelyParseInteger(String input) {
        Optional<Integer> parsed = Optional.empty();
        try {
            int d = Integer.parseInt(input);
            parsed = Optional.of(d);
        } catch (NumberFormatException ignored) {

        }
        return parsed;
    }

    public static double roundToDecimals(double num, int n) {
        String format = String.format("%s.%df", "%", n);
        String formattedNumber = String.format(format, num);
        log.trace("formattedNumber: {}", formattedNumber);
        return Double.parseDouble(formattedNumber);
    }

    private double getAtomicMass(Atom a) {
        Chemical chem = new Chemical();
        chem.addAtom(a);
        return chem.getMass();
    }

    @Override
    public Class<ChemicalSubstance> getEntityClass() {
        return ChemicalSubstance.class;
    }
}
