package example.imports;

import gov.nih.ncats.molwitch.Chemical;
import gsrs.dataexchange.model.MappingAction;
import gsrs.module.substance.importers.importActionFactories.NameExtractorActionFactory;
import gsrs.module.substance.importers.importActionFactories.ProteinSequenceExtractorActionFactory;
import gsrs.module.substance.importers.model.ChemicalBackedSDRecordContext;
import gsrs.module.substance.importers.model.DefaultPropertyBasedRecordContext;
import gsrs.module.substance.importers.model.PropertyBasedDataRecordContext;
import ix.ginas.models.v1.*;
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
        ProteinSubstance proteinSubstance = new ProteinSubstance();

        DefaultPropertyBasedRecordContext ctx = new DefaultPropertyBasedRecordContext();

        ctx.setProperty("proteinSequence", sequence);
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("sequenceFieldName", "proteinSequence");
        inputParams.put("subunitDelimiter", "\\|");

        MappingAction<Substance, PropertyBasedDataRecordContext> action= proteinSequenceExtractorActionFactory.create(inputParams);
        action.act(proteinSubstance, ctx);
        String sequenceFromProtein = proteinSubstance.protein.subunits.get(0).sequence;
        Assertions.assertEquals(sequence, sequenceFromProtein);
    }

}
