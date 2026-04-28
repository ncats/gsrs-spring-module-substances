package gsrs.module.substance.exporters;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.module.substance.exporters.profiles.ExporterProfile;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Factory for creating FHIR R4B SubstanceDefinition exporters.
 * 
 * Configuration is passed via setter injection from gsrs.exporter.factories config.
 * The profile is specified as a fully-qualified class name that implements ExporterProfile.
 * 
 * Follows the pattern of JmespathSpreadsheetExporterFactory.
 */
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class FhirExporterFactory implements ExporterFactory<Substance> {
    
    private OutputFormat format = new OutputFormat("fhir", "FHIR R4B SubstanceDefinition (gzipped JSON)");
    
    private String profile = "gsrs.module.substance.exporters.profiles.GsrsFhirExportProfile";
    
    /**
     * Set the output format via configuration injection.
     * Accepts a map with "extension" and "displayName" keys.
     * @param m configuration map
     */
    public void setFormat(Map<String, String> m) {
        this.format = new OutputFormat(m.get("extension"), m.get("displayName"));
    }
    
    /**
     * Set the profile class name via configuration injection.
     * The class must implement ExporterProfile.
     * @param profileClassName fully-qualified class name
     */
    public void setProfile(String profileClassName) {
        if (profileClassName == null || profileClassName.trim().isEmpty()) {
            throw new IllegalArgumentException("Profile class name cannot be null or empty");
        }
        this.profile = profileClassName;
        log.info("FHIR exporter profile set to: {}", profileClassName);
    }
    
    @Override
    public boolean supports(Parameters params) {
        if (params == null || params.getFormat() == null) {
            return false;
        }
        return format.getExtension().equals(params.getFormat().getExtension());
    }
    
    @Override
    public Set<OutputFormat> getSupportedFormats() {
        return Collections.singleton(format);
    }
    
    @Override
    public Exporter<Substance> createNewExporter(OutputStream out, Parameters params) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("Output stream cannot be null");
        }
        
        // Instantiate the profile from configured value
        ExporterProfile profileInstance = instantiateProfile(profile);
        
        log.info("Creating FHIR exporter with profile: {}", profile);
        
        return new FhirExporter(out, profileInstance);
    }
    
    /**
     * Instantiate a profile class by name.
     * @param profileClassName fully-qualified class name
     * @return instantiated profile
     * @throws RuntimeException if instantiation fails
     */
    private ExporterProfile instantiateProfile(String profileClassName) {
        try {
            Class<?> profileClass = Class.forName(profileClassName);
            if (!ExporterProfile.class.isAssignableFrom(profileClass)) {
                throw new IllegalArgumentException(
                    profileClassName + " does not implement ExporterProfile"
                );
            }
            return (ExporterProfile) profileClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("Failed to instantiate profile: {}", profileClassName, e);
            throw new RuntimeException("Failed to instantiate FHIR export profile: " + profileClassName, e);
        }
    }
    
    @Override
    public JsonNode getSchema() {
        return JsonNodeFactory.instance.objectNode();
    }
}
