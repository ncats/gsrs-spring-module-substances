package gsrs.module.substance.misc.emasmsfhir;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ix.core.models.Keyword;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.StringType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EmaSmsSimpleRecordFhirMapper {
    private static String DEFAULT_NAME_SOURCE = "FDA SUBSTANCE REGISTRATION SYSTEM";
    private static String DEFAULT_LANGUAGE = "en";
    private static String SodiumChlorideUuid = "306d24b9-a6b8-4091-8024-02f9ec24b705";
    // http://localhost:8080/api/v1/substances/306d24b9-a6b8-4091-8024-02f9ec24b705/@emaSmsRecord

    private static String sodiumGlutonateUuid = "90e9191d-1a81-4a53-b7ee-560bf9e68109";
    // http://localhost:8080/api/v1/substances/90e9191d-1a81-4a53-b7ee-560bf9e68109/@emaSmsRecord

    private static Map<String,String> FIELD_MAP;
    static {
        FIELD_MAP = new HashMap<>();
        FIELD_MAP.put("smsId", "SMS_ID");
        FIELD_MAP.put("substanceName", "Substance_Name");
        FIELD_MAP.put("isPreferredName", "Is_Preferred_Name");
        FIELD_MAP.put("language", "Language");
        FIELD_MAP.put("substanceType", "Substance_Type");
        FIELD_MAP.put("evCode", "EV_Code");
        FIELD_MAP.put("unii", "UNII");
        FIELD_MAP.put("innNumber", "INN_Number");
        FIELD_MAP.put("ecListNumber", "EC_List/Number");
    }

    ObjectMapper mapper = new ObjectMapper();

    public EmaSmsSimpleRecord generateEmaSmsSimpleRecordFromSubstance(Substance substance) {
        EmaSmsSimpleRecord emaSmsSimpleRecord = new EmaSmsSimpleRecord();
        Optional<Name> optionalDisplayName = substance.getDisplayName();
        Name displayName = null;
        if(optionalDisplayName.isPresent()) {
            displayName = optionalDisplayName.get();
        }
        if (displayName!=null) {
            emaSmsSimpleRecord.setSubstanceName(new StringType(displayName.getName()));
            emaSmsSimpleRecord.setIsPreferredName(new BooleanType(true));
            // language in root of Fhir record seems to be reserved.
            emaSmsSimpleRecord.setLanguage2(new StringType(displayName.languages.stream().findFirst().orElse(new Keyword()).getValue()));
            emaSmsSimpleRecord.setUnii(new StringType((substance.getApprovalID())));
            emaSmsSimpleRecord.setNameSource(new StringType(DEFAULT_NAME_SOURCE));
        }
        emaSmsSimpleRecord.setSubstanceType(new StringType(substance.substanceClass.name()));
        emaSmsSimpleRecord.setEvCode(new StringType(findCodeByCodeSystem("EVMPD", substance)));
        // Maybe this should be used to make code portable; other agency might have non-unii approval id.
        emaSmsSimpleRecord.setUnii(new StringType(findCodeByCodeSystem(" FDA UNII", substance)));
        emaSmsSimpleRecord.setInnNumber(new StringType(findCodeByCodeSystem("INN", substance)));
        emaSmsSimpleRecord.setEcListNumber((new StringType(findCodeByCodeSystem("ECHA (EC/EINECS)", substance))));
        emaSmsSimpleRecord.setGsrsSubstance(new StringType(gsrsSubstanceToQuotedJson(substance)));
        return emaSmsSimpleRecord;
    }

    private String findCodeByCodeSystem(String codeSystem, Substance substance) {
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

    public String gsrsSubstanceToQuotedJson(Substance substance) {
        mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(substance);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

