package example.substance.export;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.module.substance.scrubbers.basic.BasicSubstanceScrubberParameters;
import ix.ginas.exporters.SpecificExporterSettings;
import ix.ginas.exporters.ExporterSpecificExportSettings;
import ix.ginas.exporters.GeneralExportSettings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class ExportConfigTest {
    @Test
    public void testConfiguration1() throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        ExporterSpecificExportSettings exporterSpecificExportSettings = ExporterSpecificExportSettings.builder()
                .columnNames(Arrays.asList("PT", "UNII", "UUID"))
                .includeRepeatingDataOnEveryRow(false)
                .build();
        JsonNode exporterSettings = objectMapper.valueToTree(exporterSpecificExportSettings);
        GeneralExportSettings generalExportSettings = GeneralExportSettings.builder()
                .approvalIdCodeSystem("Universal Approval Code")
                .copyApprovalIdToCode(true)
                .newAbstractUser("registrar")
                .removeApprovalId(true)
                .setAllAuditorsToAbstractUser(true)
                .build();
        SpecificExporterSettings settings = SpecificExporterSettings.builder()
                .exporterKey("SDF")
                .exporterSettings(exporterSettings)
                .configurationKey("Basic SDFiles")
                .configurationId("1892")
                .build();
        ObjectMapper mapper = new ObjectMapper();

        String configString =mapper.writeValueAsString(settings);
        System.out.println(configString);

        Assertions.assertTrue(configString.length()>0);
    }

    @Test
    public void testScrubberConfig() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode scrubberSettings = JsonNodeFactory.instance.objectNode();
        scrubberSettings.put("removeAllDates", true);
        scrubberSettings.put("removeAllAuditUser", false);
        scrubberSettings.put("auditUserName", "Smith");
        scrubberSettings.put("excludeReferencePattern", ".*IND*.");

        String scrubberConfig = scrubberSettings.toPrettyString();
        System.out.println(scrubberConfig);
        Assertions.assertTrue(scrubberConfig.length()>10);
    }

    @Test
    public void testScrubberSchema() throws JsonProcessingException {
        BasicSubstanceScrubberParameters schema = new BasicSubstanceScrubberParameters();
        schema.setRemoveAllLockedAccessGroupsToInclude( Arrays.asList( "Center for top-secret research"));
        schema.setRemoveCodesBySystemCodeSystemsToKeep(Arrays.asList("CAS", "ChemSpider"));
        schema.setApprovalIdCleanupApprovalIdCodeSystem("Approval ID");
        schema.setChangeAllStatuses(false);
        schema.setDeidentifyAuditUser(false);
        schema.setRemoveReferencesByCriteria(true);
        schema.setRemoveReferencesByCriteriaReferenceTypesToRemove( Arrays.asList("IND", "NDA"));
        ObjectMapper objectMapper = new ObjectMapper();
        String schemaString = objectMapper.writeValueAsString(schema);
        System.out.printf("schemaString: %s", schemaString);
        Assertions.assertTrue(schemaString.length()>0);
    }

}
