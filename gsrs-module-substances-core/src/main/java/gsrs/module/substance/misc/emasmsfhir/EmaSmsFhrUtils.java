package gsrs.module.substance.misc.emasmsfhir;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ix.ginas.models.v1.Substance;

import java.util.Optional;

public class EmaSmsFhrUtils {


    public static String findCodeByCodeSystem (String codeSystem, Substance substance) {
        // Do we need to check if public?
        boolean publicOnly = false;
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


    private static final ObjectMapper mapper = new ObjectMapper();

    public static String gsrsSubstanceToQuotedJson (Substance substance){
        try {
            return mapper.writeValueAsString(substance);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
