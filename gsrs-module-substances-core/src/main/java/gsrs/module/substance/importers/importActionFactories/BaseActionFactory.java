package gsrs.module.substance.importers.importActionFactories;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.dataexchange.model.MappingActionFactory;
import gsrs.module.substance.importers.model.PropertyBasedDataRecordContext;
import ix.ginas.models.GinasAccessControlled;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonData;
import ix.ginas.models.v1.Substance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class BaseActionFactory implements MappingActionFactory<Substance, PropertyBasedDataRecordContext> {

    Map<String, Object> parameters = new HashMap<>();

    private JsonNode adapterSchema;

    private static void assignReferences(GinasAccessReferenceControlled object, Object referenceList) {
        List<String> refs = (List<String>) referenceList;
        if (refs != null) {
            refs.forEach(r -> object.addReference(r));
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
