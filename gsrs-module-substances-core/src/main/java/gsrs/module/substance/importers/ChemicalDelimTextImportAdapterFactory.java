package gsrs.module.substance.importers;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import gsrs.dataexchange.model.MappingAction;
import gsrs.importer.PropertyBasedDataRecordContext;
import gsrs.imports.ImportAdapter;
import gsrs.module.substance.importers.importActionFactories.*;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

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
            JsonMapper mapper = JsonMapper.builderWithJackson2Defaults()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build();
            initializationParameters = mapper.convertValue(adapterSettings.get("parameters"), new TypeReference<>() {
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

    @Override
    protected void defaultInitialize() {
        log.trace("using default actions");
        registry.put("common_name", new NameExtractorActionFactory());
        registry.put("code_import", new CodeExtractorActionFactory());
        registry.put("structure_and_moieties_from_text", new StructureExtractorActionFactory());
        registry.put("note_import", new NotesExtractorActionFactory());
        registry.put("property_import", new PropertyExtractorActionFactory());
        registry.put("no-op", new NoOpActionFactory());
        registry.put(SIMPLE_REFERENCE_ACTION, new ReferenceExtractorActionFactory());
    }

}
