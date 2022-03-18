package gsrs.module.substance.importers.importActionFactories;

import gsrs.module.substance.importers.MappingActionFactoryMetadata;
import gsrs.module.substance.importers.MappingParameter;
import gsrs.module.substance.importers.actions.ImportMappingAction;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;

import java.util.Map;

import static gsrs.module.substance.importers.SDFImportAdaptorFactory.resolveParametersMap;

public class CodeExtractorActionFactory extends BaseActionFactory {
    public ImportMappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) {
        return (sub, sdRec) -> {
            Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
            Code c = new Code((String) params.get("codeSystem"), (String) params.get("code"));
            c.type = (String) params.get("codeType");
            doBasicsImports(c, params);
            //TODO: more params
            sub.addCode(c);
            return sub;
        };
    }

    @Override
    public MappingActionFactoryMetadata getMetadata() {
        MappingActionFactoryMetadata.MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadata.MappingActionFactoryMetadataBuilder();
        return builder.setLabel("Create Code")
                .addParameterField(MappingParameter.builder()
                        .setFieldName("codeValue")
                        .setValueType(String.class)
                        .setRequired(true).build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("codeSystem")
                        .setValueType(String.class)
                        .setRequired(true)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("codeType")
                        .setValueType(String.class)
                        .setDefaultValue("PRIMARY")
                        .build())
                .build();
    }


}