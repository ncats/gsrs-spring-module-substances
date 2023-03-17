package gsrs.module.substance.importers;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.imports.ImportAdapter;
import gsrs.module.substance.importers.importActionFactories.SubstanceImportAdapterFactoryBase;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.List;


@Slf4j
public class GSRSJSONImportAdapterFactory extends SubstanceImportAdapterFactoryBase {

    private String description = "Importer for the GSRS-specific JSON file used by GSRS since version 2";

    protected Class stagingAreaService;

    @Override
    public String getAdapterName() {
        return "GSRS JSON Adapter";
    }

    @Override
    public String getAdapterKey() {
        return "GSRSJSON";
    }

    @Override
    public List<String> getSupportedFileExtensions() {
        return this.extensions;
    }

    @Override
    public void setSupportedFileExtensions(List<String> extensions) {
        this.extensions = extensions;
    }

    private List<String> extensions = Arrays.asList("gz", "gsrs");

    @Override
    public ImportAdapter<Substance> createAdapter(JsonNode adapterSettings) {
        log.trace("starting in createAdapter. adapterSettings: " + adapterSettings.toPrettyString());
        GSRSJSONImportAdapter importAdapter = new GSRSJSONImportAdapter();
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
