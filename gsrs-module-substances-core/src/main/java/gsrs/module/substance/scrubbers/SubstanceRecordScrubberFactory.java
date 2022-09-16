package gsrs.module.substance.scrubbers;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.springUtils.AutowireHelper;
import ix.ginas.exporters.RecordScrubber;
import ix.ginas.exporters.RecordScrubberFactory;
import ix.ginas.exporters.ScrubberExportSettings;
import ix.ginas.models.v1.Substance;

public class SubstanceRecordScrubberFactory implements RecordScrubberFactory<Substance> {
    @Override
    public RecordScrubber<Substance> createScrubber(JsonNode settings) {
        GSRSPublicScrubber scrubber = new GSRSPublicScrubber();
        scrubber= AutowireHelper.getInstance().autowireAndProxy(scrubber);
        scrubber.setSettingsNode(settings);
        return scrubber;
    }
}
