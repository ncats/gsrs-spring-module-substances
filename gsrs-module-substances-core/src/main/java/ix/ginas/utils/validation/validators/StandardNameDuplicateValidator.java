package ix.ginas.utils.validation.validators;

import java.util.*;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.models.Keyword;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class StandardNameDuplicateValidator extends AbstractValidatorPlugin<Substance> {
    @Autowired
    private SubstanceRepository substanceRepository;

    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {

        // User adds/edits standard name in substance record that has a duplicate standard name within
        // the substance record and in the same language.
        // ==> warning

        // User adds/edits standard name in substance record that has a duplicate in another substance
        // record ==> error

        Map<String, Set<String>> stdNameSetByLanguage = new HashMap<>();

        objnew.names.forEach((Name name) -> {

            if (name.stdName != null) {
                // Include language default or trust that it is already done?
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
                while(iter.hasNext()){
                    String language = iter.next().getValue();
                    // System.out.println("language for " + name.stdName + "  = " + language);
                    Set<String> stdNames = stdNameSetByLanguage.computeIfAbsent(language, k->new HashSet<>());
                    if(!stdNames.add(uppercaseStdName)){
                        GinasProcessingMessage mes = GinasProcessingMessage
                                .WARNING_MESSAGE(
                                        "Standard Name '"
                                                + name.stdName
                                                + "' is a duplicate standard name in the record.")
                                .markPossibleDuplicate();
                        callback.addMessage(mes);
                    }
                }

                SubstanceRepository.SubstanceSummary s2 = checkStdNameForDuplicateInOtherRecords(objnew, name.stdName);
                if (s2 != null) {
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .ERROR_MESSAGE(
                                    "Name '"
                                            + name.stdName
                                            + "' collides (possible duplicate) with existing standard name for substance:")
                            .addLink(ValidationUtils.createSubstanceLink(s2.toSubstanceReference()));
                    callback.addMessage(mes);
                }
            }
        });
    }

    public SubstanceRepository.SubstanceSummary checkStdNameForDuplicateInOtherRecords(Substance s, String stdName) {
        try {
            List<SubstanceRepository.SubstanceSummary> sr = substanceRepository.findByNames_StdNameIgnoreCase(stdName);
            if (sr!=null && !sr.isEmpty()) {
                SubstanceRepository.SubstanceSummary s2 = null;
                Iterator<SubstanceRepository.SubstanceSummary> it = sr.iterator();
                while(it.hasNext()){
                    s2 = it.next();
                    if (!s2.getUuid().equals(s.getOrGenerateUUID())) {
                        return s2;
                    }
                }
            }
        } catch (Exception e) {
            // Should we be throwing an error?
            log.warn("Problem checking for duplicate standard name.", e);
        }
        return null;
    }
}
