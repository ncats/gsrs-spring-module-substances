package gsrs.module.substance.exporters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import gsrs.module.substance.utils.JoseUtil;
import ix.core.controllers.EntityFactory;
import ix.ginas.exporters.Exporter;
import ix.ginas.models.v1.Substance;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by epuzanov on 8/30/21.
 */
public class JsonPortableExporter implements Exporter<Substance> {
    private final BufferedWriter out;

    private static final String LEADING_HEADER= "\t\t";
    private final ObjectWriter writer =  EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().writer();
    private static final List<String> fieldsToRemove = Arrays.asList("_name","_nameHTML","_formulaHTML","_approvalIDDisplay","_isClassification","_self","self","approvalID","approved","approvedBy","changeReason","created","createdBy","lastEdited","lastEditedBy","deprecated","uuid","refuuid","originatorUuid","linkingID","id","documentDate","status","version");
    private static final String gsrsVersion = "3.0.2";
    private static final boolean sign = false;

    public JsonPortableExporter(OutputStream out) throws IOException{
        Objects.requireNonNull(out);
        this.out = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
    }
    @Override
    public void export(Substance obj) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode tree = mapper.readTree(writer.writeValueAsString(obj));
        out.write(LEADING_HEADER);
        out.write(this.makePortable(tree));
        out.newLine();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    private static String makePortable(JsonNode tree) {
        deleteValidationNotes((ObjectNode) tree);
        uuidToIndex(tree);
        scrub(tree);
        checkAccess(tree);
        addMetadata(tree);
        String out = tree.toString();
        if (sign) {
            out = JoseUtil.getInstance().sign(out);
        }
        return out;
    }

    private static void addMetadata(JsonNode tree) {
        ObjectNode metadata = JsonNodeFactory.instance.objectNode();
        metadata.set("ori", new TextNode(tree.hasNonNull("_self") ? tree.get("_self").asText() : "Unknown"));
        metadata.set("ver", new TextNode(gsrsVersion));
        metadata.set("dat", new TextNode(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)));
        ((ObjectNode) tree).set("_metadata", metadata);
    }

    private static void uuidToIndex (JsonNode node) {
        Map<JsonNode, TextNode> references = new HashMap<JsonNode, TextNode>();
        int i = 0;
        for (JsonNode r: (ArrayNode) node.at("/references")) {
            references.put(r.get("uuid"), new TextNode(String.valueOf(i++)));
        }
        for (JsonNode refsNode: node.findValues("references")) {
            if (refsNode.isArray()) {
                ArrayNode refs = (ArrayNode) refsNode;
                for (i = 0; i < refs.size(); i++) {
                    JsonNode ref = refs.get(i);
                    if (ref.isTextual() && !ref.asText().chars().allMatch(Character::isDigit)) {
                        refs.set(i, references.get(ref));
                    }
                }
            }
        }
    }

    private static void deleteValidationNotes (ObjectNode node) {
        List<String> vRefs = new ArrayList<String>();
        ArrayNode notes = (ArrayNode) node.get("notes");
        for (int ni = notes.size() - 1; ni >= 0; ni--) {
            ArrayNode refs = (ArrayNode) notes.get(ni).get("references");
            if (notes.get(ni).get("note").asText().startsWith("[Validation]")) {
                for (int ri = 0; ri < refs.size(); ri++) {
                    String rUuid = refs.get(ri).asText();
                    if (!vRefs.contains(rUuid)) {
                        vRefs.add(rUuid);
                    }
                }
                notes.remove(ni);
            }
        }
        if (!vRefs.isEmpty()) {
            ArrayNode references = (ArrayNode) node.remove("references");
            String nodeJson = node.toString();
            for (int ri = references.size() - 1; ri >= 0; ri--) {
                String rUuid = references.get(ri).get("uuid").asText();
                if (vRefs.contains(rUuid) && !nodeJson.contains(rUuid)) {
                    references.remove(ri);
                }
            }
            node.set("references", references);
        }
    }

    private static void checkAccess (JsonNode node) {
        if (node.isObject()) {
            Iterator<String> it = node.fieldNames();
            while (it.hasNext()) {
                String key = it.next();
                checkAccess(node.get(key));
            }
            if (node.has("access") && !node.get("access").isEmpty()) {
                JoseUtil.getInstance().encrypt((ObjectNode)node);
            }
        } else if (node.isArray()) {
            Iterator<JsonNode> it = node.elements();
            while (it.hasNext()) {
                checkAccess(it.next());
            }
        }
    }

    private static void scrub(JsonNode node) {
        for (JsonNode n: node.findParents("created")) {
            ((ObjectNode) n).remove(fieldsToRemove);
        }
    }
}