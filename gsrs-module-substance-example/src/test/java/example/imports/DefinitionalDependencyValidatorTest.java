package example.imports;

import example.GsrsModuleSubstanceApplication;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.models.v1.*;
import ix.ginas.utils.validation.validators.DefinitionalDependencyValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@Slf4j
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
public class DefinitionalDependencyValidatorTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Test
    public void testMissingSubstanceFlag() {
        ProteinSubstance testProtein= createProtein();
        String missingSubstanceName ="Random missing substance";
        Relationship newRelationship = new Relationship();
        newRelationship.relatedSubstance = new SubstanceReference();
        newRelationship.relatedSubstance.refuuid= UUID.randomUUID().toString();
        newRelationship.relatedSubstance.refPname= missingSubstanceName;
        newRelationship.type="Metabolite->Parent";
        testProtein.addRelationship(newRelationship);

        DefinitionalDependencyValidator validator = new DefinitionalDependencyValidator();
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse<Substance> response= validator.validate(testProtein, null);
        Assertions.assertTrue(response.getValidationMessages().stream().anyMatch(vm->vm.getMessage().contains("is missing")
                && vm.getMessage().contains(missingSubstanceName)
                && vm.getMessageType()== ValidationMessage.MESSAGE_TYPE.ERROR));
    }

    @Test
    public void testMissingSubstanceDefFlag() {
        ProteinSubstance testProtein= createProtein();
        String missingSubstanceName ="Random missing substance";
        AgentModification agentModification = new AgentModification();
        agentModification.agentModificationProcess ="CULTURE";
        agentModification.agentSubstance = new SubstanceReference();
        agentModification.agentSubstance.refuuid= UUID.randomUUID().toString();
        agentModification.agentSubstance.refPname= missingSubstanceName;
        testProtein.modifications = new Modifications();
        testProtein.modifications.agentModifications.add(agentModification);
        DefinitionalDependencyValidator validator = new DefinitionalDependencyValidator();
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse<Substance> response= validator.validate(testProtein, null);
        Assertions.assertTrue(response.getValidationMessages().stream().anyMatch(vm->vm.getMessage().contains("is missing")
                && vm.getMessage().contains(missingSubstanceName)
                && vm.getMessageType()== ValidationMessage.MESSAGE_TYPE.ERROR));
    }

    @Test
    public void testNoMissingSubstanceFlag() {
        ProteinSubstance testProtein= createProtein();
        String missingSubstanceName ="Random missing substance";

        DefinitionalDependencyValidator validator = new DefinitionalDependencyValidator();
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse<Substance> response= validator.validate(testProtein, null);
        Assertions.assertFalse(response.getValidationMessages().stream().anyMatch(vm->vm.getMessage().contains("is missing")
                && vm.getMessage().contains(missingSubstanceName)
                && vm.getMessageType()== ValidationMessage.MESSAGE_TYPE.ERROR));
    }

    @Test
    public void testSameSubstanceNoFlag() {
        ProteinSubstance testProtein= createProtein();
        UUID substanceUuid=UUID.randomUUID();
        testProtein.setUuid(substanceUuid);
        String missingSubstanceName ="Random missing substance";
        Relationship newRelationship = new Relationship();
        newRelationship.relatedSubstance = new SubstanceReference();
        newRelationship.relatedSubstance.refuuid= substanceUuid.toString();
        newRelationship.relatedSubstance.refPname= missingSubstanceName;
        newRelationship.type="Same->Thing";
        testProtein.addRelationship(newRelationship);

        DefinitionalDependencyValidator validator = new DefinitionalDependencyValidator();
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse<Substance> response= validator.validate(testProtein, null);
        Assertions.assertFalse(response.getValidationMessages().stream().anyMatch(vm->vm.getMessage().contains("is missing")
                && vm.getMessage().contains(missingSubstanceName)
                && vm.getMessageType()== ValidationMessage.MESSAGE_TYPE.ERROR));
    }
    private ProteinSubstance createProtein(){
        ProteinSubstanceBuilder builder = new ProteinSubstanceBuilder();
        Protein protein = new Protein();
        Subunit unit = new Subunit();
        unit.sequence="MVAEVCSMPAASAVKKPFDLRSKMGKWCHHRFPCCRGSGKSNMGTSGDHDDSFMKTLRSK\n" +
                "MGKCCHHCFPCCRGSGTSNVGTSGDHDNSFMKTLRSKMGKWCCHCFPCCRGSGKSNVGTW\n" +
                "GDYDDSAFMEPRYHVRREDLDKLHRAAWWGKVPRKDLIVMLRDTDMNKRDKQKRTALHLA\n" +
                "SANGNSEVVQLLLDRRCQLNVLDNKKRTALIKAVQCQEDECVLMLLEHGADGNIQDEYGN\n" +
                "TALHYAIYNEDKLMAKALLLYGADIESKNKCGLTPLLLGVHEQKQQVVKFLIKKKANLNA\n" +
                "LDRYGRTALILAVCCGSASIVNLLLEQNVDVSSQDLSGQTAREYAVSSHHHVICELLSDY\n" +
                "KEKQMLKISSENSNPEQDLKLTSEEESQRLKVSENSQPEKMSQEPEINKDCDREVEEEIK\n" +
                "KHGSNPVGLPENLTNGASAGNGDDGLIPQRKSRKPENQQFPDTENEEYHSDEQNDTQKQL\n" +
                "SEEQNTGISQDEILTNKQKQIEVAEKEMNSKLSLSHKKEEDLLRENSMLREEIAMLRLEL\n" +
                "DETKHQNQLRENKILEEIESVKEKLLKAIQLNEEALTKTS";
        protein.subunits.add(unit);
        builder.setProtein(protein);
        Reference reference = new Reference();
        reference.docType="UNIPROT";
        reference.citation="A0JP26";
        reference.url="https://rest.uniprot.org/uniprotkb/A0JP26";
        builder.addReference(reference);

        builder.addName("POTE ankyrin domain family member B3");
        ProteinSubstance substance = builder.build();
        return substance;
    }
}
