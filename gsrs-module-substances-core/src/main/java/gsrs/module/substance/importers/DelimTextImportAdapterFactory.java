package gsrs.module.substance.importers;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.imports.ImportAdapter;
import gsrs.imports.ImportAdapterFactory;
import gsrs.imports.ImportAdapterStatistics;
import ix.ginas.models.v1.Substance;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class DelimTextImportAdapterFactory implements ImportAdapterFactory<Substance> {

    private String fieldDelimiter;

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

    @Override
    public ImportAdapter<Substance> createAdapter(JsonNode adapterSettings) {

        return null;
    }

    @Override
    public ImportAdapterStatistics predictSettings(InputStream is) {
        return null;
    }

    @Override
    public void setFileName(String fileName) {

    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public Class getHoldingAreaService() {
        return null;
    }

    @Override
    public void setHoldingAreaService(Class holdingService) {

    }

    @Override
    public Class getHoldingAreaEntityService() {
        return null;
    }

    @Override
    public void setHoldingAreaEntityService(Class holdingAreaEntityService) {

    }

    @Override
    public List<Class> getEntityServices() {
        return null;
    }

    @Override
    public void setEntityServices(List<Class> services) {

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
