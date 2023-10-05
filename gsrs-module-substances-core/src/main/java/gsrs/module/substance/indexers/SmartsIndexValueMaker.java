package gsrs.module.substance.indexers;

import gov.nih.ncats.molwitch.search.MolSearcher;
import gov.nih.ncats.molwitch.search.MolSearcherFactory;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class SmartsIndexValueMaker extends HashMap<String, Object> implements IndexValueMaker<Substance> {

    private static final String FACET_NAME_PREFIX ="Structure_Facet_";

    private String configData = "nitro_[$([NX3](=O)=O),$([NX3+](=O)[O-])][!#8]€nitroso_[NX2]=[OX1]";
    List<SmartsIndexable> indexables;

    public SmartsIndexValueMaker(){
        indexables = new ArrayList<>();
        for(String fragmentName : this.keySet()) {
            String fragmentSmarts = (String) this.get(fragmentName);
            log.trace("handling smarts {} with name {}", fragmentSmarts, fragmentName);
            SmartsIndexable indexable = new SmartsIndexable(fragmentName, fragmentSmarts);
            indexables.add(indexable);
        }
        if( this.keySet().isEmpty()) {
            String[] tokens = configData.split("\\€");
            for(String token : tokens) {
                String[] parts = token.split("\\_");

                SmartsIndexable indexable = new SmartsIndexable(parts[0], parts[1]);
                indexables.add(indexable);
            }
        }
    }

    @Override
    public Class<Substance> getIndexedEntityClass() {
        return Substance.class;
    }

    @Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {
        if( substance instanceof ChemicalSubstance) {
            ChemicalSubstance chemical = (ChemicalSubstance) substance;
            indexables.forEach(i->{
                log.trace("Looking for chemical matches using smarts with name {}", i.getName());

                Optional<MolSearcher> optSearcher= MolSearcherFactory.create(i.getSMARTS());
                if( optSearcher.isPresent()) {
                    MolSearcher searcher= optSearcher.get();
                    Optional<int[]> matches= searcher.search(chemical.toChemical());
                    if( matches.isPresent() && matches.get().length>0) {
                        log.trace("chemical matches smarts with name {}", i.getName());
                        consumer.accept(IndexableValue.simpleStringValue(FACET_NAME_PREFIX+ i.getName(), "true"));
                    } else {
                        consumer.accept(IndexableValue.simpleStringValue(FACET_NAME_PREFIX+ i.getName(), "false"));
                    }
                    log.trace("after indexing");
                }
            });
        }
    }
}
