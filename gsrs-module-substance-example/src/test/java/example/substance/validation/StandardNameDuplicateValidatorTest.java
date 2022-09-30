package example.substance.validation;

import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.models.Keyword;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
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
        import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
        import lombok.extern.slf4j.Slf4j;
        import org.junit.jupiter.api.Assertions;
        import org.junit.jupiter.api.BeforeEach;
        import org.junit.jupiter.api.Test;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.core.io.ClassPathResource;
        import org.springframework.security.test.context.support.WithMockUser;

        import java.io.File;
        import java.io.IOException;
        import java.util.List;
        import java.util.UUID;

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
AutowireHelper.getInstance().autowireAndProxy( substanceRepository);
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
public void testSaveSubstance() {
/*
        List<UUID> idsAll = substanceRepository.getAllIds();
        UUID uuid0 = idsAll.get(0);
        UUID uuid1 = idsAll.get(1);
        Substance s0 = substanceRepository.getOne(uuid0);
        s0.names.get(0).name="Test1";
        s0.names.get(0).stdName="Same Test1 std";
        s0.names.get(1).name="Test2";
        s0.names.get(1).stdName="Same Test1 std";
        substanceRepository.saveAndFlush(s0);
*/
        Substance s1 = new Substance();
        Name name1 = new Name();
        name1.name ="Test1";
        name1.stdName ="Test1 std";
        name1.languages.add(new Keyword("en"));
        name1.languages.add(new Keyword("fr"));
        s1.names.add(name1);
        substanceRepository.saveAndFlush(s1);

        Substance s2 = new Substance();
        Name name2 = new Name();
        name2.name ="Test2";
        name2.stdName ="Test1 std";
        name2.languages.add(new Keyword("en"));
        name2.languages.add(new Keyword("fr"));
        s2.names.add(name2);

        StandardNameDuplicateValidator validator = new StandardNameDuplicateValidator();
        ValidationResponse<Substance> response = validator.validate(s2, null);
        response.getValidationMessages().forEach(vm->{
                System.out.println( String.format("type: %s; message: %s", vm.getMessageType(), vm.getMessage()));
        });

        // Assertions.assertEquals(fullyStdName, chemical.names.get(0).stdName);
}

}
/*
    Substance s1 = new Substance();
    Name name1 = new Name();
    name1.name ="Test1";
    name1.stdName ="Test1 std";
        name1.languages.add(new Keyword("en"));
        name1.languages.add(new Keyword("fr"));
        s1.names.add(name1);

    Substance s2 = new Substance();
    Name name2 = new Name();
    name2.name ="Test1";
    name2.stdName ="Test1 std";
        name2.languages.add(new Keyword("en"));
        name2.languages.add(new Keyword("fr"));
        s2.names.add(name2);

    StandardNameDuplicateValidator validator = new StandardNameDuplicateValidator();
    ValidationResponse<Substance> response = validator.validate(s2, null);
        response.getValidationMessages().forEach(vm->{
        log.trace( String.format("type: %s; message: %s", vm.getMessageType(), vm.getMessage()));
    });

        Assertions.assertEquals(fullyStdName, chemical.names.get(0).stdName);

 */