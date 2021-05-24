package gsrs.module.substance.indexers;

import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;


/**
 * Adds inchikey values to text index for {@link ChemicalSubstance} objects.
 *
 *
 * @author peryeata
 *
 */
@Component
public class InchiKeyIndexValueMaker implements IndexValueMaker<Substance>{

    @Override
    public Class<Substance> getIndexedEntityClass() {
        return Substance.class;
    }
    @Override
    public void createIndexableValues(Substance s, Consumer<IndexableValue> consumer) {
        if(s instanceof ChemicalSubstance) {
            try {
                extractInchiKeys((ChemicalSubstance)s, consumer);
            }catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void extractInchiKeys(ChemicalSubstance s, Consumer<IndexableValue> consumer) {
        consumer.accept(IndexableValue.simpleStringValue("root_structure_inchikey", s.structure.getInChIKey()));
        s.moieties.stream().forEach(m->{
            consumer.accept(IndexableValue.simpleStringValue("root_moieties_structure_inchikey", m.structure.getInChIKey()));
        });
    }

}
