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
public class NSRSSupplierExtractorActionFactory extends BaseActionFactory{
/*
Use NSRSCustomCodeExtractorActionFactory instead
 */
    private final String CODE_SYSTEM="Supplier";
    private final String CODE_TYPE ="PRIMARY";
    /*
    Supplier is an NSRS field represented as a code.
    Many, many substances may share the same Supplier.
     */
    @Override
    public MappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) throws Exception {
        MappingActionFactoryMetadata metaData = getMetadata();
        return (sub, sdRec) -> {

            Map<String, Object> adaptedParams = resolveParametersMap(sdRec, abstractParams);
            Map<String, Object> params = metaData.resolveAndValidate(adaptedParams);
            Code c = new Code(CODE_SYSTEM, (String) adaptedParams.get("code"));
            c.type = CODE_TYPE;
            if( adaptedParams.get("url") != null) {
                String url =(String) adaptedParams.get("url");
                c.url = url;
            }
            doBasicsImports(c, params);
            sub.addCode(c);
            return sub;
        };
    }

    @Override
    public MappingActionFactoryMetadata getMetadata() {
        MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadataBuilder();
        return builder.setLabel("Create Code")
                .addParameterField(MappingParameter.builder()
                        .setFieldNameAndLabel("code", "Supplier")
                        .setValueType(String.class)
                        .setRequired(true).build())
                //this next one is primarily for testing right now; the processing is a little more complicated than the other fields
                // we may remove this after showing this to users
                .addParameterField(MappingParameter.builder()
                        .setFieldNameAndLabel("url", "Supplier URL")
                        .setValueType(String.class)
                        .build())
                .build();
    }

}
