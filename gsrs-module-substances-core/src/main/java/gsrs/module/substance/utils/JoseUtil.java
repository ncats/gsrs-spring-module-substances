package gsrs.module.substance.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.ContentEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweEncryption;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweJsonConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweJsonEncryptionEntry;
import org.apache.cxf.rs.security.jose.jwe.JweJsonProducer;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jwe.KeyEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jws.JwsCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;

/**
 *
 * @author Egor Puzanov
 */

@Slf4j
public class JoseUtil {
    private static final JsonWebKeys jwks = loadJwks("export.jwks");
    private static final String privateKeyId = findPrivateKeyId();
    private static final ContentAlgorithm enc = ContentAlgorithm.A256GCM;
    private static final KeyAlgorithm alg = KeyAlgorithm.RSA_OAEP;
    private static final SignatureAlgorithm sig = SignatureAlgorithm.RS256;
    private static final JoseUtil INSTANCE = new JoseUtil();

    private JoseUtil() {
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

    public static JoseUtil getInstance() {
        return INSTANCE;
    }

    public static String sign(String str, Map<String, Object> headers) {
        JwsHeaders protectedHeaders = new JwsHeaders(headers);
        protectedHeaders.setKeyId(privateKeyId);
        protectedHeaders.setContentType("application/json");
        try {
            JwsCompactProducer jwsProducer = new JwsCompactProducer(protectedHeaders, str);
            JwsSignatureProvider jwsp = JwsUtils.getSignatureProvider(jwks.getKey(privateKeyId), sig);
            str = jwsProducer.signWith(jwsp);
        } catch (Exception e) {
        }
        return str;
    }

    public static ObjectNode verify(String jwsCompactStr) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        JwsCompactConsumer jwsConsumer = new JwsCompactConsumer(jwsCompactStr);
        try {
            JwsHeaders headers = jwsConsumer.getJwsHeaders();
            boolean verified = jwsConsumer.verifySignatureWith(jwks.getKey(headers.getKeyId()), headers.getSignatureAlgorithm());
            result = (ObjectNode) mapper.readTree(jwsConsumer.getDecodedJwsPayloadBytes());
            result.set("_metadata", mapper.readTree(jwsConsumer.getDecodedJsonHeaders()));
            ((ObjectNode) result.get("_metadata")).set("verified", JsonNodeFactory.instance.booleanNode(verified));
        } catch (Exception e) {
        }
        return result;
    }

    public static void encrypt(ObjectNode node) {
        JsonWebKey key;
        KeyEncryptionProvider keyEncryption;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode result = JsonNodeFactory.instance.objectNode();
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
        node.removeAll();
        Iterator<Map.Entry<String, JsonNode>> fields = result.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            node.set(field.getKey(), field.getValue());
        }
    }

    public static void decrypt(ObjectNode node) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        JweJsonConsumer consumer = new JweJsonConsumer(node.toString());
        node.removeAll();
        try {
            KeyAlgorithm keyAlgo = consumer.getSharedUnprotectedHeader().getKeyEncryptionAlgorithm();
            ContentAlgorithm ctAlgo = consumer.getProtectedHeader().getContentEncryptionAlgorithm();
            JweDecryptionProvider jwe = JweUtils.createJweDecryptionProvider(JweUtils.getKeyDecryptionProvider(jwks.getKey(privateKeyId), keyAlgo), ctAlgo);
            for (JweJsonEncryptionEntry encEntry : consumer.getRecipients()) {
                if (privateKeyId.equals(encEntry.getUnprotectedHeader().getKeyId())) {
                    result = (ObjectNode) mapper.readTree(consumer.decryptWith(jwe, encEntry).getContent());
                    break;
                }
            }
        } catch (Exception e) {
        }
        Iterator<Map.Entry<String, JsonNode>> fields = result.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            node.set(field.getKey(), field.getValue());
        }
    }
}