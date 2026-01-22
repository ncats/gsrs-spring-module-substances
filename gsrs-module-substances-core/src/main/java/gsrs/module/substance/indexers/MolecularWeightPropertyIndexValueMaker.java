package gsrs.module.substance.indexers;

import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.Substance;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Created by VenkataSaiRa.Chavali on 4/20/2017.
 */
@Component
public class MolecularWeightPropertyIndexValueMaker implements IndexValueMaker<Substance> {
    @Override
    public Class<Substance> getIndexedEntityClass() {
        return Substance.class;
    }
    @Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {
        if (substance.properties != null) {
        	substance.properties.stream()
                    .filter(a ->a.getName().toUpperCase().contains("MOL_WEIGHT") && a.getValue()!=null)
                    .forEach(p -> {
                        Double avg = p.getValue().average;
                        if (avg != null) {
                            //
                            consumer.accept(IndexableValue.simpleFacetLongValue("Molecular Weight", (long)Math.floor(avg), new long[]{0, 200, 400, 600, 800, 1000}).setFormat("%1$.0f").setSortable());
                            consumer.accept(IndexableValue.simpleDoubleValue("root_structure_mwt", avg).setSortable());
                        }
                    });
        }
    }
}