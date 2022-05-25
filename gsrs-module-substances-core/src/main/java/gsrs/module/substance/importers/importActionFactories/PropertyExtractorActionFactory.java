package gsrs.module.substance.importers.importActionFactories;

import gsrs.dataExchange.model.MappingAction;
import gsrs.dataExchange.model.MappingActionFactoryMetadata;
import gsrs.dataExchange.model.MappingActionFactoryMetadataBuilder;
import gsrs.dataExchange.model.MappingParameter;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.Amount;
import ix.ginas.models.v1.Property;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import scala.sys.Prop;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gsrs.module.substance.importers.SDFImportAdapterFactory.resolveParametersMap;

@Slf4j
public class PropertyExtractorActionFactory extends BaseActionFactory {
    public MappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) throws Exception {
        log.trace("in create");
        Pattern rangePattern = Pattern.compile("(\\d+\\.?\\d+)\\-(\\d+\\.?\\d+)(.+)");
        Pattern variationPattern = Pattern.compile("(\\d+\\.?\\d+)\\±(\\d+\\.?\\d+)(.+)");
        return (sub, sdRec) -> {
            Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
            if(params.get("name")==null ||params.get("propertyType")==null){
                log.info("property skipped because required name and/or property type field is missing");
                return sub;
            }
            Property p = new Property();
            p.setName((String) params.get("name"));
            log.trace("property name: " + params.get("name"));

            p.setPropertyType((String) params.get("propertyType"));
            log.trace("property type: " + params.get("propertyType"));

            doBasicsImports(p, params);
            Amount amt = new Amount();
            p.setValue(amt);
            if( params.get("valueRange") !=null ) {
                String rangeRaw = (String)params.get("valueRange");
                log.trace("rangeRaw: '{}'", rangeRaw);
                Matcher m = rangePattern.matcher(rangeRaw);
                if( m.matches()) {
                    log.trace("rangePattern matches");
                    String lowerRaw = m.group(1);
                    String upperRaw = m.group(2);
                    String units = m.group(3);
                    amt.low= Double.parseDouble(lowerRaw);
                    amt.high=Double.parseDouble(upperRaw);
                    amt.units=units.trim();
                } else {
                    Matcher m2= variationPattern.matcher(rangeRaw);
                    if( m2.matches()){
                        log.trace("variationPattern matches");
                        String baseRaw = m2.group(1);
                        String variationRaw=m2.group(2);
                        Double base = null;
                        Double variation = null;
                        try {
                            base = Double.parseDouble(baseRaw);
                            variation= Double.parseDouble(variationRaw);
                            amt.low=base-variation;
                            amt.high=base+variation;
                            log.trace("created low {} and high {} from base {} and variation {}",
                                    amt.low, amt.high, base, variation);
                        } catch (Exception ex){
                            log.warn("Error parsing rangeRaw: " + rangeRaw);
                        }
                    }
                }
            }
            Optional.ofNullable(params.get("valueAverage")).ifPresent(aa -> {
                amt.average = (Double.parseDouble(aa.toString()));
                log.trace("average: " + amt.average);
            });
            Optional.ofNullable(params.get("valueLow")).ifPresent(aa -> {
                amt.low = (Double.parseDouble(aa.toString()));
            });
            Optional.ofNullable(params.get("valueHigh")).ifPresent(aa -> {
                amt.high = (Double.parseDouble(aa.toString()));
            });
            Optional.ofNullable(params.get("valueNonNumeric")).ifPresent(aa -> {
                amt.nonNumericValue = aa.toString();
                log.trace("valueNonNumeric: " + amt.nonNumericValue);
            });
            Optional.ofNullable(params.get("valueUnits")).ifPresent(aa -> {
                amt.units = aa.toString();
            });
            Optional.ofNullable(params.get("defining")).ifPresent(aa -> {
                p.setDefining(Boolean.parseBoolean(params.getOrDefault("defining", "false").toString()));
            });
            //TODO: more params
            if( propertyComplete(p)) {
                sub.properties.add(p);
            } else {
                log.warn("Skipping incomplete property");
            }
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

    private boolean propertyComplete(Property testProperty) {
        if (testProperty.getName() == null || testProperty.getName().length()==0) {
            log.trace("propertyComplete - no name!");
            return false;
        }
        if(testProperty.getValue()==null) {
            log.trace("propertyComplete - no value!");
            return false;
        }

        log.trace("looking at property {} with high {} low {} average {} non-numeric {}",
                testProperty.getName(), testProperty.getValue().high, testProperty.getValue(). low, testProperty.getValue().average,
                testProperty.getValue().nonNumericValue);
        if(testProperty.getValue().high==null && testProperty.getValue().low ==null && testProperty.getValue().average == null
            && (testProperty.getValue().nonNumericValue== null || testProperty.getValue().nonNumericValue.length()==0)) {
            log.trace("propertyComplete - no numbers!");
            return false;
        }
        return true;
    }
}
