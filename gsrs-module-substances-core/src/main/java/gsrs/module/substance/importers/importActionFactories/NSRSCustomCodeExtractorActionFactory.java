package gsrs.module.substance.importers.importActionFactories;

import gsrs.dataExchange.model.*;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static gsrs.module.substance.importers.SDFImportAdaptorFactory.resolveParametersMap;

@Slf4j
@Data
public class NSRSCustomCodeExtractorActionFactory extends BaseActionFactory {
    private String codeSystem;
    private String CODE_TYPE ="PRIMARY";
    private String codeSystemLabel;
    private String actionName;
    private String actionLabel;
    private String codeValueParameterName;

    private String[] parameterInfo;

    public NSRSCustomCodeExtractorActionFactory() {
    }

    public NSRSCustomCodeExtractorActionFactory(String[] parameters) {
        parameterInfo= parameters;
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
            Map<String, Object> resolvedParameters = metaData.resolve(adaptedParams);
            String codeValue = (String)  resolvedParameters.get(codeValueParameterName);
            Code c = new Code(codeSystem, codeValue);
            c.type = CODE_TYPE;
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
        MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadataBuilder();
        builder.setLabel(actionLabel);
        for(String parameter : parameterInfo) {
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
        }
        return builder.build();
    }

}
