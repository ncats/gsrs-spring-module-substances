package fda.gsrs.substance.exporters;

import ix.ginas.exporters.OutputFormat;
import ix.ginas.exporters.Exporter;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Set;

public interface FDAExporterFactory {
    /**
     * Configuration Parameters to tell the factory
     * what export options to use.
     */
    interface Parameters{

        OutputFormat getFormat();

        default boolean shouldCompress(){
            return false;
        }

        default boolean publicOnly() { return false;}
        
        default String source() { return null; }
    }

    /**
     * Can This factory make an Exporter that meets
     * these Parameter requirements.
     * @param params the {@link Parameters} to consider.
     * @return {@code true} if it does support those parameters;
     *      {@code false otherwise}.
     */
    boolean supports(Parameters params);

    /**
     * Get all the {@link OutputFormat}s that this factory
     * can support.
     * @return a Set of {@link OutputFormat}s; should never be null,
     * but could be empty.
     */
    Set<OutputFormat> getSupportedFormats();

    /**
     * Create a new {@link Exporter} using the given {@link Parameters} that will
     * write the export data to the given {@link OutputStream}.
     *
     * @param out the {@link OutputStream} to write to.
     * @param params the {@link Parameters} configuration to tune the Exporter.  These {@link Parameters}
     *               should always be supported.
     *
     * @return a new Exporter; should never be null.
     *
     * @throws IOException if there is a problem creating the Exporter.
     *
     * @see #supports(Parameters)
     */
    <T> Exporter<T> createNewExporter(OutputStream out, Parameters params) throws IOException;

}