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

import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.ContentEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionOutput;
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
public class JoseCryptoService implements CryptoService {

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

    public static CryptoService INSTANCE() {
        return (CryptoService) _instanceSupplier.get();
    }

    @Override
    public boolean isReady() {
        return config == null ? false : true;
    }

    @Override
    public String sign(String str, Map<String, Object> metadata) {
        if (config.getPrivateKeyId() == null) {
            return "";
        }
        JwsHeaders protectedHeaders = new JwsHeaders(metadata);
        try {
            protectedHeaders.setKeyId(config.getPrivateKeyId());
            protectedHeaders.setContentType(JoseConstants.MEDIA_TYPE_JOSE_JSON);
            JwsCompactProducer jwsProducer = new JwsCompactProducer(protectedHeaders, str);
            JwsSignatureProvider jwsp = JwsUtils.getSignatureProvider(config.getPrivateKey(), config.getSignatureAlgorithm());
            str = jwsProducer.signWith(jwsp);
        } catch (Exception e) {
            log.error(e.toString());
        }
        return str;
    }

    @Override
    public JsonNode verify(String jwsCompactStr) {
        boolean verified = true;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode result = null;
        JwsCompactConsumer jwsConsumer = new JwsCompactConsumer(jwsCompactStr);
        try {
            JwsHeaders headers = jwsConsumer.getJwsHeaders();
            if (config.getStrictVerification() || config.getPreserveMetadata()) {
                JsonWebKey key = config.getKey(headers.getKeyId());
                if (key != null) {
                    verified = jwsConsumer.verifySignatureWith(key, headers.getSignatureAlgorithm());
                } else {
                    verified = false;
                }
            }
            if (config.getStrictVerification() && ! verified ) {
                return null;
            }
            result = mapper.readTree(jwsConsumer.getDecodedJwsPayloadBytes());
            if (config.getPreserveMetadata()) {
                ObjectNode metadata = JsonNodeFactory.instance.objectNode();
                Map<String, Object> hm = headers.asMap();
                StringBuilder txt = new StringBuilder("Exported");
                if (hm.containsKey("dat")) {
                    metadata.set("documentDate", JsonNodeFactory.instance.numberNode(Long.valueOf(headers.getHeader("dat").toString())));
                    txt = txt
                        .append(" on ")
                        .append(config.getDateFormat().format(new Date(Long.valueOf(hm.getOrDefault("dat", new Date().getTime()).toString()))));
                }
                txt = txt
                    .append(" by ")
                    .append(hm.getOrDefault("usr", "unknown"))
                    .append(" from ")
                    .append(verified ? "trusted" : "untrusted")
                    .append(" source ")
                    .append(headers.getKeyId())
                    .append(" (SRS schema version:")
                    .append(hm.getOrDefault("ver", "unknown"))
                    .append(")");
                metadata.set("txt", JsonNodeFactory.instance.textNode(txt.toString()));
                if (hm.containsKey("ori")) {
                    metadata.set("url", JsonNodeFactory.instance.textNode(headers.getHeader("ori").toString()));
                }
                ((ObjectNode) result).put("_metadata", metadata);
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
        return result;
    }

    @Override
    public void encrypt(ObjectNode node, List<String> recipients) {
        JsonWebKey key;
        KeyEncryptionProvider keyEncryption;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode result = JsonNodeFactory.instance.objectNode();
        JweHeaders protectedHeaders = new JweHeaders(config.getContentAlgorithm());
        protectedHeaders.setType(JoseType.JOSE_JSON);
        JweHeaders sharedUnprotectedHeaders = new JweHeaders();
        if (JoseConstants.JWE_DEFLATE_ZIP_ALGORITHM.equals(config.getZipAlgorithm())) {
            protectedHeaders.setZipAlgorithm(config.getZipAlgorithm());
        }
        sharedUnprotectedHeaders.setKeyEncryptionAlgorithm(config.getKeyAlgorithm());
        ContentEncryptionProvider contentEncryption = JweUtils.getContentEncryptionProvider(config.getContentAlgorithm(), true);
        List<JweEncryptionProvider> jweProviders = new LinkedList<JweEncryptionProvider>();
        List<JweHeaders> perRecipientHeaders = new LinkedList<JweHeaders>();
        if (config.getPrivateKeyId() != null && !recipients.contains(config.getPrivateKeyId())) {
            recipients.add(config.getPrivateKeyId());
        }
        for (String keyId : recipients) {
            key = config.getKey(keyId);
            if (key != null) {
                keyEncryption = JweUtils.getKeyEncryptionProvider(key, config.getKeyAlgorithm());
                jweProviders.add(new JweEncryption(keyEncryption, contentEncryption));
                perRecipientHeaders.add(new JweHeaders(key.getKeyId()));
            }
        }
        if (!jweProviders.isEmpty()) {
            try {
                JweJsonProducer p = new JweJsonProducer(protectedHeaders,
                                                        sharedUnprotectedHeaders,
                                                        node.toString().getBytes("UTF-8"));
                result = mapper.readTree(p.encryptWith(jweProviders, perRecipientHeaders));
            } catch (Exception e) {
                log.error(e.toString());
            }
        }
        node.removeAll();
        Iterator<Map.Entry<String, JsonNode>> fields = result.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            node.set(field.getKey(), field.getValue());
        }
    }

    @Override
    public void decrypt(ObjectNode node) {
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
            log.error(e.toString());
        }
        Iterator<Map.Entry<String, JsonNode>> fields = result.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            node.set(field.getKey(), field.getValue());
        }
    }
}
