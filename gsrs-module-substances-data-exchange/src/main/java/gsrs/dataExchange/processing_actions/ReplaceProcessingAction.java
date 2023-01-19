package gsrs.dataexchange.processing_actions;

import gsrs.dataexchange.model.ProcessingAction;
import ix.ginas.models.v1.Substance;

import java.util.Map;
import java.util.function.Consumer;

public class ReplaceProcessingAction implements ProcessingAction<Substance> {
    @Override
    public Substance process(Substance source, Substance existing, Map<String, Object> parameters, Consumer<String> log) throws Exception {
        log.accept("Starting in process. going to return " + source);
        return source;
    }
}
