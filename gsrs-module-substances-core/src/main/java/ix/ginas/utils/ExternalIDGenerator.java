package ix.ginas.utils;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.ginas.models.v1.Substance;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("ix.ginas.approval-id-generator")
@Data
@Slf4j
public class ExternalIDGenerator implements SubstanceApprovalIdGenerator{
    public static final String PARAMETER_ERROR_MESSAGE = 
        "ExternalIDGenerator requires configuration properties in ix.ginas.approvalIdGenerator.externalIDGenerator";

    public static final String GENERATE_ERROR_MESSAGE = 
        "There was a problem getting a valid approvalID from remote service. Status code was: %s";

    public static final String VALIDATE_ERROR_MESSAGE = 
        "There was a problem validating approvalID from remote service. Status code was: %s";

    private Map<String, Object> externalIDGenerator;

    private String generateUrl;
    private String validateUrl;
    private String accesstoken;

    private HttpClient httpClient;

    @Autowired
    private SubstanceRepository substanceRepository;

    @PostConstruct
    private void initialize() {
        if(externalIDGenerator == null){
            throw new RuntimeException(PARAMETER_ERROR_MESSAGE);
        }
        generateUrl = externalIDGenerator.get("generateUrl").toString();
        validateUrl = externalIDGenerator.get("validateUrl").toString();
        accesstoken = externalIDGenerator.get("accesstoken").toString();
        httpClient = HttpClient.newHttpClient();
    }

	public String getName(){
		return this.getClass().getSimpleName();
	}

	public String generateId(Substance obj) {      
        String id = "";
        try {
            HttpResponse<String> response = httpClient.send(CreateGenerateHttpRequest(), HttpResponse.BodyHandlers.ofString());

            id = response.body();
            if(response.statusCode() != 200 || id.isBlank() || !isValidId(id)){
				throw new IOException(
                    String.format(GENERATE_ERROR_MESSAGE, response.statusCode())
                    );
			}
        } 
        catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
        return id;
	}
               
	public boolean isValidId(String id){
        boolean isValid = false;
        try {
            HttpResponse<String> response = httpClient.send(CreateValidateHttpRequest(id), HttpResponse.BodyHandlers.ofString());

            isValid = Boolean.parseBoolean(response.body());
            if(response.statusCode() != 200){
				throw new IOException(
                    String.format(VALIDATE_ERROR_MESSAGE, response.statusCode())
                    );
			}
        } 
        catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
        return isValid && !substanceRepository.existsByApprovalID(id);
	}

    private HttpRequest CreateGenerateHttpRequest(){
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(generateUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.noBody());
        
        if(accesstoken != null && !accesstoken.isBlank()){
            requestBuilder = requestBuilder.setHeader("Authorization", "Bearer " + accesstoken);
        }

       return requestBuilder.build();
	}

    private HttpRequest CreateValidateHttpRequest(String id){
        String requestBody = String.format("{\"ApprovalId\": \"%s\"}", id);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(validateUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody));
        
        if(accesstoken != null && !accesstoken.isBlank()){
            requestBuilder = requestBuilder.setHeader("Authorization", "Bearer " + accesstoken);
        }

       return requestBuilder.build();
	}
}