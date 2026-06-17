package ix.ginas.utils.validation.validators;

import ix.core.util.ModelUtils;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.NucleicAcidSubstance;
import ix.ginas.models.v1.Property;
import ix.ginas.models.v1.Site;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class HybridizationFeatureValidator extends AbstractValidatorPlugin<Substance> {

    public final static String COMPLEMENTARY_REGION_PROPERTY_NAME = "Complementary Region";

    /*
    1) Make sure the substance is a nucleic acid
    2) Make sure it has a property with the specified nAme and that the property has non-numeric value with the list of sites
    3) Make sure that the sites point to sequences of bases
    4) Make sure that bases are complementary -- non-complementary bases yield a warning.
     */
    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {
        if(! (objnew instanceof NucleicAcidSubstance)) {
            log.trace("substance is not a nucleic acid");
            return;
        }
        NucleicAcidSubstance nucleicAcidSubstance = (NucleicAcidSubstance) objnew;
        if(nucleicAcidSubstance.properties == null || nucleicAcidSubstance.properties.isEmpty()) {
            log.trace("has no properties");
            return;
        }
        nucleicAcidSubstance.properties.stream().filter(p->p.getName().equals(COMPLEMENTARY_REGION_PROPERTY_NAME)).forEach(property-> {
            if (property.getValue().nonNumericValue == null || property.getValue().nonNumericValue.length() == 0) {
                callback.addMessage(GinasProcessingMessage
                        .ERROR_MESSAGE(String.format("%s properties must have at least 2 regions", COMPLEMENTARY_REGION_PROPERTY_NAME)));
                return;
            }
            if(nucleicAcidSubstance.nucleicAcid.subunits == null || nucleicAcidSubstance.nucleicAcid.subunits.isEmpty()){
                // the Nucleic Acid validator handles a check for the existence of subunits
                return;
            }
            String[] regions = property.getValue().nonNumericValue.split(";");
            if (regions == null || regions.length < 2 || (regions.length % 2) != 0) {
                callback.addMessage(GinasProcessingMessage
                        .ERROR_MESSAGE(String.format("%s properties must have at least 2 regions", COMPLEMENTARY_REGION_PROPERTY_NAME)));
                return;
            }
            for(int i = 0; i<regions.length/2; i = 1+2) {
                String strandIndications = regions[i];
                List<Site> sites1 = ModelUtils.parseShorthandRanges(regions[i]);
                List<Site> sites2 = ModelUtils.parseShorthandRanges(regions[i+1]);
                if(sites1 == null || sites2 == null || sites1.size() == 0 || sites2.size() == 0) {
                    log.warn("Error making sites out of {}", regions[i]);
                    callback.addMessage(GinasProcessingMessage
                            .ERROR_MESSAGE(String.format("Error parsing %s and/or %s into sites", regions[i], regions[i+1])));
                    return;
                }
                if(sites1.size() != sites2.size()) {
                    callback.addMessage(GinasProcessingMessage
                            .ERROR_MESSAGE(String.format("Error sites %s and %s have different counts", regions[i], regions[i+1])));
                    return;
                }
                List<Character> chars1 = new ArrayList<>();
                for(int s = 0; s< sites1.size(); s++) {
                    chars1.add(nucleicAcidSubstance.nucleicAcid.subunits.get(sites1.get(s).subunitIndex).sequence.toUpperCase().charAt(sites1.get(s).residueIndex));
                }
                List<Character> chars2 = new ArrayList<>();
                for(int s = 0; s< sites2.size(); s++) {
                    chars2.add(nucleicAcidSubstance.nucleicAcid.subunits.get(sites2.get(s).subunitIndex).sequence.toUpperCase().charAt(sites2.get(s).residueIndex));
                }
                boolean complementary = true;
                for(int p=0; p< chars1.size(); p++) {
                    if(!AreComplementary(chars1.get(p), chars2.get(p))){
                        complementary = false;
                    }
                }
                if(!complementary){
                    StringBuilder sb1 = new StringBuilder(chars1.size());
                    for (Character c : chars1) {
                        sb1.append(c);
                    }
                    StringBuilder sb2 = new StringBuilder(chars1.size());
                    for (Character c : chars2) {
                        sb2.append(c);
                    }
                    callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(String.format("sequences '%s' and '%s' are not complementary",
                            sb1.toString(), sb2.toString())));
                }
            }
        });


    }

    private boolean AreComplementary(Character char1, Character char2) {
        if(char1 == null || char2 ==null) {
            return false;
        }
        if((char1 == 'A' && (char2 != 'T' && char2 != 'U'))
            || (char1 == 'T' && char2 != 'A')
            || (char1 == 'U' && char2 != 'A')
            || (char1 == 'G' && char2 != 'C')
            || (char1 == 'C' && char2 != 'g'))
        {
            return false;
        }
        return true;
    }
}
