package example.chem;

import gov.nih.ncats.molwitch.Chemical;
import ix.core.chem.Chem;
import ix.ginas.models.v1.FragmentVocabularyTerm;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@Slf4j
public class VocabFragmentCleanupTest {

    @Test
    void testQueryFeatures() throws IOException {
        FragmentVocabularyTerm fragmentVocabularyTerm = new FragmentVocabularyTerm();
        fragmentVocabularyTerm.setFragmentStructure("[*]N[C@@H](CS[*])C([*])=O |$_R1;;;;;_R3;;_R2;$|");
        String inputStructure = fragmentVocabularyTerm.getFragmentStructure().split(" ")[0];
        Chemical chem = Chemical.parse(inputStructure);
        chem = Chem.RemoveQueryFeaturesForPseudoInChI(chem);
        String inchiKey = chem.toInchi().getKey();
        log.debug("Created InChIKey: {}", inchiKey);
        Assertions.assertTrue(inchiKey.length()>0);
    }

    @Test
    void testOutput() throws IOException {
        FragmentVocabularyTerm fragmentVocabularyTerm = new FragmentVocabularyTerm();
        fragmentVocabularyTerm.setFragmentStructure("[*]N[C@@H](CS[*])C([*])=O |$_R1;;;;;_R3;;_R2;$|");
        String inputStructure = fragmentVocabularyTerm.getFragmentStructure().split(" ")[0];
        Chemical chem = Chemical.parse(inputStructure);
        chem = Chem.RemoveQueryFeaturesForPseudoInChI(chem);
        String smiles = chem.toSmiles();
        log.debug("Created SMILES: {}", smiles);
        Assertions.assertTrue(smiles.length()>0);
    }

}
