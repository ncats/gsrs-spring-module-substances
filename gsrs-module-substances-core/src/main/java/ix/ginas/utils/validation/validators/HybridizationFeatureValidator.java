package ix.ginas.utils.validation.validators;

import ix.core.util.ModelUtils;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.*;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        if (!(objnew instanceof NucleicAcidSubstance)) {
            log.trace("substance is not a nucleic acid");
            return;
        }
        NucleicAcidSubstance nucleicAcidSubstance = (NucleicAcidSubstance) objnew;
        if (nucleicAcidSubstance.properties == null || nucleicAcidSubstance.properties.isEmpty()) {
            log.trace("has no properties");
            return;
        }
        nucleicAcidSubstance.properties.stream().filter(p -> COMPLEMENTARY_REGION_PROPERTY_NAME.equals(p.getName())).forEach(property -> {
            if(property.getValue() == null) {
                callback.addMessage(GinasProcessingMessage
                        .ERROR_MESSAGE(String.format("%s properties must have a value", COMPLEMENTARY_REGION_PROPERTY_NAME)));
                return;
            }
            if (property.getValue().nonNumericValue == null || property.getValue().nonNumericValue.length() == 0) {
                callback.addMessage(GinasProcessingMessage
                        .ERROR_MESSAGE(String.format("%s properties must have at least 2 regions", COMPLEMENTARY_REGION_PROPERTY_NAME)));
                return;
            }
            if (nucleicAcidSubstance.nucleicAcid == null || nucleicAcidSubstance.nucleicAcid.subunits == null || nucleicAcidSubstance.nucleicAcid.subunits.isEmpty()) {
                // the Nucleic Acid validator handles a check for the existence of subunits
                return;
            }
            String[] regions = property.getValue().nonNumericValue.split(";");
            if (regions.length < 2 || (regions.length % 2) != 0) {
                callback.addMessage(GinasProcessingMessage
                        .ERROR_MESSAGE(String.format("%s properties must have at least 2 regions", COMPLEMENTARY_REGION_PROPERTY_NAME)));
                return;
            }
            try {

                for (int i = 0; i < regions.length; i += 2) {
                    List<Site> sites1 = ModelUtils.parseShorthandRanges(regions[i]);
                    List<Site> sites2 = ModelUtils.parseShorthandRanges(regions[i + 1]);
                    if (sites1.size() == 0 || sites2.size() == 0) {
                        log.warn("Error making sites out of {}", regions[i]);
                        callback.addMessage(GinasProcessingMessage
                                .ERROR_MESSAGE(String.format("Error parsing %s and/or %s into sites", regions[i], regions[i + 1])));
                        return;
                    }
                    if (sites1.size() != sites2.size()) {
                        callback.addMessage(GinasProcessingMessage
                                .ERROR_MESSAGE(String.format("Error sites %s and %s have different counts", regions[i], regions[i + 1])));
                        return;
                    }
                    List<Character> bases1 = parseBases(sites1, nucleicAcidSubstance.nucleicAcid.subunits);
                    List<Character> bases2 = parseBases(sites2, nucleicAcidSubstance.nucleicAcid.subunits);

                    boolean complementary = true;
                    for (int p = 0; p < bases1.size(); p++) {
                        if (!areComplementary(bases1.get(p), bases2.get(p))) {
                            complementary = false;
                            break;
                        }
                    }
                    if (!complementary) {
                        StringBuilder sb1 = new StringBuilder(bases1.size());
                        for (Character c : bases1) {
                            sb1.append(c);
                        }
                        StringBuilder sb2 = new StringBuilder(bases1.size());
                        for (Character c : bases2) {
                            sb2.append(c);
                        }
                        callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(String.format("sequences '%s' and '%s' are not complementary",
                                sb1, sb2)));
                    }
                }
            } catch (Exception ex) {
                log.error("Error during nucleic acid hybridization validation ", ex);
                callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE("Error analyzing hybridization " + ex.getMessage()));
            }
        });
    }

    private boolean areComplementary(Character char1, Character char2) {
        if (char1 == null || char2 == null) {
            return false;
        }
        return (char1 != 'A' || (char2 == 'T' || char2 == 'U'))
                && (char1 != 'T' || char2 == 'A')
                && (char1 != 'U' || char2 == 'A')
                && (char1 != 'G' || char2 == 'C')
                && (char1 != 'C' || char2 == 'G');
    }

    private List<Character> parseBases(List<Site> sites, List<Subunit> subunits) {
        List<Character> bases = new ArrayList<>();
        Map<Integer, Subunit> subunitsByIndex = subunits.stream()
                .collect(Collectors.toMap(s -> s.subunitIndex, s -> s));

        try {
            for (int s = 0; s < sites.size(); s++) {
                //data in sites is 1-based for users; data within the subunits is 0-based for programmers
                int residueIndex = sites.get(s).residueIndex - 1;
                Subunit subunit = subunitsByIndex.get(sites.get(s).subunitIndex);
                if (subunit == null) {
                    throw new IllegalStateException("No subunit found for site " + s);
                }

                bases.add(subunit.sequence.toUpperCase().charAt(residueIndex));
            }
        } catch (Exception ex) {
            log.warn("Error parsing bases", ex);
            throw ex;
        }
        return bases;
    }
}
