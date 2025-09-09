package gsrs.module.substance.exporters;

import com.fasterxml.jackson.databind.ObjectWriter;
import ix.core.controllers.EntityFactory;
import ix.core.validator.ValidationResponse;
import ix.ginas.exporters.Exporter;
import ix.ginas.models.v1.Substance;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Egor Puzanov on 10/18/22.
 */
@Slf4j
public class GsrsApiExporter implements Exporter<Substance> {

    private final HttpHeaders headers;
    private final BufferedWriter out;
    private final RestTemplate restTemplate;
    private final boolean allowedExport;
    private final boolean validate;
    private final String newAuditor;
    private final String changeReason;
    private final ObjectWriter writer = EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().writer();
    private final Pattern AUDIT_PAT = Pattern.compile("edBy\":\"[^\"]*\"");

    public GsrsApiExporter(OutputStream out, RestTemplate restTemplate, Map<String, String> headers, boolean allowedExport,  boolean validate, String newAuditor, String changeReason) throws IOException {
        Objects.requireNonNull(out);
        this.out = new BufferedWriter(new OutputStreamWriter(out));
        Objects.requireNonNull(restTemplate);
        this.restTemplate = restTemplate;
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            h.set(entry.getKey(), entry.getValue());
        }
        this.headers = h;
        this.allowedExport = allowedExport;
        this.validate = validate;
        this.newAuditor = newAuditor != null ? "edBy\":\"" + newAuditor + "\"" : null;
        this.changeReason = changeReason;
        log.debug("BaseUrl: " + restTemplate.getUriTemplateHandler().expand("/") + " Headers: " + h.toString());
    }

    private HttpEntity<String> makeRequest(Substance obj) throws Exception {
        String jsonStr = writer.writeValueAsString(obj);
        if (newAuditor != null) {
            jsonStr = AUDIT_PAT.matcher(jsonStr).replaceAll(newAuditor);
        }
        HttpEntity<String> request = new HttpEntity<String>(jsonStr, headers);
        if (validate) {
            @SuppressWarnings("unchecked")
            ValidationResponse<Substance> vr = restTemplate.postForObject("/@validate", request, ValidationResponse.class);
            if (vr.getValidationMessages().isEmpty()) throw new Exception("GSRS API not found");
            if (vr.hasError()) throw new Exception(vr.toString());
        }
        return request;
    }

    @Override
    public void export(Substance obj) throws IOException {
        HttpEntity<String> request;
        Date date = new Date();
        try {
            if (!allowedExport) {
                throw new Exception("Export denied");
            }
            if (changeReason != null && obj.version != null) {
                obj.changeReason = changeReason.replace("{{version}}", obj.version).replace("{{changeReason}}", obj.changeReason != null ? obj.changeReason : "").trim();
            }
            try {
                obj.version = restTemplate.getForObject("/{uuid}/version", String.class, obj.getUuid().toString()).replace("\"", "");
                request = makeRequest(obj);
                restTemplate.put("/", request);
            } catch (HttpClientErrorException.NotFound ex) {
                if (!ex.getMessage().contains("\"status\":404")) {
                    throw new Exception(ex.getMessage());
                }
                obj.version = "1";
                String approvalID = obj.getApprovalID();
                if (approvalID != null) {
                    obj.approvalID = null;
                }
                request = makeRequest(obj);
                Substance newObj = restTemplate.postForObject("/", request, Substance.class);
                if (newObj.getUuid() == null) throw new Exception("GSRS API not found");
                if (approvalID != null) {
                    obj.approvalID = approvalID;
                    request = makeRequest(obj);
                    restTemplate.put("/", request);
                }
            }
            out.write(String.format("%tF %tT Substance: %s %s - SUCCESS", date, date, obj.getUuid().toString(), obj.getName()));
        } catch (Exception ex) {
            out.write(String.format("%tF %tT Substance: %s %s - ERROR: %s", date, date, obj.getUuid().toString(), obj.getName(), ex.getMessage()));
        }
        out.newLine();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
