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
public class DeprecatedIndexValueMaker implements IndexValueMaker<Substance> {

    @Override
    public Class<Substance> getIndexedEntityClass() {
        return Substance.class;
    }

    @Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {
        if (substance.deprecated){
            consumer.accept(IndexableValue.simpleFacetStringValue("Deprecated","Deprecated"));
        }else{
        	consumer.accept(IndexableValue.simpleFacetStringValue("Deprecated","Not Deprecated"));
        }
    }
}