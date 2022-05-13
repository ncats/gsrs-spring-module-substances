package gsrs.module.substance.importers.importActionFactories;

import gsrs.dataExchange.model.MappingAction;
import gsrs.dataExchange.model.MappingActionFactoryMetadata;
import gsrs.dataExchange.model.MappingActionFactoryMetadataBuilder;
import gsrs.dataExchange.model.MappingParameter;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.Amount;
import ix.ginas.models.v1.Property;
import ix.ginas.models.v1.Substance;

import java.util.Map;
import java.util.Optional;

import static gsrs.module.substance.importers.SDFImportAdapterFactory.resolveParametersMap;

public class PropertyExtractorActionFactory extends BaseActionFactory {
    public MappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) throws Exception {
        return (sub, sdRec) -> {
            Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
            Property p = new Property();
            p.setName((String) params.get("name"));
            doBasicsImports(p, params);
            Amount amt = new Amount();
            p.setValue(amt);
            Optional.ofNullable(params.get("valueAverage")).ifPresent(aa -> {
                amt.average = (Double.parseDouble(aa.toString()));
            });
            Optional.ofNullable(params.get("valueLow")).ifPresent(aa -> {
                amt.low = (Double.parseDouble(aa.toString()));
            });
            Optional.ofNullable(params.get("valueHigh")).ifPresent(aa -> {
                amt.high = (Double.parseDouble(aa.toString()));
            });
            Optional.ofNullable(params.get("valueNonNumeric")).ifPresent(aa -> {
                amt.nonNumericValue = aa.toString();
            });
            Optional.ofNullable(params.get("valueUnits")).ifPresent(aa -> {
                amt.units = aa.toString();
            });
            Optional.ofNullable(params.get("defining")).ifPresent(aa -> {
                p.setDefining(Boolean.parseBoolean(params.getOrDefault("defining", "false").toString()));
            });
            //TODO: more params
            sub.properties.add(p);
            return sub;
        };
    }

    @Override
    public MappingActionFactoryMetadata getMetadata() {
        MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadataBuilder();
        return builder.setLabel("Create Property")
                .addParameterField(MappingParameter.builder()
                        .setFieldName("name")
                        .setValueType(String.class)
                        .setRequired(true)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("valueAverage")
                        .setValueType(Double.class)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("valueLow")
                        .setValueType(Double.class)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("valueHigh")
                        .setValueType(Double.class)
                        .build())

                .addParameterField(MappingParameter.builder()
                        .setFieldName("opticalActivity")
                        .setValueType(String.class)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("valueNonNumeric")
                        .setValueType(String.class)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("defining")
                        .setValueType(Boolean.class)
                        .build())
                .build();
    }
}
