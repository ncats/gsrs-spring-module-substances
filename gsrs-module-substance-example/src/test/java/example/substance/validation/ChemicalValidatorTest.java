package example.substance.validation;

import example.GsrsModuleSubstanceApplication;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.chem.StructureProcessor;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles="Admin")
@Slf4j
public class ChemicalValidatorTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    StructureProcessor structureProcessor;

    @Autowired
    private TestGsrsValidatorFactory factory;

    private final String molV3000= "\n" +
            "  SciTegic05122218582D\n" +
            "\n" +
            "  0  0  0  0  0  0            999 V3000\n" +
            "M  V30 BEGIN CTAB\n" +
            "M  V30 COUNTS 11 9 0 0 0\n" +
            "M  V30 BEGIN ATOM\n" +
            "M  V30 1 N 3.875 -7.0625 0 0\n" +
            "M  V30 2 C 4.8982 -6.4718 0 0\n" +
            "M  V30 3 C 5.9213 -7.0625 0 0\n" +
            "M  V30 4 C 6.9445 -6.4718 0 0\n" +
            "M  V30 5 N 7.9677 -7.0625 0 0\n" +
            "M  V30 6 C 8.9908 -6.4718 0 0\n" +
            "M  V30 7 C 10.014 -7.0625 0 0\n" +
            "M  V30 8 C 11.0372 -6.4718 0 0\n" +
            "M  V30 9 N 12.0604 -7.0625 0 0\n" +
            "M  V30 10 * 7.9063 -9.3125 0 0\n" +
            "M  V30 11 C 8.9291 -8.7219 0 0\n" +
            "M  V30 END ATOM\n" +
            "M  V30 BEGIN BOND\n" +
            "M  V30 1 1 1 2\n" +
            "M  V30 2 1 2 3\n" +
            "M  V30 3 1 3 4\n" +
            "M  V30 4 1 4 5\n" +
            "M  V30 5 1 5 6\n" +
            "M  V30 6 1 6 7\n" +
            "M  V30 7 1 7 8\n" +
            "M  V30 8 1 8 9\n" +
            "M  V30 9 1 11 10 ENDPTS=(3 1 5 9) ATTACH=ANY\n" +
            "M  V30 END BOND\n" +
            "M  V30 END CTAB\n" +
            "M  END\n";

    private final String molV2000= "\n" +
            "  SciTegic08152218232D\n" +
            "\n" +
            " 61 53  0  0  0  0            999 V2000\n" +
            "    2.9966    0.9517    0.0000 C   0  0\n" +
            "    2.2345    1.3897    0.0000 C   0  0\n" +
            "    2.9966    0.1172    0.0000 C   0  0\n" +
            "    3.5414    1.3690    0.0000 C   0  0\n" +
            "    1.4828    1.0172    0.0000 C   0  0\n" +
            "    2.2345   -0.3103    0.0000 C   0  0\n" +
            "    3.5414    2.2069    0.0000 N   0  3\n" +
            "    1.4828    0.1414    0.0000 C   0  0\n" +
            "    3.5414    3.0414    0.0000 C   0  0\n" +
            "    4.3586    2.2793    0.0000 C   0  0\n" +
            "    2.7586    2.2793    0.0000 C   0  0\n" +
            "   -3.9586    1.2276    0.0000 C   0  0\n" +
            "   -4.7207    1.6655    0.0000 C   0  0\n" +
            "   -3.9586    0.3897    0.0000 C   0  0\n" +
            "   -3.4172    1.6517    0.0000 C   0  0\n" +
            "   -5.4552    1.2276    0.0000 C   0  0\n" +
            "   -4.7207   -0.0345    0.0000 C   0  0\n" +
            "   -3.4172    2.4897    0.0000 N   0  3\n" +
            "   -5.4552    0.3897    0.0000 C   0  0\n" +
            "   -2.5966    2.5586    0.0000 C   0  0\n" +
            "   -4.2034    2.5586    0.0000 C   0  0\n" +
            "   -3.4172    3.3276    0.0000 C   0  0\n" +
            "    5.4690   -2.7483    0.0000 C   0  0\n" +
            "    5.6828   -3.5483    0.0000 C   0  0\n" +
            "    4.6690   -2.5345    0.0000 C   0  0\n" +
            "    5.0828   -4.1345    0.0000 C   0  0\n" +
            "    4.4448   -1.7345    0.0000 C   0  0\n" +
            "    4.2828   -3.9172    0.0000 C   0  0\n" +
            "    3.6552   -1.5207    0.0000 C   0  0\n" +
            "    4.0724   -3.1172    0.0000 C   0  0\n" +
            "    3.4345   -0.7207    0.0000 C   0  0\n" +
            "    3.2724   -2.9034    0.0000 C   0  0\n" +
            "    2.6345   -0.5034    0.0000 C   0  0\n" +
            "    3.0586   -2.1034    0.0000 C   0  0\n" +
            "    2.4207    0.2966    0.0000 X   0  0\n" +
            "   -1.4897   -2.4724    0.0000 C   0  0\n" +
            "   -1.2793   -3.2724    0.0000 C   0  0\n" +
            "   -2.2897   -2.2586    0.0000 C   0  0\n" +
            "   -1.8690   -3.8586    0.0000 C   0  0\n" +
            "   -2.5069   -1.4586    0.0000 C   0  0\n" +
            "   -2.6793   -3.6345    0.0000 C   0  0\n" +
            "   -3.3034   -1.2414    0.0000 C   0  0\n" +
            "   -2.8897   -2.8345    0.0000 C   0  0\n" +
            "   -3.5172   -0.4414    0.0000 C   0  0\n" +
            "   -3.6897   -2.6207    0.0000 C   0  0\n" +
            "   -4.3172   -0.2207    0.0000 C   0  0\n" +
            "   -3.8966   -1.8207    0.0000 C   0  0\n" +
            "   -4.5310    0.5690    0.0000 X   0  0\n" +
            "    0.3552    1.6897    0.0000 N   0  3\n" +
            "    1.0448    1.6966    0.0000 C   0  0\n" +
            "    0.3828    0.9414    0.0000 C   0  0\n" +
            "    0.3310    2.3172    0.0000 C   0  0\n" +
            "   -0.3690    1.6897    0.0000 C   0  0\n" +
            "    2.1345    0.9276    0.0000 X   0  0\n" +
            "   -5.4655    1.8517    0.0000 C   0  0\n" +
            "   -4.9793    1.1793    0.0000 X   0  0\n" +
            "    1.1034   -0.3345    0.0000 C   0  0\n" +
            "    1.8552    0.4172    0.0000 X   0  0\n" +
            "    0.4207    3.4897    0.0000 Cl  0  5\n" +
            "    5.1828    3.3414    0.0000 Cl  0  5\n" +
            "   -1.7793    3.6172    0.0000 Cl  0  5\n" +
            "  4  7  1  0\n" +
            "  5  8  1  0\n" +
            "  7  9  1  0\n" +
            " 23 24  1  0\n" +
            " 23 25  1  0\n" +
            " 24 26  1  0\n" +
            " 25 27  1  0\n" +
            " 26 28  1  0\n" +
            " 27 29  1  0\n" +
            " 28 30  1  0\n" +
            " 29 31  1  0\n" +
            " 30 32  1  0\n" +
            " 31 33  1  0\n" +
            " 32 34  1  0\n" +
            " 33 35  1  0\n" +
            "  7 10  1  0\n" +
            " 12 13  1  0\n" +
            " 12 14  2  0\n" +
            " 12 15  1  0\n" +
            " 13 16  2  0\n" +
            " 14 17  1  0\n" +
            " 36 37  1  0\n" +
            " 36 38  1  0\n" +
            " 37 39  1  0\n" +
            " 38 40  1  0\n" +
            " 39 41  1  0\n" +
            " 40 42  1  0\n" +
            " 41 43  1  0\n" +
            " 42 44  1  0\n" +
            " 43 45  1  0\n" +
            " 44 46  1  0\n" +
            " 45 47  1  0\n" +
            " 46 48  1  0\n" +
            " 15 18  1  0\n" +
            " 16 19  1  0\n" +
            " 18 20  1  0\n" +
            " 18 21  1  0\n" +
            " 18 22  1  0\n" +
            " 17 19  2  0\n" +
            " 49 50  1  0\n" +
            " 49 51  1  0\n" +
            " 49 52  1  0\n" +
            " 49 53  1  0\n" +
            " 50 54  1  0\n" +
            "  7 11  1  0\n" +
            "  6  8  2  0\n" +
            " 55 56  1  0\n" +
            "  1  2  1  0\n" +
            "  1  3  2  0\n" +
            " 57 58  1  0\n" +
            "  1  4  1  0\n" +
            "  2  5  2  0\n" +
            "  3  6  1  0\n" +
            "M  CHG  6   7   1  18   1  49   1  59  -1  60  -1  61  -1\n" +
            "M  END";

    @Test
    public void testV3000MolAllowed() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemV3000 = builder
                .addName("Some name")
                .setStructureWithDefaultReference(molV3000)
                .build();
        ChemicalValidator chemicalValidator = new ChemicalValidator();
        chemicalValidator.setStructureProcessor(structureProcessor);
        chemicalValidator.setAllowV3000Molfiles(true);
        ValidationResponse<Substance> response= chemicalValidator.validate(chemV3000, null);
        Assertions.assertTrue(response.getValidationMessages().stream().noneMatch(m->m.isError() && m.getMessage().contains("V3000 molfile")));
    }

    @Test
    public void testV2000V3000MolNotAllowed() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemV2000 = builder
                .addName("Some name")
                .setStructureWithDefaultReference(molV2000)
                .build();
        ChemicalValidator chemicalValidator = new ChemicalValidator();
        chemicalValidator.setStructureProcessor(structureProcessor);
        chemicalValidator.setAllowV3000Molfiles(false);
        ValidationResponse<Substance> response= chemicalValidator.validate(chemV2000, null);
        Assertions.assertTrue(response.getValidationMessages().stream().noneMatch(m->m.isError()));
    }

    @Test
    public void testV2000V3000MolAllowed() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemV2000 = builder
                .addName("Some name")
                .setStructureWithDefaultReference(molV2000)
                .build();
        ChemicalValidator chemicalValidator = new ChemicalValidator();
        chemicalValidator.setStructureProcessor(structureProcessor);
        chemicalValidator.setAllowV3000Molfiles(true);
        ValidationResponse<Substance> response= chemicalValidator.validate(chemV2000, null);
        Assertions.assertTrue(response.getValidationMessages().stream().noneMatch(m->m.isError() && m.getMessage().contains("V3000 molfile")));
    }

    @Test
    public void testV3000MolNotAllowed() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemV3000 = builder
                .addName("Some name")
                .setStructureWithDefaultReference(molV3000)
                .build();
        ChemicalValidator chemicalValidator = new ChemicalValidator();
        chemicalValidator.setStructureProcessor(structureProcessor);
        chemicalValidator.setAllowV3000Molfiles(false);
        ValidationResponse<Substance> response= chemicalValidator.validate(chemV3000, null);
        Assertions.assertTrue(response.getValidationMessages().stream().anyMatch(m->m.isError() && m.getMessage().contains("V3000 molfile")));
    }
}
