package gsrs.module.substance.importers.importActionFactories;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.dataexchange.model.MappingAction;
import gsrs.dataexchange.model.MappingActionFactory;
import gsrs.imports.ActionConfigImpl;
import gsrs.imports.ImportAdapter;
import gsrs.imports.ImportAdapterFactory;
import gsrs.imports.ImportAdapterStatistics;
import gsrs.module.substance.importers.model.PropertyBasedDataRecordContext;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SubstanceImportAdapterFactoryBase implements ImportAdapterFactory<AbstractSubstanceBuilder> {

    public final static String SIMPLE_REFERENCE_ACTION = "public_reference";

    protected ImportAdapterStatistics statistics;

    protected Map<String, MappingActionFactory<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> registry = new ConcurrentHashMap<>();

    protected List<ActionConfigImpl> fileImportActions;

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
    public ImportAdapter<AbstractSubstanceBuilder> createAdapter(JsonNode adapterSettings) {
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

    public List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> getMappingActions(JsonNode adapterSettings) throws Exception {
        List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> actions = new ArrayList<>();
        adapterSettings.get("actions").forEach(js -> {
            String actionName = js.get("actionName").asText();
            JsonNode actionParameters = js.get("actionParameters");
            ObjectMapper mapper = new ObjectMapper();
            log.trace("about to call convertValue");
            Map<String, Object> params = mapper.convertValue(actionParameters, new TypeReference<Map<String, Object>>() {});
            log.trace("Finished call to convertValue");
            MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> action = null;
            try {
                log.trace("looking for action {}; registry size: {}", actionName, registry.size());
                MappingActionFactory<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> mappingActionFactory = registry.get(actionName);
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

    protected void defaultInitialize(){}


    public void setFileImportActions(List<ActionConfigImpl> fileImportActions) {
        log.trace("setFileImportActions");
        this.fileImportActions = fileImportActions;
    }

    @Override
    public void initialize(){
        log.trace("fileImportActions: " + fileImportActions);
        registry.clear();
        if(fileImportActions !=null && fileImportActions.size() >0) {
            log.trace("config-specific init");
            ObjectMapper mapper = new ObjectMapper();
            fileImportActions.forEach(fia->{
                String actionName =fia.getActionName();
                Class actionClass =fia.getActionClass();
                MappingActionFactory<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> mappingActionFactory;
                try {
                    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    mappingActionFactory = (MappingActionFactory<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>) mapper.convertValue(fia,
                            actionClass);

                    if(!fia.getParameters().isEmpty()) {
                        mappingActionFactory.setParameters(fia.getParameters());
                        mappingActionFactory.implementParameters();
                    }
                } catch (Exception e) {
                    log.error("Error parsing parameter metadata", e);
                    throw new RuntimeException(e);
                }
                registry.put(actionName, mappingActionFactory);
            });
        }
        else {
            defaultInitialize();
        }
    }

}