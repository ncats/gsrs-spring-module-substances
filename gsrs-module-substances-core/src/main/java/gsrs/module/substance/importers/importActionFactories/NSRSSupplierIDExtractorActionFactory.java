package gsrs.module.substance.importers.importActionFactories;

import gsrs.dataexchange.model.MappingAction;
import gsrs.dataexchange.model.MappingActionFactoryMetadata;
import gsrs.dataexchange.model.MappingActionFactoryMetadataBuilder;
import gsrs.dataexchange.model.MappingParameter;
import gsrs.module.substance.importers.model.PropertyBasedDataRecordContext;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;

import java.util.Map;

import static gsrs.module.substance.importers.SDFImportAdapterFactory.resolveParametersMap;

@Deprecated
public class NSRSSupplierIDExtractorActionFactory extends BaseActionFactory{

    private final String CODE_SYSTEM="Supplier ID";
    private final String CODE_TYPE ="PRIMARY";
    /*
        Use NSRSCustomCodeExtractorActionFactory instead
    */

    /*
    Supplier ID is an NSRS field represented as a code.
    It is generally shared by a small group of Substances.
     */
    @Override
    public MappingAction<Substance, PropertyBasedDataRecordContext> create(Map<String, Object> abstractParams) throws Exception {
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
                        .setFieldNameAndLabel("code", "Supplier ID")
                        .setValueType(String.class)
                        .setRequired(true).build())
                .build();
    }
}
