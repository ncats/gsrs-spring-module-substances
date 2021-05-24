package gsrs.module.substance.indexers;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.chem.StructureProcessor;
import ix.core.chem.StructureProcessorTask;
import ix.core.models.Structure;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.PolymerSubstance;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Adds structure hash index values to index for the polymer substances. Note that this is a pretty
 * hacky way to make these work.
 *
 * @author peryeata
 *
 */
@Component
public class PolymerStructureHashIndexValueMaker implements IndexValueMaker<Substance>{
    @Autowired
    private StructureProcessor structureProcessor;

    @Override
    public Class<Substance> getIndexedEntityClass() {
        return Substance.class;
    }
    //This is the method which does the work
    @Override
    public void createIndexableValues(Substance s, Consumer<IndexableValue> consumer) {
        if(s instanceof PolymerSubstance){
            createPolymerStructureHashes((PolymerSubstance)s, consumer);
        }
    }


    public void createPolymerStructureHashes(PolymerSubstance s, Consumer<IndexableValue> consumer) {
        try{
            Set<Structure> components = new HashSet<>();
            Structure structure = structureProcessor.instrument(s.polymer.displayStructure.molfile, components);




            consumer.accept(IndexableValue.simpleStringValue("root_structure_properties_term", structure.getStereoInsensitiveHash()));
            consumer.accept(IndexableValue.simpleStringValue("root_structure_properties_term", structure.getExactHash()));


            components.forEach(m->{
                consumer.accept(IndexableValue.simpleStringValue("root_moieties_structure_properties_term", m.getStereoInsensitiveHash()));
                consumer.accept(IndexableValue.simpleStringValue("root_moieties_structure_properties_term", m.getExactHash()));
            });

        }catch(Exception e){
            e.printStackTrace();
        }

    }

}
