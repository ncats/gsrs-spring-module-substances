package gsrs.module.substance.importers.importActionFactories;

import gsrs.dataexchange.model.*;
import gsrs.importer.PropertyBasedDataRecordContext;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class NoOpActionFactory extends BaseActionFactory {

    @Override
    public MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> create(Map<String, Object> params) throws Exception {
        log.trace("in create");
        return (sub, sdRec) -> sub;
    }

    @Override
    public MappingActionFactoryMetadata getMetadata() {
        MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadataBuilder();
        return builder.setLabel("Ignore Field")
                .addParameterField(MappingParameter.builder()
                        .setFieldName("fieldName")
                        .setValueType(String.class)
                        .setRequired(true).build())
                .build();
    }

    @Override
    public Map<String, Object> getParameters() {
        return null;
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
    }

    @Override
    public void implementParameters() {
    }
}
