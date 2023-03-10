package gov.nih.ncats;

import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import validators.DEAValidator;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class DEAValidatorTest extends AbstractSubstanceJpaEntityTest {

    private static boolean configured = false;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @BeforeEach
    public void setup() {
        if (!configured) {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(DEAValidator.class);
            config.setNewObjClass(Substance.class);
            factory.addValidator("substances", config);
            configured = true;
            System.out.println("configured!");
        }
    }

    @Test
    public void testInit() throws IOException {
        DEAValidator validator = new DEAValidator();
        validator.init();

        Map<String, String> inchiKeyToDeaNumber= validator.getInchiKeyToDeaNumber();

        Assertions.assertTrue(inchiKeyToDeaNumber.size()>0);
    }

    @Test
    public void testSubstance1() throws Exception {
        //create a substance that's on the list
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemical= builder.setStructureWithDefaultReference("COc1ccc(CC(C)N)cc1")
                .addName("d")
                .addCode("UNII", "OVB8F8P39Q")
                .build();
        ValidationResponse response = substanceEntityService.validateEntity(chemical.toFullJsonNode());
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        Assertions.assertTrue( s1.anyMatch(m->
                m.getMessageType()== ValidationMessage.MESSAGE_TYPE.WARNING && m.getMessage().contains("This substance has DEA schedule:")));
    }
}
