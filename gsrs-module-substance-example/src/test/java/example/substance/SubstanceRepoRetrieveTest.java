package example.substance;

import gsrs.cache.GsrsCache;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import ix.ginas.utils.validation.validators.SaltValidator;
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
public class SubstanceRepoRetrieveTest extends AbstractSubstanceJpaEntityTest {

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

        //prevent validations from occurring multiple times
        File dataFile = new ClassPathResource("testdumps/rep18.gsrs").getFile();
        cache.clearCache();
        loadGsrsFile(dataFile);
        log.trace("loaded rep18 data file");
    }

    @Test
    public void testAllIds() {
        List<UUID> idsAll = substanceRepository.getAllIds();
        log.trace("total substances: {}", idsAll.size());
        Assertions.assertEquals(18, idsAll.size());
    }

    @Test
    public void testAllChemicalIds() {
        List<UUID> idsChemicals = substanceRepository.getAllChemicalIds();
        System.out.println("total chemicals: " + idsChemicals.size());
        Assertions.assertEquals(9, idsChemicals.size());
    }

}
