package gsrs.module.substance.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.StaticContextAccessor;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jws.JwsCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

/**
 *
 * @author Egor Puzanov
 */

@Slf4j
@Service
public class JoseCryptoService implements CryptoService {

    private final String privateKeyId;
    private final JsonWebKeys jsonWebKeys;
    private final ContentAlgorithm contentAlgorithm;
    private final KeyAlgorithm keyAlgorithm;
    private final SignatureAlgorithm signatureAlgorithm;
    private final String zipAlgorithm;
    private final String metadataTemplate;
    private final boolean strictVerification;
    private final SimpleDateFormat dateFormat;
    private static CachedSupplier<JoseCryptoService> _instanceSupplier = CachedSupplier.of(()->{
        JoseCryptoService instance;
        try {
            instance = StaticContextAccessor.getBean(JoseCryptoService.class);
        } catch (Exception e) {
            instance = new JoseCryptoService(JoseCryptoServiceConfiguration.INSTANCE());
        }
        return instance;
    });

    @Autowired
    public JoseCryptoService(JoseCryptoServiceConfiguration config) {
        JsonWebKeys jwksobj = new JsonWebKeys();
        String privateKeyId = config.getPrivateKeyId();
        Map<String, Object> jwksmap = config.getJsonWebKeys();
        if (jwksmap == null) {
            jwksmap = new HashMap<String, Object>() {{ put("filename", "file:" + config.getIxHome() + "keystore.jwks"); }};
        }
        if (jwksmap.containsKey("filename")) {
            String filename = (String) jwksmap.get("filename");
            String password = (String) jwksmap.get("password");
            if (!filename.contains(":")) {
                filename = "file:" + filename;
            }
            try (InputStream is = new UrlResource(filename).getInputStream();) {
                if (password != null && !password.isEmpty()) {
                    CryptoUtils.installBouncyCastleProvider();
                    jwksobj = JwkUtils.decryptJwkSet(is, password.toCharArray());
                    CryptoUtils.removeBouncyCastleProvider();
                } else {
                    jwksobj = JwkUtils.readJwkSet(is);
                }
            } catch (FileNotFoundException e) {
                log.warn("The keystore " + filename + " not found. Generate a new one");
                String kid = "localhost";
                if (privateKeyId != null && !privateKeyId.isEmpty()) {
                    kid = privateKeyId;
                } else if (config.getApplicationHost() != null && config.getApplicationHost().contains("//")) {
                    kid = config.getApplicationHost().split("/")[2];
                }
                jwksobj = generateKeyStore(filename, kid, password);
            } catch (Exception e) {
                log.error(e.toString());
            }
        }
        if (jwksobj.size() == 0 && jwksmap.containsKey("keys")) {
            try {
                jwksobj.setKeys(((Map<String, Object>) jwksmap.get("keys")).values().stream().map(k->new JsonWebKey((Map<String, Object>) k)).collect(Collectors.toList()));
            } catch (Exception e) {
                log.error(e.toString());
            }
        }
        this.jsonWebKeys = jwksobj;

        if (privateKeyId != null && !jwksobj.getKeyIdMap().containsKey(privateKeyId)) {
            privateKeyId = null;
        }
        if (privateKeyId == null && jwksobj.size() > 0) {
            privateKeyId = (String) jwksobj.getKeys()
                    .stream()
                    .filter(k->(k.getKeyProperty(JsonWebKey.RSA_PRIVATE_EXP) != null || k.getKeyProperty(JsonWebKey.EC_PRIVATE_KEY) != null))
                    .findFirst()
                    .map(k->k.getKeyId())
                    .orElse(null);
        }
        this.privateKeyId = privateKeyId;
        this.contentAlgorithm = ContentAlgorithm.valueOf(config.getContentAlgorithm());
        this.keyAlgorithm = KeyAlgorithm.valueOf(config.getKeyAlgorithm());
        this.signatureAlgorithm = SignatureAlgorithm.valueOf(config.getSignatureAlgorithm());
        this.zipAlgorithm = String.valueOf(config.getZipAlgorithm());
        this.metadataTemplate = config.getMetadataTemplate();
        this.strictVerification = config.getStrictVerification();
        this.dateFormat = new SimpleDateFormat(config.getDateFormat());
        log.debug("Keys: " + String.valueOf(this.jsonWebKeys.getKeyIdMap().keySet()) + " privateKeyId: " + this.privateKeyId);
    }

    public static CryptoService INSTANCE() {
        return (CryptoService) _instanceSupplier.get();
    }

    private JsonWebKey getPrivateKey() {
        if (privateKeyId != null) {
            return jsonWebKeys.getKey(privateKeyId);
        }
        return null;
    }

    private JsonWebKey getKey(String keyId) {
        if (keyId != null) {
            return jsonWebKeys.getKey(keyId);
        }
        return getPrivateKey();
    }

    private static JsonWebKeys generateKeyStore(String filename, String kid, String password) {
        JsonWebKeys jwksobj = new JsonWebKeys();
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            JsonWebKey jwk = JwkUtils.fromRSAPrivateKey((RSAPrivateKey) pair.getPrivate(), null, kid);
            jwksobj.setKey(jwk);
            try (FileWriter fileWriter = new FileWriter(new UrlResource(filename).getFile())) {
                if (password != null && !password.isEmpty()) {
                    CryptoUtils.installBouncyCastleProvider();
                    fileWriter.write(JwkUtils.encryptJwkSet(jwksobj, password.toCharArray()));
                    CryptoUtils.removeBouncyCastleProvider();
                } else {
                    fileWriter.write(JwkUtils.jwkSetToJson(jwksobj));
                }
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
        return jwksobj;
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
        return privateKeyId == null ? false : true;
    }

    @Override
    public String sign(String str, Map<String, Object> metadata) {
        if (privateKeyId == null) {
            return "";
        }
        JwsHeaders protectedHeaders = new JwsHeaders(metadata);
        try {
            protectedHeaders.setKeyId(privateKeyId);
            protectedHeaders.setContentType(JoseConstants.MEDIA_TYPE_JOSE_JSON);
            JwsCompactProducer jwsProducer = new JwsCompactProducer(protectedHeaders, str);
            JwsSignatureProvider jwsp = JwsUtils.getSignatureProvider(getPrivateKey(), signatureAlgorithm);
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
            if (strictVerification || !metadataTemplate.isEmpty()) {
                JsonWebKey key = getKey(headers.getKeyId());
                if (key != null) {
                    verified = jwsConsumer.verifySignatureWith(key, headers.getSignatureAlgorithm());
                } else {
                    verified = false;
                }
            }
            if (strictVerification && ! verified ) {
                return null;
            }
            result = mapper.readTree(jwsConsumer.getDecodedJwsPayloadBytes());
            if (!metadataTemplate.isEmpty()) {
                ObjectNode metadata = JsonNodeFactory.instance.objectNode();
                Map<String, String> parameterMap = new HashMap<String, String>();
                if (headers.containsProperty("ori")) {
                    metadata.set("url", JsonNodeFactory.instance.textNode(headers.getHeader("ori").toString()));
                }
                if (headers.containsProperty("dat")) {
                    metadata.set("documentDate", JsonNodeFactory.instance.numberNode(Long.valueOf(headers.getHeader("dat").toString())));
                    parameterMap.put("date", dateFormat.format(new Date(Long.valueOf(headers.getHeader("dat").toString()))));
                } else {
                    parameterMap.put("date", "unknown date");
                }
                parameterMap.put("user", headers.containsProperty("usr") ? headers.getHeader("usr").toString() : "unknown");
                parameterMap.put("version", headers.containsProperty("ver") ? headers.getHeader("ver").toString() : "unknown");
                parameterMap.put("source", headers.getKeyId());
                parameterMap.put("verified", verified ? "trusted" : "untrusted");
                metadata.set("txt", JsonNodeFactory.instance.textNode(format(metadataTemplate, parameterMap)));
                ((ObjectNode) result).set("_metadata", metadata);
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
        JweHeaders protectedHeaders = new JweHeaders(contentAlgorithm);
        protectedHeaders.setType(JoseType.JOSE_JSON);
        JweHeaders sharedUnprotectedHeaders = new JweHeaders();
        if (JoseConstants.JWE_DEFLATE_ZIP_ALGORITHM.equals(zipAlgorithm)) {
            protectedHeaders.setZipAlgorithm(zipAlgorithm);
        }
        sharedUnprotectedHeaders.setKeyEncryptionAlgorithm(keyAlgorithm);
        ContentEncryptionProvider contentEncryption = JweUtils.getContentEncryptionProvider(contentAlgorithm, true);
        List<JweEncryptionProvider> jweProviders = new LinkedList<JweEncryptionProvider>();
        List<JweHeaders> perRecipientHeaders = new LinkedList<JweHeaders>();
        if (privateKeyId != null && !recipients.contains(privateKeyId)) {
            recipients.add(privateKeyId);
        }
        for (String keyId : recipients) {
            key = getKey(keyId);
            if (key != null) {
                keyEncryption = JweUtils.getKeyEncryptionProvider(key, keyAlgorithm);
                jweProviders.add(new JweEncryption(keyEncryption, contentEncryption));
                perRecipientHeaders.add(new JweHeaders(key.getKeyId()));
            }
        }
        if (!jweProviders.isEmpty()) {
            try {
                byte[] bytes = node.toString().getBytes("UTF-8");
                // Jwe Compression Issue Workaround start
                if (JoseConstants.JWE_DEFLATE_ZIP_ALGORITHM.equals(zipAlgorithm)) {
                    protectedHeaders.removeProperty("zip");
                    bytes = CompressionUtils.deflate(bytes, true);
                }
                // Jwe Compression Issue Workaround end
                JweJsonProducer p = new JweJsonProducer(protectedHeaders,
                                                        sharedUnprotectedHeaders,
                                                        bytes);
                result = mapper.readTree(p.encryptWith(jweProviders, perRecipientHeaders));
                // Jwe Compression Issue Workaround start
                if (JoseConstants.JWE_DEFLATE_ZIP_ALGORITHM.equals(zipAlgorithm)) {
                    ((ObjectNode) result.get("unprotected")).put("zip", zipAlgorithm);
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
        if (privateKeyId == null) {
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        try {
            KeyAlgorithm keyAlgo = consumer.getSharedUnprotectedHeader().getKeyEncryptionAlgorithm();
            ContentAlgorithm ctAlgo = consumer.getProtectedHeader().getContentEncryptionAlgorithm();
            JweDecryptionProvider jwe = JweUtils.createJweDecryptionProvider(JweUtils.getKeyDecryptionProvider(getPrivateKey(), keyAlgo), ctAlgo);
            for (JweJsonEncryptionEntry encEntry : consumer.getRecipients()) {
                if (privateKeyId.equals(encEntry.getUnprotectedHeader().getKeyId())) {
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
