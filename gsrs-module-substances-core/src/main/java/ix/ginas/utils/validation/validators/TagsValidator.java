package ix.ginas.utils.validation.validators;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
@Slf4j
public class TagsValidator extends AbstractValidatorPlugin<Substance> {

    @Value("#{new Boolean('${gsrs.substance.addtagwhenintagspresentinnames:false}')}")
    private Boolean addTagWhenInTagsPresentInNames;

    @Value("#{new Boolean('${gsrs.substance.removetagwhenintagsmissingfromnames:false}')}")
    private Boolean removeTagWhenInTagsMissingFromNames;

    @Autowired
    private SubstanceRepository substanceRepository;

    // Can't figure out how to set property in test; so doing this.
    public void setAddTagWhenInTagsPresentInNames(Boolean b) {
        this.addTagWhenInTagsPresentInNames = b;
    }

    // Can't figure out how to set property in test; so doing this.
    public void setRemoveTagWhenInTagsMissingFromNames(Boolean b) {
        this.removeTagWhenInTagsMissingFromNames = b;
    }

    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
        log.info("Inside Tags Validator.validate");

        boolean skipRest = false;
        if (removeTagWhenInTagsMissingFromNames == null) { removeTagWhenInTagsMissingFromNames = false; }

        List<String> tagTermsFromNames = s.extractDistinctTagTermsFromNames();
        List<String> tagTerms = s.grabTagTerms();

        if (tagTermsFromNames == null) {
            log.info("tagTermsFromNames is null.");
            GinasProcessingMessage mes = GinasProcessingMessage
                .ERROR_MESSAGE("Null value encountered when extracting tag terms from substance names.");
            callback.addMessage(mes);
            skipRest = true;
        }
        if (tagTerms == null) {
            log.info("tagTerms is null.");
            GinasProcessingMessage mes = GinasProcessingMessage
                    .ERROR_MESSAGE("Null value encountered when getting substance tag terms.");
            callback.addMessage(mes);
            skipRest = true;
        }

        if(!skipRest) {
            log.info("Checking for differences between tag terms in names and tags.");
            List<String> inNamesMissingFromTags = s.compareTagTermsInNamesMissingFromTags(
                    tagTermsFromNames,
                    tagTerms
            );
            List<String> inTagsMissingFromNames = s.compareTagTermsInTagsMissingFromNames(
                    tagTerms,
                    tagTermsFromNames
            );

            // If there is a name with [TAGY], but no tag "TAGY", then throw warning but add TAGY to the list of tags.
            // Currently, another validator process in NamesValidator calls Substance.addLocator. This adds the tag.
            // Actually does not seem to get added by that process ... so including this for now, discuss with Tyler/Larry
            if (!inNamesMissingFromTags.isEmpty()) {
                if (addTagWhenInTagsPresentInNames == false) {
                    log.info("Processing inNamesMissingFromTags.");
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .WARNING_MESSAGE("These tag terms found in substance names are not present in substance tags:" + inNamesMissingFromTags.toString() + ".");
                    callback.addMessage(mes);
                } else if(addTagWhenInTagsPresentInNames)  {
                    System.out.println("====>  addTagWhenInTagsPresentInNames is true");
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .WARNING_MESSAGE("Tags WILL be added. The following tag terms were found in substance names" + inNamesMissingFromTags.toString() + " but are not present in the tags list.");
                    callback.addMessage(mes, () -> {
                        System.out.println("====>  before for loop");
                        for(String tagTerm: inNamesMissingFromTags) {
                            System.out.println("====>  inside for loop " + tagTerm);
                            s.addTagString(tagTerm);
                        }
                    });

                }

            }

            // If there is NO name with [TAGZ], but there is a tag "TAGZ", then throw warning and
            // remove from tags list if configured to do so.
            if (!inTagsMissingFromNames.isEmpty()) {
                log.info("inTagsMissingFromNames is not empty.");
                if (removeTagWhenInTagsMissingFromNames == false) {
                    log.info("Will NOT auto remove tags when present in names.");
                    GinasProcessingMessage mes = GinasProcessingMessage
                        .WARNING_MESSAGE("The substance has these tags " + inTagsMissingFromNames.toString() + " that are not present in substance names. These tags will be kept.")
                        .appliableChange(true);
                    callback.addMessage(mes);
                } else if(removeTagWhenInTagsMissingFromNames)  {
                    log.info("WILL auto remove tags when not present in names.");
                    GinasProcessingMessage mes = GinasProcessingMessage
                        .WARNING_MESSAGE("Tags will be removed! The substance has these tags " + inTagsMissingFromNames.toString() + " that are not present in substance names.")
                        .appliableChange(true);
                    callback.addMessage(mes, () -> {
                        log.info("Inside validator callback");
                        for(String tagTerm: inTagsMissingFromNames) {
                            log.info("Inside validator callback for-loop");
                            s.removeTagString(tagTerm);
                        }
                    });
                }
            }
        }
    }
}
