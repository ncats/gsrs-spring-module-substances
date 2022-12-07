package example.imports;

import gsrs.dataexchange.model.MappingAction;
import gsrs.module.substance.importers.importActionFactories.ProteinSequenceExtractorActionFactory;
import gsrs.module.substance.importers.model.DefaultPropertyBasedRecordContext;
import gsrs.module.substance.importers.model.PropertyBasedDataRecordContext;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class ProteinSequenceExtractorTest {

    @Test
    public void testCreateSubunit() throws Exception {
        ProteinSequenceExtractorActionFactory proteinSequenceExtractorActionFactory = new ProteinSequenceExtractorActionFactory();
        String sequence="MLAAMGSLAAALWAVVHPRTLLLGTVAFLLAADFLKRRRPKNYPPGPWRLPFLGNFFLVDFEQSHLEVQLFVKKYGNLFSLELGDISAVLITGLPLIKEALIHMDQNFGNRPVTPMREHIFKKNGLIMSSGQAWKEQRRFTLTALRNFGLGKKSLEERIQEEAQHLTEAIKEENGQPFDPHFKINNAVSNIICSITFGERFEYQDSWFQQLLKLLDEVTYLEASKTCQLYNVFPWIMKFLPGPHQTLFSNWKKLKLFVSHMIDKHRKDWNPAETRDFIDAYLKEMSKHTGNPTSSFHEENLICSTLDLFFAGTETTSTTLRWALLYMALYPEIQEKVQAEIDRVIGQGQQPSTAARESMPYTNAVIHEVQRMGNIIPLNVPREVTVDTTLAGYHLPKGTMILTNLTALHRDPTEWATPDTFNPDHFLENGQFKKREAFMPFSIGKRACLGEQLARTELFIFFTSLMQKFTFRPPNNEKLSLKFRMGITISPVSHRLCAVPQV";
        String name= "HUMAN CYTOCHROME P450 2J2 (OXIDIZED)";
        ProteinSubstanceBuilder proteinSubstance = new ProteinSubstanceBuilder();

        DefaultPropertyBasedRecordContext ctx = new DefaultPropertyBasedRecordContext();

        ctx.setProperty("proteinSequence", sequence);
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("sequenceFieldName", "proteinSequence");
        inputParams.put("subunitDelimiter", "\\|");

        MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> action= proteinSequenceExtractorActionFactory.create(inputParams);
        action.act(proteinSubstance, ctx);
        String sequenceFromProtein = proteinSubstance.build().protein.subunits.get(0).sequence;
        Assertions.assertEquals(sequence, sequenceFromProtein);
    }

}
