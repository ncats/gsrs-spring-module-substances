package example.substance.validation;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.MixtureSubstance;
import ix.ginas.models.v1.NucleicAcidSubstance;
import ix.ginas.models.v1.PolymerSubstance;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.SpecifiedSubstanceGroup1Substance;
import ix.ginas.models.v1.StructurallyDiverseSubstance;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("fullstack")
public class CreateRep18SubstanceTypesTest extends AbstractSubstanceJpaEntityTest {

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void createFirstChemicalFromRep18() throws Exception {
        ChemicalSubstance created = (ChemicalSubstance) ensurePass(
                substanceEntityService.createEntity(firstOfClass(Substance.SubstanceClass.chemical), true));

        assertNotNull(created.getStructure());
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void createFirstProteinFromRep18() throws Exception {
        ProteinSubstance created = (ProteinSubstance) ensurePass(
                substanceEntityService.createEntity(firstOfClass(Substance.SubstanceClass.protein), true));

        assertNotNull(created.protein);
        assertNotNull(created.protein.subunits);
        assertFalse(created.protein.subunits.isEmpty());
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void createFirstNucleicAcidFromRep18() throws Exception {
        JsonNode json = firstOfClass(Substance.SubstanceClass.nucleicAcid);
        NucleicAcidSubstance created = (NucleicAcidSubstance) ensurePass(
                substanceEntityService.createEntity(json, true));

        assertEquals(json.get("uuid").asText(), created.getUuid().toString());
        assertNotNull(created.nucleicAcid);
        assertNotNull(created.nucleicAcid.getSubunits());
        assertFalse(created.nucleicAcid.getSubunits().isEmpty());
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void createFirstPolymerFromRep18() throws Exception {
        PolymerSubstance created = (PolymerSubstance) ensurePass(
                substanceEntityService.createEntity(firstOfClass(Substance.SubstanceClass.polymer), true));

        assertNotNull(created.polymer);
        assertTrue(created.polymer.displayStructure != null
                || created.polymer.idealizedStructure != null
                || (created.polymer.monomers != null && !created.polymer.monomers.isEmpty()));
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void createFirstStructurallyDiverseFromRep18() throws Exception {
        StructurallyDiverseSubstance created = (StructurallyDiverseSubstance) ensurePass(
                substanceEntityService.createEntity(firstOfClass(Substance.SubstanceClass.structurallyDiverse), true));

        assertNotNull(created.structurallyDiverse);
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void createFirstSpecifiedSubstanceGroup1FromRep18() throws Exception {
        SpecifiedSubstanceGroup1Substance created = (SpecifiedSubstanceGroup1Substance) ensurePass(
                substanceEntityService.createEntity(firstOfClass(Substance.SubstanceClass.specifiedSubstanceG1), true));

        assertNotNull(created.specifiedSubstance);
        assertNotNull(created.specifiedSubstance.constituents);
        assertFalse(created.specifiedSubstance.constituents.isEmpty());
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void createFirstMixtureFromRep18() throws Exception {
        MixtureSubstance created = (MixtureSubstance) ensurePass(
                substanceEntityService.createEntity(firstOfClass(Substance.SubstanceClass.mixture), true));

        assertNotNull(created.mixture);
        assertNotNull(created.mixture.getMixture());
        assertFalse(created.mixture.getMixture().isEmpty());
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void createFirstConceptFromRep18() throws Exception {
        Substance created = ensurePass(
                substanceEntityService.createEntity(firstOfClass(Substance.SubstanceClass.concept), true));

        assertEquals(Substance.SubstanceClass.concept, created.substanceClass);
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void createFirstReferenceFromRep18() throws Exception {
        Substance created = ensurePass(
                substanceEntityService.createEntity(firstOfClass(Substance.SubstanceClass.reference), true));

        assertNotNull(created);
    }

    private JsonNode firstOfClass(Substance.SubstanceClass substanceClass) throws Exception {
        File dataFile = new ClassPathResource("testdumps/rep18.gsrs").getFile();
        return yieldSubstancesFromGsrsFile(dataFile, substanceClass)
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("No " + substanceClass + " record found in rep18.gsrs"));
    }
}
