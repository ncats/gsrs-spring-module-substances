package gsrs.module.substance.indexers;

import ix.core.models.Group;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.core.search.text.IndexableValueFromRaw;
import ix.ginas.models.GinasSubstanceDefinitionAccess;
import ix.ginas.models.v1.Substance;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by mandavag on 10/24/16.
 */
@Component
public class RecordAccessIndexValueMaker implements IndexValueMaker<Substance> {

    @Override
    public Class<Substance> getIndexedEntityClass() {
        return Substance.class;
    }
    @Override
    public void createIndexableValues(Substance t, Consumer<IndexableValue> consumer) {

        makeRecordAccessLevelValues(t, consumer);
        makeDisplayNameValues(t, consumer);
        makeDefinitionLevelAccessValues(t, consumer);
    }

    public void makeRecordAccessLevelValues(Substance t, Consumer<IndexableValue> consumer) {
        Set<Group> groups=t.getAccess();

        makeAccessLevelFacet("Record Level Access",groups, consumer);
    }

    public void makeDisplayNameValues(Substance t, Consumer<IndexableValue> consumer) {
        Set<Group> groups= t.getDisplayName().map(n -> n.getAccess()).orElse(null);

        makeAccessLevelFacet("Display Name Level Access",groups, consumer);
    }

    public void makeDefinitionLevelAccessValues(Substance t, Consumer<IndexableValue> consumer) {
        Set<Group> groups=null;

        if(t instanceof GinasSubstanceDefinitionAccess){
            groups= Optional.ofNullable(((GinasSubstanceDefinitionAccess) t).getDefinitionElement())
                    .map(d->d.getAccess())
                    .orElse(null);
            makeAccessLevelFacet("Definition Level Access",groups, consumer);
        }
        
    }
    public void makeAccessLevelFacet(String facetName, Set<Group> groups, Consumer<IndexableValue> consumer) {
        
        if(groups== null || groups.isEmpty()){
            consumer.accept(new IndexableValueFromRaw(facetName, "public").dynamic());
        }else {
            for (Group grp: groups) {
                consumer.accept(new IndexableValueFromRaw(facetName, grp.name).dynamic());
            }
        }
    }
    

}
