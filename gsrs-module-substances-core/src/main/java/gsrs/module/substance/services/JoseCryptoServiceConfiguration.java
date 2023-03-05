package gsrs.module.substance.services;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.StaticContextAccessor;

import java.util.HashMap;
import java.util.Map;

import lombok.Setter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("gsrs.crypto")
public class JoseCryptoServiceConfiguration {
    private static CachedSupplier<JoseCryptoServiceConfiguration> _instanceSupplier = CachedSupplier.of(()->{
        JoseCryptoServiceConfiguration instance;
        try {
            instance = StaticContextAccessor.getBean(JoseCryptoServiceConfiguration.class);
        } catch (Exception e) {
            instance = new JoseCryptoServiceConfiguration();
        }
        return instance;
    });
    @Setter
    private Map<String, Object> jsonWebKeys;
    @Setter
    private String encryptionMethod;
    @Setter
    private String keyAlgorithm;
    @Setter
    private String curve;
    @Setter
    private String signatureAlgorithm;
    @Setter
    private String zipAlgorithm;
    @Setter
    private String metadataTemplate;
    @Setter
    private String dateFormat;
    @Setter
    private String privateKeyId;
    @Setter
    private Boolean strictVerification;
    @Value("${ix.home}")
    @Setter
    private String ixHome;
    @Value("${application.host}")
    @Setter
    private String applicationHost;

    public Map<String, Object> getJsonWebKeys() {
        return jsonWebKeys != null ? jsonWebKeys : new HashMap<String, Object>() {{ put("filename", "file:" + getIxHome() + "keystore.jwks"); }};
    }

    public String getPrivateKeyId() {
        return privateKeyId;
    }

    public String getEncryptionMethod() {
        return encryptionMethod != null ? encryptionMethod : "A256GCM";
    }

    public String getKeyAlgorithm() {
        return keyAlgorithm != null ? keyAlgorithm : "RSA-OAEP-256";
    }

    public String getCurve() {
        return curve != null ? curve : "P-256";
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm != null ? signatureAlgorithm : "RS256";
    }

    public String getZipAlgorithm() {
        return zipAlgorithm != null ? zipAlgorithm : "DEF";
    }

    public String getMetadataTemplate() {
        return metadataTemplate != null ? metadataTemplate : "Exported on ${date} by ${user} from ${verified} source ${source} (SRS schema version:${version})";
    }

    public String getDateFormat() {
        return dateFormat != null ? dateFormat : "yyyy-MM-dd HH:mm:ss";
    }

    public String getIxHome() {
        return ixHome != null ? ixHome + "/" : "";
    }

    public String getApplicationHost() {
        return applicationHost != null ? applicationHost : "http://localhost:8080";
    }

    public boolean getStrictVerification() {
        return strictVerification != null ? strictVerification.booleanValue() : false;
    }

    public static JoseCryptoServiceConfiguration INSTANCE() {
        return _instanceSupplier.get();
    }
}
