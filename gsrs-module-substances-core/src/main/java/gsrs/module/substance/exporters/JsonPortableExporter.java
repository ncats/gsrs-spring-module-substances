package gsrs.module.substance.exporters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import ix.core.controllers.EntityFactory;
import ix.ginas.exporters.Exporter;
import ix.ginas.models.v1.Substance;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.ContentEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweEncryption;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweJsonProducer;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jwe.KeyEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.PrivateKeyJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;


/**
 * Created by epuzanov on 8/30/21.
 */
public class JsonPortableExporter implements Exporter<Substance> {
    private final BufferedWriter out;

    private static final String LEADING_HEADER= "\t\t";
    private final ObjectWriter writer =  EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().writer();
    private static final List<String> fieldsToRemove = Arrays.asList("_name","_nameHTML","_formulaHTML","_approvalIDDisplay","_isClassification","_self","self","approvalID","approved","approvedBy","changeReason","created","createdBy","lastEdited","lastEditedBy","deprecated","uuid","refuuid","originatorUuid","linkingID","id","documentDate","status","version");
    private static final JsonWebKeys jwks = loadJwks("export.jwks");
    private static final String privateKeyId = findPrivateKeyId();
    private static final ContentAlgorithm enc = ContentAlgorithm.A256GCM;
    private static final KeyAlgorithm alg = KeyAlgorithm.RSA_OAEP;
    private static final SignatureAlgorithm sig = SignatureAlgorithm.RS256;
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
        String privKeyId = findPrivateKeyId();
        ObjectMapper mapper = new ObjectMapper();
        JwsHeaders protectedHeaders = new JwsHeaders();
        protectedHeaders.setKeyId(privateKeyId);
        protectedHeaders.setContentType("application/json");
        JsonNode ori = tree.get("_self");
        if (ori == null) {
            ori = new TextNode("Unknown");
        }
        protectedHeaders.setHeader("ori", ori.asText());
        protectedHeaders.setHeader("ver", gsrsVersion);
        protectedHeaders.setHeader("dat", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        deleteValidationNotes((ObjectNode) tree);
        uuidToIndex(tree);
        scrub(tree);
        checkAccess(tree);
        String out = tree.toString();
        if (sign) {
            JwsCompactProducer jwsProducer = new JwsCompactProducer(protectedHeaders, out);
            JwsSignatureProvider jwsp = JwsUtils.getSignatureProvider(jwks.getKey(privateKeyId), sig);
            out = jwsProducer.signWith(jwsp);
        }
        return out;
    }

    private static void encrypt(JsonNode node) {
        JsonWebKey key;
        KeyEncryptionProvider keyEncryption;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode result = mapper.createObjectNode();
        JweHeaders protectedHeaders = new JweHeaders(enc);
        protectedHeaders.setType(JoseType.JOSE_JSON);
        JweHeaders sharedUnprotectedHeaders = new JweHeaders();
        sharedUnprotectedHeaders.setKeyEncryptionAlgorithm(alg);
        ContentEncryptionProvider contentEncryption = JweUtils.getContentEncryptionProvider(enc, true);
        List<JweEncryptionProvider> jweProviders = new LinkedList<JweEncryptionProvider>();
        List<JweHeaders> perRecipientHeades = new LinkedList<JweHeaders>();
        if (privateKeyId != null && node.get("access").findValue(privateKeyId) == null) {
            keyEncryption = JweUtils.getKeyEncryptionProvider(jwks.getKey(privateKeyId), alg);
            jweProviders.add(new JweEncryption(keyEncryption, contentEncryption));
            perRecipientHeades.add(new JweHeaders(privateKeyId));
        }
        Iterator<JsonNode> it = node.get("access").elements();
        while (it.hasNext()) {
            key = jwks.getKey(it.next().asText());
            if (key != null) {
                keyEncryption = JweUtils.getKeyEncryptionProvider(key, alg);
                jweProviders.add(new JweEncryption(keyEncryption, contentEncryption));
                perRecipientHeades.add(new JweHeaders(key.getKeyId()));
            }
        }
        if (!jweProviders.isEmpty()) {
            JweJsonProducer p = new JweJsonProducer(protectedHeaders,
                                        sharedUnprotectedHeaders,
                                        StringUtils.toBytesUTF8(node.toString()));
            try {
                result = mapper.readTree(p.encryptWith(jweProviders, perRecipientHeades));
            } catch (Exception e) {
            }
        }
        ((ObjectNode) node).removeAll();
        Iterator<Map.Entry<String, JsonNode>> fields = result.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            ((ObjectNode) node).set(field.getKey(), field.getValue());
        }
    }

    private static JsonWebKeys loadJwks(String fileName) {
        try {
            return JwkUtils.readJwkSet(new String(Files.readAllBytes(Paths.get(fileName))));
        } catch (Exception e) {
            return new JsonWebKeys();
        }
    }

    private static String findPrivateKeyId () {
        return  jwks.getKeys()
                    .stream()
                    .filter(k->k.getKeyProperty(JsonWebKey.RSA_PRIVATE_EXP) != null)
                    .map(k->k.getKeyId())
                    .findFirst()
                    .orElse(null);
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
                encrypt(node);
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