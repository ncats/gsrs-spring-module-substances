package gsrs.module.substance.services;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.StaticContextAccessor;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
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
    private Boolean strictVerification;
    private Boolean preserveMetadata;
    private SimpleDateFormat dateFormat;

    @PostConstruct
    public void init() {
        if (jsonWebKeys == null) {
            setJsonWebKeys(new HashMap<String, String>() {{ put("filename", "keystore.jwks"); }});
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
        if (strictVerification == null) {
            strictVerification = false;
        }
        if (preserveMetadata == null) {
            preserveMetadata = true;
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
            if (!filename.contains(":")) {
                filename = "classpath:" + filename;
            }
            try (InputStream is = new UrlResource(filename).getInputStream();) {
                jwksobj = JwkUtils.readJwkSet(is);
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
        return jsonWebKeys.getKey(keyId);
    }

    public static JoseCryptoServiceConfiguration INSTANCE() {
        return _instanceSupplier.get();
    }
}
