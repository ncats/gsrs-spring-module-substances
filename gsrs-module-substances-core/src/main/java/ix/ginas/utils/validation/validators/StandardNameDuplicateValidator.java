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

    // Please adjust test class if you change these message texts.
    private final String DUPLICATE_IN_SAME_RECORD_MESSAGE = "Standard Name '%s' is a duplicate standard name in the same record.";
    private final String DUPLICATE_IN_OTHER_RECORD_MESSAGE = "Standard Name '%s' collides (possible duplicate) with existssing standard name for other substance:";

    @Autowired
    private SubstanceRepository substanceRepository;


    private boolean checkDuplicateInOtherRecord = true;
    private boolean checkDuplicateInSameRecord = true;
    private boolean onDuplicateInOtherRecordShowError = true;
    private boolean onDuplicateInSameRecordShowError = false;

    // User adds/edits standard name in substance record that has a duplicate standard name within
    // the substance record and in the same language.
    // ==> warning

    // User adds/edits standard name in substance record that has a duplicate in another substance
    // record ==> error

    public void setSubstanceRepository(SubstanceRepository substanceRepository) {
        this.substanceRepository = substanceRepository;
    }

    public void setOnDuplicateInOtherRecordShowError(boolean onDuplicateInOtherRecordShowError) {
        this.onDuplicateInOtherRecordShowError = onDuplicateInOtherRecordShowError;
    }

    public void setOnDuplicateInSameRecordShowError(boolean onDuplicateInSameRecordShowError) {
        this.onDuplicateInSameRecordShowError = onDuplicateInSameRecordShowError;
    }

    public void setCheckDuplicateInOtherRecord(boolean checkDuplicateInOtherRecord) {
        this.checkDuplicateInOtherRecord = checkDuplicateInOtherRecord;
    }

    public void setCheckDuplicateInSameRecord(boolean checkDuplicateInSameRecord) {
        this.checkDuplicateInSameRecord = checkDuplicateInSameRecord;
    }

    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {

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
                if(checkDuplicateInSameRecord) {
                    Iterator<Keyword> iter = name.languages.iterator();
                    String uppercaseStdName = name.stdName.toUpperCase();
                    while (iter.hasNext()) {
                        String language = iter.next().getValue();
                        Set<String> stdNames = stdNameSetByLanguage.computeIfAbsent(language, k -> new HashSet<>());
                        if (!stdNames.add(uppercaseStdName)) {
                            if (onDuplicateInSameRecordShowError) {
                                GinasProcessingMessage mes = GinasProcessingMessage.ERROR_MESSAGE(String.format(DUPLICATE_IN_SAME_RECORD_MESSAGE, name.stdName));
                                mes.markPossibleDuplicate();
                                callback.addMessage(mes);
                            } else {
                                GinasProcessingMessage mes = GinasProcessingMessage.WARNING_MESSAGE(String.format(DUPLICATE_IN_SAME_RECORD_MESSAGE, name.stdName));
                                mes.markPossibleDuplicate();
                                callback.addMessage(mes);
                            }

                        }
                    }
                }
                if(checkDuplicateInOtherRecord) {
                    SubstanceRepository.SubstanceSummary s2 = checkStdNameForDuplicateInOtherRecords(objnew, name.stdName);
                    if (s2 != null) {
                        if (onDuplicateInOtherRecordShowError) {
                            GinasProcessingMessage mes = GinasProcessingMessage.ERROR_MESSAGE(String.format(DUPLICATE_IN_OTHER_RECORD_MESSAGE, name.stdName));
                            mes.addLink(ValidationUtils.createSubstanceLink(s2.toSubstanceReference()));
                            callback.addMessage(mes);
                        } else {
                            GinasProcessingMessage mes = GinasProcessingMessage.WARNING_MESSAGE(String.format(DUPLICATE_IN_OTHER_RECORD_MESSAGE, name.stdName));
                            mes.addLink(ValidationUtils.createSubstanceLink(s2.toSubstanceReference()));
                            callback.addMessage(mes);
                        }

                    }
                }
            } // if stdName not null

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
            System.out.println("Problem checking for duplicate standard name.");
            e.printStackTrace();
            log.warn("Problem checking for duplicate standard name.", e);
        }
        return null;
    }
}
