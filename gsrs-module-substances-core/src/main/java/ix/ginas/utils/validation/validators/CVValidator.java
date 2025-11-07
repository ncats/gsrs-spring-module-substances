package ix.ginas.utils.validation.validators;

import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.ControlledVocabulary;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class CVValidator extends AbstractValidatorPlugin<ControlledVocabulary> {

    public void setAllowDuplicateValues(boolean allowDuplicateValuesParm) {
        this.allowDuplicateValues = allowDuplicateValuesParm;
    }

    private boolean allowDuplicateValues = true;

    @Override
    public void validate(ControlledVocabulary objnew, ControlledVocabulary objold, ValidatorCallback callback) {
        log.trace("allowDuplicateValues: {}", allowDuplicateValues);
        //look for duplicates
        Map<String, Long> occurrencesOfTerms= objnew.terms.stream()
                .map(t-> t.value)
                .collect(Collectors.groupingBy(s-> s.toUpperCase(), Collectors.counting()));
        Map<String, Long> occurrencesOfDisplays= objnew.terms.stream()
                .map(t-> t.display)
                .collect(Collectors.groupingBy(s-> s.toUpperCase(), Collectors.counting()));
        List<String> duplicateTerms = occurrencesOfTerms.entrySet().stream()
                .filter(en->en.getValue() >1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        List<String> duplicateDisplays = occurrencesOfDisplays.entrySet().stream()
                .filter(en->en.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if(!allowDuplicateValues && !duplicateTerms.isEmpty()) {
            callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
                    "These terms occur more than once: %s", String.join(", ", duplicateTerms)));
            callback.setInvalid();
            log.info("found duplicate vocabulary terms: {}", String.join(", ", duplicateTerms));
        }
        if( !duplicateDisplays.isEmpty()) {
            log.trace("we have some duplicate values");
            callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
                    "The display values occur more than once: %s", String.join(", ", duplicateDisplays)));
            callback.setInvalid();
            log.info("found duplicate vocabulary displays: {}", String.join(", ", duplicateDisplays));
        }
    }
}
