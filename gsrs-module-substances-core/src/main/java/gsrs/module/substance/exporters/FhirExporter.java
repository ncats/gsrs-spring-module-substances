package gsrs.module.substance.exporters;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gsrs.module.substance.exporters.profiles.ExporterProfile;
import ix.ginas.exporters.Exporter;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * FHIR R4B SubstanceDefinition Exporter.
 * 
 * Accumulates Substance objects as FHIR SubstanceDefinition resources and
 * exports them as a gzipped FHIR Collection Bundle in JSON format.
 * 
 * Each substance is mapped according to the export profile's rules.
 */
@Slf4j
public class FhirExporter implements Exporter<Substance> {
    
    private final List<Bundle.BundleEntryComponent> entries;
    private final IParser fhirJsonParser;
    private final GZIPOutputStream gzipOut;
    private final ExporterProfile profile;
    private final FhirContext fhirContext;
    
    private int totalCount = 0;
    private boolean closed = false;
    
    /**
     * Create a new FHIR exporter
     * @param out the output stream
     * @param profile the export profile to use
     * @throws IOException if the GZIP output stream cannot be created
     */
    public FhirExporter(OutputStream out, ExporterProfile profile) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("Output stream cannot be null");
        }
        if (profile == null) {
            throw new IllegalArgumentException("Export profile cannot be null");
        }
        
        this.profile = profile;
        this.entries = new ArrayList<>();
        this.fhirContext = FhirContext.forR4();
        this.fhirJsonParser = fhirContext.newJsonParser();
        this.gzipOut = new GZIPOutputStream(out);
    }
    
    @Override
    public void export(Substance substance) throws IOException {
        if (closed) {
            throw new IllegalStateException("Exporter is closed");
        }
        
        if (substance == null) {
            log.warn("Null substance provided to exporter, skipping");
            return;
        }
        
        try {
            // Map substance to FHIR SubstanceDefinition using profile
            SubstanceDefinition sd = profile.mapSubstanceToFhir(substance);
            
            if (sd != null) {
                // Add to bundle
                Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
                entry.setFullUrl("SubstanceDefinition/" + sd.getId());
                entry.setResource(sd);
                entries.add(entry);
                totalCount++;
                
                if (totalCount % 100 == 0) {
                    log.debug("Exported {} substances", totalCount);
                }
            } else {
                log.warn("Failed to map substance {} to FHIR", substance.getUuid());
            }
        } catch (Exception e) {
            log.error("Error exporting substance {}: {}", substance.getUuid(), e.getMessage(), e);
        }
    }
    
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        
        try {
            // Create FHIR Collection Bundle
            Bundle bundle = createBundle();
            
            // Write bundle as gzipped JSON
            String bundleJson = fhirJsonParser.encodeResourceToString(bundle);
            
            // Write to GZIP stream
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(gzipOut))) {
                writer.write(bundleJson);
            }
            
            log.info("FHIR export completed: {} substances exported to gzipped bundle", totalCount);
        } catch (Exception e) {
            log.error("Error closing FHIR exporter: {}", e.getMessage(), e);
            throw new IOException("Error writing FHIR bundle", e);
        } finally {
            gzipOut.close();
            closed = true;
        }
    }
    
    /**
     * Create a FHIR Collection Bundle containing all exported substances
     * @return FHIR Bundle
     */
    private Bundle createBundle() {
        Bundle bundle = new Bundle();
        
        // Set bundle metadata
        bundle.setId(UUID.randomUUID().toString());
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTimestamp(new Date());
        
        // Set total count
        bundle.setTotal(totalCount);
        
        // Add all entries
        bundle.setEntry(entries);
        
        return bundle;
    }
    
    /**
     * Get the number of substances exported
     * @return export count
     */
    public int getExportCount() {
        return totalCount;
    }
    
    /**
     * Check if the exporter is closed
     * @return true if closed
     */
    public boolean isClosed() {
        return closed;
    }
}
