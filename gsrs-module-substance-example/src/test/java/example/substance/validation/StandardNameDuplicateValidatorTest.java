package example.substance.validation;

import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.models.Keyword;
import ix.core.validator.ValidationResponse;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.StandardNameDuplicateValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import gsrs.cache.GsrsCache;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestIndexValueMakerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import java.io.File;
import java.io.IOException;

@WithMockUser(username = "admin", roles="Admin")
@Slf4j
public class StandardNameDuplicateValidatorTest extends AbstractSubstanceJpaEntityTest {

@Autowired
private SubstanceRepository substanceRepository;

@Autowired
private GsrsCache cache;

@Autowired
private TestIndexValueMakerFactory testIndexValueMakerFactory;


@BeforeEach
public void runSetup() throws IOException {
        log.trace("runSetup");
        AutowireHelper.getInstance().autowireAndProxy(substanceRepository);
        SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
        AutowireHelper.getInstance().autowire(hashIndexer);
        testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);
        // prevent validations from occurring multiple times
        File dataFile = new ClassPathResource("testdumps/rep18.gsrs").getFile();
        cache.clearCache();
        loadGsrsFile(dataFile);
        log.trace("loaded rep18 data file");
}

@Test
public void testDuplicateInOtherRecord() {
        // This test is not working as expected.

        Substance s1 = new Substance();
        Name name1 = new Name();
        name1.name ="Test1";
        name1.stdName ="Test1 std";
        if (name1.languages == null) {
                name1.languages = new EmbeddedKeywordList();
        }
        name1.languages.add(new Keyword("en"));
        name1.languages.add(new Keyword("fr"));

        s1.names.add(name1);
        substanceRepository.saveAndFlush(s1);

        Substance s2 = new Substance();
        Name name2 = new Name();
        name2.name ="Test2";
        name2.stdName ="Test1 std";
        if (name2.languages == null) {
                name2.languages = new EmbeddedKeywordList();
        }
        name2.languages.add(new Keyword("en"));
        name2.languages.add(new Keyword("fr"));
        s2.names.add(name2);

        substanceRepository.saveAndFlush(s2);
        System.out.println(s2.getUuid());
        cache.clearCache();

        StandardNameDuplicateValidator validator = new StandardNameDuplicateValidator();
        ValidationResponse<Substance> response = validator.validate(s2, null);
        response.getValidationMessages().forEach(vm->{
                System.out.println( String.format("type: %s; message: %s", vm.getMessageType(), vm.getMessage()));
        });

        // Assertions.assertTrue(response.getValidationMessages().get(0).getMessage().contains("collides (possible duplicate) with existing standard name for substance"));

}


        @Test
        public void testDuplicateInSameRecord() {
                Substance s1 = new Substance();
                Name name1 = new Name();
                name1.name ="TestSame1";
                name1.stdName ="TestSame1 std";
                if (name1.languages == null) {
                        name1.languages = new EmbeddedKeywordList();
                }
                name1.languages.add(new Keyword("en"));
                name1.languages.add(new Keyword("fr"));
                s1.names.add(name1);
                Name name2 = new Name();
                name2.name ="TestSame2";
                name2.stdName ="TestSame1 std";
                if (name1.languages == null) {
                        name1.languages = new EmbeddedKeywordList();
                }
                name2.languages.add(new Keyword("en"));
                name2.languages.add(new Keyword("fr"));
                s1.names.add(name2);
                substanceRepository.saveAndFlush(s1);
                cache.clearCache();

                StandardNameDuplicateValidator validator = new StandardNameDuplicateValidator();
                ValidationResponse<Substance> response = validator.validate(s1, null);
                response.getValidationMessages().forEach(vm->{
                        System.out.println( String.format("type: %s; message: %s", vm.getMessageType(), vm.getMessage()));
                });
                Assertions.assertTrue(response.getValidationMessages().get(0).getMessage().contains("duplicate standard name in the record"));
        }
}
