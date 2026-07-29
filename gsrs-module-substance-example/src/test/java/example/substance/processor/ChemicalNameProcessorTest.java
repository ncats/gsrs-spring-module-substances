package example.substance.processor;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.processors.ChemicalNameProcessor;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.core.models.Group;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Collections;

import java.util.List;
import java.util.stream.Collectors;

import static example.testutilities.substanceUtility.createSubstanceWithoutPubchemReference;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ChemicalNameProcessorTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Test
    void prepersistTest() throws EntityProcessor.FailProcessingException {
        Substance testSubstance = createSubstanceWithoutPubchemReference();
        List<String> namesBefore = testSubstance.names.stream()
                .map(n->n.name)
                .collect(Collectors.toList());
        String expectedIupacName = "3-phenylpropylbenzene";

        ChemicalNameProcessor processor = new ChemicalNameProcessor();
        AutowireHelper.getInstance().autowire(processor);

        processor.setNameLang("en");
        processor.setNameType("cn");
        processor.prePersist(testSubstance);
        List<String> namesAfter = testSubstance.names.stream()
                .map(n->n.name)
                .collect(Collectors.toList());
        Assertions.assertEquals(namesAfter.size(), namesBefore.size()+1);
        Assertions.assertTrue(namesAfter.contains(expectedIupacName));
    }

    @Test
    void prepersistTest2() throws EntityProcessor.FailProcessingException {
        Substance testSubstance = createSubstanceWithoutPubchemReference();
        ((ChemicalSubstance)testSubstance).getStructure().setAccess(Collections.singleton(new Group("protected")));
        List<String> namesBefore = testSubstance.names.stream()
                .map(n->n.name)
                .collect(Collectors.toList());
        String expectedIupacName = "3-phenylpropylbenzene";

        ChemicalNameProcessor processor = new ChemicalNameProcessor();
        processor.setForbiddenGroups(Collections.singletonList("PROTECTED"));
        AutowireHelper.getInstance().autowire(processor);

        processor.setNameLang("en");
        processor.setNameType("cn");
        processor.prePersist(testSubstance);
        List<String> namesAfter = testSubstance.names.stream()
                .map(n->n.name)
                .collect(Collectors.toList());
        Assertions.assertEquals(namesAfter.size(), namesBefore.size());
        Assertions.assertFalse(namesAfter.contains(expectedIupacName));
    }
}
