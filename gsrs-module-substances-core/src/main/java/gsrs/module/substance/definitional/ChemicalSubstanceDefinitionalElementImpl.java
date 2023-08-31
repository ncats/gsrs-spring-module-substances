package gsrs.module.substance.definitional;

import gsrs.module.substance.services.DefinitionalElementImplementation;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Moiety;
import ix.utils.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
@Slf4j
public class ChemicalSubstanceDefinitionalElementImpl implements DefinitionalElementImplementation {

    @Autowired
    private StructureProcessor structureProcessor;

    private List<String> stereoUsingOpticalActivities = Arrays.asList( "UNKNOWN", "MIXED", "EPIMERIC");

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
        if(structure.properties.isEmpty()) {
            log.warn("instrumenting structure to avoid error on substance {}",
                    (chemicalSubstance.getUuid() !=null ? chemicalSubstance.getUuid() : "[ID not available]"));
            Util.printAllExecutingStackTraces();
            structure = structureProcessor.instrument(chemicalSubstance.getStructure().toChemical(), true);
        }

        log.debug("starting computeDefinitionalElements (ChemicalSubstance)");
        consumer.accept(DefinitionalElement.of("structure.properties.hash1",
                structure.getStereoInsensitiveHash(), 1));
        log.debug("structure.getStereoInsensitiveHash(): "  + structure.getStereoInsensitiveHash());
        consumer.accept(DefinitionalElement.of("structure.properties.hash2",
                structure.getExactHash(), 2));
        log.debug("structure.getExactHash(): " + structure.getExactHash());

        if(structure.stereoChemistry!=null){
            consumer.accept(DefinitionalElement.of("structure.properties.stereoChemistry",
                    structure.stereoChemistry.toString(), 2));
            log.debug("structure.stereoChemistry : " + structure.stereoChemistry.toString());
            
            if(structure.opticalActivity!=null){
                String stereoTxt=structure.stereoChemistry.toString().toUpperCase();
                if(stereoUsingOpticalActivities.contains(stereoTxt)){
                    log.trace("using optical activity in def hash for structure");
                    consumer.accept(DefinitionalElement.of("structure.properties.opticalActivity",
                            structure.opticalActivity.toString(), 2));
                    log.debug("structure.opticalActivity.toString(): " + structure.opticalActivity.toString());
                }
            }
        }
      
        if( chemicalSubstance.moieties != null) {
            for(Moiety m: chemicalSubstance.moieties){
                String mh=m.structure.getStereoInsensitiveHash();
                log.debug("processing moiety with hash " + mh);
                consumer.accept(DefinitionalElement.of("moiety.hash1", m.structure.getStereoInsensitiveHash(),1));
                consumer.accept(DefinitionalElement.of("moiety.hash2", m.structure.getExactHash(),2));
                log.debug("m.structure.getExactHash(): " + m.structure.getExactHash());
                consumer.accept(DefinitionalElement.of("moiety[" + mh + "].stereoChemistry",
                        m.structure.stereoChemistry.toString(), 2));
                log.debug("m.structure.stereoChemistry.toString(): " + m.structure.stereoChemistry.toString());
                
                String stereoTxt= m.structure.stereoChemistry.toString().toUpperCase();
                if( stereoUsingOpticalActivities.contains(stereoTxt)){
                    log.trace("using optical activity in def hash for moiety");
                    consumer.accept(DefinitionalElement.of("moiety[" + mh + "].opticalActivity",
                            m.structure.opticalActivity.toString(), 2));
                    log.debug("m.structure.opticalActivity.toString(): " + m.structure.opticalActivity.toString());
                }
                consumer.accept(DefinitionalElement.of("moiety[" + mh + "].countAmount",
                        m.getCountAmount().toString(), 2));
                log.debug("m.getCountAmount().toString(): " + m.getCountAmount().toString());
            }
        }
    }
}
