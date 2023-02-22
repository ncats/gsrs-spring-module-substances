package example.imports;

import gsrs.dataexchange.processing_actions.RejectProcessingAction;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class IgnoreProcessingActionTest {

    @Test
    public void simpleIgnoreTest() throws Exception {
        ChemicalSubstanceBuilder builder= new ChemicalSubstanceBuilder();
        builder.setStructureWithDefaultReference("NCCCCN");
        builder.addName("putrecine");
        builder.addCode("CHEMBL", "CHEMBL46257");
        ChemicalSubstance chemical1 = builder.build();

        ChemicalSubstanceBuilder builder2= new ChemicalSubstanceBuilder();
        builder2.setStructureWithDefaultReference("OCCCCO");
        builder2.addName("1,4-BUTANEDIOL");
        builder2.addCode("CHEMBL", "CHEMBL171623");
        ChemicalSubstance chemical2 = builder2.build();

        Map<String, Object> parms = new HashMap<>();

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = (p)-> buildMessage.append(p);

        RejectProcessingAction action = new RejectProcessingAction();
        ChemicalSubstance selected = (ChemicalSubstance) action.process(chemical1, chemical2, parms, logger);
        Assertions.assertEquals(chemical2.getStructure().smiles, selected.getStructure().smiles);
        Assertions.assertNotEquals(chemical1.getStructure().smiles, selected.getStructure().smiles);
        Assertions.assertEquals("Starting in process",  buildMessage.toString());
    }

}
