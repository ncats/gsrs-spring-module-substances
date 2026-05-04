package example.substance.validation;

import gsrs.module.substance.repository.ReferenceRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.services.PrivilegeService;
import ix.core.models.Group;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.core.models.Keyword;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.NamesValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class NamesValidatorTest {

    @Mock
    private ReferenceRepository referenceRepository;
    @Mock
    private SubstanceRepository substanceRepository;
    @Mock
    private PrivilegeService privilegeService;

    private NamesValidator validator;

    @BeforeEach
    void setUp() {
        validator = new NamesValidator();
        validator.setReferenceRepository(referenceRepository);
        validator.setSubstanceRepository(substanceRepository);
        ReflectionTestUtils.setField(validator, "privilegeService", privilegeService);

        lenient().when(substanceRepository.findByNames_NameIgnoreCase(anyString())).thenReturn(Collections.emptyList());
        lenient().when(privilegeService.canDo(anyString())).thenReturn(false);
    }

    /*
    Confirm correct new (January 2023) behavior - duplicate names -> warning
     */
    @Test
    void testValidationNoErrors() {
        ChemicalSubstance chemical =createSimpleChemicalDuplicateNames();
        ValidationResponse<Substance> response = validator.validate(chemical, null);
        Assertions.assertEquals(0, response.getValidationMessages().stream()
                .filter(m -> m.getMessageType().equals(ValidationMessage.MESSAGE_TYPE.ERROR)).count());
        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessageType().equals(ValidationMessage.MESSAGE_TYPE.WARNING)).count());
    }

    @Test
    void testChangeDisplayNameAdmin() {
        List<Substance> concepts = buildBeforeAndAfterSubstance();
        Substance conceptBefore = concepts.get(0);
        Substance conceptAfter = concepts.get(1);
        ValidationResponse<Substance> response = validator.validate(conceptAfter, conceptBefore);
        Assertions.assertTrue(response.getValidationMessages().stream().anyMatch(
                v->v.getMessageType()== ValidationMessage.MESSAGE_TYPE.WARNING && v.getMessage().contains("Preferred Name has been changed")));
    }

    @Test
    void testChangeDisplayNameEditor() {
        List<Substance> concepts = buildBeforeAndAfterSubstance();
        Substance conceptBefore = concepts.get(0);
        Substance conceptAfter = concepts.get(1);
        conceptAfter.status = Substance.STATUS_APPROVED;
        ValidationResponse<Substance> response = validator.validate(conceptAfter, conceptBefore);
        String messages = response.getValidationMessages().stream()
                .map(v -> v.getMessageType() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        Assertions.assertTrue(response.getValidationMessages().stream().anyMatch(
                v->v.getMessageType()== ValidationMessage.MESSAGE_TYPE.ERROR && v.getMessage().toLowerCase().contains("display name")), messages);
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

    private List<Substance> buildBeforeAndAfterSubstance() {
        SubstanceBuilder conceptBuilder = new SubstanceBuilder();
        String originalDisplayNameValue ="Substance One";
        String originalSynonymValue = "Substance 1";

        Reference nameRef = new Reference();
        nameRef.docType = "Book";
        nameRef.citation = "Page 2";

        Name originalDisplayName = new Name();
        originalDisplayName.displayName = true;
        originalDisplayName.setName(originalDisplayNameValue);
        Set<Group> access = new HashSet<>();
        access.add( new Group("PROTECTED"));
        originalDisplayName.setAccess(access);
        originalDisplayName.addLanguage("en");
        originalDisplayName.addReference(nameRef);
        originalDisplayName.type = "cn";
        conceptBuilder.addName(originalDisplayName);
        Name originalSynonym = new Name();
        originalSynonym.setName(originalSynonymValue);
        originalSynonym.setAccess(access);
        originalSynonym.addLanguage("en");
        originalSynonym.type = "cn";
        originalSynonym.addReference(nameRef);
        conceptBuilder.addName(originalSynonym);
        conceptBuilder.addReference(nameRef);
        Substance conceptBefore = conceptBuilder.build();

        SubstanceBuilder builder2 = new SubstanceBuilder();
        Name copyName1 = new Name();
        copyName1.displayName = false;
        copyName1.setName(originalDisplayNameValue);
        copyName1.setAccess(access);
        copyName1.addLanguage("en");
        copyName1.addReference(nameRef);
        copyName1.type = "cn";
        builder2.addName(copyName1);

        Name copySynonym = new Name();
        copySynonym.setName(originalSynonymValue);
        copySynonym.displayName = true;
        copySynonym.setAccess(access);
        copySynonym.addLanguage("en");
        copySynonym.type = "cn";
        copySynonym.addReference(nameRef);
        builder2.addName(copySynonym);
        builder2.addReference(nameRef);
        Substance conceptAfter = builder2.build();
        return Arrays.asList(conceptBefore, conceptAfter);
    }
}
