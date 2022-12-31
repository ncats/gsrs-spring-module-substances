package gsrs.module.substance.exporters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import gsrs.buildInfo.BuildInfoFetcher;
import gsrs.module.substance.services.JoseCryptoService;
import ix.core.controllers.EntityFactory;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.models.v1.Substance;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by epuzanov on 8/30/21.
 */
public class JsonPortableExporter implements Exporter<Substance> {

    @Autowired
    private BuildInfoFetcher buildInfoFetcher;

    private static final JoseCryptoService joseCryptoService = JoseCryptoService.INSTANCE();

    private final BufferedWriter out;
    private static final String LEADING_HEADER= "\t\t";
    private final ObjectWriter writer =  EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().writer();
    private final List<String> fieldsToRemove;
    private final String originBase;
    private final ExporterFactory.Parameters parameters;

    public JsonPortableExporter(OutputStream out, ExporterFactory.Parameters parameters, List<String> fieldsToRemove, String originBase) throws IOException{
        Objects.requireNonNull(out);
        this.out = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
        this.parameters = parameters;
        this.fieldsToRemove = fieldsToRemove;
        this.originBase = originBase;
    }

    @Override
    public void export(Substance obj) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode tree = mapper.readTree(writer.writeValueAsString(obj));
        String line = this.makePortable(tree);
        if (line != null && !line.isEmpty()) {
            out.write(LEADING_HEADER);
            out.write(line);
            out.newLine();
        }
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    private String makePortable(JsonNode tree) {
        Map<String, Object> metadata = buildMetadata(tree);
        deleteValidationNotes((ObjectNode) tree);
        uuidToIndex(tree);
        scrub(tree);
        joseCryptoService.protect(tree);
        if (tree.size() == 0) {
            return null;
        }
        return joseCryptoService.sign(tree.toString(), metadata);
    }

    private Map<String, Object> buildMetadata(JsonNode tree) {
        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("ori", originBase != null ? originBase + tree.get("uuid").asText() : tree.get("_self").asText());
        metadata.put("ver", buildInfoFetcher.getBuildInfo().getVersion());
        metadata.put("dat", new Date().getTime());
        metadata.put("usr", parameters.getUsername());
        return metadata;
    }

    private void uuidToIndex (JsonNode node) {
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

    private void scrub(JsonNode node) {
        for (JsonNode n: node.findParents("created")) {
            ((ObjectNode) n).remove(fieldsToRemove);
        }
    }
}
