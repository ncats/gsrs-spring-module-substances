package gsrs.module.substance.importers.importActionFactories;

import gsrs.module.substance.importers.actions.ImportMappingActionFactory;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.GinasAccessControlled;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonData;
import ix.ginas.models.v1.Substance;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class BaseActionFactory implements ImportMappingActionFactory<Substance, SDRecordContext> {

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
}
