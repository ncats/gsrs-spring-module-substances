package gsrs.module.substance.definitional;

import gsrs.module.substance.services.DefinitionalElementImplementation;
import ix.core.models.Structure;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Moiety;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;
@Slf4j
public class ChemicalSubstanceDefinitionalElementImpl implements DefinitionalElementImplementation {
    @Override
    public boolean supports(Object s) {
        return s instanceof ChemicalSubstance;
    }

    @Override
    public void computeDefinitionalElements(Object s, Consumer<DefinitionalElement> consumer) {
        ChemicalSubstance chemicalSubstance = (ChemicalSubstance) s;
        Structure structure = chemicalSubstance.getStructure();
        if(structure==null){
            //shouldn't happen unless we get invalid submission
            return;
        }
        //log.debug("starting addStructureDefinitialElements (ChemicalSubstance)");
        consumer.accept(DefinitionalElement.of("structure.properties.hash1",
                structure.getStereoInsensitiveHash(), 1));
        //log.debug("structure.getStereoInsensitiveHash(): "  + structure.getStereoInsensitiveHash());
        consumer.accept(DefinitionalElement.of("structure.properties.hash2",
                structure.getExactHash(), 2));
        //log.debug("structure.getExactHash(): " + structure.getExactHash());

        if(structure.stereoChemistry!=null){
            consumer.accept(DefinitionalElement.of("structure.properties.stereoChemistry",
                    structure.stereoChemistry.toString(), 2));
            //log.debug("structure.stereoChemistry : " + structure.stereoChemistry.toString());
        }
        if(structure.opticalActivity!=null){
            consumer.accept(DefinitionalElement.of("structure.properties.opticalActivity",
                    structure.opticalActivity.toString(), 2));
            //log.debug("structure.opticalActivity.toString(): " + structure.opticalActivity.toString());
        }
        if( chemicalSubstance.moieties != null) {
            for(Moiety m: chemicalSubstance.moieties){
                String mh=m.structure.getStereoInsensitiveHash();
                //log.debug("processing moiety with hash " + mh);
                consumer.accept(DefinitionalElement.of("moiety.hash1", m.structure.getStereoInsensitiveHash(),1));
                consumer.accept(DefinitionalElement.of("moiety.hash2", m.structure.getExactHash(),2));
                //log.debug("m.structure.getExactHash(): " + m.structure.getExactHash());
                consumer.accept(DefinitionalElement.of("moiety[" + mh + "].stereoChemistry",
                        m.structure.stereoChemistry.toString(), 2));
                //log.debug("m.structure.stereoChemistry.toString(): " + m.structure.stereoChemistry.toString());
                consumer.accept(DefinitionalElement.of("moiety[" + mh + "].opticalActivity",
                        m.structure.opticalActivity.toString(), 2));
                //log.debug("m.structure.opticalActivity.toString(): " + m.structure.opticalActivity.toString());
                consumer.accept(DefinitionalElement.of("moiety[" + mh + "].countAmount",
                        m.getCountAmount().toString(), 2));
                //log.debug("m.getCountAmount().toString(): " + m.getCountAmount().toString());
            }
        }
    }
}
