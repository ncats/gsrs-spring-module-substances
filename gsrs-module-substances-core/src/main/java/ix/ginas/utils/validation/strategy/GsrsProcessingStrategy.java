package ix.ginas.utils.validation.strategy;

import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.models.v1.Substance;

import java.util.List;
import java.util.function.Predicate;

public interface GsrsProcessingStrategy extends Predicate<GinasProcessingMessage> {

    void processMessage(GinasProcessingMessage gpm);

    boolean test(GinasProcessingMessage gpm);
    void addAndProcess(List<GinasProcessingMessage> source, List<GinasProcessingMessage> destination);

    void setIfValid(ValidationResponse validationResponse, List<GinasProcessingMessage> messages);

    boolean handleMessages(Substance cs, List<GinasProcessingMessage> list) ;

    void addProblems(Substance cs, List<GinasProcessingMessage> list);
}
