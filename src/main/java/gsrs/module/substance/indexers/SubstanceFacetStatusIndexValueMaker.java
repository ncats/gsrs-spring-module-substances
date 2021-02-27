package gsrs.module.substance.indexers;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Consumer;

@Component
public class SubstanceFacetStatusIndexValueMaker implements IndexValueMaker<Substance> {

    /*
    In GSRS 2.x this used to be a method in the Substance class that
    was annotated with @Indexable but it made calls to the equivalent
    of the Substance Repository which shouldn't be done from the actual model
    so I pulled this out to an indexvalue maker for GSRS 3.
     */

    @Autowired
    private SubstanceRepository substanceRepository;

    @Override
    public Class<Substance> getIndexedEntityClass() {
        return Substance.class;
    }

    @Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {
        consumer.accept(IndexableValue.simpleFacetStringValue("Record Status", getFacetStatus(substance)));
    }

    private String getFacetStatus(Substance substance){
        if(substance.isNonSubstanceConcept()){
            return "Concept";
        }else if(substance.isSubstanceVariant()){
            SubstanceReference sr=substance.getParentSubstanceReference();
            if(sr!=null){
                Optional<SubstanceRepository.SubstanceSummary> summary = substanceRepository.findSummaryBySubstanceReference(sr);
                if(summary.isPresent() && Substance.STATUS_APPROVED.equals(summary.get().getStatus())){
                    return "Validated Subconcept";
                }
                if(sr.approvalID!=null){
                    return "Validated Subconcept";
                }
            }


            return "Pending Subconcept";
        }else{
            return substance.status;
        }
    }
}
