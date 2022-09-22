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
        GSRSPublicScrubber scrubber = new GSRSPublicScrubber(settingsObject);

        scrubber= AutowireHelper.getInstance().autowireAndProxy(scrubber);

        return scrubber;
    }
}
