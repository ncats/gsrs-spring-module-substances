package gsrs.module.substance.expanders.basic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.AutowireHelper;
import ix.ginas.exporters.RecordExpander;
import ix.ginas.exporters.RecordExpanderFactory;
import ix.ginas.models.v1.Substance;

public class BasicRecordExpanderFactory implements RecordExpanderFactory<Substance> {
    private final static String JSONSchema ="{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
            "  \"$id\": \"https://gsrs.ncats.nih.gov/#/export.expander.schema.json\",\n" +
            "  \"title\": \"Expander Parameters\",\n" +
            "  \"description\": \"Factors that control the behavior of a Java class that appends related objects when examining a data object before the object is shared\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"includeDefinitionalItems\": {\n" +
            "      \"comments\": \"When true, add data objects that are part of the starting object's definition\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"title\": \"Include definitional objects\"\n" +
            "    },\n" +
            "    \"definitionalGenerations\": {\n" +
            "      \"comments\": \"number of generations to go back \",\n" +
            "      \"type\": \"integer\",\n" +
            "      \"title\": \"Number of generations of objects to include\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"includeDefinitionalItems\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"includedRelatedItems\": {\n" +
            "      \"comments\": \"When true, add items at the other end of inter-object relationships\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"title\": \"Include related objects\"\n" +
            "    },\n" +
            "    \"relatedGenerations\": {\n" +
            "      \"comments\": \"Number of generations of relationships to include\",\n" +
            "      \"type\": \"integer\",\n" +
            "      \"title\": \"Number of generations of relationships to include\",\n" +
            "      \"visibleIf\": {\n" +
            "        \"includedRelatedItems\": [\n" +
            "          true\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"includeModifyingItems\": {\n" +
            "      \"comments\": \"When true, add items that are part of 'modifications' of the starting item.  This is based on Substance AgentModifications\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"title\": \"Include modification items\"\n" +
            "    },\n" +
            "    \"includeMediatingItems\": {\n" +
            "      \"comments\": \"When true, add items that are used in the definitions of amounts\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"title\": \"Include objects mentioned in amounts\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": []\n" +
            "}\n";

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
    public RecordExpander<Substance> createExpander(JsonNode settings) {
        BasicRecordExpander expander = new BasicRecordExpander();
        expander=AutowireHelper.getInstance().autowireAndProxy(expander);
        expander.applySettings(settings);
        return expander;
    }

    @Override
    public JsonNode getSettingsSchema() {
        return schemaSupplier.get();
    }

}
