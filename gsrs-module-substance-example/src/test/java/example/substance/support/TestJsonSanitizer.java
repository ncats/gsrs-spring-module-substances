package example.substance.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class TestJsonSanitizer {

    private TestJsonSanitizer() {
        // utility class
    }

    public static JsonNode stripAccessFields(JsonNode source) {
        JsonNode copy = source.deepCopy();
        removeAccessRecursively(copy);
        return copy;
    }

    private static void removeAccessRecursively(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.remove("access");
            objectNode.remove("createdBy");
            objectNode.remove("lastEditedBy");
            objectNode.remove("approvedBy");
            objectNode.fields().forEachRemaining(entry -> removeAccessRecursively(entry.getValue()));
            return;
        }
        if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            arrayNode.forEach(TestJsonSanitizer::removeAccessRecursively);
        }
    }
}

