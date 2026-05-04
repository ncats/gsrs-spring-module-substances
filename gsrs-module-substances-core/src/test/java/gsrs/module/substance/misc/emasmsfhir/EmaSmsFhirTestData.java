package gsrs.module.substance.misc.emasmsfhir;

import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;

import java.util.HashMap;
import java.util.Map;

final class EmaSmsFhirTestData {

    private EmaSmsFhirTestData() {
    }

    static Substance chemicalSubstance() {
        return new SubstanceBuilder()
                .asChemical()
                .generateNewUUID()
                .build();
    }

    static Substance chemicalSubstanceWithDisplayName(String name) {
        return new SubstanceBuilder()
                .asChemical()
                .generateNewUUID()
                .addName(name, n -> {
                    n.displayName = true;
                    n.addLanguage("en");
                })
                .build();
    }

    static Code code(String codeSystem, String code, String type) {
        Code c = new Code(codeSystem, code);
        c.type = type;
        return c;
    }

    static Object configuration() {
        Object cfg;
        try {
            Class<?> cfgClass = Class.forName("gsrs.module.substance.misc.emasmsfhir.EmaSmsFhirConfiguration");
            cfg = cfgClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("EMA SMS FHIR extension classes are not available on the core test classpath", e);
        }

        Map<String, Map<String, String>> codeConfigs = new HashMap<>();
        codeConfigs.put("ecListNumber", codeConfig("EC-TID", "ECHA (EC/EINECS)", "https://sms/codes/ec"));
        codeConfigs.put("evCode", codeConfig("EV-TID", "EVMPD", "https://sms/codes/ev"));
        codeConfigs.put("innNumber", codeConfig("INN-TID", "INN", "https://sms/codes/inn"));
        codeConfigs.put("smsId", codeConfig("SMS-TID", "SMS ID", "https://sms/codes/sms"));
        codeConfigs.put("unii", codeConfig("UNII-TID", "FDA UNII", "https://sms/codes/unii"));
        invokeSetter(cfg, "setCodeConfigs", codeConfigs);

        Map<String, Map<String, String>> substanceTypeConfigs = new HashMap<>();
        Map<String, String> chemical = new HashMap<>();
        chemical.put("SMS Term ID", "CHEM-TID");
        chemical.put("SMS URL", "https://sms/types/chemical");
        substanceTypeConfigs.put("chemical", chemical);
        invokeSetter(cfg, "setSubstanceTypeConfigs", substanceTypeConfigs);

        Map<String, Map<String, String>> miscDefaults = new HashMap<>();
        Map<String, String> nameLanguageCoding = new HashMap<>();
        nameLanguageCoding.put("system", "urn:ietf:bcp:47");
        miscDefaults.put("name_language_coding", nameLanguageCoding);

        Map<String, String> nameStatusCoding = new HashMap<>();
        nameStatusCoding.put("code", "official");
        nameStatusCoding.put("system", "https://sms/name-status");
        nameStatusCoding.put("display", "Official");
        miscDefaults.put("name_status_coding", nameStatusCoding);

        invokeSetter(cfg, "setMiscDefaultConfigs", miscDefaults);
        return cfg;
    }

    private static void invokeSetter(Object target, String methodName, Object value) {
        try {
            target.getClass().getMethod(methodName, Map.class).invoke(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Could not call method " + methodName + " on " + target.getClass().getName(), e);
        }
    }

    static void setField(Object target, String fieldName, Object value) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                java.lang.reflect.Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalArgumentException("Could not find field: " + fieldName);
    }

    static Object getField(Object target, String fieldName) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                java.lang.reflect.Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalArgumentException("Could not find field: " + fieldName);
    }

    private static Map<String, String> codeConfig(String smsTermId, String gsrsCvTerm, String smsUrl) {
        Map<String, String> config = new HashMap<>();
        config.put("smsTermId", smsTermId);
        config.put("gsrsCvTerm", gsrsCvTerm);
        config.put("smsUrl", smsUrl);
        return config;
    }
}
