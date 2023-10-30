package example.substance.validation;

import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.core.models.Keyword;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.BasicNameValidator;
import ix.ginas.utils.validation.validators.NamesValidator;
import org.checkerframework.checker.units.qual.K;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

public class NamesValidatorTest extends AbstractSubstanceJpaEntityTest {

    /*
    Confirm correct new (January 2023) behavior - duplicate names -> warning
     */
    @Test
    public void testValidationNoErrors() {
        NamesValidator validator = new NamesValidator();
        validator= AutowireHelper.getInstance().autowireAndProxy(validator);
        ChemicalSubstance chemical =createSimpleChemicalDuplicateNames();
        ValidationResponse<Substance> response = validator.validate(chemical, null);
        Assertions.assertEquals(0, response.getValidationMessages().stream()
                .filter(m -> m.getMessageType().equals(ValidationMessage.MESSAGE_TYPE.ERROR)).count());
        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessageType().equals(ValidationMessage.MESSAGE_TYPE.WARNING)).count());
    }

    private ChemicalSubstance createSimpleChemicalDuplicateNames(){
        String basicName = "ethanol";
        Reference referencePublic = new Reference();
        referencePublic.citation="Ethanol";
        referencePublic.docType="Wikipedia";
        referencePublic.publicDomain=true;
        referencePublic.setAccess(new HashSet<>());
        referencePublic.tags.add( new Keyword("PUBLIC_DOMAIN_RELEASE"));

        Name ethanol1 = new Name();
        ethanol1.name=basicName;
        ethanol1.addReference(referencePublic);
        ethanol1.languages.add(new Keyword("en"));
        ethanol1.displayName=true;
        ethanol1.type="sy";
        Name ethanol2 = new Name();
        ethanol2.name=basicName;
        ethanol2.addReference(referencePublic);
        ethanol2.languages.add(new Keyword("en"));
        ethanol2.displayName=false;
        ethanol2.type="sy";

        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemical = builder
                .addName(ethanol1)
                .addName(ethanol2)
                .addReference(referencePublic)
                .setStructureWithDefaultReference("CCO")
                .build();
        return chemical;
    }
}
