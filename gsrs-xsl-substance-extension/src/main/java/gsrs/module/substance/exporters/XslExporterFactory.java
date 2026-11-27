package gsrs.module.substance.exporters;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gsrs.module.substance.utils.LinkingIDUpdater;
import gsrs.module.substance.utils.XslParameterUtils;

import ix.ginas.exporters.*;
import ix.ginas.models.v1.Substance;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/**
 * Created by Egor Puzanov.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class XslExporterFactory implements ExporterFactory<Substance> {

    private OutputFormat format = new OutputFormat("gsrsp", "Json Portable Export (gsrsp) File");
    private String templateFile = "";
    private String header = "";
    private String footer = "";
    private String delimiter = "";
    private Map<String, String> xslParameters = new LinkedHashMap<>();
    private Map<String, String> xslNamespaces = new LinkedHashMap<>();
    private boolean shouldCompress = false;
    private String linkingIdCodeSystem = "";

    public void setFormat(Map<String, String> m) {
        this.format = new OutputFormat(m.get("extension"), m.get("displayName"));
    }

    public void setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Sets the additional parameters passed to the XSL template on every record.
     *
     * @param xslParameters map of template parameter names to their values
     */
    public void setXslParameters(Map<String, String> xslParameters) {
        this.xslParameters = xslParameters;
    }

    public Map<String, String> getXslParameters() {
        return xslParameters;
    }

    /**
     * Sets the namespace prefix mappings used to resolve prefixed parameter names.
     *
     * @param xslNamespaces mapping of namespace prefixes to namespace URIs
     */
    public void setXslNamespaces(Map<String, String> xslNamespaces) {
        this.xslNamespaces = xslNamespaces;
    }

    public Map<String, String> getXslNamespaces() {
        return xslNamespaces;
    }

    public void setShouldCompress(boolean shouldCompress) {
        this.shouldCompress = shouldCompress;
    }

    /**
     * Sets the code system used to build the {@code linkingID} values of the
     * exported substance references.
     *
     * <p>When the value is a code system name, every reference's
     * {@code linkingID} is written as {@code <codeSystem>:<code>}, where the
     * code belongs to the referenced substance. An empty value leaves the
     * identifiers GSRS derived untouched.</p>
     *
     * @param linkingIdCodeSystem the code system name, may be {@code null}
     */
    public void setLinkingIdCodeSystem(String linkingIdCodeSystem) {
        this.linkingIdCodeSystem = linkingIdCodeSystem;
    }

    public String getLinkingIdCodeSystem() {
        return linkingIdCodeSystem;
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
        JsonNode detailedParameters = params == null ? null : params.detailedParameters();
        Map<String, String> parameters = XslParameterUtils.merge(
            xslParameters,
            XslParameterUtils.fromJson(detailedParameters, XslParameterUtils.PARAMETERS_PROPERTY));
        Map<String, String> namespaces = XslParameterUtils.merge(
            xslNamespaces,
            XslParameterUtils.namespacesFromJson(detailedParameters));
        // linkingID handling lives entirely in LinkingIDUpdater; drop this line
        // to remove the feature when GSRS resolves linkingID natively.
        LinkingIDUpdater linkingIDUpdater = LinkingIDUpdater.create(
            linkingIdCodeSystem, detailedParameters);
        if(shouldCompress) {
            try {
                return new XslExporter(new GZIPOutputStream(out), templateFile, header, footer, delimiter,
                    parameters, namespaces, linkingIDUpdater);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        try {
            return new XslExporter(out, templateFile, header, footer, delimiter, parameters, namespaces,
                linkingIDUpdater);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Publishes the exporter parameters, including the {@code linkingIdCodeSystem}
     * value that {@link LinkingIDUpdater} reads.
     */
    @Override
    public JsonNode getSchema() {
        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        ObjectNode xslParameters = parameters.putObject(XslParameterUtils.PARAMETERS_PROPERTY);
        xslParameters.put("type", "object")
            .put("title", "XSL Parameters")
            .put("description", "Additional parameters passed to the XSL template");
        xslParameters.putObject("additionalProperties").put("type", "string");
        ObjectNode namespaces = parameters.putObject(XslParameterUtils.NAMESPACES_PROPERTY);
        namespaces.put("type", "object")
            .put("title", "XSL Parameter Namespaces")
            .put("description", "Namespace prefixes used by the XSL parameter names");
        namespaces.putObject("additionalProperties").put("type", "string");
        parameters.putObject(LinkingIDUpdater.PARAMETER_PROPERTY)
            .put("type", "string")
            .put("title", "Linking ID Code System")
            .put("description", "Code system whose codes are written to the linkingID of the exported "
                + "substance references as <codeSystem>:<code>. Leave empty to keep the identifiers GSRS derives.");
        return generateSchemaNode(getClass().getSimpleName(), parameters);
    }
}
