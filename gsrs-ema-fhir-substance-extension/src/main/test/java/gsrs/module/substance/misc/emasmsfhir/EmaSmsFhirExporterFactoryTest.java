package gsrs.module.substance.misc.emasmsfhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ix.ginas.exporters.DefaultParameters;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Substance;
import org.hl7.fhir.r5.model.SubstanceDefinition;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmaSmsFhirExporterFactoryTest {

    @Test
    void supportsAndSupportedFormatsBehaveAsExpected() {
        EmaSmsFhirExporterFactory factory = new EmaSmsFhirExporterFactory();
        OutputFormat supported = factory.getSupportedFormats().iterator().next();

        assertTrue(factory.supports(params(supported, JsonNodeFactory.instance.objectNode())));
        assertFalse(factory.supports(params(new OutputFormat("other.txt", "other"), JsonNodeFactory.instance.objectNode())));

        Set<OutputFormat> formats = factory.getSupportedFormats();
        assertEquals(1, formats.size());
        assertTrue(formats.contains(supported));
    }

    @Test
    void schemaIncludesApprovalIdAndOptionalPrimaryCodeSystemNode() {
        EmaSmsFhirExporterFactory factory = new EmaSmsFhirExporterFactory();

        JsonNode schemaWithoutPrimary = factory.getSchema();
        String noPrimary = schemaWithoutPrimary.toString();
        assertTrue(noPrimary.contains(EmaSmsFhirExporterFactory.APPROVAL_ID_NAME_PARAMETERS));
        assertFalse(noPrimary.contains(EmaSmsFhirExporterFactory.PRIMARY_CODE_SYSTEM_PARAMETERS));

        factory.setPrimaryCodeSystem("BDNUM");
        JsonNode schemaWithPrimary = factory.getSchema();
        String withPrimary = schemaWithPrimary.toString();
        assertTrue(withPrimary.contains(EmaSmsFhirExporterFactory.PRIMARY_CODE_SYSTEM_PARAMETERS));
        assertTrue(withPrimary.contains("BDNUM"));
        assertEquals("BDNUM", factory.getPrimaryCodeSystem());
    }

    @Test
    void createNewExporterExportsSubstanceDefinitionJson() throws Exception {
        EmaSmsFhirExporterFactory factory = new EmaSmsFhirExporterFactory();
        EmaSmsSubstanceDefinitionFhirMapper mapper = mock(EmaSmsSubstanceDefinitionFhirMapper.class);
        SubstanceDefinition definition = new SubstanceDefinition();
        definition.setId("example");
        when(mapper.generateEmaSmsSubstanceDefinitionFromSubstance(any(Substance.class))).thenReturn(definition);

        setField(factory, "emaSmsSubstanceDefinitionFhirMapper", mapper);
        setField(factory, "emaSmsFhirConfiguration", new EmaSmsFhirConfiguration());

        OutputFormat supported = factory.getSupportedFormats().iterator().next();
        ObjectNode details = JsonNodeFactory.instance.objectNode();
        details.put(EmaSmsFhirExporterFactory.APPROVAL_ID_NAME_PARAMETERS, "APPROVAL_ID");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Exporter<Substance> exporter = factory.createNewExporter(out, params(supported, details));

        Substance substance = new SubstanceBuilder().asChemical().generateNewUUID().build();
        exporter.export(substance);
        exporter.close();

        String payload = out.toString(StandardCharsets.UTF_8.name());
        assertTrue(payload.contains("SubstanceDefinition"));
        verify(mapper, times(1)).generateEmaSmsSubstanceDefinitionFromSubstance(any(Substance.class));
    }

    @Test
    void exporterConstructorReadsDetailedParametersBranches() throws Exception {
        EmaSmsSubstanceDefinitionFhirMapper mapper = mock(EmaSmsSubstanceDefinitionFhirMapper.class);
        when(mapper.generateEmaSmsSubstanceDefinitionFromSubstance(any(Substance.class)))
                .thenReturn(new SubstanceDefinition());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectNode details = JsonNodeFactory.instance.objectNode();
        details.put(EmaSmsFhirExporterFactory.PRIMARY_CODE_SYSTEM_PARAMETERS, true);
        details.put(EmaSmsFhirExporterFactory.APPROVAL_ID_NAME_PARAMETERS, "  CUSTOM_APPROVAL  ");

        EmaSmsFhirExporter exporter = new EmaSmsFhirExporter(
                out,
                params(new OutputFormat("emasmsfhir.txt", "Fhir export, (.emasmsfhir.txt)"), details),
                "BDNUM",
                new EmaSmsFhirConfiguration(),
                mapper
        );

        assertTrue((Boolean) getField(exporter, "omitPrimaryCodeSystem"));
        assertEquals("CUSTOM_APPROVAL", getField(exporter, "chosenApprovalIdName"));
        exporter.close();
    }

    private static ExporterFactory.Parameters params(OutputFormat format, ObjectNode details) {
        return new DefaultParameters(format, false, details);
    }

    private static void setField(Object target, String name, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object getField(Object target, String name) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

