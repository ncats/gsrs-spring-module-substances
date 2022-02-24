package ix.ginas.utils.validation.strategy;

import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.models.v1.Substance;

import java.util.List;

public class BatchProcessingStrategy implements GsrsProcessingStrategy{
    private final GsrsProcessingStrategy delegate;

    public BatchProcessingStrategy(GsrsProcessingStrategy delegate) {
        this.delegate = delegate;
    }

    @Override
    public void processMessage(GinasProcessingMessage gpm) {
        delegate.processMessage(gpm);
    }

    @Override
    public boolean test(GinasProcessingMessage gpm) {
        return delegate.test(gpm);
    }

    @Override
    public void addAndProcess(List<GinasProcessingMessage> source, List<GinasProcessingMessage> destination) {
        delegate.addAndProcess(source, destination);
    }

    @Override
    public boolean handleMessages(Substance cs, List<GinasProcessingMessage> list) {
        return delegate.handleMessages(cs, list);
    }

    @Override
    public void addProblems(Substance cs, List<GinasProcessingMessage> list) {
        delegate.addProblems(cs, list);
    }

    @Override
    public void setIfValid(ValidationResponse resp, List<GinasProcessingMessage> messages) {
        //mark everything valid?
        resp.setValid(true);
    }
}
