package example.chem;

import ix.core.chem.InchiStandardizer;
import ix.ginas.models.v1.GinasChemicalStructure;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class SmilesGenerationTest {

    @Test
    void generateSmiles1() throws IOException {
        String molfileText = IOUtils.toString(
                this.getClass().getResourceAsStream("/molfiles/TG72BS085Y.mol"),
                "UTF-8"
        );
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile = molfileText;
        InchiStandardizer standardizer = new InchiStandardizer();
        String smiles = standardizer.canonicalSmiles(structure, molfileText);
        log.warn("canonical SMILES within first test: {}", smiles);
        assertNotNull(smiles);
    }

    @Test
    void generateSmiles2() throws IOException {
        String molfileText = IOUtils.toString(
                this.getClass().getResourceAsStream("/molfiles/TG72BS085Y_cleaned2.mol"),
                "UTF-8"
        );
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile = molfileText;
        InchiStandardizer standardizer = new InchiStandardizer();
        String smiles = standardizer.canonicalSmiles(structure, molfileText);
        log.warn("canonical SMILES: {}", smiles);
        assertNotNull(smiles);
    }

    /*
    No Sgroups
     */
    @Test
    void generateSmiles3() throws IOException {
        String molfileText = IOUtils.toString(
                this.getClass().getResourceAsStream("/molfiles/6L522LAQ9U.mol"),
                "UTF-8"
        );
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile = molfileText;
        InchiStandardizer standardizer = new InchiStandardizer();
        String smiles = standardizer.canonicalSmiles(structure, molfileText);
        log.warn("canonical SMILES: {}", smiles);
        assertNotNull(smiles);
    }

}
