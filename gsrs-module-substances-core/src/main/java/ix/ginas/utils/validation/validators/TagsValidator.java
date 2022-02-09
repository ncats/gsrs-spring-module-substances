package ix.ginas.utils.validation.validators;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.swing.text.html.HTML;
import java.util.*;
@Slf4j


public class TagsValidator extends AbstractValidatorPlugin<Substance> {
    /**
     * <h1>Tags Validator</h1>
     * @since   2022-02-07
     *
     * Keep tags in substance names consistent with explicit substance tags.
     **
     * The idea of substance tags appears in two ways in the GSRS.
     *   1) in the list of names.
     *   2) in a list of explicit Substance->Tag<Keyword> objects.
     * A tag is included in a substance name by putting a bracketed term at the end of the name.
     * For example:
     *   ASPIRIN [INN]
     * When this name is added, the GSRS will extract "INN" and index this value for faceting.
     * The facet category called "Source Tag" works with this indexed value.
     *
     * In the GSRS Frontend, explicit tag values can be added manually to this list.
     *
     * Thus, there are two sources of truth for substance tags.
     *
     * This validator may be used to keep the lists consistent.
     *
     * This should be configured in your src/main/resources/application.conf.
     *
     * Without this setting, the validator will not be run on submission of a substance.
     *
     *     gsrs.validators.substances += {
     *         "validatorClass" = "ix.ginas.utils.validation.validators.TagsValidator",
     *         "newObjClass" = "ix.ginas.models.v1.Substance",
     *         "addExplicitTagsExtractedFromNamesOnCreate": false,
     *         "addExplicitTagsExtractedFromNamesOnUpdate": false,
     *         "removeExplicitTagsMissingFromNamesOnCreate": false,
     *         "removeExplicitTagsMissingFromNamesOnUpdate": false
     *     }
     *
     * Tag consistency also comes into play when substances are copied in the frontend . "Copying"
     * takes the old JSON and puts it in a new record, with some clean up modifications.  Copying
     * keeps tags from the old record. So if you wipe all the names it won't also wipe all the tags.
     * But, if THIS validation rule is configured appropriately, it should deal with that case too.
     *
     * *** Note also that there is an idea of the locator.  This is used in conjunction with references.
     * Currently, this is OFF in GSRS (controlled by a boolean value extractLocators). If that were on,
     * a method in NamesValidator.java (addLocator) would add tags found in names to the tags list. We should
     * probably remove this piggybacking in case locators is ever turned back on.
     */


    // WARNING!!!! changing these messages may have an impact on tests.
    private static final String NAME_TAGS_WILL_BE_ADDED = "Tags WILL be added. The following tag terms were found in substance names %s but are not present in the tags list.";
    private static final String NAME_TAGS_WILL_NOT_BE_AUTOMATICALLY_ADDED = "These tag terms found in substance names are not present in substance tags: %s. They will not be automatically added to the tags list.";
    private static final String EXPLICIT_TAGS_WILL_BE_REMOVED = "Tags will be removed! The substance has these tags %s that are not present in substance names.";
    private static final String EXPLICIT_TAGS_WILL_NOT_BE_AUTOMATICALLY_REMOVED = "The substance has these tags %s that are not present in substance names. These tags will be kept.";

    private Boolean addExplicitTagsExtractedFromNamesOnCreate;
    private Boolean addExplicitTagsExtractedFromNamesOnUpdate;
    private Boolean removeExplicitTagsMissingFromNamesOnCreate;
    private Boolean removeExplicitTagsMissingFromNamesOnUpdate;

    @Autowired
    private SubstanceRepository substanceRepository;

    public void setAddExplicitTagsExtractedFromNamesOnCreate(Boolean b) {
        this.addExplicitTagsExtractedFromNamesOnCreate = b;
    }

    public void setAddExplicitTagsExtractedFromNamesOnUpdate(Boolean b) {
        this.addExplicitTagsExtractedFromNamesOnUpdate = b;
    }

    public void setRemoveExplicitTagsMissingFromNamesOnCreate(Boolean b) {
        this.removeExplicitTagsMissingFromNamesOnCreate = b;
    }

    public void setRemoveExplicitTagsMissingFromNamesOnUpdate(Boolean b) {
        this.removeExplicitTagsMissingFromNamesOnUpdate = b;
    }

    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
//        log.info("===== Inside Tags Validator.validate ====");
//        log.info("Inside Tags Validator.validate");
//        log.info("addExplicitTagsExtractedFromNamesOnCreate:" + addExplicitTagsExtractedFromNamesOnCreate);
//        log.info("addExplicitTagsExtractedFromNamesOnUpdate:" + addExplicitTagsExtractedFromNamesOnUpdate);
//        log.info("removeExplicitTagsMissingFromNamesOnCreate:" + removeExplicitTagsMissingFromNamesOnCreate);
//        log.info("removeExplicitTagsMissingFromNamesOnUpdate:" + removeExplicitTagsMissingFromNamesOnUpdate);

        boolean skipRest = false;

        Set<String> nameTagTerms = TagUtilities.extractBracketNameTags(s);
        Set<String> explicitTagTerms = TagUtilities.extractExplicitTags(s);

        if (addExplicitTagsExtractedFromNamesOnCreate == null) {
            this.setAddExplicitTagsExtractedFromNamesOnCreate(false);
            log.info("Tags Validator setting addExplicitTagsExtractedFromNames to false; was null.");
        }
        if (addExplicitTagsExtractedFromNamesOnUpdate == null) {
            this.setAddExplicitTagsExtractedFromNamesOnUpdate(false);
            log.info("Tags Validator setting addExplicitTagsExtractedFromNames to false; was null.");
        }
        if (removeExplicitTagsMissingFromNamesOnCreate == null) {
            this.setRemoveExplicitTagsMissingFromNamesOnCreate(false);
            log.info("Tags Validator setting removeExplicitTagsMissingFromNames to false; was null.");
        }
        if (removeExplicitTagsMissingFromNamesOnUpdate == null) {
            this.setRemoveExplicitTagsMissingFromNamesOnUpdate(false);
            log.info("Tags Validator setting removeExplicitTagsMissingFromNames to false; was null.");
        }

        Boolean shouldAddExplicitTagsExtractedFromNames = (s != null && addExplicitTagsExtractedFromNamesOnCreate)
                || (objold != null && addExplicitTagsExtractedFromNamesOnUpdate);

        Boolean shouldRemoveExplicitTagsMissingFromNames = (s != null && removeExplicitTagsMissingFromNamesOnCreate)
                || (objold != null && removeExplicitTagsMissingFromNamesOnUpdate);

        if (nameTagTerms == null) {
            log.info("Tags Validator nameTagTerms is null.");
            GinasProcessingMessage mes = GinasProcessingMessage
                .ERROR_MESSAGE("Null value encountered when extracting tag terms from substance names.");
            callback.addMessage(mes);
            skipRest = true;
        }
        if (explicitTagTerms == null) {
            log.info("Tags Validator explicitTagTerms is null.");
            GinasProcessingMessage mes = GinasProcessingMessage
                    .ERROR_MESSAGE("Null value encountered when getting substance tag terms.");
            callback.addMessage(mes);
            skipRest = true;
        }

        if(!skipRest) {
            Set<String> inNamesMissingFromExplicitTags = TagUtilities.getSetAExcludesB(
                    nameTagTerms,
                    explicitTagTerms
            );
            Set<String> inExplicitTagsMissingFromNames = TagUtilities.getSetAExcludesB(
                    explicitTagTerms,
                    nameTagTerms
            );

            // If there is a name with [TAGY], but no tag "TAGY", then throw warning but add TAGY to the list of tags.
            if (!inNamesMissingFromExplicitTags.isEmpty()) {
                if (shouldAddExplicitTagsExtractedFromNames) {
                    GinasProcessingMessage mes = GinasProcessingMessage
                            // Note changing this message may have an impact on tests.
                            .WARNING_MESSAGE(String.format(NAME_TAGS_WILL_BE_ADDED, TagUtilities.sortTagsHashSet(inNamesMissingFromExplicitTags).toString()))
                            .appliableChange(true);
                    callback.addMessage(mes, () -> {
                        for(String tagTerm: inNamesMissingFromExplicitTags) {
                            s.addTagString(tagTerm);
                        }
                    });
              } else {
                    GinasProcessingMessage mes = GinasProcessingMessage
                            // Note changing this message may have an impact on tests.
                            .WARNING_MESSAGE(String.format(NAME_TAGS_WILL_NOT_BE_AUTOMATICALLY_ADDED, TagUtilities.sortTagsHashSet(inNamesMissingFromExplicitTags).toString()));
                    callback.addMessage(mes);
                }

            }

            // If there is NO name with [TAGZ], but there is a tag "TAGZ", then throw warning and
            // remove from tags list if configured to do so.
            if (!inExplicitTagsMissingFromNames.isEmpty()) {
                if (shouldRemoveExplicitTagsMissingFromNames) {
                    log.info("Tags Validator WILL auto remove tags when not present in names.");
                    GinasProcessingMessage mes = GinasProcessingMessage
                            // Note changing this message may have an impact on tests.
                            .WARNING_MESSAGE(String.format(EXPLICIT_TAGS_WILL_BE_REMOVED, TagUtilities.sortTagsHashSet(inExplicitTagsMissingFromNames).toString()))
                            .appliableChange(true);
                    callback.addMessage(mes, () -> {
                        for(String tagTerm: inExplicitTagsMissingFromNames) {
                            s.removeTagString(tagTerm);
                        }
                    });
                } else {
                    log.info("Tags Validator WILL NOT auto remove tags when present in names.");
                    GinasProcessingMessage mes = GinasProcessingMessage
                            // Note changing this message may have an impact on tests.
                            .WARNING_MESSAGE(String.format(EXPLICIT_TAGS_WILL_NOT_BE_AUTOMATICALLY_REMOVED, TagUtilities.sortTagsHashSet(inExplicitTagsMissingFromNames).toString()));
                    callback.addMessage(mes);
                }
            }
        }
    }
}
