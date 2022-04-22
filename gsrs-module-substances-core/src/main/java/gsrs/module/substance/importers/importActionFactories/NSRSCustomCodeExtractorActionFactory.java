package gsrs.module.substance.importers.importActionFactories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.dataExchange.model.*;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static gsrs.module.substance.importers.SDFImportAdaptorFactory.resolveParametersMap;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NSRSCustomCodeExtractorActionFactory extends BaseActionFactory {
    private String actionName;
    private String actionLabel;
    private String codeSystem;
    /*
    private String codeSystemLabel;
    private String codeValueParameterName;*/
    private List<Map<String, Object>> fields;

    public NSRSCustomCodeExtractorActionFactory() {
    }

    /*
    This class creates GSRS codes for
        Supplier
        Supplier ID
        Salt Code
        Salt Equivalent
        CAS
     within NSRS
     */
    @Override
    public MappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) throws Exception {
        MappingActionFactoryMetadata metaData = getMetadata();
        return (sub, sdRec) -> {

            Map<String, Object> adaptedParams = resolveParametersMap(sdRec, abstractParams);
            Map<String, Object> resolvedParameters = metaData.resolveAndValidate(adaptedParams);
            String codeValue = (String)  resolvedParameters.get("code");
            Code c = new Code(codeSystem, codeValue);
            c.type = (String) resolvedParameters.get("codeType");
            //todo: add url and other fields as default parameters to metadata (not expected to change)
            if(  resolvedParameters.get("url") != null) {
                String url =(String)  resolvedParameters.get("url");
                if(url.endsWith("=")) {
                    url+=codeValue;
                }
                c.url = url;
            }
            doBasicsImports(c, resolvedParameters);
            sub.addCode(c);
            return sub;
        };
    }

    @Override
    public MappingActionFactoryMetadata getMetadata() {
        /*
        Deserialize a 'fields' into metadata
        fields is a list of Map<String, Object> into a list of Mapping Parameters
        when you pass around types,
         */
        List<MappingParameter> params= (new ObjectMapper()).convertValue(fields, new TypeReference<ArrayList<MappingParameter>>() {});

        MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadataBuilder();
        builder.setParameterFields(params);
        builder.setLabel(actionLabel);

        /*for(String parameter : parameterInfo) {
            String[] parameterParts = parameter.split("`");
            String fieldName = parameterParts[0];
            String fieldLabel = parameterParts[1];
            String typeName = parameterParts[2];
            String requiredString = parameterParts[3];
            String defaultValue = null;
            if( parameterParts.length> 4) {
                defaultValue= parameterParts[4];
            }
            Boolean showInUI= true;
            if( parameterParts.length>5 && parameterParts[5] != null && parameterParts[5].equalsIgnoreCase("FALSE")) {
                showInUI= false;
            }

            MappingParameterBuilder parameterBuilder = MappingParameter.builder();
            parameterBuilder.setFieldName(fieldName);
            parameterBuilder.setLabel(fieldLabel);
            Class parameterType;
            try {
                parameterType = Class.forName(typeName);
                parameterBuilder.setValueType(parameterType);
            }
            catch (ClassNotFoundException ex) {

            }
            parameterBuilder.setDefaultValue(defaultValue);
            if(requiredString.equalsIgnoreCase("false")) {
                parameterBuilder.setRequired(false);
            } else {
                parameterBuilder.setRequired(true);
            }
            parameterBuilder.setDisplayInUI(showInUI);
            builder.addParameterField(parameterBuilder.build());
        }*/
        return builder.build();
    }

}
