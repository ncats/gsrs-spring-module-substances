package gsrs.module.substance.importers.importActionFactories;

import gsrs.dataexchange.model.MappingAction;
import gsrs.dataexchange.model.MappingActionFactoryMetadata;
import gsrs.dataexchange.model.MappingActionFactoryMetadataBuilder;
import gsrs.dataexchange.model.MappingParameter;
import gsrs.module.substance.importers.model.PropertyBasedDataRecordContext;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.Note;
import ix.ginas.models.v1.Substance;

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

    public MappingAction<Substance, PropertyBasedDataRecordContext> create(Map<String, Object> abstractParams) {
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
            sub.notes.add(n);
            return sub;
        };
    }

    @Override
    public MappingActionFactoryMetadata getMetadata() {
        return this.metadata;
    }
}