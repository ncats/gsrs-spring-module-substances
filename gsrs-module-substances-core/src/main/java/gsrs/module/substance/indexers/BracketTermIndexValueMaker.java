package gsrs.module.substance.indexers;

import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.tags.TagUtilities;
import java.util.function.Consumer;

public class BracketTermIndexValueMaker implements IndexValueMaker<Substance> {
    private final String TAG_FACET_NAME = "GInAS Tag";

    @Override
    public Class<Substance> getIndexedEntityClass() {
        return Substance.class;
    }

    @Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {
        if (substance.names!= null) {
            for (String tagTerm : TagUtilities.extractBracketNameTags(substance)) {
                consumer.accept(IndexableValue.simpleFacetStringValue(TAG_FACET_NAME, tagTerm));
            }
        }
        /*
        Pattern p = Pattern.compile("(?:[ \\]])\\[([ \\-A-Za-z0-9]+)\\]");
        if (substance.names != null) {
            substance.names.stream()
                    .filter(a -> a.getName().trim().endsWith("]"))
                    .forEach(n -> {
                        //ASPIRIN1,23[asguyasgda]asgduytqwqd [INN][USAN]
                        Matcher m = p.matcher(n.getName());
                        while (m.find()) {
                            String loc = m.group(1);
                            consumer.accept(IndexableValue.simpleFacetStringValue("GInAS Tag",loc));
                        }
                    });
        }

         */
    }
}
