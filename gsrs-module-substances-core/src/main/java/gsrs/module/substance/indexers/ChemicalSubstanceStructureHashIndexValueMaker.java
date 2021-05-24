package gsrs.module.substance.indexers;

import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Adds exact and stereo insenstivie structure hashes
 * as computed by @StructureProcessor} to the indexer.
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
            Structure structure = structureProcessor.instrument(s.structure.molfile);



            System.out.println("stereo insensitive hash = " + structure.getStereoInsensitiveHash());
            consumer.accept(IndexableValue.simpleStringValue("root_structure_properties_term", structure.getStereoInsensitiveHash()));
            consumer.accept(IndexableValue.simpleStringValue("root_structure_properties_term", structure.getExactHash()));


        }catch(Exception e){
            e.printStackTrace();
        }

    }

}
