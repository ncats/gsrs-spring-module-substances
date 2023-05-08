package example.substance.validation;

import example.GsrsModuleSubstanceApplication;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.models.Group;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.PubChemValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;

@ActiveProfiles("test")
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
@Slf4j
public class PubChemValidatorTest extends AbstractSubstanceJpaFullStackEntityTest {

    private String molfileAspirin = "\n" +
            "   JSDraw212282218372D\n" +
            "\n" +
            " 13 13  0  0  0  0            999 V2000\n" +
            "   11.8560   -6.1880    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   10.5050   -5.4080    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   10.5050   -3.8480    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   13.2070   -5.4080    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   13.2070   -3.8480    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   11.8560   -3.0680    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   14.5580   -3.0680    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   14.5580   -1.5080    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   15.9090   -3.8480    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   14.5580   -6.1880    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   14.5580   -7.7480    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   15.9090   -8.5280    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   13.2070   -8.5280    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "  1  2  2  0  0  0  0\n" +
            "  2  3  1  0  0  0  0\n" +
            "  1  4  1  0  0  0  0\n" +
            "  4  5  2  0  0  0  0\n" +
            "  5  6  1  0  0  0  0\n" +
            "  6  3  2  0  0  0  0\n" +
            "  5  7  1  0  0  0  0\n" +
            "  7  8  2  0  0  0  0\n" +
            "  7  9  1  0  0  0  0\n" +
            "  4 10  1  0  0  0  0\n" +
            " 10 11  1  0  0  0  0\n" +
            " 11 12  1  0  0  0  0\n" +
            " 11 13  2  0  0  0  0\n" +
            "M  END";
    private String offbeatMolfile = "\n" +
            "  ACCLDraw12282219152D\n" +
            "\n" +
            " 24 24  0  0  0  0  0  0  0  0999 V2000\n" +
            "    6.5067   -6.1242    0.0000 Si  0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    8.2751   -5.1025    0.0000 Si  0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    7.0975   -5.1025    0.0000 Si  0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    8.8658   -6.1257    0.0000 Si  0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    7.0997   -7.1515    0.0000 Si  0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    8.2802   -7.1515    0.0000 Si  0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    5.3256   -6.1242    0.0000 P   0  0  3  0  0  0  0  0  0  0  0  0\n" +
            "    8.8656   -4.0797    0.0000 P   0  0  3  0  0  0  0  0  0  0  0  0\n" +
            "    6.5070   -4.0797    0.0000 P   0  0  3  0  0  0  0  0  0  0  0  0\n" +
            "   10.0469   -6.1257    0.0000 P   0  0  3  0  0  0  0  0  0  0  0  0\n" +
            "    6.5092   -8.1743    0.0000 P   0  0  3  0  0  0  0  0  0  0  0  0\n" +
            "    8.8707   -8.1743    0.0000 P   0  0  3  0  0  0  0  0  0  0  0  0\n" +
            "    5.9165   -5.1011    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    7.6877   -6.1256    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    7.6845   -6.1254    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    7.6847   -6.1285    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    9.4612   -7.1486    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    7.6903   -6.1286    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    4.7350   -5.1013    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   10.0467   -4.0797    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    7.0975   -3.0568    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   10.6375   -7.1486    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    5.3281   -8.1743    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    8.2802   -9.1972    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "  6  4  1  0  0  0  0\n" +
            "  5  6  2  0  0  0  0\n" +
            "  2  3  1  0  0  0  0\n" +
            "  1  5  1  0  0  0  0\n" +
            "  4  2  2  0  0  0  0\n" +
            "  3  1  2  0  0  0  0\n" +
            "  1  7  1  0  0  0  0\n" +
            "  2  8  1  0  0  0  0\n" +
            "  3  9  1  0  0  0  0\n" +
            "  4 10  1  0  0  0  0\n" +
            "  5 11  1  0  0  0  0\n" +
            "  6 12  1  0  0  0  0\n" +
            "  1 13  1  0  0  0  0\n" +
            "  3 14  1  0  0  0  0\n" +
            "  2 15  1  0  0  0  0\n" +
            "  4 16  1  0  0  0  0\n" +
            "  6 17  1  0  0  0  0\n" +
            "  5 18  1  0  0  0  0\n" +
            "  7 19  1  0  0  0  0\n" +
            "  8 20  1  0  0  0  0\n" +
            "  9 21  1  0  0  0  0\n" +
            " 10 22  1  0  0  0  0\n" +
            " 11 23  1  0  0  0  0\n" +
            " 12 24  1  0  0  0  0\n" +
            "M  END\n";

    @Test
    public void testPubChemMatch() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemAspirin = builder
                .addName("acetylsalicylic acid")
                .setStructureWithDefaultReference(molfileAspirin)
                .setAccess(Collections.singleton(new Group("protected")))
                .build();

        PubChemValidator validator = new PubChemValidator();
        ValidationResponse<Substance> response = validator.validate(chemAspirin, null);
        Assertions.assertTrue(response.getValidationMessages().stream().anyMatch(m -> m.getMessageType().equals(ValidationMessage.MESSAGE_TYPE.WARNING) && m.getMessage().contains("marked non-public")));
    }

    @Test
    public void testPubChemNotMarkedMatch() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemAspirin = builder
                .addName("acetylsalicylic acid")
                .setStructureWithDefaultReference(molfileAspirin)
                .build();

        PubChemValidator validator = new PubChemValidator();
        ValidationResponse<Substance> response = validator.validate(chemAspirin, null);
        Assertions.assertTrue(response.getValidationMessages().stream().noneMatch(m -> m.getMessageType().equals(ValidationMessage.MESSAGE_TYPE.WARNING) && m.getMessage().contains("marked non-public")));
    }

    @Test
    public void testPubChemNoMatch() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemWeird = builder
                .addName("some chemical")
                .setStructureWithDefaultReference(offbeatMolfile)
                .setAccess(Collections.singleton(new Group("protected")))
                .build();

        PubChemValidator validator = new PubChemValidator();
        ValidationResponse<Substance> response = validator.validate(chemWeird, null);
        Assertions.assertTrue(response.getValidationMessages().stream().noneMatch(m -> m.getMessageType().equals(ValidationMessage.MESSAGE_TYPE.WARNING) && m.getMessage().contains("marked non-public")));
    }

}
