package gsrs.module.substance.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.nimbusds.jose.CompressionAlgorithm;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.PasswordBasedDecrypter;
import com.nimbusds.jose.crypto.PasswordBasedEncrypter;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jose.crypto.ECDHEncrypter;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.factories.DefaultJWEDecrypterFactory;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.produce.JWSSignerFactory;
import com.nimbusds.jose.proc.JWEDecrypterFactory;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.IOUtils;
import com.nimbusds.jose.util.JSONObjectUtils;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.StaticContextAccessor;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
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
    private final JWKSet jsonWebKeys;
    private final EncryptionMethod encryptionMethod;
    private final JWEAlgorithm keyAlgorithm;
    private final JWSAlgorithm signatureAlgorithm;
    private final Curve curve;
    private final CompressionAlgorithm zipAlgorithm;
    private final String metadataTemplate;
    private final boolean strictVerification;
    private final SimpleDateFormat dateFormat;
    private final static JWEDecrypterFactory jweDecrFactory = new DefaultJWEDecrypterFactory();
    private final static JWSSignerFactory jwsSignFactory = new DefaultJWSSignerFactory();
    private final static JWSVerifierFactory jwsVeriFactory = new DefaultJWSVerifierFactory();
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
        JWKSet jwks = null;
        String privateKeyId = config.getPrivateKeyId();
        Map<String, Object> jwksmap = config.getJsonWebKeys();
        if (jwksmap.containsKey("filename")) {
            String filename = (String) jwksmap.get("filename");
            String password = (String) jwksmap.get("password");
            if (!filename.contains(":")) {
                filename = "file:" + filename;
            }
            try (InputStream is = new UrlResource(filename).getInputStream();) {
                if (password != null && !password.isEmpty()) {
                    JWEObject jweo = JWEObject.parse(IOUtils.readInputStreamToString(is));
                    jweo.decrypt(new PasswordBasedDecrypter(password));
                    jwks = JWKSet.parse(jweo.getPayload().toString());
                } else {
                    jwks = JWKSet.load(is);
                }
            } catch (FileNotFoundException e) {
                log.warn("The keystore " + filename + " not found. Generate a new one");
                String kid = "localhost";
                if (privateKeyId != null && !privateKeyId.isEmpty()) {
                    kid = privateKeyId;
                } else if (config.getApplicationHost() != null && config.getApplicationHost().contains("//")) {
                    kid = config.getApplicationHost().split("/")[2];
                }
                jwks = generateKeyStore(filename, kid, JWEAlgorithm.parse(config.getKeyAlgorithm()), Curve.parse(config.getCurve()), password);
            } catch (Exception e) {
                log.error(e.toString());
            }
        }
        if (jwks == null && jwksmap.containsKey("keys")) {
            try {
                jwks = JWKSet.parse(jwksmap);
            } catch (Exception e) {
                log.error(e.toString());
            }
        }
        this.jsonWebKeys = jwks;

        if (privateKeyId != null && jwks.getKeyByKeyId(privateKeyId) == null) {
            privateKeyId = null;
        }
        if (privateKeyId == null) {
            privateKeyId = (String) jwks.getKeys()
                    .stream()
                    .filter(k->k.isPrivate())
                    .findFirst()
                    .map(k->k.getKeyID())
                    .orElse(null);
        }
        this.privateKeyId = privateKeyId;
        this.encryptionMethod = EncryptionMethod.parse(config.getEncryptionMethod());
        JWEAlgorithm alg = JWEAlgorithm.parse(config.getKeyAlgorithm());
        JWSAlgorithm sig = JWSAlgorithm.parse(config.getSignatureAlgorithm());
        if (privateKeyId != null) {
            JWK privateKey = jwks.getKeyByKeyId(privateKeyId);
            if (KeyType.RSA.equals(privateKey.getKeyType())) {
                if (!JWKMatcher.forJWEHeader(new JWEHeader(alg, this.encryptionMethod)).matches(privateKey)) {
                    alg = JWEAlgorithm.RSA_OAEP_256;
                }
                if (!JWKMatcher.forJWSHeader(new JWSHeader(sig)).matches(privateKey)) {
                    sig = JWSAlgorithm.RS256;
                }
            } else if (KeyType.EC.equals(privateKey.getKeyType())) {
                if (!JWKMatcher.forJWEHeader(new JWEHeader(alg, this.encryptionMethod)).matches(privateKey)) {
                    alg = JWEAlgorithm.ECDH_ES_A128KW;
                }
                if (!JWKMatcher.forJWSHeader(new JWSHeader(sig)).matches(privateKey)) {
                    sig = JWSAlgorithm.ES256;
                }
            }
        }
        this.keyAlgorithm = alg;
        this.signatureAlgorithm = sig;
        this.curve = Curve.parse(config.getCurve());
        this.zipAlgorithm = config.getZipAlgorithm() != null ? new CompressionAlgorithm(config.getZipAlgorithm()) : null;
        this.metadataTemplate = config.getMetadataTemplate();
        this.strictVerification = config.getStrictVerification();
        this.dateFormat = new SimpleDateFormat(config.getDateFormat());
        log.debug("Keys: " + String.join(", ", this.jsonWebKeys.getKeys().stream().map(JWK::getKeyID).collect(Collectors.toList())) + " privateKeyId: " + this.privateKeyId);
    }

    public static CryptoService INSTANCE() {
        return (CryptoService) _instanceSupplier.get();
    }

    private JWK getPrivateKey() {
        if (privateKeyId != null) {
            return jsonWebKeys.getKeyByKeyId(privateKeyId);
        }
        return null;
    }

    private JWK getKey(String keyId) {
        if (keyId != null) {
            return jsonWebKeys.getKeyByKeyId(keyId);
        }
        return null;
    }

    private static JWKSet generateKeyStore(String filename, String kid, JWEAlgorithm alg, Curve crv, String password) {
        JWKSet jwks = new JWKSet();
        try {
            if (RSAEncrypter.SUPPORTED_ALGORITHMS.contains(alg)) {
                jwks = new JWKSet(new RSAKeyGenerator(2048).keyID(kid).algorithm(alg).generate());
            } else if (ECDHEncrypter.SUPPORTED_ELLIPTIC_CURVES.contains(crv)) {
                jwks = new JWKSet(new ECKeyGenerator(crv).keyID(kid).algorithm(alg).generate());
            }
            try (FileWriter fileWriter = new FileWriter(new UrlResource(filename).getFile())) {
                if (password != null && !password.isEmpty()) {
                    JWEHeader header = new JWEHeader(JWEAlgorithm.PBES2_HS256_A128KW, EncryptionMethod.A128CBC_HS256);
                    JWEObject jweo = new JWEObject(header, new Payload(jwks.toJSONObject(false)));
                    jweo.encrypt(new PasswordBasedEncrypter(password, 8, 1000));
                    fileWriter.write(jweo.serialize());
                } else {
                    fileWriter.write(jwks.toString(false));
                }
            }
        } catch (Exception e) {
            jwks = new JWKSet();
            log.error(e.toString());
        }
        return jwks;
    }

    private static String format(String template, Map<String, Object> parameters) {
        StringBuilder newTemplate = new StringBuilder(template);
        List<String> valueList = new ArrayList<String>();
        Matcher matcher = Pattern.compile("[$][{](\\w+)}").matcher(template);
        while (matcher.find()) {
            String key = matcher.group(1);
            String paramName = "${" + key + "}";
            int index = newTemplate.indexOf(paramName);
            if (index != -1) {
                newTemplate.replace(index, index + paramName.length(), "%s");
                valueList.add(parameters.getOrDefault(key, "unknown").toString());
            }
        }
        return String.format(newTemplate.toString(), valueList.toArray());
    }

    @Override
    public boolean isReady() {
        return privateKeyId == null ? false : true;
    }

    @Override
    public String sign(String payload, Map<String, Object> metadata) {
        if (privateKeyId != null) {
            try {
                JWSHeader header = new JWSHeader.Builder(signatureAlgorithm)
                                                .keyID(getPrivateKey().getKeyID())
                                                .contentType(JOSEObjectType.JOSE_JSON.toString())
                                                .customParams(metadata)
                                                .build();
                JWSObject jwso = new JWSObject(header, new Payload(payload));
                jwso.sign(jwsSignFactory.createJWSSigner(getPrivateKey(), signatureAlgorithm));
                payload = jwso.serialize();
            } catch (Exception e) {
                log.error(e.toString());
            }
        }
        return payload;
    }

    @Override
    public ObjectNode verify(String payload) {
        boolean verified = false;
        ObjectNode result = null;
        try {
            JWSObject jwso = JWSObject.parse(payload);
            JWSHeader header = jwso.getHeader();
            if (strictVerification || !metadataTemplate.isEmpty()) {
                AsymmetricJWK key = (AsymmetricJWK) getKey(header.getKeyID());
                if (key != null) {
                    verified = jwso.verify(jwsVeriFactory.createJWSVerifier(header, key.toPublicKey()));
                }
            }
            if (strictVerification && !verified ) {
                return null;
            }
            Map<String, Object> customParams = header.getCustomParams();
            result = (ObjectNode) new ObjectMapper().readTree(jwso.getPayload().toString());
            if (!metadataTemplate.isEmpty()) {
                ObjectNode metadata = result.putObject("_metadata");
                Map<String, Object> parameterMap = JSONObjectUtils.newJSONObject();
                if (customParams.containsKey("ori")) {
                    metadata.put("url", customParams.get("ori").toString());
                }
                if (customParams.containsKey("dat")) {
                    metadata.put("documentDate", Long.valueOf(customParams.get("dat").toString()));
                    parameterMap.put("date", dateFormat.format(new Date(Long.valueOf(customParams.get("dat").toString()))));
                } else {
                    parameterMap.put("date", "unknown date");
                }
                parameterMap.put("user", customParams.getOrDefault("usr", "unknown").toString());
                parameterMap.put("version", customParams.getOrDefault("ver", "unknown").toString());
                parameterMap.put("source", header.getKeyID());
                parameterMap.put("verified", verified ? "trusted" : "untrusted");
                metadata.put("txt", format(metadataTemplate, parameterMap));
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
        return result;
    }

    @Override
    public void encrypt(ObjectNode node, List<String> recipients) {
        try {
            if (!recipients.contains(privateKeyId)) {
                recipients.add(0, privateKeyId);
            }
            ObjectMapper mapper = new ObjectMapper();
            Payload payload = new Payload(mapper.writeValueAsString(node));
            node.removeAll();
            JWEObject jweo = null;
            JWEEncrypter encrypter = null;
            JWEAlgorithm alg = keyAlgorithm;
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(encryptionMethod.cekBitLength());
            SecretKey cek = keyGenerator.generateKey();
            JWEHeader header = new JWEHeader.Builder(alg, encryptionMethod)
                                            .compressionAlgorithm(zipAlgorithm)
                                            .build();
            Map<String, Object> aadMap = header.toJSONObject();
            //aadMap.remove("alg"); custom aad support
            node.put("protected", Base64URL.encode(JSONObjectUtils.toJSONString(aadMap)).toString());
            final byte[] aad = node.get("protected").asText().getBytes();
            ArrayNode recipientsList = node.putArray("recipients");
            for (String kid : recipients) {
                JWK key = jsonWebKeys.getKeyByKeyId(kid);
                if (key != null) {
                    try {
                        alg = key.getAlgorithm() != null ? JWEAlgorithm.parse(key.getAlgorithm().toString()) : keyAlgorithm;
                        System.out.println(alg);
                        header = new JWEHeader.Builder(alg, encryptionMethod)
                                            .compressionAlgorithm(zipAlgorithm)
                                            // .keyID(kid) custom aad support
                                            .build();
                        jweo = new JWEObject(header, payload);
                        if (RSAEncrypter.SUPPORTED_ALGORITHMS.contains(header.getAlgorithm())) {
                            encrypter = new RSAEncrypter(key.toRSAKey().toRSAPublicKey(), cek); // custom aad support
                        } else if (ECDHEncrypter.SUPPORTED_ALGORITHMS.contains(header.getAlgorithm())) {
                            encrypter = new ECDHEncrypter(key.toECKey().toECPublicKey(), cek); // custom aad support
                        } else {
                            continue;
                        }
                        jweo.encrypt(encrypter);
                        ObjectNode recipient = recipientsList.addObject();
                        recipient.put("encrypted_key", jweo.getEncryptedKey().toString());
                        ObjectNode recipientsHeader = recipient.putObject("header");
                        recipientsHeader.setAll((ObjectNode) mapper.readTree(jweo.getHeader().toString()));
                        recipientsHeader.put("kid", kid); // custom aad support
                        recipientsHeader.remove("alg"); // custom aad support
                        recipientsHeader.remove("enc");
                        recipientsHeader.remove("zip");
                        if (!node.has("ciphertext")) {
                            payload = new Payload("");
                            node.put("iv", jweo.getIV().toString());
                            node.put("ciphertext", jweo.getCipherText().toString());
                            node.put("tag", jweo.getAuthTag().toString());
                        }
                    } catch (Exception e) {
                        log.error(e.toString());
                        continue;
                    }
                }
            }
            if (recipientsList.size() < 1) {
                node.removeAll();
            }
        } catch (Exception e) {
            log.error(e.toString());
            node.removeAll();
        }
    }

    @Override
    public void decrypt(ObjectNode node) {
        if (privateKeyId == null) {
            node.removeAll();
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode recipients = (ArrayNode) node.get("recipients");
        try {
            JsonNode encryptedKey = node.get("encrypted_key");
            Map<String, Object> headerMap = JSONObjectUtils.parse(Base64URL.from(node.get("protected").asText()).decodeToString());
            final byte[] aad = node.get("protected").asText().getBytes();
            if (recipients != null) {
                for (int i = 0; i < recipients.size(); i++) {
                    try {
                        if (privateKeyId.equals(recipients.get(i).get("header").get("kid").asText())) {
                            // custom aad support
                            //headerMap.putAll(mapper.convertValue(recipients.get(i).get("header"), new TypeReference<Map<String, Object>>(){}));
                            encryptedKey = recipients.get(i).get("encrypted_key");
                            break;
                        }
                    } catch (Exception e) {
                        log.error(e.toString());
                        continue;
                    }
                }
            }
            JWEObject jweo = new JWEObject( Base64URL.encode(JSONObjectUtils.toJSONString(headerMap)),
                                            Base64URL.from(encryptedKey.asText()),
                                            Base64URL.from(node.get("iv").asText()),
                                            Base64URL.from(node.get("ciphertext").asText()),
                                            Base64URL.from(node.get("tag").asText()));
            JWEAlgorithm alg = (getPrivateKey().getAlgorithm() != null) ? JWEAlgorithm.parse(getPrivateKey().getAlgorithm().toString()) : keyAlgorithm;
            if (RSADecrypter.SUPPORTED_ALGORITHMS.contains(alg)) {
                jweo.decrypt(new RSADecrypter(getPrivateKey().toRSAKey().toRSAPrivateKey(), null, false)); // custom aad support
            } else if (ECDHDecrypter.SUPPORTED_ALGORITHMS.contains(alg)) {
                jweo.decrypt(new ECDHDecrypter(getPrivateKey().toECKey().toECPrivateKey(), null)); // custom aad support
            }

            node.removeAll();
            if (JWEObject.State.DECRYPTED.equals(jweo.getState())) {
                node.setAll((ObjectNode) mapper.readTree(jweo.getPayload().toString()));
            }
        } catch (Exception e) {
            node.removeAll();
            log.error(e.toString());
        }
    }
}
