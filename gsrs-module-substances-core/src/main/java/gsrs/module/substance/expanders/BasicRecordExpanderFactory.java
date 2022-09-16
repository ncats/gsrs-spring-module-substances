package gsrs.module.substance.expanders;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.springUtils.AutowireHelper;
import ix.ginas.exporters.RecordExpander;
import ix.ginas.exporters.RecordExpanderFactory;
import ix.ginas.models.v1.Substance;

public class BasicRecordExpanderFactory implements RecordExpanderFactory<Substance> {
    @Override
    public RecordExpander<Substance> createExpander(JsonNode settings) {
        BasicRecordExpander expander = new BasicRecordExpander();
        expander=AutowireHelper.getInstance().autowireAndProxy(expander);
        expander.applySettings(settings);
        return expander;
    }
}
