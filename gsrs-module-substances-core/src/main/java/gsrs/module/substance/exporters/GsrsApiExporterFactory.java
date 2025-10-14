package gsrs.module.substance.exporters;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gsrs.repository.UserProfileRepository;
import gsrs.services.PrivilegeService;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Substance;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;


/**
 * Created by Egor Puzanov on 10/18/22.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GsrsApiExporterFactory implements ExporterFactory<Substance> {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PrivilegeService privilegeService;

    private OutputFormat format = new OutputFormat("gsrsapi", "Send to ...");
    private int timeout = 120000;
    private boolean trustAllCerts = false;
    private boolean validate = true;
    private Role allowedRole = null;
    private String newAuditor = null;
    private String changeReason = null;
    private String baseUrl = "http://localhost:8080/api/v1/substances";
    private Map<String, String> headers = new HashMap<String, String>();
    private final TrustManager[] trustAllCertificates = new TrustManager[]{
        new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(
               X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(
               X509Certificate[] certs, String authType) {
           }
        }
    };

    private Map<String, String> getHeaders(UserProfile profile) {
        Map<String, String> userHeaders = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String value = entry.getValue();
            switch(value) {
                case "{{user.name}}":
                    value = profile.getIdentifier();
                    break;
                case "{{user.email}}":
                    value = profile.user.email;
                    break;
                case "{{user.apikey}}":
                    value = profile.getKey();
                    break;
                default:
                    break;
            }
            userHeaders.put(entry.getKey(), value);
        }
        return userHeaders;
    }

    public void setFormat(Map<String, String> m) {
        this.format = new OutputFormat(m.get("extension"), m.get("displayName"));
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout.intValue();
    }

    public void setTrustAllCerts(boolean trustAllCerts) {
        this.trustAllCerts = trustAllCerts;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setAllowedRole(String allowedRole) {
        this.allowedRole = Role.of(allowedRole);
    }

    public void setNewAuditor(String newAuditor) {
        this.newAuditor = newAuditor;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    @Override
    public boolean supports(Parameters params) {
        return params.getFormat().equals(format);
    }

    @Override
    public Set<OutputFormat> getSupportedFormats() {
        return Collections.singleton(format);
    }

    @Override
    public Exporter<Substance> createNewExporter(OutputStream out, Parameters params) throws IOException {
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(timeout)
            .build();
        HttpClientBuilder client = HttpClients.custom()
            .useSystemProperties()
            .setDefaultRequestConfig(requestConfig);
        if (trustAllCerts) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCertificates, new SecureRandom());
                SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
                client = client.setSSLSocketFactory(connectionFactory);
            } catch (Exception ex) {
            }
        }
        ClientHttpRequestFactory clientFactory = new HttpComponentsClientHttpRequestFactory(client.build());
        RestTemplate restTemplate = new RestTemplate(clientFactory);
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(baseUrl));
        UserProfile profile = userProfileRepository.findByUser_UsernameIgnoreCase(params.getUsername());
        //boolean allowedExport = (allowedRole == null || profile.hasRole(allowedRole)) ? true : false;
        boolean allowedExport = privilegeService.canDo("Export Data");
        return new GsrsApiExporter(out, restTemplate, getHeaders(profile), allowedExport, validate, newAuditor, changeReason);
    }

    @Override
    public JsonNode getSchema() {
        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        return parameters;
    }
}
