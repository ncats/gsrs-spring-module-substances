package gsrs.dataexchange.processingactions;

import gsrs.dataexchange.model.ProcessingAction;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import ix.core.EntityFetcher;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class CreateBatchProcessingAction implements ProcessingAction<Substance> {
    @Autowired
    private SubstanceLegacySearchService searchService;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    public final static String BATCH_RELATIONSHIP_TYPE= "BATCH->PARENT";
    public final static String BATCH_CODE_SYSTEM = "Batch Code";

    @Override
    public Substance process(Substance stagingAreaRecord, Substance additionalRecord, Map<String, Object> parameters, Consumer<String> log) throws Exception {

        String batchCodeSystem = (parameters.get("BatchCodeSystem") !=null && parameters.get("BatchCodeSystem").toString().length()>0) ?
                parameters.get("BatchCodeSystem").toString() : null;
        String batchRelationshipType = (parameters.get("BatchRelationshipType") !=null && parameters.get("BatchRelationshipType").toString().length()>0) ?
                parameters.get("BatchRelationshipType").toString() : null;
        Objects.requireNonNull(batchCodeSystem, "Need a code system to create batches");
        Objects.requireNonNull(batchRelationshipType, "Need a relationship type to create batches");
        SubstanceBuilder batchBuilder = new SubstanceBuilder();
        Code batchCode= new Code();
        batchCode.codeSystem= batchCodeSystem;
        batchCode.code= getNextBatchCode(additionalRecord, batchRelationshipType, batchCodeSystem);
        batchCode.type= "PRIMARY";
        UUID newSubstanceUUID = UUID.randomUUID();
        batchBuilder.setUUID(newSubstanceUUID);
        batchBuilder.addCode(batchCode);

        Relationship batchRelationship = new Relationship();
        batchRelationship.relatedSubstance= new SubstanceReference();
        batchRelationship.relatedSubstance= additionalRecord.asSubstanceReference();
        batchRelationship.type= batchRelationshipType;
        batchBuilder.addRelationship(batchRelationship);
        //batchBuilder

        return batchBuilder.build();
    }

    public String getNextBatchCode(Substance parentSubstance, String relationshipType, String codeSystem){
        //test:
        //strategy: look all substance with the specified relationship type to the parent substance,
        // find the one with the greatest value of the supplied code system, increment the value and
        // return it as a string.
        StringBuilder queryStringBuilder = new StringBuilder();
        queryStringBuilder.append("root_relationships_type:\"^");
        queryStringBuilder.append(relationshipType);
        queryStringBuilder.append("$\" AND root_relationships_relatedSubstance_refuuid:\"^");
        queryStringBuilder.append(parentSubstance.uuid.toString());
        queryStringBuilder.append("$\"");
        String query= queryStringBuilder.toString();
        log.trace("getNextBatchCode using query {}", query);

        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(0)
                .query(query)
                .top(Integer.MAX_VALUE)
                .build();
        List<Substance> substances = getSearchList(request);
        log.trace("search retrieved {}", substances.size());

        AtomicInteger maxId= new AtomicInteger(0);
        substances.forEach(s->{
            EntityUtils.Key skey = EntityUtils.Key.of(Substance.class, s.uuid);
            Optional<Substance> substance = EntityFetcher.of(skey).getIfPossible().map(o->(Substance)o);
            if(substance.isPresent()) {
                //need to check relationships to verify that this is a valid hit!
                AtomicBoolean validHit = new AtomicBoolean(false);
                for(Relationship r: substance.get().relationships){
                    if(r.type.equals(relationshipType) && r.relatedSubstance.refuuid.equals(parentSubstance.uuid.toString())){
                        validHit.set(true);
                        break;
                    }
                }
                if(validHit.get()) {
                    substance.get().codes.forEach(c -> {
                        if (c.codeSystem.equals(codeSystem)) {
                            log.trace("retrieved code {}", c.code);
                            try {
                                int current = Integer.parseInt(c.code);
                                if (current > maxId.get()) {
                                    maxId.set(current);
                                }
                            } catch (NumberFormatException ex) {
                                //nothing to do; move on the next item
                            }
                        }
                    });
                }
            }
        });
        maxId.getAndIncrement();
        return String.format("%04d", maxId.get());
    }

    private List<Substance> getSearchList(SearchRequest sr) {
        TransactionTemplate transactionSearch = new TransactionTemplate(transactionManager);
        List<Substance> substances = transactionSearch.execute(ts -> {
            try {
                SearchResult sresult = searchService.search(sr.getQuery(), sr.getOptions());
                List<Substance> first = sresult.getMatches();
                return first.stream()
                        //force fetching
                        .peek(ss -> EntityUtils.EntityWrapper.of(ss).toInternalJson())
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("Error in getSearchList: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });
        return substances;
    }

    @Override
    public String getActionName() {
        return "Create Batch";
    }
}
