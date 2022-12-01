package gsrs.module.substance.importers.importActionFactories;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.dataexchange.model.MappingAction;
import gsrs.dataexchange.model.MappingActionFactory;
import gsrs.imports.ImportAdapter;
import gsrs.imports.ImportAdapterFactory;
import gsrs.imports.ImportAdapterStatistics;
import gsrs.module.substance.importers.model.PropertyBasedDataRecordContext;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SubstanceImportAdapterFactoryBase implements ImportAdapterFactory<Substance> {

    protected ImportAdapterStatistics statistics;

    protected Map<String, MappingActionFactory<Substance, PropertyBasedDataRecordContext>> registry = new ConcurrentHashMap<>();

    @Override
    public String getAdapterName() {
        return null;
    }

    @Override
    public String getAdapterKey() {
        return null;
    }

    @Override
    public List<String> getSupportedFileExtensions() {
        return null;
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

    public List<MappingAction<Substance, PropertyBasedDataRecordContext>> getMappingActions(JsonNode adapterSettings) throws Exception {
        List<MappingAction<Substance, PropertyBasedDataRecordContext>> actions = new ArrayList<>();
        adapterSettings.get("actions").forEach(js -> {
            String actionName = js.get("actionName").asText();
            JsonNode actionParameters = js.get("actionParameters");
            ObjectMapper mapper = new ObjectMapper();
            log.trace("about to call convertValue");
            Map<String, Object> params = mapper.convertValue(actionParameters, new TypeReference<Map<String, Object>>() {
            });
            log.trace("Finished call to convertValue");
            MappingAction<Substance, PropertyBasedDataRecordContext> action = null;
            try {
                log.trace("looking for action {}; registry size: {}", actionName, registry.size());
                MappingActionFactory<Substance, PropertyBasedDataRecordContext> mappingActionFactory = registry.get(actionName);
                if (mappingActionFactory instanceof BaseActionFactory && statistics != null) {
                    ((BaseActionFactory) mappingActionFactory).setAdapterSchema(statistics.getAdapterSchema());
                    log.trace("called setAdapterSchema");
                }
                log.trace("mappingActionFactory: " + mappingActionFactory);
                if (mappingActionFactory != null) {
                    action = mappingActionFactory.create(params);
                    actions.add(action);
                } else {
                    log.error("No action found for " + actionName);
                }
            } catch (Exception ex) {
                log.error("Error in getMappingActions: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        return actions;
    }

}