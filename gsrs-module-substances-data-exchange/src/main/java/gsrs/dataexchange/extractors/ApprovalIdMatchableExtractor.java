package gsrs.dataexchange.extractors;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.stagingarea.model.MatchableKeyValueTuple;
import gsrs.stagingarea.model.MatchableKeyValueTupleExtractor;
import ix.ginas.models.v1.Substance;

import java.util.function.Consumer;

public class ApprovalIdMatchableExtractor implements MatchableKeyValueTupleExtractor<Substance> {
    public final String APPROVAL_ID_KEY ="Approval ID";
    public final int APPROVAL_ID_LAYER=0;

    public ApprovalIdMatchableExtractor(JsonNode config){

    }
    @Override
    public void extract(Substance substance, Consumer<MatchableKeyValueTuple> c) {
        if(substance.approvalID!= null && substance.approvalID.length() >0) {
            MatchableKeyValueTuple tuple =
                    MatchableKeyValueTuple.builder()
                            .key(APPROVAL_ID_KEY)
                            .value(substance.approvalID)
                            .layer(APPROVAL_ID_LAYER)
                            .build();
            c.accept(tuple);
        }
    }
}
