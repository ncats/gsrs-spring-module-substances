package gsrs.module.substance.services;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.StaticContextAccessor;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

@Slf4j
@Data
@Configuration
@ConfigurationProperties("gsrs.crypto")
public class JoseCryptoServiceConfiguration {
    private static CachedSupplier<JoseCryptoServiceConfiguration> _instanceSupplier = CachedSupplier.of(()->{
        JoseCryptoServiceConfiguration instance;
        try {
            instance = StaticContextAccessor.getBean(JoseCryptoServiceConfiguration.class);
        } catch (Exception e) {
            instance = new JoseCryptoServiceConfiguration();
            instance.init();
        }
        return instance;
    });
    private JsonWebKeys jsonWebKeys;
    private String privateKeyId;
    private ContentAlgorithm contentAlgorithm;
    private KeyAlgorithm keyAlgorithm;
    private SignatureAlgorithm signatureAlgorithm;
    private String zipAlgorithm;
    private String metadataTemplate;
    private Boolean strictVerification;
    private SimpleDateFormat dateFormat;
    @Value("${ix.home}")
    private String ixHome;
    @Value("${application.host}")
    private String applicationHost;

    @PostConstruct
    public void init() {
        if (jsonWebKeys == null) {
            setJsonWebKeys(new HashMap<String, String>() {{ put("filename", "file:" + ixHome + "/keystore.jwks"); }});
        }
        if (privateKeyId != null && jsonWebKeys.getKeyIdMap().containsKey(privateKeyId)) {
            privateKeyId = null;
        }
        if (privateKeyId == null && jsonWebKeys.size() > 0) {
            privateKeyId = (String) jsonWebKeys.getKeys()
                    .stream()
                    .filter(k->(k.getKeyProperty(JsonWebKey.RSA_PRIVATE_EXP) != null || k.getKeyProperty(JsonWebKey.EC_PRIVATE_KEY) != null))
                    .findFirst()
                    .map(k->k.getKeyId())
                    .orElse(null);
        }
        if (contentAlgorithm == null) {
            contentAlgorithm = ContentAlgorithm.A256GCM;
        }
        if (keyAlgorithm == null) {
            keyAlgorithm = KeyAlgorithm.RSA_OAEP;
        }
        if (signatureAlgorithm == null) {
            signatureAlgorithm = SignatureAlgorithm.RS256;
        }
        if (zipAlgorithm == null) {
            zipAlgorithm = JoseConstants.JWE_DEFLATE_ZIP_ALGORITHM;
        }
        if (metadataTemplate == null) {
            metadataTemplate = "Exported on ${date} by ${user} from ${verified} source ${source} (SRS schema version:${version})";
        }
        if (strictVerification == null) {
            strictVerification = false;
        }
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
        log.debug("Keys: " + String.valueOf(jsonWebKeys.getKeyIdMap().keySet()) + " privateKeyId: " + privateKeyId);
    }

    public void setJsonWebKeys(Map<String, ?> jwksmap) {
        JsonWebKeys jwksobj = new JsonWebKeys();
        if (jwksmap.containsKey("filename")) {
            String filename = (String) jwksmap.get("filename");
            String password = (String) jwksmap.get("password");
            if (!filename.contains(":")) {
                filename = "file:" + filename;
            }
            try (InputStream is = new UrlResource(filename).getInputStream();) {
                if (password != null && !password.isEmpty()) {
                    jwksobj = JwkUtils.decryptJwkSet(is, password.toCharArray());
                } else {
                    jwksobj = JwkUtils.readJwkSet(is);
                }
            } catch (FileNotFoundException e) {
                log.error(e.toString());
                jwksobj = generateKeyStore(filename, applicationHost, password);
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
    }

    public JsonWebKey getPrivateKey() {
        if (privateKeyId != null) {
            return jsonWebKeys.getKey(privateKeyId);
        }
        return null;
    }

    public JsonWebKey getKey(String keyId) {
        if (keyId != null) {
            return jsonWebKeys.getKey(keyId);
        }
        return null;
    }

    public static JoseCryptoServiceConfiguration INSTANCE() {
        return _instanceSupplier.get();
    }

    private static JsonWebKeys generateKeyStore(String filename, String applicationHost, String password) {
        JsonWebKeys jwksobj = new JsonWebKeys();
        try {
            String kid = new URL(applicationHost).getHost();
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            JsonWebKey jwk = JwkUtils.fromRSAPrivateKey((RSAPrivateKey) pair.getPrivate(), null, kid);
            jwksobj.setKey(jwk);
            try (FileWriter fileWriter = new FileWriter(new UrlResource(filename).getFile())) {
                if (password != null && !password.isEmpty()) {
                    fileWriter.write(JwkUtils.encryptJwkSet(jwksobj, password.toCharArray()));
                } else {
                    fileWriter.write(JwkUtils.jwkSetToJson(jwksobj));
                }
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
        return jwksobj;
    }
}
