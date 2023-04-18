package gsrs.dataexchange.processingactions;

import gsrs.dataexchange.model.ProcessingAction;
import ix.ginas.models.v1.Substance;

import java.util.Map;
import java.util.function.Consumer;

/*
Create a new record in the main database, ignoring the matched record
 */
public class CreateProcessingAction implements ProcessingAction<Substance> {
    @Override
    public Substance process(Substance source, Substance existing, Map<String, Object> parameters, Consumer<String> log) throws Exception {
        log.accept("Starting in process. going to return " + source);
        return source;
    }

    @Override
    public String getActionName() {
        return "Create";
    }
}
