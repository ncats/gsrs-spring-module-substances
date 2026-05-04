package gsrs.module.substance.misc.emasmsfhir;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ix.core.models.Keyword;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.StringType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
@Component
public class EmaSmsSimpleRecordFhirMapper {
    private static String DEFAULT_NAME_SOURCE = "FDA SUBSTANCE REGISTRATION SYSTEM";
    private static String DEFAULT_LANGUAGE = "en";
    private static String sodiumGlutonateUuid = "90e9191d-1a81-4a53-b7ee-560bf9e68109";

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
        emaSmsSimpleRecord.setEvCode(new StringType(EmaSmsFhrUtils.findCodeByCodeSystem("EVMPD", substance)));
        // Maybe this should be used to make code portable; other agency might have non-unii approval id.
        emaSmsSimpleRecord.setUnii(new StringType(EmaSmsFhrUtils.findCodeByCodeSystem("FDA UNII", substance)));
        emaSmsSimpleRecord.setInnNumber(new StringType(EmaSmsFhrUtils.findCodeByCodeSystem("INN", substance)));
        emaSmsSimpleRecord.setEcListNumber((new StringType(EmaSmsFhrUtils.findCodeByCodeSystem("ECHA (EC/EINECS)", substance))));
        emaSmsSimpleRecord.setGsrsSubstance(new StringType(EmaSmsFhrUtils.gsrsSubstanceToQuotedJson(substance)));
        return emaSmsSimpleRecord;
    }
}

