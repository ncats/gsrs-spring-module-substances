package gsrs.module.substance.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.StaticContextAccessor;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import org.apache.cxf.common.util.CompressionUtils;
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

    private static String format(String template, Map<String, String> parameters) {
        StringBuilder newTemplate = new StringBuilder(template);
        List<String> valueList = new ArrayList<String>();
        Matcher matcher = Pattern.compile("[$][{](\\w+)}").matcher(template);
        while (matcher.find()) {
            String key = matcher.group(1);
            String paramName = "${" + key + "}";
            int index = newTemplate.indexOf(paramName);
            if (index != -1) {
                newTemplate.replace(index, index + paramName.length(), "%s");
                valueList.add(parameters.get(key));
            }
        }
        return String.format(newTemplate.toString(), valueList.toArray());
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
            if (config.getStrictVerification() || !config.getMetadataTemplate().isEmpty()) {
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
            if (!config.getMetadataTemplate().isEmpty()) {
                ObjectNode metadata = JsonNodeFactory.instance.objectNode();
                Map<String, String> parameterMap = new HashMap<String, String>();
                if (headers.containsProperty("ori")) {
                    metadata.set("url", JsonNodeFactory.instance.textNode(headers.getHeader("ori").toString()));
                }
                if (headers.containsProperty("dat")) {
                    metadata.set("documentDate", JsonNodeFactory.instance.numberNode(Long.valueOf(headers.getHeader("dat").toString())));
                    parameterMap.put("date", config.getDateFormat().format(new Date(Long.valueOf(headers.getHeader("dat").toString()))));
                } else {
                    parameterMap.put("date", "unknown date");
                }
                parameterMap.put("user", headers.containsProperty("usr") ? headers.getHeader("usr").toString() : "unknown");
                parameterMap.put("version", headers.containsProperty("ver") ? headers.getHeader("ver").toString() : "unknown");
                parameterMap.put("source", headers.getKeyId());
                parameterMap.put("verified", verified ? "trusted" : "untrusted");
                metadata.set("txt", JsonNodeFactory.instance.textNode(format(config.getMetadataTemplate(), parameterMap)));
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
                byte[] bytes = node.toString().getBytes("UTF-8");
                // Jwe Compression Issue Workaround start
                if (JoseConstants.JWE_DEFLATE_ZIP_ALGORITHM.equals(config.getZipAlgorithm())) {
                    protectedHeaders.removeProperty("zip");
                    bytes = CompressionUtils.deflate(bytes, true);
                }
                // Jwe Compression Issue Workaround end
                JweJsonProducer p = new JweJsonProducer(protectedHeaders,
                                                        sharedUnprotectedHeaders,
                                                        bytes);
                result = mapper.readTree(p.encryptWith(jweProviders, perRecipientHeaders));
                // Jwe Compression Issue Workaround start
                if (JoseConstants.JWE_DEFLATE_ZIP_ALGORITHM.equals(config.getZipAlgorithm())) {
                    ((ObjectNode) result.get("unprotected")).put("zip", "DEF");
                }
                // Jwe Compression Issue Workaround end
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
