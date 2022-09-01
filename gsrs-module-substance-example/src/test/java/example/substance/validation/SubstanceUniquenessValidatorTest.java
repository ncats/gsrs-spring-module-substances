package example.substance.validation;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import example.GsrsModuleSubstanceApplication;
import gov.nih.ncats.common.io.IOUtil;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.chem.StructureProcessor;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.StructurallyDiverseSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import ix.ginas.utils.validation.validators.SubstanceUniquenessValidator;
import ix.utils.Util;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author mitch
 */
@ActiveProfiles("test")
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
@Slf4j
//@TestPropertySource(properties = {
//        "logging.level.gsrs.module.substance.definitional=trace"
        //,
//        "logging.level.ix.core.util=trace",
//        "logging.level.ix.ginas.utils.validation=trace",
//        "logging.level.gsrs.module.substance.indexers=trace"
//})
public class SubstanceUniquenessValidatorTest extends AbstractSubstanceJpaFullStackEntityTest {

    public SubstanceUniquenessValidatorTest() {
    }

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @Autowired
    StructureProcessor structureProcessor;
    
    private TestInfo tinfo = null;
    
    private boolean logit = true;

    private final String fileName = "rep18.gsrs";

    @BeforeAll
    public static void deleteOld() {
        IOUtil.deleteRecursivelyQuitely(tempDir);
    }
  
    @AfterEach
    public void deleteOldAfter(TestInfo info) {
        logit=false;
        IOUtil.deleteRecursivelyQuitely(tempDir);
        System.out.println("Finished test:" + info.getDisplayName());
    }
    
    @BeforeEach
    public void setupIndexers(TestInfo info) throws IOException {
        System.gc();
        logit=true;
        tinfo=info;
        System.out.println("Starting next test:" + info.getDisplayName());
        
//        IOUtil.deleteRecursivelyQuitely(tempDir);
        System.out.println("Found :" + tempDir.getAbsolutePath());
        
        log.trace("setupIndexers");
        SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
        AutowireHelper.getInstance().autowire(hashIndexer);
        testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);
        {
            ValidatorConfig config = new DefaultValidatorConfig();
            //ensure calculation of def hash
            config.setValidatorClass(ChemicalValidator.class);
            config.setNewObjClass(ChemicalSubstance.class);
            factory.addValidator("substances", config);
            log.trace("completed set-up of validator config");
        }

        File dataFile = new ClassPathResource(fileName).getFile();
        System.out.println("Loading file:" + info.getDisplayName());
        loadGsrsFile(dataFile);
        log.trace("setupIndexers complete");
        System.out.println("Finished setup:" + info.getDisplayName());
        
        new Thread(()->{
           int c= 0;
           while(logit) {
               try {
                System.out.println("Running:" + tinfo.getDisplayName() + " :" + (c++));
                Util.printAllExecutingStackTraces();

                long heapSize = Runtime.getRuntime().totalMemory()/(1024*1024);
                long heapMaxSize = Runtime.getRuntime().maxMemory()/(1024*1024);
                long heapFreeSize = Runtime.getRuntime().freeMemory()/(1024*1024);

                System.out.println("HEAP Size:" + heapSize + " HEAP Max:" + heapMaxSize + " HEAP Free:" + heapFreeSize);
                Thread.sleep(30_000); 
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                logit=false;
            }
               
           }
            System.out.println("StoppingNow:" + tinfo.getDisplayName() + " :" + (c));
        }).start();
    }

    @Test
    public void testValidation() {
        log.trace("Starting in testValidation");
        String name ="G6867RWN6N";
        String completeDuplicateMessage= "appears to be a full duplicate";
        ChemicalSubstance protein = getChemicalSubstanceFromFile(name);
        protein.uuid= UUID.randomUUID();
        SubstanceUniquenessValidator validator = new SubstanceUniquenessValidator();
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse response = validator.validate(protein, null);
        Assertions.assertTrue( response.getValidationMessages().stream().anyMatch(m-> ((GinasProcessingMessage) m).message.contains(completeDuplicateMessage)));
    }

    @Test
    public void testExactDuplicateStrDiv() {
        log.trace("Starting in testValidation");
        String name ="N5WWR36MDJ";
        String completeDuplicateMessage= "appears to be a full duplicate";
        Substance substance = getSubstanceFromFile(name);
        substance.uuid= UUID.randomUUID();
        
        SubstanceUniquenessValidator validator = new SubstanceUniquenessValidator();
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse response = validator.validate(substance, null);
        Assertions.assertTrue( response.getValidationMessages().stream().anyMatch(m-> ((GinasProcessingMessage) m).message.contains(completeDuplicateMessage)));
    }
    @Test
    public void testValidationDiastereomer() {
        log.trace("Starting in testValidation");
        String name ="G6867RWN6N-diast";
        String possibleDuplicateMessage= "is a possible duplicate";
        ChemicalSubstance chem = getChemicalSubstanceFromFile(name);
        chem.uuid=UUID.randomUUID();
        
        SubstanceUniquenessValidator validator = new SubstanceUniquenessValidator();
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse response = validator.validate(chem, null);
        Assertions.assertTrue(response.getValidationMessages().stream().anyMatch(m-> ((GinasProcessingMessage) m).message.contains(possibleDuplicateMessage)));
    }
    
    @Test
    public void testDuplicateDoesNotFindItself() {
        log.trace("Starting in testValidation");
        String name ="G6867RWN6N-diast";
        String possibleDuplicateMessage= "is a possible duplicate";
        ChemicalSubstance chem = getChemicalSubstanceFromFile(name);
        SubstanceUniquenessValidator validator = new SubstanceUniquenessValidator();
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse response = validator.validate(chem, null);
        //should not contain a duplicate message
        Assertions.assertFalse(response.getValidationMessages().stream().anyMatch(m-> ((GinasProcessingMessage) m).message.contains(possibleDuplicateMessage)));
    }

    @Test
    public void testValidationNoDuplicates() {
        log.trace("Starting in testValidationNoDuplicates");
        String name ="G6867RWN6N-Si";//one carbon in the starting structure was replaced with Si to make the structure unique
        String completeDuplicateMessage= "appears to be a full duplicate";
        ChemicalSubstance protein = getChemicalSubstanceFromFile(name);
        SubstanceUniquenessValidator validator = new SubstanceUniquenessValidator();
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse response = validator.validate(protein, null);
        Assertions.assertTrue( response.getValidationMessages().stream().noneMatch(m-> ((GinasProcessingMessage) m).message.contains(completeDuplicateMessage)));
    }

    private ChemicalSubstance getChemicalSubstanceFromFile(String name) {
        try {
            File proteinFile = new ClassPathResource("testJSON/" + name + ".json").getFile();
            ChemicalSubstanceBuilder builder = SubstanceBuilder.from(proteinFile);
            ChemicalValidator chemicalValidator = new ChemicalValidator();
            chemicalValidator.setStructureProcessor(structureProcessor);
            ChemicalSubstance chem=builder.build();
            chemicalValidator.validate(chem, null);
            
            return builder.build();
        } catch (IOException ex) {
            log.error("Error retrieving substance from file", ex);
        }
        return null;
    }

    private Substance getSubstanceFromFile(String name) {
        try {
            File substanceFile = new ClassPathResource("testJSON/" + name + ".json").getFile();
            StructurallyDiverseSubstanceBuilder builder = SubstanceBuilder.from(substanceFile);
            
            return builder.build();
        } catch (IOException ex) {
            log.error("Error retrieving substance from file", ex);
        }
        return null;
    }

}
