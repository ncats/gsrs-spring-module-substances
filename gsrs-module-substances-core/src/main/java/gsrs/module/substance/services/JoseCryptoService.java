package gsrs.module.substance.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.StaticContextAccessor;

import java.util.Date;
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
import org.apache.cxf.rs.security.jose.jws.JwsCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.springframework.stereotype.Service;

/**
 *
 * @author Egor Puzanov
 */

@Slf4j
@Service
public class JoseCryptoService {

    private static final JoseCryptoServiceConfiguration config = JoseCryptoServiceConfiguration.INSTANCE();
    private static CachedSupplier<JoseCryptoService> _instanceSupplier = CachedSupplier.of(()->{
        JoseCryptoService instance;
        try {
            instance = StaticContextAccessor.getBean(JoseCryptoService.class);
        } catch (Exception e) {
            instance = new JoseCryptoService();
        }
        return instance;
    });

    public static JoseCryptoService INSTANCE() {
        return _instanceSupplier.get();
    }

    public static String sign(String str, Map<String, Object> metadata) {
        if (config.getPrivateKeyId() == null) {
            return "";
        }
        JwsHeaders protectedHeaders = new JwsHeaders(metadata);
        try {
            protectedHeaders.setKeyId(config.getPrivateKeyId());
            protectedHeaders.setContentType("application/json");
            JwsCompactProducer jwsProducer = new JwsCompactProducer(protectedHeaders, str);
            JwsSignatureProvider jwsp = JwsUtils.getSignatureProvider(config.getPrivateKey(), config.getSignatureAlgorithm());
            str = jwsProducer.signWith(jwsp);
        } catch (Exception e) {
        }
        return str;
    }

    public static JsonNode verify(String jwsCompactStr) {
        boolean verified = true;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode result = null;
        JwsCompactConsumer jwsConsumer = new JwsCompactConsumer(jwsCompactStr);
        try {
            JwsHeaders headers = jwsConsumer.getJwsHeaders();
            if (config.getStrictVerification() || config.getPreserveMetadata()) {
                verified = jwsConsumer.verifySignatureWith(config.getKey(headers.getKeyId()), headers.getSignatureAlgorithm());
            }
            if (config.getStrictVerification() && ! verified ) {
                return null;
            }
            result = mapper.readTree(jwsConsumer.getDecodedJwsPayloadBytes());
            if (config.getPreserveMetadata()) {
                Map<String, Object> hm = headers.asMap();
                StringBuilder citation = new StringBuilder("Exported on ")
                    .append(config.getDateFormat().format(new Date(Long.valueOf(hm.getOrDefault("dat", new Date().getTime()).toString()))))
                    .append(" by ")
                    .append(hm.getOrDefault("usr", "unknown"))
                    .append(" from ")
                    .append(verified ? "trusted" : "untrusted")
                    .append(" source ")
                    .append(headers.getKeyId())
                    .append(" (SRS schema version:")
                    .append(hm.getOrDefault("ver", "unknown"))
                    .append(")");
                ObjectNode ref = JsonNodeFactory.instance.objectNode();
                ref.set("docType", JsonNodeFactory.instance.textNode("SYSTEM"));
                ref.set("citation", JsonNodeFactory.instance.textNode(citation.toString()));
                if (hm.containsKey("ori")) {
                    ref.set("url", JsonNodeFactory.instance.textNode(headers.getHeader("ori").toString()));
                }
                if (hm.containsKey("dat")) {
                    ref.set("documentDate", JsonNodeFactory.instance.numberNode(Long.valueOf(headers.getHeader("dat").toString())));
                }
                ((ObjectNode) result).put("_metadata", ref);
            }
        } catch (Exception e) {
        }
        return result;
    }

    public static void encrypt(ObjectNode node) {
        JsonWebKey key;
        KeyEncryptionProvider keyEncryption;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode result = JsonNodeFactory.instance.objectNode();
        JweHeaders protectedHeaders = new JweHeaders(config.getContentAlgorithm());
        protectedHeaders.setType(JoseType.JOSE_JSON);
        //protectedHeaders.setZipAlgorithm("DEF");
        JweHeaders sharedUnprotectedHeaders = new JweHeaders();
        sharedUnprotectedHeaders.setKeyEncryptionAlgorithm(config.getKeyAlgorithm());
        ContentEncryptionProvider contentEncryption = JweUtils.getContentEncryptionProvider(config.getContentAlgorithm(), true);
        List<JweEncryptionProvider> jweProviders = new LinkedList<JweEncryptionProvider>();
        List<JweHeaders> perRecipientHeades = new LinkedList<JweHeaders>();
        JsonWebKey privateKey = config.getPrivateKey();
        if (config.getPrivateKeyId() != null && node.get("access").findValue(config.getPrivateKeyId()) == null) {
            keyEncryption = JweUtils.getKeyEncryptionProvider(privateKey, config.getKeyAlgorithm());
            jweProviders.add(new JweEncryption(keyEncryption, contentEncryption));
            perRecipientHeades.add(new JweHeaders(config.getPrivateKeyId()));
        }
        Iterator<JsonNode> it = node.get("access").elements();
        while (it.hasNext()) {
            key = config.getKey(it.next().asText());
            if (key != null) {
                keyEncryption = JweUtils.getKeyEncryptionProvider(key, config.getKeyAlgorithm());
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
        JweJsonConsumer consumer = new JweJsonConsumer(node.toString());
        node.removeAll();
        if (config.getPrivateKeyId() == null) {
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        try {
            KeyAlgorithm keyAlgo = consumer.getSharedUnprotectedHeader().getKeyEncryptionAlgorithm();
            ContentAlgorithm ctAlgo = consumer.getProtectedHeader().getContentEncryptionAlgorithm();
            JweDecryptionProvider jwe = JweUtils.createJweDecryptionProvider(JweUtils.getKeyDecryptionProvider(config.getPrivateKey(), keyAlgo), ctAlgo);
            for (JweJsonEncryptionEntry encEntry : consumer.getRecipients()) {
                if (config.getPrivateKeyId().equals(encEntry.getUnprotectedHeader().getKeyId())) {
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

    public static void protect(JsonNode node) {
        if (node.isObject()) {
            Iterator<String> it = node.fieldNames();
            while (it.hasNext()) {
                String key = it.next();
                protect(node.get(key));
            }
            if (node.has("access") && node.get("access").has(0)) {
                encrypt((ObjectNode)node);
            }
        } else if (node.isArray()) {
            Iterator<JsonNode> it = node.elements();
            while (it.hasNext()) {
                protect(it.next());
            }
        }
    }

    public static void unprotect(JsonNode node) {
        if (node.isObject()) {
            if (node.has("ciphertext")) {
                decrypt((ObjectNode)node);
            }
            Iterator<String> it = node.fieldNames();
            while (it.hasNext()) {
                String key = it.next();
                unprotect(node.get(key));
            }
        } else if (node.isArray()) {
            Iterator<JsonNode> it = node.elements();
            while (it.hasNext()) {
                unprotect(it.next());
            }
        }
    }
}
