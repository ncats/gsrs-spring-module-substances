package gsrs.module.substance.importers.importActionFactories;

import gsrs.module.substance.importers.MappingActionFactoryMetadata;
import gsrs.module.substance.importers.MappingParameter;
import gsrs.module.substance.importers.actions.ImportMappingAction;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;

import java.util.Map;

import static gsrs.module.substance.importers.SDFImportAdaptorFactory.resolveParametersMap;

public class NameExtractorActionFactory extends BaseActionFactory {
        public ImportMappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) {
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
                        //TODO: more params
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
            MappingActionFactoryMetadata.MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadata.MappingActionFactoryMetadataBuilder();
            return builder.setLabel("Create Name")
                    .addParameterField(MappingParameter.builder()
                            .setFieldName("nameValue")
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
