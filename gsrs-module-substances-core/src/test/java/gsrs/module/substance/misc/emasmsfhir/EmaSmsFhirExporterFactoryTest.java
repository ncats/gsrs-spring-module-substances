package gsrs.module.substance.misc.emasmsfhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ix.ginas.exporters.DefaultParameters;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EmaSmsFhirExporterFactoryTest {

    private static final String FACTORY_CLASS = "gsrs.module.substance.misc.emasmsfhir.EmaSmsFhirExporterFactory";
    private static final String MAPPER_CLASS = "gsrs.module.substance.misc.emasmsfhir.EmaSmsSubstanceDefinitionFhirMapper";

    private static final String PRIMARY_CODE_SYSTEM_PARAMETERS = "PRIMARY_CODE_SYSTEM_PARAMETERS";
    private static final String APPROVAL_ID_NAME_PARAMETERS = "APPROVAL_ID_NAME_PARAMETERS";
    private static final String DEFAULT_APPROVAL_ID_NAME = "DEFAULT_APPROVAL_ID_NAME";

    @Test
    void supportsOnlyEmaSmsFhirFormat() {
        Object factory = newFactory();
        OutputFormat supported = supportedFormat(factory);

        assertTrue((Boolean) invoke(factory, "supports",
                new DefaultParameters(supported, false, JsonNodeFactory.instance.objectNode())));
        assertFalse((Boolean) invoke(factory, "supports",
                new DefaultParameters(new OutputFormat("other.txt", "Other"), false,
                        JsonNodeFactory.instance.objectNode())));
    }

    @Test
    void schemaIncludesApprovalIdAndOptionalPrimaryCodeSystemParameter() {
        Object factory = newFactory();
        String approvalIdParamName = staticField(FACTORY_CLASS, APPROVAL_ID_NAME_PARAMETERS);
        String primaryCodeParamName = staticField(FACTORY_CLASS, PRIMARY_CODE_SYSTEM_PARAMETERS);

        JsonNode schemaWithoutPrimaryCode = (JsonNode) invoke(factory, "getSchema");
        JsonNode parametersWithoutPrimaryCode = schemaWithoutPrimaryCode.get("properties");
        assertTrue(parametersWithoutPrimaryCode.has(approvalIdParamName));
        assertFalse(parametersWithoutPrimaryCode.has(primaryCodeParamName));

        invoke(factory, "setPrimaryCodeSystem", "BDNUM");
        JsonNode schemaWithPrimaryCode = (JsonNode) invoke(factory, "getSchema");
        JsonNode parametersWithPrimaryCode = schemaWithPrimaryCode.get("properties");
        assertTrue(parametersWithPrimaryCode.has(primaryCodeParamName));
        assertEquals("BDNUM", invoke(factory, "getPrimaryCodeSystem"));
    }

    @Test
    void createNewExporterWritesSubstanceDefinitionJson() throws IOException {
        Object factory = configuredFactory();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectNode details = JsonNodeFactory.instance.objectNode();
        details.put(staticField(FACTORY_CLASS, PRIMARY_CODE_SYSTEM_PARAMETERS), true);
        details.put(staticField(FACTORY_CLASS, APPROVAL_ID_NAME_PARAMETERS), "Custom Approval");
        OutputFormat supported = supportedFormat(factory);

        Exporter<Substance> exporter = (Exporter<Substance>) invoke(factory, "createNewExporter", outputStream,
                new DefaultParameters(supported, false, details));

        exporter.export(substanceWithDisplayNameAndCodes());
        exporter.close();

        String output = outputStream.toString(StandardCharsets.UTF_8.name());
        assertTrue(output.contains("\"resourceType\":\"SubstanceDefinition\""));
        assertTrue(output.contains("Sodium Chloride"));
        assertTrue(output.endsWith(System.lineSeparator()));
    }

    @Test
    void exporterAcceptsNullDetailedParameters() throws IOException {
        Object factory = configuredFactory();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputFormat supported = supportedFormat(factory);

        Exporter<Substance> exporter = (Exporter<Substance>) invoke(factory, "createNewExporter", outputStream,
                new DefaultParameters(supported, false, null));

        exporter.export(substanceWithDisplayNameAndCodes());
        exporter.close();

        assertFalse(outputStream.toString(StandardCharsets.UTF_8.name()).isEmpty());
    }

    @Test
    void exporterFallsBackToDefaultsWhenDetailedParametersAreBlankOrFalse() throws IOException {
        Object factory = configuredFactory();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectNode details = JsonNodeFactory.instance.objectNode();
        details.put(staticField(FACTORY_CLASS, PRIMARY_CODE_SYSTEM_PARAMETERS), false);
        details.put(staticField(FACTORY_CLASS, APPROVAL_ID_NAME_PARAMETERS), "  ");
        OutputFormat supported = supportedFormat(factory);

        Exporter<Substance> exporter = (Exporter<Substance>) invoke(factory, "createNewExporter", outputStream,
                new DefaultParameters(supported, false, details));

        assertFalse((Boolean) EmaSmsFhirTestData.getField(exporter, "omitPrimaryCodeSystem"));
        assertEquals(staticField(FACTORY_CLASS, DEFAULT_APPROVAL_ID_NAME),
                EmaSmsFhirTestData.getField(exporter, "chosenApprovalIdName"));
        exporter.close();
    }

    private static Object configuredFactory() {
        Object cfg = EmaSmsFhirTestData.configuration();
        Object mapper = newInstance(MAPPER_CLASS);
        invokeNamed(mapper, "setEmaSmsFhirConfiguration", cfg);

        Object factory = newFactory();
        invoke(factory, "setPrimaryCodeSystem", "BDNUM");
        EmaSmsFhirTestData.setField(factory, "emaSmsFhirConfiguration", cfg);
        EmaSmsFhirTestData.setField(factory, "emaSmsSubstanceDefinitionFhirMapper", mapper);
        return factory;
    }

    private static Object newFactory() {
        // These tests belong to the EMA extension; skip cleanly when extension classes are absent from core module.
        Assumptions.assumeTrue(isClassPresent(FACTORY_CLASS),
                "EMA SMS FHIR extension classes are not present on core test classpath");
        return newInstance(FACTORY_CLASS);
    }

    private static OutputFormat supportedFormat(Object factory) {
        Set<OutputFormat> formats = (Set<OutputFormat>) invoke(factory, "getSupportedFormats");
        return formats.iterator().next();
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static Object newInstance(String className) {
        try {
            return Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not instantiate " + className, e);
        }
    }

    private static String staticField(String className, String fieldName) {
        try {
            Class<?> cls = Class.forName(className);
            return (String) cls.getField(fieldName).get(null);
        } catch (Exception e) {
            throw new RuntimeException("Could not read field " + className + "." + fieldName, e);
        }
    }

    private static Object invoke(Object target, String methodName, Object... args) {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean assignable = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (args[i] != null && !parameterTypes[i].isAssignableFrom(args[i].getClass())) {
                    assignable = false;
                    break;
                }
            }
            if (assignable) {
                try {
                    return method.invoke(target, args);
                } catch (Exception e) {
                    throw new RuntimeException("Could not invoke method " + methodName + " on " + target.getClass().getName(), e);
                }
            }
        }
        throw new RuntimeException("Could not find compatible method " + methodName + " on " + target.getClass().getName());
    }

    private static Object invokeNamed(Object target, String methodName, Object arg) {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                try {
                    return method.invoke(target, arg);
                } catch (Exception e) {
                    throw new RuntimeException("Could not invoke method " + methodName + " on " + target.getClass().getName(), e);
                }
            }
        }
        throw new RuntimeException("Could not find method " + methodName + " on " + target.getClass().getName());
    }

    private static Substance substanceWithDisplayNameAndCodes() {
        Substance substance = EmaSmsFhirTestData.chemicalSubstanceWithDisplayName("Sodium Chloride");
        substance.codes.add(EmaSmsFhirTestData.code("FDA UNII", "451W47IQ8X", "PRIMARY"));
        return substance;
    }
}
