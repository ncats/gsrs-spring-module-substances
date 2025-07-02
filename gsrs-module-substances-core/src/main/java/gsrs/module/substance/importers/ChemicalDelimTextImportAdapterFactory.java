package gsrs.module.substance.importers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.dataexchange.model.MappingAction;
import gsrs.importer.PropertyBasedDataRecordContext;
import gsrs.imports.ImportAdapter;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ChemicalDelimTextImportAdapterFactory extends DelimTextImportAdapterFactory{
    @SneakyThrows
    @Override
    public ImportAdapter<Substance> createAdapter(JsonNode adapterSettings) {
        log.trace("starting in createAdapter. adapterSettings: " + adapterSettings.toPrettyString());
        List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> actions = getMappingActions(adapterSettings);
        Map<String, Object> initializationParameters = null;
        if (adapterSettings.hasNonNull("parameters")) {
            log.trace("adapterSettings has parameters");
            ObjectMapper mapper = new ObjectMapper();
            initializationParameters = mapper.convertValue(adapterSettings.get("parameters"), new TypeReference<Map<String, Object>>() {
            });
        } else {
            log.trace("adapterSettings has NO parameters");
            initializationParameters = new HashMap<>();
            if (this.lineValueDelimiter != null) {
                initializationParameters.put("lineValueDelimiter", this.lineValueDelimiter);
            }
            initializationParameters.put("linesToSkip", this.linesToSkip);
            initializationParameters.put("removeQuotes", this.removeQuotes);
            initializationParameters.put("substanceClassName", this.substanceClassName);
            log.trace("created map");
        }
        log.trace("initializationParameters: " + initializationParameters);
        ChemicalDelimTextImportAdapter importAdapter = new ChemicalDelimTextImportAdapter(actions, initializationParameters);
        return importAdapter;
    }

    @Override
    public String getAdapterKey() {
        return "DelimitedTextChemical";
    }
}
