package gsrs.module.substance.importers.importActionFactories;

import gsrs.dataExchange.model.MappingAction;
import gsrs.dataExchange.model.MappingActionFactoryMetadata;
import gsrs.dataExchange.model.MappingActionFactoryMetadataBuilder;
import gsrs.dataExchange.model.MappingParameter;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static gsrs.module.substance.importers.SDFImportAdapterFactory.resolveParametersMap;

@Slf4j
public class NameExtractorActionFactory extends BaseActionFactory {
    @Override
    public MappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) throws Exception {
        log.trace("in create");
        return (sub, sdRec) -> {
            Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);

            boolean splitNames = Boolean.parseBoolean(params.getOrDefault("split_names", "true").toString());

            String suppliedName = (String) params.get("name");
            String nameType = params.get("nameType") != null ? (String) params.get("nameType") : "cn";

            if (splitNames) {
                for (String sn : suppliedName.trim().split("\n")) {
                    if (sn.isEmpty()) continue;
                    //check for duplicates
                    sn = sn.trim();
                    String finalSn = sn; //weird limitation of lambdas in Java
                    if(sub.names.stream().anyMatch(n->n.name.equals(finalSn))){
                        log.info(String.format("duplicate name '%s' skipped", sn));
                        continue;
                    }
                    Name n = new Name();
                    n.setName(sn);
                    n.type=nameType;
                    doBasicsImports(n, params);
                    //TODO: more params
                    sub.names.add(n);
                }
            } else {
                String finalSn = suppliedName; //weird limitation of lambdas in Java
                if(sub.names.stream().anyMatch(n->n.name.equals(finalSn))){
                    log.info(String.format("duplicate name '%s' skipped", suppliedName));
                }
                else {
                    Name n = new Name();
                    n.setName(suppliedName.trim());
                    n.type=nameType;
                    doBasicsImports(n, params);
                    sub.names.add(n);
                }
            }
            return sub;
        };
    }

    @Override
    public MappingActionFactoryMetadata getMetadata() {
        MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadataBuilder();
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
