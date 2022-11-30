package gsrs.module.substance.importers.importActionFactories;

import gsrs.dataexchange.model.MappingAction;
import gsrs.dataexchange.model.MappingActionFactoryMetadata;
import gsrs.dataexchange.model.MappingActionFactoryMetadataBuilder;
import gsrs.dataexchange.model.MappingParameter;
import gsrs.module.substance.importers.model.PropertyBasedDataRecordContext;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;

import java.util.Map;

import static gsrs.module.substance.importers.SDFImportAdapterFactory.resolveParametersMap;

public class NSRSSampleNameExtractorActionFactory extends BaseActionFactory {
    @Override
    public MappingAction<Substance, PropertyBasedDataRecordContext> create(Map<String, Object> abstractParams) throws Exception {
        return (sub, sdRec) -> {
            Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);

            boolean splitNames = Boolean.parseBoolean(params.getOrDefault("split_names", "true").toString());

            String suppliedName = (String) params.get("name");

            if (splitNames) {
                for (String sn : suppliedName.trim().split("\n")) {
                    if (sn.isEmpty()) continue;
                    sn = sn.trim();
                    Name n = new Name();
                    n.setName(sn);
                    doBasicsImports(n, params);
                    //TODO: more params and additional manipulations of the data
                    sub.names.add(n);
                }
            } else {
                Name n = new Name();
                n.setName(suppliedName.trim());
                doBasicsImports(n, params);
                sub.names.add(n);
            }
            return sub;
        };
    }

    @Override
    public MappingActionFactoryMetadata getMetadata() {
        MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadataBuilder();
        return builder.setLabel("Create Name")
                .addParameterField(MappingParameter.builder()
                        .setFieldNameAndLabel("nameValue", "Sample Name")
                        .setValueType(String.class)
                        .setRequired(true).build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("nameType")
                        .setValueType(String.class)
                        .setDefaultValue("cn")
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("nameLang")
                        .setValueType(String.class)
                        .setDefaultValue("en")
                        .build())
                .build();
    }

}
