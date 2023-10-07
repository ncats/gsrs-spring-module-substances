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

    private static final String FACET_NAME_PREFIX ="Structure_Facet_";

    private static final String FACET_NAME_FULL ="StructureFacet";
    private final String DEFAULT_CONFIG_DATA = "nitro_[$([NX3](=O)=O),$([NX3+](=O)[O-])][!#8]€nitroso_[NX2]=[OX1]";

    private Map<String, LinkedHashMap<Integer, String>> namedSmarts;

    List<SmartsIndexable> indexables;

    private boolean setupComplete=false;

    private void completeSetup(){
        log.trace("completeSetup");
        indexables = new ArrayList<>();
        if( namedSmarts==null || this.namedSmarts.entrySet().isEmpty()) {
            String usableConfig = DEFAULT_CONFIG_DATA;
            log.trace("using alternate config {}", usableConfig);
            String[] tokens = usableConfig.split("\\€");
            for(String token : tokens) {
                String[] parts = token.split("\\_");

                SmartsIndexable indexable = new SmartsIndexable(parts[0], Collections.singletonList( parts[1]));
                indexables.add(indexable);
            }
        }
        else {
            log.trace("using real config");
            for (Map.Entry<String, LinkedHashMap<Integer, String>> entry : namedSmarts.entrySet()) {
                String fragmentName = entry.getKey();
                LinkedHashMap<Integer, String> listOfSmarts = entry.getValue();
                log.trace("handling smarts set with name {}", fragmentName);
                List<String> smartsList = new ArrayList<>(listOfSmarts.values());
                SmartsIndexable indexable = new SmartsIndexable(fragmentName, smartsList);
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
        if(!setupComplete) {
            completeSetup();
            setupComplete=true;
        }
        if( substance instanceof ChemicalSubstance) {
            ChemicalSubstance chemical = (ChemicalSubstance) substance;
            indexables.forEach(i->{
                log.trace("Looking for chemical matches using smarts with name {}", i.getName());
                for(String smarts : i.getSMARTSList()) {
                    Optional<MolSearcher> optSearcher= MolSearcherFactory.create(smarts);
                    if( optSearcher.isPresent()) {
                        MolSearcher searcher= optSearcher.get();
                        Optional<int[]> matches= searcher.search(chemical.toChemical());
                        if( matches.isPresent() && matches.get().length>0) {
                            log.trace("chemical matches smarts with name {}", i.getName());
                            consumer.accept(IndexableValue.simpleFacetStringValue (FACET_NAME_PREFIX+ i.getName(), "true"));
                            consumer.accept(IndexableValue.simpleFacetStringValue(FACET_NAME_FULL, i.getName()));
                            break;
                        } else {
                            consumer.accept(IndexableValue.simpleFacetStringValue(FACET_NAME_PREFIX+ i.getName(), "false"));
                        }
                        log.trace("after indexing");
                    }
                }

            });
        }
    }

    public void setNamedSmarts(Map<String, LinkedHashMap<Integer, String>> namedSmarts) {
        this.namedSmarts = namedSmarts;
    }

}
