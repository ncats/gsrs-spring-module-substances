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

/*
=== Tags Validator Documentation ===

The idea of substance tags appears in two ways in the GSRS. 1) in the list of names. 2) in the list of tags.

A tag is included in a substance name by putting a bracketed term at the end of the name.
For example:
  ASPIRIN [INN]

When this name is added, the GSRS will extract "INN" and index this value for faceting.
The facet category called "Source Tag" works with this indexed value.
There is ALSO a substance->tags list of Keyword objects.
In the GSRS Frontend, tag values can be added manually to this list.
Thus, there are two sources of truth for substance tags.
This validator may be used to keep the lists consistent.

This should be configured in your src/main/resources/application.conf.

  # Without this setting, the validator will not be run on submission of a substance.

    gsrs.validators.substances += {
        "validatorClass" = "ix.ginas.utils.validation.validators.TagsValidator",
        "newObjClass" = "ix.ginas.models.v1.Substance",
        "addExplicitTagsExtractedFromNames":false,
        "removeExplicitTagsMissingFromNames":false
    }

Tag consistency also comes into play when substances are copied. "Copying" takes the old
JSON and puts it in a new record, with some clean up modifications.  Copying keeps tags from
the old record. So if you wipe all the names it won't also wipe all the tags. But, if the
validation rules is configured appropriately, it should deal with that case too.

*** Note also that there is an idea of the locator.  This is used in conjunction with references.
Currently, this is OFF in GSRS (controlled by a boolean value extractLocators). If that were on,
a method in NamesValidator.java (addLocator) would add tags found in names to the tags list. We should
probably remove this piggybacking in case locators is ever turned back on.
*/

public class TagsValidator extends AbstractValidatorPlugin<Substance> {

    private Boolean addExplicitTagsExtractedFromNames;
    private Boolean removeExplicitTagsMissingFromNames;

    @Autowired
    private SubstanceRepository substanceRepository;

    public void setAddExplicitTagsExtractedFromNames(Boolean b) {
        this.addExplicitTagsExtractedFromNames = b;
    }

    public void setRemoveExplicitTagsMissingFromNames(Boolean b) {
        this.removeExplicitTagsMissingFromNames = b;
    }

    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
        // log.info("===== Inside Tags Validator.validate ====");
        // log.info("Inside Tags Validator.validate");
        // log.info("addExplicitTagsExtractedFromNames:" + addExplicitTagsExtractedFromNames);
        // log.info("removeExplicitTagsMissingFromNames:" + removeExplicitTagsMissingFromNames);

        boolean skipRest = false;

        Set<String> nameTagTerms = TagUtilities.extractBracketNameTags(s);
        Set<String> explicitTagTerms = TagUtilities.extractExplicitTags(s);

        if (removeExplicitTagsMissingFromNames == null) {
            this.setRemoveExplicitTagsMissingFromNames(false);
            log.info("Tags Validator setting removeExplicitTagsMissingFromNames to false; was null.");
        }

        if (addExplicitTagsExtractedFromNames == null) {
            this.setAddExplicitTagsExtractedFromNames(false);
            log.info("Tags Validator setting addExplicitTagsExtractedFromNames to false; was null.");
        }

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
                if (addExplicitTagsExtractedFromNames) {
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .WARNING_MESSAGE("Tags WILL be added. The following tag terms were found in substance names " + TagUtilities.sortTagsHashSet(inNamesMissingFromExplicitTags).toString() + " but are not present in the tags list.")
                            .appliableChange(true);
                    callback.addMessage(mes, () -> {
                        for(String tagTerm: inNamesMissingFromExplicitTags) {
                            s.addTagString(tagTerm);
                        }
                    });
              } else {
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .WARNING_MESSAGE("These tag terms found in substance names are not present in substance tags: " + TagUtilities.sortTagsHashSet(inNamesMissingFromExplicitTags).toString() + ".");
                    callback.addMessage(mes);
                }

            }

            // If there is NO name with [TAGZ], but there is a tag "TAGZ", then throw warning and
            // remove from tags list if configured to do so.
            if (!inExplicitTagsMissingFromNames.isEmpty()) {
                if (removeExplicitTagsMissingFromNames) {
                    log.info("Tags Validator WILL auto remove tags when not present in names.");
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .WARNING_MESSAGE("Tags will be removed! The substance has these tags " + TagUtilities.sortTagsHashSet(inExplicitTagsMissingFromNames).toString() + " that are not present in substance names.")
                            .appliableChange(true);
                    callback.addMessage(mes, () -> {
                        for(String tagTerm: inExplicitTagsMissingFromNames) {
                            s.removeTagString(tagTerm);
                        }
                    });
                } else {
                    log.info("Tags Validator WILL NOT auto remove tags when present in names.");
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .WARNING_MESSAGE("The substance has these tags " + TagUtilities.sortTagsHashSet(inExplicitTagsMissingFromNames).toString() + " that are not present in substance names. These tags will be kept.");
                    callback.addMessage(mes);
                }
            }
        }
    }
}
