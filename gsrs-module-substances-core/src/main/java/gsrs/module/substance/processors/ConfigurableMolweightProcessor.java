package gsrs.module.substance.processors;

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
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * Updated for GSRS 3.0
 * Changed 'inherent' to 'intrinsic'
 * @author mitch
 */
@Slf4j
public class ConfigurableMolweightProcessor implements EntityProcessor<ChemicalSubstance> {

    private Map<QualifiedAtom, Double> atomicWeights = new HashMap<>();
    private String atomWeightFilePath;
    private String persistenceMode;
    private String propertyName;
    private Integer decimalDigits = 0;
    private String oldPropertyName;

    private final String PROPERTY_TYPE = "PHYSICAL";

    public ConfigurableMolweightProcessor(Map initialValues) {
        log.trace("starting in ConfigurableMolweightProcessor");
        atomWeightFilePath = (String) initialValues.get("atomWeightFilePath");
        if (!atomWeightFilePath.isEmpty() && Files.exists(Paths.get(atomWeightFilePath))) {
            initAtomicWeights(atomWeightFilePath);
        } else {
            log.trace("file not found: " + atomWeightFilePath);
            log.trace("working dir: " + System.getProperty("user.dir"));
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

    public String getAtomWeightFilePath() {
        return atomWeightFilePath;
    }

    public void setAtomWeightFilePath(String atomWeightFilePath) {
        this.atomWeightFilePath = atomWeightFilePath;
        if (Files.exists(Paths.get(atomWeightFilePath))) {
            initAtomicWeights(atomWeightFilePath);
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

    private void initAtomicWeights(String filePath) {
        log.debug("starting in initAtomicWeights. path: " + filePath);
        atomicWeights = new HashMap<>();
        String commentIntro = "#";
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            log.debug("total lines " + lines.size());
            for (int lineNum = 1; lineNum < lines.size(); lineNum++) {
                String line = lines.get(lineNum);
                log.debug("line: " + line);
                if (line.trim().isEmpty() || line.trim().startsWith(commentIntro)) {
                    log.debug(String.format("line %d is blank", lineNum));
                    continue;
                }
                String[] lineParts = line.split(",");
                //log.debug("lineParts len: " + lineParts.length);
                String symbol = lineParts[1];
                String rawAw = lineParts[3];
                log.debug("symbol: " + symbol + "; rawAw: " + rawAw);
                if (!rawAw.isEmpty()) {
                    Optional<Double> parsedWt = safelyParseDouble(rawAw);
                    log.debug("parsedWt.isPresent(): " + parsedWt.isPresent());
                    if (parsedWt.isPresent()) {
                        int massIndication = 0;
                        if (lineParts.length >= 5 && !lineParts[4].isEmpty()) {
                            Optional<Integer> parsedMassIndication = safelyParseInteger(lineParts[4]);
                            if (parsedMassIndication.isPresent()) {
                                massIndication = parsedMassIndication.get();
                            }
                        }
                        log.debug("massIndication: " + massIndication);
                        QualifiedAtom qa = new QualifiedAtom(symbol, massIndication);
                        log.trace(String.format("adding atomic weight for symbol %s, massIndication: %d, value: %s",
                                symbol, massIndication, parsedWt.get()));
                        atomicWeights.put(qa, parsedWt.get());
                    }
                    else {
                        log.debug("no double in input " + rawAw);
                    }
                }
                else {
                    log.debug("skipping blank line " + line);
                }
            }
        } catch (Exception ex) {
            log.error("Error reading atomic weights: " + ex.getMessage());
            ex.printStackTrace();
        }
        log.debug("initAtomicWeights completed");
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
        //propertyAmount.type= PROPERTY_TYPE;
        //propertyAmount.nonNumericValue = this.getClass().getName();
        mwProperty.setValue(propertyAmount);
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
        } catch (NumberFormatException ex) {

        }
        return parsed;
    }

    private Optional<Integer> safelyParseInteger(String input) {
        Optional<Integer> parsed = Optional.empty();
        try {
            int d = Integer.parseInt(input);
            parsed = Optional.of(d);
        } catch (NumberFormatException ex) {

        }
        return parsed;
    }

    public static double roundToDecimals(double num, int n) {
        String format = String.format("%s.%df", "%", n);
        String formattedNumber = String.format(format, num);
        log.trace("formattedNumber: " + formattedNumber);
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
