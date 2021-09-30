package gsrs.module.substance.indexers;

import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.Substance;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BracketTermIndexValueMaker implements IndexValueMaker<Substance> {

    @Override
    public Class<Substance> getIndexedEntityClass() {
        return Substance.class;
    }

    @Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {
        
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
    }
}
