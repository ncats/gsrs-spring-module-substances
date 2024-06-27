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
        //test like we have a vocabulary term here
        FragmentVocabularyTerm fragmentVocabularyTerm = new FragmentVocabularyTerm();
        fragmentVocabularyTerm.setFragmentStructure("[*]N[C@@H](CS[*])C([*])=O |$_R1;;;;;_R3;;_R2;$|"); //_R...  - atom alias
        String inputStructure = fragmentVocabularyTerm.getFragmentStructure().split(" ")[0];
        //todo: include optional square brackets around *...
        //  2 replacements

        String lexicallyCleaned = inputStructure.replace("*", "6He");
        System.out.printf("lexicallyCleaned: %s\n", lexicallyCleaned);

        Chemical chem = Chemical.parse(inputStructure);
        chem = Chem.RemoveQueryFeaturesForPseudoInChI(chem);
        String processedSmiles = chem.toSmiles();
        System.out.printf("processedSmiles: %s\n", processedSmiles);
        String inChIKey = chem.toInchi().getKey();

        String expectedInChIKey =getInChiKey(lexicallyCleaned);// "OTIPWSTYBNONSP-CQOJXGFHSA-N";
        log.debug("Created InChIKey: {}", inChIKey);
        Assertions.assertEquals(expectedInChIKey, inChIKey);
        String molfile = chem.toMol();
        System.out.printf("molfile:\n %s\n", molfile);
        Assertions.assertFalse(molfile.contains("*"));
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


    @Test
    void testParsing0() {
        String testSmiles = "OP([*])([*])=O";

    }

    private String getInChiKey(String smiles) throws IOException {
        Chemical chem = Chemical.parse(smiles);
        String inChIKey = chem.toInchi().getKey();
        return inChIKey;
    }
}
