package example.chem;

import gov.nih.ncats.molwitch.Chemical;
import ix.core.chem.Chem;
import ix.ginas.models.v1.FragmentVocabularyTerm;
import ix.ginas.utils.validation.validators.CVFragmentStructureValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

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
        log.trace("lexicallyCleaned: {}}", lexicallyCleaned);

        Chemical chem = Chemical.parse(inputStructure);
        chem = Chem.RemoveQueryFeaturesForPseudoInChI(chem);
        String processedSmiles = chem.toSmiles();
        log.trace("processedSmiles:{}", processedSmiles);
        String inChIKey = chem.toInchi().getKey();

        String expectedInChIKey =getInChiKey(lexicallyCleaned);// "OTIPWSTYBNONSP-CQOJXGFHSA-N";
        log.debug("Created InChIKey: {}", inChIKey);
        Assertions.assertEquals(expectedInChIKey, inChIKey);
        String molfile = chem.toMol();
        log.trace("molfile:\n {}", molfile);
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
    void testSmilesParse() throws IOException {
        //test like we have a vocabulary term here
        FragmentVocabularyTerm fragmentVocabularyTerm = new FragmentVocabularyTerm();
        fragmentVocabularyTerm.setFragmentStructure("CCCCCCCC\\C=C/CCCCCCCCCCCCOCC(CO[*])OCCCCCCCCCCCC\\C=C/CCCCCCCC |$;;;;;;;;;;;;;;;;;;;;;;;;;;;_R92;;;;;;;;;;;;;;;;;;;;;;;$|");
        String inputStructure = fragmentVocabularyTerm.getFragmentStructure().split(" ")[0];
        String lexicallyCleaned = inputStructure.replace("*", "6He");
        log.trace("lexicallyCleaned: {}", lexicallyCleaned);

        Chemical chem = Chemical.parse(inputStructure);
        chem = Chem.RemoveQueryFeaturesForPseudoInChI(chem);
        String processedSmiles = chem.toSmiles();
        log.trace("processedSmiles: {}", processedSmiles);
        String inChIKey = chem.toInchi().getKey();

        String expectedInChIKey =getInChiKey(lexicallyCleaned);
        log.debug("Created InChIKey: {}", inChIKey);
        Assertions.assertEquals(expectedInChIKey, inChIKey);
        String molfile = chem.toMol();
        log.trace("molfile:\n {}", molfile);
        Assertions.assertFalse(molfile.contains("*"));
    }

    private static Stream<Arguments> fragmentSmiles() {
        return Stream.of(
                Arguments.of("C[C@H](N[*])C([*])=O |$;;;_R1;;_R2;$|"),
                Arguments.of("[*]N[C@@H](CS[*])C([*])=O |$_R1;;;;;_R3;;_R2;$|"),
                Arguments.of("[*]N[C@@H](CC([*])=O)C([*])=O |$_R1;;;;;_R3;;;_R2;$|"),
                Arguments.of("[*]N[C@@H](CCC([*])=O)C([*])=O |$_R1;;;;;;_R3;;;_R2;$|"),
                Arguments.of("[*]N[C@@H](CC1=CC=CC=C1)C([*])=O |$_R1;;;;;;;;;;;_R2;$,c:6,8,t:4|"),
                Arguments.of("[*]NCC([*])=O |$_R1;;;;_R2;$|"),
                Arguments.of("[*]N[C@@H](CC1=CNC=N1)C([*])=O |$_R1;;;;;;;;;;_R2;$,c:7,t:4|"),
                Arguments.of("CC[C@H](C)[C@H](N[*])C([*])=O |$;;;;;;_R1;;_R2;$|"),
                Arguments.of("[*]N[C@@H](CCCCN[*])C([*])=O |$_R1;;;;;;;;_R3;;_R2;$|"),
                Arguments.of("CC(C)C[C@H](N[*])C([*])=O |$;;;;;;_R1;;_R2;$|"),
                Arguments.of("CSCC[C@H](N[*])C([*])=O |$;;;;;;_R1;;_R2;$|"),
                Arguments.of("NC(=O)C[C@H](N[*])C([*])=O |$;;;;;;_R1;;_R2;$|"),
                Arguments.of("[*]N1CCC[C@H]1C([*])=O |$_R1;;;;;;;_R2;$|"),
                Arguments.of("NC(=O)CC[C@H](N[*])C([*])=O |$;;;;;;;_R1;;_R2;$|"),
                Arguments.of("NC(=N)NCCC[C@H](N[*])C([*])=O |$;;;;;;;;;_R1;;_R2;$|"),
                Arguments.of("OC[C@H](N[*])C([*])=O |$;;;;_R1;;_R2;$|"),
                Arguments.of("C[C@@H](O)[C@H](N[*])C([*])=O |$;;;;;_R1;;_R2;$|"),
                Arguments.of("CC(C)[C@H](N[*])C([*])=O |$;;;;;_R1;;_R2;$|"),
                Arguments.of("[*]N[C@@H](CC1=CNC2=C1C=CC=C2)C([*])=O |$_R1;;;;;;;;;;;;;;_R2;$,c:7,10,12,t:4|"),
                Arguments.of("OC1=CC=C(C[C@H](N[*])C([*])=O)C=C1 |$;;;;;;;;_R1;;_R2;;;$,c:12,t:1,3|"),
                Arguments.of("CCCCCCCC\\C=C/CCCCCCCCCCCCOCC(CO[*])OCCCCCCCCCCCC\\C=C/CCCCCCCC |$;;;;;;;;;;;;;;;;;;;;;;;;;;;_R92;;;;;;;;;;;;;;;;;;;;;;;$|"),
                Arguments.of("[H]C(=O)c1ccc(cc1)C(=O)NCCCCCCO[*] |$;;;;;;;;;;;;;;;;;;;_R92$|"),
                Arguments.of("[*]OC[C@]12CO[C@H]([C@H]([*])O1)[C@H]2O[*] |$_R91;;;;;;;;_R90;;;;_R92$|"),
                Arguments.of("OC[C@H]([*])O[C@H](CO[*])CO[*] |$;;;_R90;;;;;_R91;;;_R92$|"),
                Arguments.of("FC(F)(F)C(OCCO[C@H]1[C@H]([*])O[C@H](CO[*])[C@H]1O[*])(C(F)(F)F)C(F)(F)F |$;;;;;;;;;;;_R90;;;;;_R91;;;_R92;;;;;;;;$|"),
                Arguments.of("O[C@@H]1[C@@H](CO[*])O[C@@H]([*])[C@@H]1O[*] |$;;;;;_R91;;;_R90;;;_R92$|"),
                Arguments.of("CO[C@H]1[C@H]([*])O[C@H](CO[*])[C@H]1O[*] |$;;;;_R90;;;;;_R91;;;_R92$|"),
                Arguments.of("O[C@H]1[C@H]([*])O[C@H](CO[*])[C@H]1N[*] |$;;;_R90;;;;;_R91;;;_R92$|"),
                Arguments.of("[*]OC[C@@]12CO[C@@H]([C@H]([*])O1)[C@@H]2O[*] |$_R91;;;;;;;;_R90;;;;_R92$|"),
                Arguments.of("O[C@H]1[C@H]([*])O[C@H](CO[*])[C@H]1O[*] |$;;;_R90;;;;;_R91;;;_R92$|"),
                Arguments.of("[*]OC[C@H]1O[C@@H]([*])C[C@@H]1O[*] |$_R91;;;;;;_R90;;;;_R92$|"),
                Arguments.of("COCCO[C@H]1[C@H]([*])O[C@H](CO[*])[C@H]1O[*] |$;;;;;;;_R90;;;;;_R91;;;_R92$|"),
                Arguments.of("[*]OC[C@@H]1CN([*])C[C@H]([*])O1 |$_R91;;;;;;_R92;;;_R90;$|"),
                Arguments.of("Oc1ccc2c(Oc3cc(O)ccc3C22OC(=O)c3ccc(cc23)C(=O)NCCCCC(CO[*])O[*])c1 |$;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;_R91;;_R92;$|"),
                Arguments.of("[*]N([*])CCCCCCO[*] |$_R91;;_R90;;;;;;;;_R92$|")
        );
    }

    @ParameterizedTest
    @MethodSource("fragmentSmiles")
    void testSmilesParsing(String rawInput) throws IOException {
        String inputSmiles = rawInput.split(" ")[0];
        String lexicallyCleaned = inputSmiles.replace("*", "6He");
        log.debug("lexicallyCleaned: %s\n", lexicallyCleaned);

        Chemical parsedChemical = Chemical.parse(inputSmiles);
        Chemical cleanedChemical = Chem.RemoveQueryFeaturesForPseudoInChI(parsedChemical);
        String processedSmiles = "unknown";
        try {
            processedSmiles= cleanedChemical.toSmiles();
        }catch (NullPointerException npe) {
            log.error("Error creating output SMILES from pased {}", inputSmiles);
        }
        log.debug("processedSmiles: {}", processedSmiles);
        String inChIKey = cleanedChemical.toInchi().getKey();

        String expectedInChIKey =getInChiKey(lexicallyCleaned);
        log.debug("Created InChIKey: {}", inChIKey);
        Assertions.assertEquals(expectedInChIKey, inChIKey);
        String molfile = cleanedChemical.toMol();
        log.trace("molfile: {}}", molfile);
        Assertions.assertFalse(molfile.contains("*"));
    }

    @Test
    void testSmilesParse2() throws IOException {
        //test like we have a vocabulary term here
        FragmentVocabularyTerm fragmentVocabularyTerm = new FragmentVocabularyTerm();
        fragmentVocabularyTerm.setFragmentStructure("CCCCCCCC\\C=C/CCCCCCCCCCCCOCC(CO[*])OCCCCCCCCCCCC\\C=C/CCCCCCCC |$;;;;;;;;;;;;;;;;;;;;;;;;;;;_R92;;;;;;;;;;;;;;;;;;;;;;;$|");
        Optional<String> hash= CVFragmentStructureValidator.getHash(fragmentVocabularyTerm);
        Assertions.assertTrue(hash.isPresent());
        log.trace("hash: {}", hash.get());
    }

    private String getInChiKey(String smiles) throws IOException {
        Chemical chem = Chemical.parse(smiles);
        String inChIKey = chem.toInchi().getKey();
        return inChIKey;
    }
}
