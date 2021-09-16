package example.prot;

import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.ProteinValidator;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.assertj.core.error.AssertionErrorCreator;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;

/**
 *
 * @author mitch
 */
@WithMockUser(username = "admin", roles = "Admin")
public class ProteinValidationTest extends AbstractSubstanceJpaEntityTest {

    public ProteinValidationTest() {
    }

    private boolean configured = false;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @BeforeEach
    public void setup() {
        if (!configured) {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(ProteinValidator.class);
            config.setNewObjClass(ProteinSubstance.class);

            factory.addValidator("substances", config);
            configured = true;
        }
    }

    @Test
    public void validateProteinTest() throws Exception {
        ProteinSubstance protein = getProteinFromFile("protein1");
        ValidationResponse response = substanceEntityService.validateEntity(protein.toFullJsonNode());
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        Assert.assertEquals(1, s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("Disulfide Link"))
                .count());

        Substance saved= this.substanceRepository.save(protein);
        Assertions.assertNotNull(saved);
    }

    private ProteinSubstance getProteinFromFile(String name) {
        try {
            File proteinFile = new ClassPathResource("testJSON/" + name + ".json").getFile();
            ProteinSubstanceBuilder builder = SubstanceBuilder.from(proteinFile);
            return builder.build();
        } catch (IOException ex) {
            Logger.getLogger(ProtCalculationTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
