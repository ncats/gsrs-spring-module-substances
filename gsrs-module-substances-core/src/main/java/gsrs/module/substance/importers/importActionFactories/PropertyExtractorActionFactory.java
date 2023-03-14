package gsrs.module.substance.importers.importActionFactories;

import gsrs.dataexchange.model.MappingAction;
import gsrs.dataexchange.model.MappingActionFactoryMetadata;
import gsrs.dataexchange.model.MappingActionFactoryMetadataBuilder;
import gsrs.dataexchange.model.MappingParameter;
import gsrs.importer.PropertyBasedDataRecordContext;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.models.v1.Amount;
import ix.ginas.models.v1.Property;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gsrs.module.substance.importers.SDFImportAdapterFactory.resolveParametersMap;

@Slf4j
public class PropertyExtractorActionFactory extends BaseActionFactory {
    Pattern rangePattern = Pattern.compile("(\\d+\\.?\\d+)\\-(\\d+\\.?\\d+)(.+)");

    Pattern variationPattern = Pattern.compile("(\\d+\\.?\\d+)\\±(\\d+\\.?\\d+) (.+)");

    Pattern singleValuePattern =Pattern.compile("(\\d+[.]?\\d*)");

    public MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> create(Map<String, Object> abstractParams) throws Exception {
        log.trace("in create");
        return (sub, sdRec) -> {
            log.trace("starting in create's lamdbda");
            Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
            if(params.get("name")==null ||params.get("propertyType")==null){
                log.trace("property skipped because required name and/or property type field is missing");
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
                        log.trace("variationPattern matches. total groups: {}", m2.groupCount());
                        String baseRaw = m2.group(1);
                        String variationRaw=m2.group(2);
                        String units ="";
                        if( m2.groupCount() >= 3) {
                            units= m2.group(3);
                        }
                        Double base = null;
                        Double variation = null;
                        try {
                            base = Double.parseDouble(baseRaw);
                            variation= Double.parseDouble(variationRaw);
                            amt.low=base-variation;
                            amt.high=base+variation;
                            if(units.length()>0) {
                                units = units.split(" ")[0].trim();
                                amt.units=units;
                            }
                            log.trace("created low {} and high {} from base {} and variation {}",
                                    amt.low, amt.high, base, variation);
                        } catch (Exception ex){
                            log.warn("Error parsing rangeRaw: {}", rangeRaw);
                        }
                    } else {
                        Matcher m3 = singleValuePattern.matcher(rangeRaw);
                        if( m3.find()) {
                            String valueRaw = m3.group(1);
                            Double base = null;
                            try {
                                base = Double.parseDouble(valueRaw);
                                amt.average = base;
                                log.trace("created average {} from {}",
                                        amt.average, valueRaw);
                            } catch (Exception ex) {
                                log.warn("Error parsing rangeRaw: {}", rangeRaw);
                            }
                        }
                    }
                }
            }
            Optional.ofNullable(params.get("valueAverage")).ifPresent(aa -> {
                Optional<Double> parsedDouble= tryParse(aa.toString());
                if( parsedDouble.isPresent()) {
                    amt.average =parsedDouble.get();
                    log.trace("average: " + amt.average);
                }
                 log.trace ("no number in {}", aa);
            });
            Optional.ofNullable(params.get("valueLow")).ifPresent(aa -> {
                Optional<Double> parsedDouble= tryParse(aa.toString());
                if( parsedDouble.isPresent()) {
                    amt.low =parsedDouble.get();
                    log.trace("low: " + amt.low);
                }
                log.trace ("no number for low in {}", aa);
            });
            Optional.ofNullable(params.get("valueHigh")).ifPresent(aa -> {
                Optional<Double> parsedDouble= tryParse(aa.toString());
                if( parsedDouble.isPresent()) {
                    amt.high =parsedDouble.get();
                    log.trace("high: " + amt.high);
                }
                log.trace ("no number for high in {}", aa.toString());
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
                sub.addProperty(p);
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
                .addParameterField(MappingParameter.builder()
                        .setFieldName("nameCV")
                        .setValueType(String.class)
                        .setDefaultValue("PROPERTY_NAME")
                        .setRequired(true)
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

    private Optional<Double> tryParse(String inputValue) {
        try{
            Double parsed=Double.parseDouble(inputValue);
            return Optional.of(parsed);
        } catch (NumberFormatException numberFormatException){

        }
        return Optional.empty();
    }
}
