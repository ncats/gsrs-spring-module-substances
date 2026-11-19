package gsrs.module.substance.importers;

import com.fasterxml.jackson.databind.JsonNode;

import gsrs.imports.ImportAdapter;
import gsrs.module.substance.importers.importActionFactories.SubstanceImportAdapterFactoryBase;
import gsrs.springUtils.AutowireHelper;

import ix.ginas.models.v1.Substance;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

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
