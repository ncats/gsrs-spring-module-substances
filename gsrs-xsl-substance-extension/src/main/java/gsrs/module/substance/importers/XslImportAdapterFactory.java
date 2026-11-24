package gsrs.module.substance.importers;

import com.fasterxml.jackson.databind.JsonNode;

import gsrs.imports.ImportAdapter;
import gsrs.module.substance.utils.XslParameterUtils;
import gsrs.module.substance.importers.importActionFactories.SubstanceImportAdapterFactoryBase;
import gsrs.springUtils.AutowireHelper;

import ix.ginas.models.v1.Substance;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating {@link XslImportAdapter} instances.
 *
 * <p>This factory is configured with the XSL template file path and optional
 * input format (JSON or XML). The template transforms the incoming file into
 * GSRS-formatted JSON substance records which are then parsed and imported.</p>
 *
 * @author Egor Puzanov
 */
@Slf4j
public class XslImportAdapterFactory extends SubstanceImportAdapterFactoryBase {

    private String description = "Importer that uses an XSL 3.0 template to convert XML or JSON into GSRS JSON";
    private String templateFile = "";
    private String inputFormat = "JSON";
    private String header = "";
    private String footer = "";
    private String delimiter = "";
    private Map<String, String> xslParameters = new LinkedHashMap<>();
    private Map<String, String> xslNamespaces = new LinkedHashMap<>();
    private Map<String, String> codeSystemMappings = new LinkedHashMap<>();
    private List<String> extensions = Arrays.asList("xml", "json");

    protected Class stagingAreaService;

    @Override
    public String getAdapterName() {
        return "XSL Import Adapter";
    }

    @Override
    public String getAdapterKey() {
        return "XSLIMPORT";
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTemplateFile() {
        return templateFile;
    }

    public void setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
    }

    public String getInputFormat() {
        return inputFormat;
    }

    public void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public String getDelimiter() {
        return delimiter;
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

    /**
     * Sets the code system name remapping applied to the imported records.
     *
     * <p>Each entry maps a code system name found in the input to the name it
     * must have after import. For example {@code {"UNII": "FDA UNII"}} rewrites
     * every code whose code system is {@code UNII} to {@code FDA UNII}.</p>
     *
     * @param codeSystemMappings mapping of input code system names to their replacements
     */
    public void setCodeSystemMappings(Map<String, String> codeSystemMappings) {
        this.codeSystemMappings = codeSystemMappings;
    }

    public Map<String, String> getCodeSystemMappings() {
        return codeSystemMappings;
    }

    @Override
    public List<String> getSupportedFileExtensions() {
        return this.extensions;
    }

    @Override
    public void setSupportedFileExtensions(List<String> extensions) {
        this.extensions = extensions;
    }

    @Override
    public ImportAdapter<Substance> createAdapter(JsonNode adapterSettings) {
        log.trace("starting in createAdapter. adapterSettings: {}", adapterSettings.toPrettyString());
        XslImportAdapter importAdapter = new XslImportAdapter();
        importAdapter.setTemplateFile(templateFile);
        importAdapter.setInputFormat(inputFormat);
        importAdapter.setHeader(header);
        importAdapter.setFooter(footer);
        importAdapter.setDelimiter(delimiter);
        importAdapter.setXslParameters(XslParameterUtils.merge(
            xslParameters,
            XslParameterUtils.fromSettings(adapterSettings)));
        importAdapter.setXslNamespaces(XslParameterUtils.merge(
            xslNamespaces,
            XslParameterUtils.namespacesFromSettings(adapterSettings)));
        importAdapter.setCodeSystemMappings(codeSystemMappings);
        AutowireHelper.getInstance().autowire(importAdapter);
        return importAdapter;
    }

    @Override
    public Class getStagingAreaService() {
        return this.stagingAreaService;
    }

    @Override
    public void setStagingAreaService(Class stagingService) {
        this.stagingAreaService = stagingService;
    }
}
