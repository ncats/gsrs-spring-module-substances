package example.imports;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.dataexchange.model.MappingAction;
import gsrs.importer.DefaultPropertyBasedRecordContext;
import gsrs.importer.PropertyBasedDataRecordContext;
import gsrs.module.substance.importers.DelimTextImportAdapterFactory;
import gsrs.module.substance.importers.importActionFactories.SimpleNucleicAcidSequenceExtractorActionFactory;
import ix.ginas.importers.InputFieldStatistics;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.modelBuilders.NucleicAcidSubstanceBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SimpleNucleicAcidSequenceExtractorActionFactoryTest {

    @Test
    public void testCreateSubunit() throws Exception {
        SimpleNucleicAcidSequenceExtractorActionFactory nucleicAcidExtractorActionFactory = new SimpleNucleicAcidSequenceExtractorActionFactory();
        String sequence="GATTAC";
        NucleicAcidSubstanceBuilder nucleicAcidSubstanceBuilder = new NucleicAcidSubstanceBuilder();

        DefaultPropertyBasedRecordContext ctx = new DefaultPropertyBasedRecordContext();

        ctx.setProperty("nucleicAcidSequenceInFile", sequence);
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("nucleicAcidSequence", "{{nucleicAcidSequenceInFile}}");
        inputParams.put("nucleicAcidType", "DNA");

        MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> action= nucleicAcidExtractorActionFactory.create(inputParams);
        action.act(nucleicAcidSubstanceBuilder, ctx);
        String sequenceFromSubstance = nucleicAcidSubstanceBuilder.build().nucleicAcid.subunits.get(0).sequence;
        Assertions.assertEquals(sequence, sequenceFromSubstance);
    }


    @Test
    public void NAMapTest() {
        Map<String, InputFieldStatistics>  inputFieldStatisticsMap = new HashMap<>();
        String sequenceFieldName = "nucleic_acid_sequence";
        InputFieldStatistics statistics = new InputFieldStatistics(sequenceFieldName, 100);
        inputFieldStatisticsMap.put(sequenceFieldName, statistics);
        DelimTextImportAdapterFactory factory = new DelimTextImportAdapterFactory();
        JsonNode processed = factory.createDefaultFileImport(inputFieldStatisticsMap);
        ObjectNode objectNode = (ObjectNode) processed;
        log.trace("processed: {}", processed.toPrettyString() );
        Assertions.assertEquals(1, processed.size());

        ArrayNode actions = (ArrayNode) objectNode.get("actions");
        boolean[] foundCreateAction = new boolean[1];
        actions.forEach(n -> {
            if( n instanceof ObjectNode) {
                ObjectNode text = (ObjectNode) n;
                if(text.hasNonNull("actionName") && text.get("actionName").asText().equals("nucleic_acid_import")) {
                    foundCreateAction[0]=true;
                }
            }
        });
        Assertions.assertTrue(foundCreateAction[0]);
    }

}
