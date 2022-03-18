package gsrs.module.substance.importers.importActionFactories;

import gsrs.module.substance.importers.MappingActionFactoryMetadata;
import gsrs.module.substance.importers.MappingParameter;
import gsrs.module.substance.importers.SDFImportAdaptorFactory;
import gsrs.module.substance.importers.actions.ImportMappingAction;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.Note;
import ix.ginas.models.v1.Substance;

import java.util.Map;

import static gsrs.module.substance.importers.SDFImportAdaptorFactory.resolveParametersMap;

public class NotesExtractorActionFactory extends BaseActionFactory {
    private static MappingActionFactoryMetadata metadata;

    static {
        setupMetadata();
    }

    private static void setupMetadata() {
        MappingActionFactoryMetadata.MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadata.MappingActionFactoryMetadataBuilder();
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

    public ImportMappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) {
        return (sub, sdRec) -> {
            Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
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
