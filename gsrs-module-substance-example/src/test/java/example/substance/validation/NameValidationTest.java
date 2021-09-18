package example.substance.validation;

import example.substance.AbstractSubstanceJpaEntityTest;
import gov.nih.ncats.common.Tuple;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.models.Keyword;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.NamesValidator;
import java.util.HashSet;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author mitch
 */
public class NameValidationTest extends AbstractSubstanceJpaEntityTest {

    private static boolean configured = false;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @BeforeEach
    public void setup() {
        if (!configured) {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(NamesValidator.class);
            config.setNewObjClass(Substance.class);
            factory.addValidator("substances", config);
            configured = true;
            System.out.println("configured!");
        }
    }

    @Test
    public void testDisplayNameChange() {
        NamesValidator validator = new NamesValidator();
        AutowireHelper.getInstance().autowire(validator);
        Tuple<Substance, Substance> set = createBeforeAndAfter();
        ValidationResponse<Substance> response = validator.validate(set.k(), set.v());
        long total = response.getValidationMessages()
                .stream()
                .filter(m -> m.getMessage().contains("to change the preferred name"))
                .filter(m -> m.isError())
                .count();
        Assertions.assertEquals(1, total);
    }

    @Test
    public void testDisplayNameNotChanged() {
        NamesValidator validator = new NamesValidator();
        AutowireHelper.getInstance().autowire(validator);
        Tuple<Substance, Substance> set = createBeforeAndAfter();
        
        Substance substance2=set.k();
        substance2.names.get(0).displayName = true;
        substance2.names.get(1).displayName = false;
        Substance substance1=set.v();
        ValidationResponse<Substance> response = validator.validate(substance2, substance1);
        
        long total = response.getValidationMessages()
                .stream()
                .filter(m -> m.getMessage().contains("to change the preferred name"))
                .filter(m -> m.isError())
                .count();
        Assertions.assertEquals(0, total);
    }
    
    @Test
    public void testDisplayNameChangeWithReason() {
        NamesValidator validator = new NamesValidator();
        AutowireHelper.getInstance().autowire(validator);
        Tuple<Substance, Substance> set = createBeforeAndAfter();
        Substance s=set.k();
        s.changeReason="Changed Display Name";
        ValidationResponse<Substance> response = validator.validate(s, set.v());
        long total = response.getValidationMessages()
                .stream()
                .filter(m -> m.getMessage().contains("to change the preferred name"))
                .filter(m -> m.isError())
                .count();
        Assertions.assertEquals(0, total);
    }

    private Tuple<Substance, Substance> createBeforeAndAfter() {
        SubstanceBuilder builder = new SubstanceBuilder();
        Reference ref1 = new Reference();
        ref1.docType = "Notes";
        ref1.citation = "Reference 1";
        ref1.publicDomain=true;
        ref1.setAccess(new HashSet<>());

        Name name1 = new Name();
        name1.name = "Name 1";
        name1.displayName = true;
        name1.type = "cn";
        name1.languages = new EmbeddedKeywordList();
        name1.languages.add(new Keyword("English"));
        name1.addReference(ref1);

        Name name2 = new Name();
        name2.name = "Name 2";
        name2.displayName = false;
        name2.type = "cn";
        name2.languages = new EmbeddedKeywordList();
        name2.languages.add(new Keyword("English"));
        name2.addReference(ref1);

        builder
                .addName(name1)
                .addName(name2);
        Substance substance1 = builder.build();
        //make an exact copy
        Substance substance2 = SubstanceBuilder.from(substance1.toFullJsonNode()).build();

        substance2.names.get(0).displayName = false;
        substance2.names.get(1).displayName = true;

        Assertions.assertNotEquals(substance1.names.get(0).displayName, substance2.names.get(0).displayName);
        return Tuple.of(substance2, substance1);
    }
}
