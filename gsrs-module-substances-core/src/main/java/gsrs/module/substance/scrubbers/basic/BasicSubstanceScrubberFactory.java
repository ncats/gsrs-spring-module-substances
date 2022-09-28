package gsrs.module.substance.scrubbers.basic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.AutowireHelper;
import ix.ginas.exporters.RecordScrubber;
import ix.ginas.exporters.RecordScrubberFactory;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BasicSubstanceScrubberFactory implements RecordScrubberFactory<Substance> {
    private final static String JSONSchema ="{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "  \"$id\": \"https://gsrs.ncats.nih.gov/#/export.scrubber.schema.json\",\n" +
            "  \"title\": \"Scrubber Parameters\",\n" +
            "  \"description\": \"Factors that control the behavior of a Java class that removes private parts of a data object before the object is shared\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"removeDates\": {\n" +
            "      \"comments\": \"When true, remove all date fields from output\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"title\": \"Remove Date\"\n" +
            "    },\n" +
            "    \"deidentifyAuditUser\": {\n" +
            "      \"comments\": \"When true, remove users listed as creator or modifier of records and subrecords\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"title\": \"Deidentify Audit User\"\n" +
            "    },\n" +
            "    \"deidentifiedReferencePatterns\": {\n" +
            "      \"comments\": \"References to replace (pattern applies to document type)\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"title\": \"Deidentified Reference Patterns\"\n" +
            "    },\n" +
            "    \"accessGroupsToInclude\": {\n" +
            "      \"comments\": \"names of access groups to that will NOT be removed \",\n" +
            "      \"type\": \"string\",\n" +
            "      \"title\": \"Access Groups to Include\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"removeAllLocked\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"accessGroupsToRemove\": {\n" +
            "      \"comments\": \"names of access groups to that WILL be removed \",\n" +
            "      \"type\": \"string\",\n" +
            "      \"title\": \"Access Groups to Remove\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"removeAllLocked\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"removeElementsIfNoExportablePublicRef\": {\n" +
            "      \"comments\": \"elements to remove when they have no public references\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"title\": \"Remove Elements if no exportable selected public domain reference\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"removeAllLocked\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"removeAllLocked\": {\n" +
            "      \"comments\": \"When true, remove any data element that is marked as non-public\",\n" +
            "      \"title\": \"Remove all Locked\",\n" +
            "      \"type\": \"boolean\"\n" +
            "    },\n" +
            "    \"removeCodesBySystem\": {\n" +
            "      \"comments\": \"When true, remove any Codes whose CodeSystem is on the list\",\n" +
            "      \"title\": \"Remove Codes by System\",\n" +
            "      \"type\": \"boolean\"\n" +
            "    },\n" +
            "    \"codeSystemsToRemove\": {\n" +
            "      \"comments\": \"Code Systems to remove\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"title\": \"Code Systems to Remove\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"removeCodesBySystem\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"codeSystemsToKeep\": {\n" +
            "      \"comments\": \"Code Systems to keep\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"title\": \"Code Systems to Keep\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"removeCodesBySystem\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"removeReferencesByCriteria\": {\n" +
            "      \"comments\": \"When true, remove any References that meet specified criteria\",\n" +
            "      \"title\": \"Remove References by Criteria\",\n" +
            "      \"type\": \"boolean\"\n" +
            "    },\n" +
            "    \"referenceTypesToRemove\": {\n" +
            "      \"comments\": \"Document Types to look at.  When a Reference is of that document type, remove it\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"title\": \"Reference Types to Remove\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"removeReferencesByCriteria\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"citationPatternsToRemove\": {\n" +
            "      \"comments\": \"Patterns (RegExes) to apply to Reference citation.  When a citation matches, remove the Reference\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"title\": \"Citation Patterns to Remove\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"removeReferencesByCriteria\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"excludeReferenceByPattern\": {\n" +
            "      \"comments\": \"Remove References by looking at citationPatternsToRemove\",\n" +
            "      \"title\": \"Exclude Reference by Pattern\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"removeReferencesByCriteria\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"substanceReferenceCleanup\": {\n" +
            "      \"comments\": \"When true, next criteria are used to process substance references\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"title\": \"Substance Reference Cleanup\"\n" +
            "    },\n" +
            "    \"removeReferencesToFilteredSubstances\": {\n" +
            "      \"comments\": \"When true, when a substance is removed, remove any references to it\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"title\": \"Remove References to Filtered Substances\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"substanceReferenceCleanup\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"removeReferencesToSubstancesNonExportedDefinitions\": {\n" +
            "      \"comments\": \"When true, when a substance's definition is removed, remove any references to it\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"title\": \"Remove References to Substances Non-Exported Definitions\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"substanceReferenceCleanup\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"removeNotes\": {\n" +
            "      \"comments\": \"When true, remove all Notes\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"title\": \"Remove Notes\"\n" +
            "    },\n" +
            "    \"removeChangeReason\": {\n" +
            "      \"comments\": \"When true, delete the 'Change Reason' field\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"title\": \"Remove Change Reason\"\n" +
            "    },\n" +
            "    \"approvalIdCleanup\": {\n" +
            "      \"comments\": \"When true, apply additional criteria to the approvalID field\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"title\": \"Approval Id clean-up\"\n" +
            "    },\n" +
            "    \"removeApprovalId\": {\n" +
            "      \"comments\": \"When true, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is removed\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"title\": \"Remove Approval Id\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"approvalIdCleanup\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"copyApprovalIdToCode\": {\n" +
            "      \"comments\": \"When true, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is copied to a code\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"title\": \"Copy Approval Id to code if code not already present\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"approvalIdCleanup\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"approvalIdCodeSystem\": {\n" +
            "      \"comments\": \"When this parameter has a value, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is copied to a code of this specified system\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"title\": \"Remove Approval Id\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"approvalIdCleanup\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"regenerateUUIDs\": {\n" +
            "      \"comments\": \"When true, all UUIDs in the object being exported will be given a newly-generated value\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"title\": \"Regenerate UUIDs\"\n" +
            "    },\n" +
            "    \"changeAllStatuses\": {\n" +
            "      \"comments\": \"When true, all status value in the object being exported will be given a value\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"title\": \"Change All Statuses\"\n" +
            "    },\n" +
            "    \"newStatusValue\": {\n" +
            "      \"comments\": \"new string value to assign to all individual status fields throughout the object\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"title\": \"New Status Value\"\n" +
            "    },\n" +
            "    \"AuditInformationCleanup\": {\n" +
            "      \"comments\": \"When true, apply succeeding criteria to audit fields\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"title\": \"Audit Information clean-up\"\n" +
            "    },\n" +
            "    \"newAuditorValue\": {\n" +
            "      \"comments\": \"new string value to assign to all auditor (creator/modifier) fields throughout the object\",\n" +
            "      \"title\": \"New Auditor Value\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"AuditInformationCleanup\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"removeAllEntryTimestamps\": {\n" +
            "      \"comments\": \"remove created and modified date fields\",\n" +
            "      \"title\": \"Remove all entry timestamps\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"AuditInformationCleanup\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"scrubbedDefinitionHandling\": {\n" +
            "      \"comments\": \"apply some additional scrubbing to definitions that had parts removed\",\n" +
            "      \"title\": \"Scrubbed Definition Handling\",\n" +
            "      \"type\": \"boolean\"\n" +
            "    },\n" +
            "    \"removeScrubbedDefinitionalElementsEntirely\": {\n" +
            "      \"comments\": \"When a defining element has been modified, remove it entirely\",\n" +
            "      \"title\": \"Remove partially/fully scrubbed definitional records entirely\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"scrubbedDefinitionHandling\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"setScrubbedDefinitionalElementsIncomplete\": {\n" +
            "      \"comments\": \"When a defining element has been modified, set definitional level to \\\"Incomplete\\\"\",\n" +
            "      \"title\": \"Set partially/fully scrubbed definitional records to definitional level \\\"Incomplete\\\"\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"scrubbedDefinitionHandling\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"convertScrubbedDefinitionsToConcepts\": {\n" +
            "      \"comments\": \"When a substance's defining element has been modified, convert the substance type to \\\"Concept\\\"\",\n" +
            "      \"title\": \"Convert partially/fully scrubbed definitional records to \\\"Concepts\\\"\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"scrubbedDefinitionHandling\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"addNoteToScrubbedDefinitions\": {\n" +
            "      \"comments\": \"When a substance's defining element has been modified, add a Note\",\n" +
            "      \"title\": \"add a note to partially/fully scrubbed definitional records\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"scrubbedDefinitionHandling\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": []\n" +
            "}";

    private static CachedSupplier<JsonNode> schemaSupplier = CachedSupplier.of(()->{
        ObjectMapper mapper =new ObjectMapper();
        try {
            return mapper.readTree(JSONSchema);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;//todo: alternate return?
    });

    @Override
    public RecordScrubber<Substance> createScrubber(JsonNode settings) {
        log.trace("in BasicSubstanceScrubberFactory.createScrubber");
        BasicSubstanceScrubberParameters settingsObject = (new ObjectMapper()).convertValue(settings, BasicSubstanceScrubberParameters.class);
        log.trace(" settingsObject: {}", (settingsObject==null ? "null" : "not null"));
        //hack for demo 28 September 2022
        //todo: make sure real settings get passe!
        if(settingsObject==null){
            settingsObject = new BasicSubstanceScrubberParameters();
            settingsObject.setRemoveNotes(true);
            settingsObject.setRemoveChangeReason(false);
            settingsObject.setRemoveDates(true);
            settingsObject.setRemoveCodesBySystem(true);
            settingsObject.setCodeSystemsToKeep("CAS\nWikipedia");
            settingsObject.setCodeSystemsToRemove("BDNUM");
        }

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(settingsObject);
        scrubber= AutowireHelper.getInstance().autowireAndProxy(scrubber);

        return scrubber;
    }

    @Override
    public JsonNode getSettingsSchema() {
        return schemaSupplier.get();
    }

}
