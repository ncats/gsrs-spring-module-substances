package example.substance.datasearch;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.ValidationUtils;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
public class ValidationSubstanceSearchTest extends AbstractSubstanceJpaFullStackEntityTest {
    @Autowired
    private SubstanceLegacySearchService searchService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;

    private String fileName = "rep18.gsrs";

    @Autowired
    private TestGsrsValidatorFactory factory;

    private boolean ranSetup=false;

    @BeforeEach
    public void clearIndexers() throws IOException {
        if(!ranSetup) {
            SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
            AutowireHelper.getInstance().autowire(hashIndexer);
            testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);
            {
                ValidatorConfig config = new DefaultValidatorConfig();
                config.setValidatorClass(ChemicalValidator.class);
                config.setNewObjClass(ChemicalSubstance.class);
                factory.addValidator("substances", config);
            }

            File dataFile = new ClassPathResource(fileName).getFile();
            loadGsrsFile(dataFile);
            ranSetup=true;
        }
    }

    @Test
    public void searchNameTest() {
        String nameToSearch ="HYPROMELLOSE 2906 (400 MPA.S)";
        List<Substance> matching= ValidationUtils.findSubstancesByName(nameToSearch, transactionManager,searchService);
        Assertions.assertEquals(1, matching.size());
        Assertions.assertEquals("159e7207-856c-4784-a95f-71dca27fbb68", matching.get(0).uuid.toString());
    }

    @Test
    public void searchNameNoMatchTest() {
        String nameToSearch ="Daisies";
        List<Substance> matching= ValidationUtils.findSubstancesByName(nameToSearch, transactionManager,searchService);
        Assertions.assertEquals(0, matching.size());
    }

    @Test
    public void searchCodeTest() {
        String codeToSearch ="G6867RWN6N";
        String codeSystemToSearch ="FDA UNII";
        List<Substance> matching= ValidationUtils.findSubstancesByCode(codeSystemToSearch, codeToSearch, transactionManager, searchService);
        Assertions.assertEquals(1, matching.size());
        Assertions.assertEquals("ac776f92-b90f-48a0-a54e-7461a60c84b3", matching.get(0).uuid.toString());
    }

    @Test
    public void searchCode2Test() {
        String codeToSearch ="1131649-96-1";
        String codeSystemToSearch ="CAS";
        List<Substance> matching= ValidationUtils.findSubstancesByCode(codeSystemToSearch, codeToSearch, transactionManager, searchService);
        Assertions.assertEquals(1, matching.size());
        Assertions.assertEquals("0d1371fc-904f-45e9-b073-ba55dacc4f30", matching.get(0).uuid.toString());
    }

    @Test
    public void searchCodeNoMatchTest() {
        String codeToSearch ="Matchless Code";
        String codeSystemToSearch ="CAS Number";
        List<Substance> matching= ValidationUtils.findSubstancesByCode(codeSystemToSearch, codeToSearch, transactionManager, searchService);
        Assertions.assertEquals(0, matching.size());
    }

}
