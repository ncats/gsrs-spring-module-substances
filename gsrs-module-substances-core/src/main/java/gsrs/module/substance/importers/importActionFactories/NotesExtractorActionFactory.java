package gsrs.module.substance.importers.importActionFactories;

import gsrs.dataexchange.model.MappingAction;
import gsrs.dataexchange.model.MappingActionFactoryMetadata;
import gsrs.dataexchange.model.MappingActionFactoryMetadataBuilder;
import gsrs.dataexchange.model.MappingParameter;
import gsrs.importer.PropertyBasedDataRecordContext;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.models.v1.Note;

import java.util.Map;

import static gsrs.module.substance.importers.SDFImportAdapterFactory.resolveParametersMap;

public class NotesExtractorActionFactory extends BaseActionFactory {
    private static MappingActionFactoryMetadata metadata;

    static {
        setupMetadata();
    }

    private static void setupMetadata() {
        MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadataBuilder();
        metadata = builder.setLabel("Create Note")
                .addParameterField(MappingParameter.builder()
                        .setFieldName("note")
                        .setValueType(String.class)
                        .setRequired(true)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldNameAndLabel("comment", "Comments ")
                        .setRequired(false)
                        .build())
                .build();
    }

    public MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> create(Map<String, Object> abstractParams) {
        return (sub, sdRec) -> {
            Map<String, Object> params = null;
            try {
                params = resolveParametersMap(sdRec, abstractParams);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Note n = new Note();
            n.note = (String) params.get("note");
            doBasicsImports(n, params);
            //TODO: more params
            sub.build().notes.add(n);
            return sub;
        };
    }

    @Override
    public MappingActionFactoryMetadata getMetadata() {
        return this.metadata;
    }
}
