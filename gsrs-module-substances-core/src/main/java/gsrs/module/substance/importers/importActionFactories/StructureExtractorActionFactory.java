package gsrs.module.substance.importers.importActionFactories;

import gsrs.module.substance.importers.MappingActionFactoryMetadata;
import gsrs.module.substance.importers.MappingParameter;
import gsrs.module.substance.importers.SDFImportAdaptorFactory;
import gsrs.module.substance.importers.actions.ImportMappingAction;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Substance;

import java.util.Map;

import static gsrs.module.substance.importers.SDFImportAdaptorFactory.resolveParametersMap;

public class StructureExtractorActionFactory extends BaseActionFactory {
    public ImportMappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) {
        return (sub, sdRec) -> {
            Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
            GinasChemicalStructure s = new GinasChemicalStructure();
            s.molfile = (String) params.get("molfile");
            doBasicsImports(s, params);
            ((ChemicalSubstance) sub).setStructure(s);
            return sub;
        };
    }

    @Override
    public MappingActionFactoryMetadata getMetadata() {
        MappingActionFactoryMetadata.MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadata.MappingActionFactoryMetadataBuilder();
        return builder.setLabel("Create Structure")
                .addParameterField(MappingParameter.builder()
                        .setFieldName("molfile")
                        .setValueType(String.class)
                        .setRequired(true)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("stereochemistry")
                        .setValueType(String.class)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("opticalActivity")
                        .setValueType(String.class)
                        .build())
                .build();
    }
}
