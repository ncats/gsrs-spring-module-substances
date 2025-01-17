package gsrs.module.substance.definitional;

import gsrs.module.substance.services.DefinitionalElementFactory;
import gsrs.module.substance.services.DefinitionalElementImplementation;
import gov.nih.ncats.molwitch.Chemical;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.ginas.models.v1.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.function.Consumer;


@Slf4j
public class PolymerDefinitionalElementImpl implements DefinitionalElementImplementation {

    @Autowired
    private StructureProcessor structureProcessor;
    @Autowired
    private DefinitionalElementFactory definitionalElementFactory;

    public DefinitionalElementFactory getDefinitionalElementFactory() {
        return definitionalElementFactory;
    }

    public void setDefinitionalElementFactory(DefinitionalElementFactory definitionalElementFactory) {
        this.definitionalElementFactory = definitionalElementFactory;
    }

    @Override
    public boolean supports(Object s) {
        return s instanceof PolymerSubstance;
    }

    @Override
    public void computeDefinitionalElements(Object s, Consumer<DefinitionalElement> consumer) {
        DefinitionalElementImplementation.super.computeDefinitionalElements(s, consumer);
        PolymerSubstance polymerSubstance = (PolymerSubstance)s;

        Polymer polymer = polymerSubstance.polymer;
        if (polymer != null) {


            for (Material monomer : polymerSubstance.polymer.monomers) {
                if (monomer.monomerSubstance != null) {
                    DefinitionalElement monomerElement = DefinitionalElement.of("polymer.monomer.monomerSubstance.refuuid",
                            monomer.monomerSubstance.refuuid, 1);
                    consumer.accept(monomerElement);
                    log.debug("adding monomer refuuid to the def hash: " + monomer.monomerSubstance.refuuid);
                    if (monomer.amount != null) {
                        DefinitionalElement monomerAmountElement = DefinitionalElement.of("polymer.monomer.monomerSubstance.amount",
                                monomer.amount.toString(), 2);
                        consumer.accept(monomerAmountElement);
                    }
                } else {
                    log.debug("monomer does not have a substance attached.");
                }
            }

            if (polymerSubstance.polymer.structuralUnits != null && !polymerSubstance.polymer.structuralUnits.isEmpty()) {
                //todo: consider canonicalizing
                List<Unit> canonicalizedUnits = polymerSubstance.polymer.structuralUnits;
                for (Unit unit : canonicalizedUnits) {
                    if (unit.type == null) {
                        log.debug("skipping null unit");
                        continue;
                    }
                    //log.debug("about to process unit structure " + unit.structure);
                    String molfile = unit.structure;//prepend newline to avoid issue later on

                    Structure structure = null;
                    try {
                        structure = structureProcessor.instrument(Chemical.parseMol(molfile), false);


                        log.debug("created structure OK. looking at unit type: " + unit.type);
                        int layer = 1;
                    /* all units are part of layer 1 as of 13 March 2020 based on https://cnigsllc.atlassian.net/browse/GSRS-1361
                    if( unit.type.contains("SRU")) {
                        layer=1;
                    }*/

                        String currentHash = structure.getExactHash();
                        DefinitionalElement structUnitElement = DefinitionalElement.of("polymer.structuralUnit.structure.l4",
                                currentHash, layer);
                        consumer.accept(structUnitElement);

                        if (unit.amount != null) {
                            DefinitionalElement structUnitAmountElement = DefinitionalElement.of("polymer.structuralUnit["
                                    + currentHash + "].amount", unit.amount.toString(), 2);
                            consumer.accept(structUnitAmountElement);
                        }
                        log.debug("adding structural unit def element: " + structUnitElement);
                    } catch (Exception ex) {
                        log.warn("Unable to parse structure from polymer unit molfile: " + molfile);
                        continue;
                    }
                }
            }

            //todo: add additional items to the definitional element list
            if (polymerSubstance.modifications != null) {
                definitionalElementFactory.addDefinitionalElementsFor(polymerSubstance.modifications, consumer);

            }

        }
    }
}
