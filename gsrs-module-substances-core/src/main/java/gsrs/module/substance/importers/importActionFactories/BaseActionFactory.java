package gsrs.module.substance.importers.importActionFactories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import gsrs.dataexchange.model.MappingActionFactory;
import gsrs.importer.PropertyBasedDataRecordContext;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.models.GinasAccessControlled;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonData;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public abstract class BaseActionFactory implements MappingActionFactory<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> {

    Map<String, Object> parameters = new HashMap<>();

    private JsonNode adapterSchema = JsonNodeFactory.instance.objectNode();

    private static void assignReferences(GinasAccessReferenceControlled object, Object referenceList) {
        if( referenceList == null) {
            log.info("in assignReferences, referenceList is null");
            return;
        }
        if( referenceList.getClass().getName().equals("java.lang.String")){
            log.trace("assigning reference {}", referenceList);
            object.addReference((String)referenceList);
        } else {
            List<String> refs = (List<String>) referenceList;
            log.trace("assigning list of refs");
            if (refs != null) {
                refs.forEach(r -> {
                    log.trace("     {}", r);
                    object.addReference(r);});
            }
        }
    }

    protected static void doBasicsImports(GinasAccessControlled object, Map<String, Object> params) {
        if (object instanceof GinasAccessReferenceControlled) {
            assignReferences((GinasAccessReferenceControlled) object, params.getOrDefault("referenceUUIDs", null));
        }
        if (object instanceof GinasCommonData) {
            if (params.get("uuid") != null) {
                ((GinasCommonData) object).setUuid(UUID.fromString(params.get("uuid").toString()));
            }
        }
        if (params.get("access") != null) {
            //TODO: need to deal with this somehow, not sure how yet because of the
            //need to use Group objects
        }
    }

    public void implementParameters() {
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public JsonNode getAdapterSchema() {
        return adapterSchema;
    }

    public void setAdapterSchema(JsonNode adapterSchema) {
        this.adapterSchema = adapterSchema;
    }
}
