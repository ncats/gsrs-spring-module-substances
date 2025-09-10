package gsrs.module.substance.indexers;

import gov.nih.ncats.molwitch.search.MolSearcher;
import gov.nih.ncats.molwitch.search.MolSearcherFactory;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class SmartsIndexValueMaker implements IndexValueMaker<Substance> {

    public static final String FACET_NAME_FULL ="Structure Facet";

    private final String DEFAULT_CONFIG_DATA = "nitro_[$([NX3](=O)=O),$([NX3+](=O)[O-])][!#8]€nitroso_[NX2]=[OX1]";

    private Map<String, List<String>> rawNamedSmarts;

    List<SmartsIndexable> indexables = new ArrayList<>();

    private boolean setupComplete=false;

    public final static String NAME_TO_VALUE_DELIM = "₠";

    private String SET_TO_SET_DELIM = "¥";

    private String VALUE_TO_VALUE_DELIM = "€";

    private void completeSetup(){
        log.trace("completeSetup indexables size: {}", indexables.size());

        if( this.indexables != null && this.indexables.size() > 0) {
            log.info("using provided configuration");
            return;
        }

        indexables = new ArrayList<>();
        if(rawNamedSmarts != null && rawNamedSmarts.size() >0) {
            log.trace("using real config");
            for (Map.Entry<String, List<String>> entry : rawNamedSmarts.entrySet()) {
                String fragmentName = entry.getKey();
                //String rawSmartsList = entry.getValue();
                List<String> temps = entry.getValue();
                String[] listOfSmarts = temps.toArray(new String[temps.size()]);
                // rawSmartsList.split("\\€");
                log.trace("handling smarts set with name {}", fragmentName);
                List<String> smartsList = Arrays.asList(listOfSmarts);
                SmartsIndexable indexable = new SmartsIndexable(fragmentName, smartsList);
                indexables.add(indexable);
            }
        } else {
            String usableConfig = DEFAULT_CONFIG_DATA;
            log.trace("using default config {}", usableConfig);
            String[] tokens = usableConfig.split("\\€");
            for(String token : tokens) {
                String[] parts = token.split("\\_");

                SmartsIndexable indexable = new SmartsIndexable(parts[0], Collections.singletonList( parts[1]));
                log.trace("indexable with {} and {}", parts[0], parts[1]);
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
        if( !(substance instanceof ChemicalSubstance)) {
            return;
        }

        if(!setupComplete) {
            completeSetup();
            setupComplete=true;
        }
            ChemicalSubstance chemical = (ChemicalSubstance) substance;
            indexables.forEach(i->{
                log.trace("Looking for chemical matches using smarts with name {} with {} SMARTS patterns to match", i.getName(), i.getSMARTSList().size());
                boolean foundMatchWithCurrentIdea =false;
                for(int item = 0; item< i.getSMARTSList().size() && !foundMatchWithCurrentIdea; item++) {
                    String smarts = i.getSMARTSList().get(item);
                    Optional<MolSearcher> optSearcher= MolSearcherFactory.create(smarts);
                    if( optSearcher.isPresent()) {
                        MolSearcher searcher= optSearcher.get();
                        Optional<int[]> matches= searcher.search(chemical.toChemical());
                        if( matches.isPresent() && matches.get().length>0) {
                            foundMatchWithCurrentIdea=true;
                        }
                    }
                }
                if( foundMatchWithCurrentIdea) {
                    log.trace("chemical matches smarts with name {}", i.getName());
                    consumer.accept(IndexableValue.simpleFacetStringValue(FACET_NAME_FULL, i.getName()));

                }
                log.trace("after indexing");
            });
    }

    public void setRawNamedSmarts(Map<String, List<String>> newRawNamedSmarts) {
        log.trace("setRawNamedSmarts with value {}", newRawNamedSmarts);
        this.rawNamedSmarts = newRawNamedSmarts;
    }

    public List<SmartsIndexable> getIndexables() {
        return indexables;
    }

    public void setIndexables(List<SmartsIndexable> indexables) {
        this.indexables = indexables;
    }

    public void setRawIndexables( LinkedHashMap<Integer, Map<String, String>> newIndexables){
        log.trace("starting in setRawIndexables");
        this.indexables.clear();
        for (Map<String, String> expression: newIndexables.values()) {
            SmartsIndexable indexable = new SmartsIndexable(expression);
            if (indexable.isValid()) {
                this.indexables.add(indexable);
            }
        }
        log.trace("indexables has {} items", indexables.size());
    }
}
