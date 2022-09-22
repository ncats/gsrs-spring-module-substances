package gsrs.module.substance.scrubbers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.springUtils.AutowireHelper;
import ix.ginas.exporters.RecordScrubber;
import ix.ginas.exporters.RecordScrubberFactory;
import ix.ginas.exporters.ScrubberExportSettings;
import ix.ginas.exporters.ScrubberParameterSchema;
import ix.ginas.models.v1.Substance;

public class SubstanceRecordScrubberFactory implements RecordScrubberFactory<Substance> {
    @Override
    public RecordScrubber<Substance> createScrubber(JsonNode settings) {
        ScrubberParameterSchema settingsObject = (new ObjectMapper()).convertValue(settings, ScrubberParameterSchema.class);
        ScrubberParameterSchema scrubberSettings;
        if( settingsObject !=null && settingsObject.getRemoveDates()!=null
            && settingsObject.getRemoveAllLocked()!=null){
            scrubberSettings=settingsObject;
        } else {
            scrubberSettings = new ScrubberParameterSchema();
            scrubberSettings.setAccessGroupsToInclude("WHO");
            scrubberSettings.setRemoveAllLocked(true);
            scrubberSettings.setRemoveNotes(true);
            scrubberSettings.setRemoveChangeReason(false);
            scrubberSettings.setRemoveDates(false);
            scrubberSettings.setApprovalIdCleanup(false);
        }

        GSRSPublicScrubber scrubber = new GSRSPublicScrubber(scrubberSettings);
        scrubber= AutowireHelper.getInstance().autowireAndProxy(scrubber);
        return scrubber;
    }
}
