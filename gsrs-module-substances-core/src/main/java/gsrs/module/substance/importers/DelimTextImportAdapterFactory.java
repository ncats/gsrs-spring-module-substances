package gsrs.module.substance.importers;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.dataexchange.model.MappingAction;
import gsrs.dataexchange.model.MappingActionFactory;
import gsrs.imports.ImportAdapter;
import gsrs.imports.ImportAdapterFactory;
import gsrs.imports.ImportAdapterStatistics;
import gsrs.module.substance.importers.importActionFactories.SubstanceImportAdapterFactoryBase;
import gsrs.module.substance.importers.model.PropertyBasedDataRecordContext;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DelimTextImportAdapterFactory extends SubstanceImportAdapterFactoryBase {

    private String fieldDelimiter;

    private boolean trimQuotesFromInput = false;

    private String inputFileName;

    private Class holdingAreaService;

    private List<Class> entityServices;

    private Class entityServiceClass;

    @Override
    public String getAdapterName() {
        return "Delimited Text Adapter";
    }

    @Override
    public String getAdapterKey() {
        return "DelimitedText";
    }

    @Override
    public List<String> getSupportedFileExtensions() {
        return Arrays.asList("csv", "txt", "tsv");
    }

    @SneakyThrows
    @Override
    public ImportAdapter<Substance> createAdapter(JsonNode adapterSettings) {
        log.trace("starting in createAdapter. adapterSettings: " + adapterSettings.toPrettyString());
        List<MappingAction<Substance, PropertyBasedDataRecordContext>> actions = getMappingActions(adapterSettings);
        DelimTextImportAdapter importAdapter = new DelimTextImportAdapter (actions);
        return importAdapter;
    }

    @Override
    public ImportAdapterStatistics predictSettings(InputStream is) {
        return null;
    }

    @Override
    public void setFileName(String fileName) {
        inputFileName=fileName;
    }

    @Override
    public String getFileName() {
        return this.inputFileName;
    }

    @Override
    public Class getHoldingAreaService() {
        return this.holdingAreaService;
    }

    @Override
    public void setHoldingAreaService(Class holdingService) {
        this.holdingAreaService=holdingService;
    }

    @Override
    public Class getHoldingAreaEntityService() {
        return this.entityServiceClass;
    }

    @Override
    public void setHoldingAreaEntityService(Class holdingAreaEntityService) {
        this.entityServiceClass=holdingAreaEntityService;
    }

    @Override
    public List<Class> getEntityServices() {
        return this.entityServices;
    }

    @Override
    public void setEntityServices(List<Class> services) {
        this.entityServices=services;
    }

    @Override
    public Class getEntityServiceClass() {
        return null;
    }

    @Override
    public void setEntityServiceClass(Class newClass) {

    }

    public void setFieldDelimiter(String newDelimiter){
        this.fieldDelimiter=newDelimiter;
    }

    public String getFieldDelimiter() {
        return this.fieldDelimiter;
    }
}
