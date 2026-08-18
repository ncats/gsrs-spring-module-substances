package example.substance.validation;

import example.GsrsModuleSubstanceApplication;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.chem.StructureProcessor;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@ActiveProfiles("test")
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles="Admin")
@Slf4j
class ChemicalValidatorTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    StructureProcessor structureProcessor;

    /*@Autowired
    private TestGsrsValidatorFactory factory;*/

    @Test
    void testV3000MolAllowed() throws IOException {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        String v3000MolfileText = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream("/molfiles/v3000.mol")),
                StandardCharsets.UTF_8
        );

        ChemicalSubstance chemV3000 = builder
                .addName("Some name")
                .setStructureWithDefaultReference(v3000MolfileText)
                .build();
        ChemicalValidator chemicalValidator = new ChemicalValidator();
        chemicalValidator.setStructureProcessor(structureProcessor);
        chemicalValidator.setAllowV3000Molfiles(true);
        ValidationResponse<Substance> response= chemicalValidator.validate(chemV3000, null);
        Assertions.assertTrue(response.getValidationMessages().stream().noneMatch(m->m.isError() && m.getMessage().contains("V3000 molfile")));
    }

    @Test
    void testV2000V3000MolNotAllowed() throws IOException {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        String v2000MolfileText = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream("/molfiles/v2000.mol")),
                StandardCharsets.UTF_8
        );
        ChemicalSubstance chemV2000 = builder
                .addName("Some name")
                .setStructureWithDefaultReference(v2000MolfileText)
                .build();
        ChemicalValidator chemicalValidator = new ChemicalValidator();
        chemicalValidator.setStructureProcessor(structureProcessor);
        chemicalValidator.setAllowV3000Molfiles(false);
        ValidationResponse<Substance> response= chemicalValidator.validate(chemV2000, null);
        Assertions.assertTrue(response.getValidationMessages().stream()
                .noneMatch(m->m.isError() && m.getMessage().contains("support V3000 molfiles")));
    }

    @Test
    void testV2000V3000MolAllowed() throws IOException {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        String v2000MolfileText = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream("/molfiles/v2000.mol")),
                StandardCharsets.UTF_8
        );

        ChemicalSubstance chemV2000 = builder
                .addName("Some name")
                .setStructureWithDefaultReference(v2000MolfileText)
                .build();
        ChemicalValidator chemicalValidator = new ChemicalValidator();
        chemicalValidator.setStructureProcessor(structureProcessor);
        chemicalValidator.setAllowV3000Molfiles(true);
        ValidationResponse<Substance> response= chemicalValidator.validate(chemV2000, null);
        Assertions.assertTrue(response.getValidationMessages().stream().noneMatch(m->m.isError() && m.getMessage().contains("V3000 molfile")));
    }

    @Test
    void testV3000MolNotAllowed() throws IOException {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        String v3000MolfileText = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream("/molfiles/v3000.mol")),
                StandardCharsets.UTF_8
        );

        ChemicalSubstance chemV3000 = builder
                .addName("Some name")
                .setStructureWithDefaultReference(v3000MolfileText)
                .build();
        ChemicalValidator chemicalValidator = new ChemicalValidator();
        chemicalValidator.setStructureProcessor(structureProcessor);
        chemicalValidator.setAllowV3000Molfiles(false);
        ValidationResponse<Substance> response= chemicalValidator.validate(chemV3000, null);
        Assertions.assertTrue(response.getValidationMessages().stream().anyMatch(m->m.isError() && m.getMessage().contains("V3000 molfile")));
    }

    @Test
    void testFlagAtomList() throws IOException {
        String molfileText = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream("/molfiles/atomlist_cn.mol")),
                StandardCharsets.UTF_8
        );

        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemWithList = builder
                .addName("Listy Molecule")
                .setStructureWithDefaultReference(molfileText)
                .build();
        ChemicalValidator chemicalValidator = new ChemicalValidator();
        chemicalValidator.setStructureProcessor(structureProcessor);
        ValidationResponse<Substance> response= chemicalValidator.validate(chemWithList, null);
        response.getValidationMessages().forEach(m-> System.out.printf("type: %s message: %s%n", m.getMessageType(), m.getMessage()));
        Assertions.assertTrue(response.getValidationMessages().stream().anyMatch(m->m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR && m.getMessage().toUpperCase().contains("LIST")));
    }

    @Test
    void testAtomListNegative()throws IOException {
        String molfileText = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream("/molfiles/no_atom_list_mol")),
                StandardCharsets.UTF_8
        );
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile = molfileText;
        ChemicalValidator validator = new ChemicalValidator();
        boolean has = validator.hasAtomLists(structure);
        Assertions.assertFalse(has);
    }

    @Test
    void testAtomListPositive()throws IOException {
        String molfileText = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream("/molfiles/atomlist_cn.mol")),
                StandardCharsets.UTF_8
        );
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile = molfileText;
        ChemicalValidator validator = new ChemicalValidator();
        boolean has = validator.hasAtomLists(structure);
        Assertions.assertTrue(has);
    }

    @Test
    void testZeroAtomStructure()throws IOException {
        String molfileText = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream("/molfiles/zero_atom.mol")),
                StandardCharsets.UTF_8
        );
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance zeroAtomSubstance = builder
                .addName("Some name")
                .setStructureWithDefaultReference(molfileText)
                .build();

        ChemicalValidator validator = new ChemicalValidator();
        validator.setStructureProcessor(structureProcessor);
        ValidationResponse<Substance> response = validator.validate(zeroAtomSubstance, null);
        Assertions.assertTrue(response.getValidationMessages().stream().anyMatch(
                m->m.isError() && m.getMessage().contains("structure with one or more atoms")));
    }

    @Test
    void testZeroAtomStructureAllowed()throws IOException {
        String molfileText = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream("/molfiles/zero_atom.mol")),
                StandardCharsets.UTF_8
        );
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance zeroAtomSubstance = builder
                .addName("Some name")
                .setStructureWithDefaultReference(molfileText)
                .build();

        ChemicalValidator validator = new ChemicalValidator();
        validator.setAllow0AtomStructures(true);
        validator.setStructureProcessor(structureProcessor);
        ValidationResponse<Substance> response = validator.validate(zeroAtomSubstance, null);
        Assertions.assertTrue(response.getValidationMessages().stream().noneMatch(
                m->m.isError() && m.getMessage().contains("structure with one or more atoms")));
    }

    @Test
    void testMultiAtomStructureAllowed()throws IOException {
        String molfileText = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream("/molfiles/4XXR6FT8ZA.mol")),
                StandardCharsets.UTF_8
        );
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance zeroAtomSubstance = builder
                .addName("Some name")
                .setStructureWithDefaultReference(molfileText)
                .build();

        ChemicalValidator validator = new ChemicalValidator();
        validator.setAllow0AtomStructures(true);
        validator.setStructureProcessor(structureProcessor);
        ValidationResponse<Substance> response = validator.validate(zeroAtomSubstance, null);
        Assertions.assertTrue(response.getValidationMessages().stream().noneMatch(
                m->m.isError() && m.getMessage().contains("structure with one or more atoms")));
    }

    @Test
    void testZeroAtomStructureBeforeAndAfter()throws IOException {
        String molfileText = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream("/molfiles/zero_atom.mol")),
                StandardCharsets.UTF_8
        );
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance zeroAtomSubstance = builder
                .addName("Some name")
                .setStructureWithDefaultReference(molfileText)
                .build();

        ChemicalSubstance substanceBefore = builder.build();
        ChemicalValidator validator = new ChemicalValidator();
        validator.setStructureProcessor(structureProcessor);
        ValidationResponse<Substance> response = validator.validate(zeroAtomSubstance, substanceBefore);
        Assertions.assertTrue(response.getValidationMessages().stream().noneMatch(
                m->m.isError() && m.getMessage().contains("structure with one or more atoms")));
    }

    @Test
    void testNullStructure(){

        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance zeroAtomSubstance = builder
                .addName("Some name")
                .build();

        ChemicalSubstance substanceBefore = builder.build();
        ChemicalValidator validator = new ChemicalValidator();
        validator.setStructureProcessor(structureProcessor);
        ValidationResponse<Substance> response = validator.validate(zeroAtomSubstance, substanceBefore);
        Assertions.assertTrue(response.getValidationMessages().stream().anyMatch(
                m->m.isError() && m.getMessage().contains("Chemical substance must have a chemical structure")));
    }

}
