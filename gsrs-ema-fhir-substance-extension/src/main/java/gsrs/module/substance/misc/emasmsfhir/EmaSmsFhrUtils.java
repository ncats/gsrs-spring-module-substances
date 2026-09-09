package gsrs.module.substance.misc.emasmsfhir;

import ix.ginas.models.v1.Substance;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EmaSmsFhrUtils {


    public static String findCodeByCodeSystem (String codeSystem, Substance substance) {
        // Do we need to check if public?
        Optional<String> optionalCode = substance.getCodes()
                .stream()
                // .filter(cd -> !(publicOnly && !cd.isPublic()))
                .filter(cd -> codeSystem.equalsIgnoreCase(cd.codeSystem))
                .findFirst()
                .map(cd -> {
                    if ("PRIMARY".equals(cd.type)) {
                        return cd.code;
                    } else {
                        return cd.code + " [" + cd.type + "]";
                    }
                });
        return optionalCode.orElse("");
    }


    private static final JsonMapper mapper = JsonMapper.builderWithJackson2Defaults().build();

    public static String gsrsSubstanceToQuotedJson (Substance substance){
        try {
            return mapper.writeValueAsString(substance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, String> gsrsSubstanceClassToSubstanceType = new HashMap<>();
    static
    {
        gsrsSubstanceClassToSubstanceType.put("chemical", "chemical");
        gsrsSubstanceClassToSubstanceType.put("protein", "protein");
        gsrsSubstanceClassToSubstanceType.put("nucleicAcid", "nucleicAcid");
        gsrsSubstanceClassToSubstanceType.put("polymer", "polymer");
        gsrsSubstanceClassToSubstanceType.put("mixture", "mixture");
        gsrsSubstanceClassToSubstanceType.put("specifiedSubstanceG1", "specifiedSubstanceGroup1");
        gsrsSubstanceClassToSubstanceType.put("specifiedSubstanceG2", "SpecifiedSubstanceGroup2");
        gsrsSubstanceClassToSubstanceType.put("specifiedSubstanceG3", "SpecifiedSubstance Group3");
        gsrsSubstanceClassToSubstanceType.put("specifiedSubstanceG4", "SpecifiedSubstanceGroup4");

//        gsrsSubstanceClassToSubstanceType.put("concept", "Other");
//        gsrsSubstanceClassToSubstanceType.put("reference", "Other");
//        gsrsSubstanceClassToSubstanceType.put("structurallyDiverse", "Other");
    }

    public static String getEmaSmsSubstanceTypeFromGsrsSubstanceClass(String substanceClass) {
        String substanceType = gsrsSubstanceClassToSubstanceType.get(substanceClass);
        return (substanceType==null) ? "UNDEFINED" :  substanceType;
    }

}
