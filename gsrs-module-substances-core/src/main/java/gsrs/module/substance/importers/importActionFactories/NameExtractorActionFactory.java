package gsrs.module.substance.importers.importActionFactories;

import gsrs.dataexchange.model.MappingAction;
import gsrs.dataexchange.model.MappingActionFactoryMetadata;
import gsrs.dataexchange.model.MappingActionFactoryMetadataBuilder;
import gsrs.dataexchange.model.MappingParameter;
import gsrs.importer.PropertyBasedDataRecordContext;
import ix.core.models.Keyword;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.models.v1.Name;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static gsrs.module.substance.importers.SDFImportAdapterFactory.resolveParametersMap;

@Slf4j
public class NameExtractorActionFactory extends BaseActionFactory {

    private static final String DEFAULT_LANGUAGE = "en";
    private static final String DEFAULT_NAME_TYPE = "cn";
    @Override
    public MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> create(Map<String, Object> abstractParams) throws Exception {
        log.trace("in create");
        return (sub, sdRec) -> {
            Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
            boolean splitNames = Boolean.parseBoolean(params.getOrDefault("split_names", "true").toString());

            String suppliedName = (String) params.get("name");
            String nameType = params.get("nameType") != null ? (String) params.get("nameType") : DEFAULT_NAME_TYPE;
            String language = params.get("lang") != null ? (String) params.get("lang") : DEFAULT_LANGUAGE;
            Boolean isDisplayName = params.get("displayName") != null && params.get("displayName").toString().equalsIgnoreCase("TRUE");


            if (splitNames) {
                log.trace("splitNames true");
                for (String sn : suppliedName.trim().split("\n")) {
                    if (sn.isEmpty()) continue;

                    //check for duplicates
                    sn = sn.trim();
                    List<String> nameSegments = new ArrayList<>();
                    if(looksLikeGsrsExportedName(sn, nameSegments)){
                        //parsing as name exported from GSRS with pipes
                        log.trace("exported from GSRS");
                        sn=nameSegments.get(0);
                        language=nameSegments.get(1);
                        if( nameSegments.size()>2 && nameSegments.get(2)!=null && nameSegments.get(2).length()>0){
                            isDisplayName=Boolean.parseBoolean(nameSegments.get(2));
                        }
                    }
                    String finalSn = sn; //weird limitation of lambdas in Java
                    if(sub.build().names.stream().anyMatch(n->n.name.equals(finalSn))){
                        log.info(String.format("duplicate name '%s' skipped", sn));
                        continue;
                    }
                    Name n = new Name();
                    n.setName(sn);
                    n.type=nameType;
                    n.languages.add(new Keyword(language));
                    n.displayName=isDisplayName;
                    doBasicsImports(n, params);
                    //TODO: more params
                    sub.addName(n);
                }
            } else {
                String finalSn = suppliedName; //weird limitation of lambdas in Java
                if(sub.build().names.stream().anyMatch(n->n.name.equals(finalSn))){
                    log.info(String.format("duplicate name '%s' skipped", suppliedName));
                }
                else {
                    Name n = new Name();
                    n.setName(suppliedName.trim());
                    n.type=nameType;
                    doBasicsImports(n, params);
                    sub.addName(n);
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
                        .setFieldName("nameType")
                        .setValueType(String.class)
                        .setDefaultValue("cn")
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("displayName")
                        .setValueType(Boolean.class)
                        .setDefaultValue(false)
                        .setRequired(false)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("nameTypeCV")
                        .setValueType(String.class)
                        .setDefaultValue("NAME_TYPE")
                        .build())

                .build();
    }

    public static boolean looksLikeGsrsExportedName(String name, List<String> nameSegments){
        if( name==null || name.length()<4) {
            return false;
        }
        //crude heuristic
        String lastTen= name;
        int lastSegment = name.length()<10 ? 0 : name.length() -10;
        lastTen=name.substring(lastSegment,  Math.max(name.length(), name.length()-lastSegment));
        //looking for cases where there are TWO pipe delimiters, separated by 2 or more chars
        if(lastTen.lastIndexOf("|") > 0 && lastTen.lastIndexOf("|", lastTen.lastIndexOf("|") -2) > 0){
            String[] segments = name.split("\\|");
            if( segments.length >2){
                String possibleBoolean = segments[segments.length-1];//this can be empty so no point in checking
                String possibleLang = segments[segments.length-2];
                if(possibleLang.length()==2 || possibleLang.length()==3){
                    //we might also check the possible language against the CV but the file may contain a value not in the cv
                    nameSegments.addAll(Arrays.asList(segments));
                    return true;
                }
            } else if(segments.length==2){
                String possibleLang = segments[segments.length-1];
                if(possibleLang.length()==2 || possibleLang.length()==3){
                    //we might also check the possible language against the CV but the file may contain a value not in the cv
                    nameSegments.addAll(Arrays.asList(segments));
                    return true;
                }
            }

        }
        return false;
    }
}
