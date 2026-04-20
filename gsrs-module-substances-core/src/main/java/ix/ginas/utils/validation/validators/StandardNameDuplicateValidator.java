package ix.ginas.utils.validation.validators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.models.Keyword;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class StandardNameDuplicateValidator extends AbstractValidatorPlugin<Substance> {

    // Please adjust test class if you change these message texts.
    private final String DUPLICATE_IN_SAME_RECORD_MESSAGE = "Standard Name '%s' is a duplicate standard name in the same record.";
    private final String DUPLICATE_IN_OTHER_RECORD_MESSAGE = "Standard Name '%s' collides (possible duplicate) with existing standard name for other substance:";

    private final String DUPLICATE_IN_SAME_RECORD_MESSAGE_TEST_FRAGMENT = "is a duplicate standard name in the same record.";
    private final String DUPLICATE_IN_OTHER_RECORD_MESSAGE_TEST_FRAGMENT = "collides (possible duplicate) with existing standard name for other substance:";

    @Autowired
    private SubstanceRepository substanceRepository;

    // User adds/edits standard name in substance record that has a duplicate standard name within
    // the substance record and in the same language.
    // ==> warning (default)

    // User adds/edits standard name in substance record that has a duplicate in another substance
    // record ==> error (default)

    public String getDUPLICATE_IN_OTHER_RECORD_MESSAGE_TEST_FRAGMENT() {
        return DUPLICATE_IN_OTHER_RECORD_MESSAGE_TEST_FRAGMENT;
    }
    public String getDUPLICATE_IN_SAME_RECORD_MESSAGE_TEST_FRAGMENT() {
        return DUPLICATE_IN_SAME_RECORD_MESSAGE_TEST_FRAGMENT;
    }

    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {

        Map<String, Set<String>> stdNameSetByLanguage = new HashMap<>();

        objnew.names.forEach((Name name) -> {

            if (name.stdName != null) {
                // Include language default or trust that it is already done?
                // Make a method for this in NameUtilities for more DRY?
                if (name.languages == null || name.languages.isEmpty()) {
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .WARNING_MESSAGE(
                                    "Must specify a language for each name. Defaults to \"English\"")
                            .appliableChange(true);
                    callback.addMessage(mes, () -> {
                        if (name.languages == null) {
                            name.languages = new EmbeddedKeywordList();
                        }
                        name.languages.add(new Keyword("en"));
                    });
                }

                // Check for duplicates in the record.
                Iterator<Keyword> iter = name.languages.iterator();
                String uppercaseStdName = name.stdName.toUpperCase();
                while (iter.hasNext()) {
                    String language = iter.next().getValue();
                    Set<String> stdNames = stdNameSetByLanguage.computeIfAbsent(language, k -> new HashSet<>());
                    if (!stdNames.add(uppercaseStdName)) {
                        GinasProcessingMessage mes = GinasProcessingMessage.ERROR_MESSAGE(DUPLICATE_IN_SAME_RECORD_MESSAGE, name.stdName);
                        mes.markPossibleDuplicate();
                        callback.addMessage(mes);
                    }
                }

                List<SubstanceRepository.SubstanceSummary> sr = substanceRepository.findByNames_StdNameIgnoreCase(name.stdName);
                if (sr != null && !sr.isEmpty()) {
                    SubstanceRepository.SubstanceSummary s2 = sr.iterator().next();
                    if (s2.getUuid() != null && !s2.getUuid().equals(objnew.getOrGenerateUUID())) {
                        GinasProcessingMessage mes = GinasProcessingMessage
                                .ERROR_MESSAGE(DUPLICATE_IN_OTHER_RECORD_MESSAGE, name.stdName)
                                .addLink(ValidationUtils.createSubstanceLink(s2.toSubstanceReference()));
                        callback.addMessage(mes);
                    }
                }
            } // if stdName not null

        });
    }
}
