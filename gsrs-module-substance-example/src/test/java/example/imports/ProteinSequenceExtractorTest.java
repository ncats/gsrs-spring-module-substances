package example.imports;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.dataexchange.model.MappingAction;
import gsrs.importer.DefaultPropertyBasedRecordContext;
import gsrs.module.substance.importers.DelimTextImportAdapterFactory;
import gsrs.module.substance.importers.importActionFactories.ProteinSequenceExtractorActionFactory;
import gsrs.importer.PropertyBasedDataRecordContext;
import ix.ginas.importers.InputFieldStatistics;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ProteinSequenceExtractorTest {

    @Test
    public void testCreateSubunit() throws Exception {
        ProteinSequenceExtractorActionFactory proteinSequenceExtractorActionFactory = new ProteinSequenceExtractorActionFactory();
        String sequence="MLAAMGSLAAALWAVVHPRTLLLGTVAFLLAADFLKRRRPKNYPPGPWRLPFLGNFFLVDFEQSHLEVQLFVKKYGNLFSLELGDISAVLITGLPLIKEALIHMDQNFGNRPVTPMREHIFKKNGLIMSSGQAWKEQRRFTLTALRNFGLGKKSLEERIQEEAQHLTEAIKEENGQPFDPHFKINNAVSNIICSITFGERFEYQDSWFQQLLKLLDEVTYLEASKTCQLYNVFPWIMKFLPGPHQTLFSNWKKLKLFVSHMIDKHRKDWNPAETRDFIDAYLKEMSKHTGNPTSSFHEENLICSTLDLFFAGTETTSTTLRWALLYMALYPEIQEKVQAEIDRVIGQGQQPSTAARESMPYTNAVIHEVQRMGNIIPLNVPREVTVDTTLAGYHLPKGTMILTNLTALHRDPTEWATPDTFNPDHFLENGQFKKREAFMPFSIGKRACLGEQLARTELFIFFTSLMQKFTFRPPNNEKLSLKFRMGITISPVSHRLCAVPQV";
        //String name= "HUMAN CYTOCHROME P450 2J2 (OXIDIZED)";
        ProteinSubstanceBuilder proteinSubstance = new ProteinSubstanceBuilder();

        DefaultPropertyBasedRecordContext ctx = new DefaultPropertyBasedRecordContext();

        ctx.setProperty("proteinSequenceInFile", sequence);
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("proteinSequence", "{{proteinSequenceInFile}}");
        inputParams.put("subunitDelimiter", "\\|");

        MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> action= proteinSequenceExtractorActionFactory.create(inputParams);
        action.act(proteinSubstance, ctx);
        String sequenceFromProtein = proteinSubstance.build().protein.subunits.get(0).sequence;
        Assertions.assertEquals(sequence, sequenceFromProtein);
    }

    @Test
    public void testProteinMap() {
        Map<String, InputFieldStatistics>  inputFieldStatisticsMap = new HashMap<>();
        String sequenceFieldName = "protein_sequence";
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
                if(text.hasNonNull("actionName") && text.get("actionName").asText().equals("protein_import")) {
                    foundCreateAction[0]=true;
                }
            }
        });
        Assertions.assertTrue(foundCreateAction[0]);
    }
}
