package gsrs.module.substance.indexers;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ix.core.chem.StructureProcessor;
import ix.core.chem.StructureProcessorTask;
import ix.core.models.Structure;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.core.search.text.ReflectingIndexValueMaker;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;

/**
 * Adds exact and stereo insensitive structure hashes
 * as computed by @StructureProcessor to the indexer. 
 * 
 * Note that this is typically unnecessary, as the {@link ReflectingIndexValueMaker} will already find these values stored
 * on the structure. The only advantage to calling this explicitly is that the full standardization of hashes can be done
 * without recalculating structural properties.
 *
 */
@Component
public class ChemicalSubstanceStructureHashIndexValueMaker implements IndexValueMaker<Substance>{
    @Autowired
    private StructureProcessor structureProcessor;

    @Override
    public Class<Substance> getIndexedEntityClass() {
        return Substance.class;
    }
    //This is the method which does the work
    @Override
    public void createIndexableValues(Substance s, Consumer<IndexableValue> consumer) {
        if(s instanceof ChemicalSubstance){
            createStructureHashes((ChemicalSubstance)s, consumer);
        }
    }


    public void createStructureHashes(ChemicalSubstance s, Consumer<IndexableValue> consumer) {
        try{
            StructureProcessorTask task = structureProcessor.taskFor(s.getStructure().molfile)
                                                            .standardize(true)
                                                            .build()
                                                            .instrument();
            
            Structure reStandardizedStructure = task.getStructure();
            
            String stereoInsensitive=reStandardizedStructure.getStereoInsensitiveHash();
            String exact=reStandardizedStructure.getExactHash();
            
            
            
            addHashes(stereoInsensitive,exact,"root_structure_properties",consumer);
            task.getComponents().forEach(st->{
                String stereoInsensitiveM=st.getStereoInsensitiveHash();
                String exactM=st.getExactHash();
                addHashes(stereoInsensitiveM,exactM,"root_moieties_properties",consumer);
            });
            
            
            
            


        }catch(Exception e){
            e.printStackTrace();
        }

    }
    
    public static void addHashes(String stereoInsensitive, String exact, String prefix, Consumer<IndexableValue> consumer) {

        if(stereoInsensitive!=null) {
            consumer.accept(IndexableValue.simpleStringValue(prefix + "_term", stereoInsensitive));
            consumer.accept(IndexableValue.simpleStringValue(prefix, stereoInsensitive));
            consumer.accept(IndexableValue.simpleStringValue(prefix + "_STEREO_INSENSITIVE_HASH", stereoInsensitive));
        }
        
        if(exact!=null) {
            consumer.accept(IndexableValue.simpleStringValue(prefix + "_term", exact));
            consumer.accept(IndexableValue.simpleStringValue(prefix, exact));
            consumer.accept(IndexableValue.simpleStringValue(prefix + "_EXACT_HASH", exact));
        }
        
        
    }

}
