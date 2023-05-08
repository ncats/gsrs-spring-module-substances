package gsrs.dataexchange.processingactions;

import gsrs.dataexchange.model.ProcessingAction;
import ix.ginas.models.v1.Substance;

import java.util.Map;
import java.util.function.Consumer;

/*
Action represents user intention to keep what's in the DB and ignore the new match
 */
public class RejectProcessingAction implements ProcessingAction<Substance> {
    @Override
    public Substance process(Substance source, Substance existing, Map<String, Object> parameters, Consumer<String> log) throws Exception {
        log.accept("Starting in process");
        return existing;
    }

    @Override
    public String getActionName() {
        return "Reject";
    }
}
