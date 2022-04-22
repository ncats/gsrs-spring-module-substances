package gsrs.module.substance.importers.importActionFactories;

import gsrs.dataExchange.model.MappingAction;
import gsrs.dataExchange.model.MappingActionFactoryMetadata;
import gsrs.dataExchange.model.MappingActionFactoryMetadataBuilder;
import gsrs.dataExchange.model.MappingParameter;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;

import java.util.Map;

import static gsrs.module.substance.importers.SDFImportAdaptorFactory.resolveParametersMap;
@Deprecated
public class NSRSCASExtractorActionFactory extends BaseActionFactory {

    /*
        Use NSRSCustomCodeExtractorActionFactory instead
    */

    private final String CODE_SYSTEM="CAS";
    private final String DEFAULT_URL = "https://commonchemistry.cas.org/detail?cas_rn=";

    @Override
    public MappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) throws Exception {
        MappingActionFactoryMetadata metaData = getMetadata();
        return (sub, sdRec) -> {
            //resolveParametersMap leaves most things unchanged. molfile undergo conversion to structures
            Map<String, Object> adaptedParams = resolveParametersMap(sdRec, abstractParams);
            //metaData.resolve turns the parameter map from input into a map of values that correspond to a GSRS Code object
            Map<String, Object> params = metaData.resolveAndValidate(adaptedParams);
            Code c = new Code(CODE_SYSTEM, (String) params.get("code"));
            c.type = (String) params.get("codeType");
            if( adaptedParams.get("url") != null) {
                String url =(String) adaptedParams.get("url");
                if( url.equals(DEFAULT_URL)) {
                    url = url + adaptedParams.get("code");
                }
                c.url = url;
            }
            doBasicsImports(c, params);
            //TODO: more params
            sub.addCode(c);
            return sub;
        };
    }

    @Override
    public MappingActionFactoryMetadata getMetadata() {
        MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadataBuilder();
        return builder.setLabel("Create Code")
                .addParameterField(MappingParameter.builder()
                        .setFieldNameAndLabel("code", "CAS Number")
                        .setValueType(String.class)
                        .setLookupKey("CASNumber")
                        .setRequired(true).build())
                .addParameterField(MappingParameter.builder()
                        .setFieldNameAndLabel("codeType", "Primary or Alternative")
                        .setValueType(String.class)
                        .setDefaultValue("PRIMARY")
                        .build())
                //this next one is primarily for testing right now; the processing is a little more complicated than the other fields
                // we may remove this after showing this to users
                .addParameterField(MappingParameter.builder()
                        .setFieldNameAndLabel("url", "CAS Number URL")
                        .setValueType(String.class)
                        .setDefaultValue(DEFAULT_URL)
                        .build())
                .build();
    }
}
