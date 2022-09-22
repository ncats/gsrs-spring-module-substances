package gsrs.module.substance.scrubbers;

import com.fasterxml.jackson.databind.JsonNode;
import ix.ginas.exporters.RecordScrubber;
import ix.ginas.exporters.ScrubberParameterSchema;
import ix.ginas.models.v1.Substance;

import java.util.Optional;

public class ConfigBasedRecordScrubber implements RecordScrubber<Substance> {
    private ScrubberParameterSchema schema;
    private JsonNode settings;

    public void setScrubberParameterSchema(ScrubberParameterSchema newSchema) {
        this.schema=newSchema;
    }


    @Override
    public Optional<Substance> scrub(Substance substance) {
        if(schema.getRemoveAllLocked()) {
            if(schema.getAccessGroupsToInclude()!=null&& schema.getAccessGroupsToInclude().length()>0){

            }
            if(substance.getAccess() !=null && substance.getAccess().size()>0) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
