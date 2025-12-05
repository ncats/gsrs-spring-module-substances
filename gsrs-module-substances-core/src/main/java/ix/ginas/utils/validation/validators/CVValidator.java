package ix.ginas.utils.validation.validators;

import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.ControlledVocabulary;
import ix.ginas.models.v1.VocabularyTerm;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
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
        List<String> displaysWithBlanks = new ArrayList<>();
        List<String> valuesWithBlanks = new ArrayList<>();
        for(VocabularyTerm term : objnew.terms) {
            if (term.display != null && term.display.length() != term.display.trim().length()) {
                log.trace("found display with space: {}", term.display);
                displaysWithBlanks.add(term.display);
                term.display = term.display.trim();
            }
            if (term.value != null && term.value.length() != term.value.trim().length()) {
                log.trace("found value with space: {}", term.value);
                valuesWithBlanks.add(term.value);
                term.value = term.value.trim();
            }
        }
        if( !displaysWithBlanks.isEmpty()) {
            GinasProcessingMessage displaysMessage =
                    GinasProcessingMessage.WARNING_MESSAGE("One or more vocabulary term displays have leading or trailing spaces. These spaces were removed")
                    .appliableChange(true);
            callback.addMessage(displaysMessage);
            log.trace("added warning about display value(s) with blanks");
        }
        if( !valuesWithBlanks.isEmpty()){
            GinasProcessingMessage valuesMessage =
                    GinasProcessingMessage.WARNING_MESSAGE("One or more vocabulary term values have leading or trailing spaces. These spaces were removed")
                    .appliableChange(true);
            callback.addMessage(valuesMessage);
            log.trace("added warning about value(s) with blanks");
        }
    }
}
