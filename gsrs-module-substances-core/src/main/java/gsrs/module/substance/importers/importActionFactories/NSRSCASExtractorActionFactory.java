package gsrs.module.substance.importers.importActionFactories;

import gsrs.dataExchange.model.MappingAction;
import gsrs.dataExchange.model.MappingActionFactoryMetadata;
import gsrs.dataExchange.model.MappingActionFactoryMetadataBuilder;
import gsrs.dataExchange.model.MappingParameter;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;

import java.util.Map;

import static gsrs.module.substance.importers.SDFImportAdaptorFactory.resolveParametersMap;

public class NSRSCASExtractorActionFactory extends BaseActionFactory {

    private final String CODE_SYSTEM="CAS";

    @Override
    public MappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) throws Exception {
        return (sub, sdRec) -> {
            Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
            Code c = new Code(CODE_SYSTEM, (String) params.get("CASNumber"));
            c.type = (String) params.get("codeType");
            doBasicsImports(c, params);
            //TODO: more params
            sub.addCode(c);
            return sub;
        };
    }

    @Override
    public MappingActionFactoryMetadata getMetadata() {
        MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadataBuilder();
        return builder.setLabel("Create Code")
                .addParameterField(MappingParameter.builder()
                        .setFieldNameAndLabel("CASNumber", "CAS Number")
                        .setValueType(String.class)
                        .setRequired(true).build())
                .addParameterField(MappingParameter.builder()
                        .setFieldNameAndLabel("codeType", "Primary or Alternative")
                        .setValueType(String.class)
                        .setDefaultValue("PRIMARY")
                        .build())
                .build();
    }
}
