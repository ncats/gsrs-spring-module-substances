package gsrs.module.substance.importers.importActionFactories;

import gsrs.module.substance.importers.MappingActionFactoryMetadata;
import gsrs.module.substance.importers.MappingParameter;
import gsrs.module.substance.importers.SDFImportAdaptorFactory;
import gsrs.module.substance.importers.actions.ImportMappingAction;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;

import java.util.Map;
import java.util.Optional;

import static gsrs.module.substance.importers.SDFImportAdaptorFactory.resolveParametersMap;

public class ReferenceExtractorActionFactory extends BaseActionFactory {
    public ImportMappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) {
        return (sub, sdRec) -> {
            Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
            Reference r = new Reference();
            r.citation = (String) params.get("citation");
            r.docType = (String) params.get("docType");
            Optional.ofNullable(params.get("url")).ifPresent(url -> {
                r.url = url.toString();
            });
            Optional.ofNullable(params.get("referenceID")).ifPresent(rid -> {
                r.id = rid.toString();
            });
            doBasicsImports(r, params);
            //TODO: more params
            sub.addReference(r);
            return sub;
        };
    }

    @Override
    public MappingActionFactoryMetadata getMetadata() {
        MappingActionFactoryMetadata.MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadata.MappingActionFactoryMetadataBuilder();
        return builder.setLabel("Create Reference")
                .addParameterField(MappingParameter.builder()
                        .setFieldName("citation")
                        .setValueType(String.class)
                        .setRequired(true)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("docType")
                        .setValueType(String.class)
                        .setRequired(true)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("url")
                        .setValueType(String.class)
                        .setDefaultValue("PRIMARY")
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("referenceID")
                        .setValueType(String.class)
                        .setDefaultValue("PRIMARY")
                        .build())
                .build();
    }

}
