package example.chem;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.validator.ValidationMessage;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.utils.validation.validators.ChemicalUniquenessValidator;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
class DuplicateCheckTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private StructureProcessor structureProcessor;

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;

    @Autowired
    private TestGsrsValidatorFactory factory;

    private static final String TEST_DATA_FILE = "rep18.gsrs";

    boolean loadedData =false;

    @BeforeEach
    public void clearIndexers() throws IOException {

        if( !loadedData) {
            SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
            AutowireHelper.getInstance().autowire(hashIndexer);
            testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);
            {
                ValidatorConfig config = new DefaultValidatorConfig();
                config.setValidatorClass(ChemicalValidator.class);
                config.setNewObjClass(ChemicalSubstance.class);
                factory.addValidator("substances", config);
            }


            File dataFile = new ClassPathResource(TEST_DATA_FILE).getFile();
            loadGsrsFile(dataFile);
            loadedData=true;
        }
    }

    @Test
    void testFindDuplicates1() throws Exception {
        String molfile = "1-7-Angiotensin II, 3-L-norleucine-5-L-isoleucine-\n   JSDraw204192113132D\n\n 66 68  0  0  1  0              0 V2000\n   30.3348  -15.1952    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   31.8007  -14.6616    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   32.0716  -13.1253    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   30.8765  -12.1227    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   29.3629  -12.5000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   28.5362  -11.1771    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n   29.5389   -9.9820    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   30.9853  -10.5664    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n   32.9957  -15.6644    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n   34.4617  -15.1308    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   35.6567  -16.1335    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   37.1227  -15.5999    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n   37.1225  -14.0399    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   38.4736  -13.2600    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   39.8246  -14.0399    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   41.1755  -13.2600    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   41.1755  -11.7000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   42.5266  -10.9199    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   43.8776  -11.7000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   43.8776  -13.2600    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   42.5266  -14.0399    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   45.2285  -10.9199    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n   38.4736  -11.7000    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n   37.1225  -10.9199    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   35.7716  -11.7000    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n   37.1225   -9.3600    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   35.7716   -8.5800    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n   34.4206   -9.3600    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   33.0695   -8.5800    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   31.7186   -9.3600    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n   30.3676   -8.5800    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   30.3676   -7.0200    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   29.0167   -6.2400    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   29.0166   -4.6800    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   27.6656   -3.9001    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n   30.3676   -3.9001    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n   31.7186   -6.2400    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n   29.0166   -9.3600    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n   33.0695   -7.0200    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   34.4206   -6.2400    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   34.4206   -4.6800    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   33.0695   -3.9001    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n   33.0695   -2.3401    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   34.4206   -1.5600    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n   31.7186   -1.5600    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n   34.4206  -10.9199    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n   38.4736   -8.5800    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   39.8246   -9.3600    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   41.1755   -8.5800    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   41.1755   -7.0200    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   35.7716  -13.2600    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n   35.3858  -17.6699    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   36.5809  -18.6725    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   36.3100  -20.2088    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   33.9199  -18.2033    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   34.1908  -16.6671    0.0000 H   0  0  0  0  0  0  0  0  0  0  0  0\n   34.7326  -13.5946    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n   29.1398  -14.1925    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n   30.0639  -16.7314    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n   28.6618  -17.4153    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   27.2845  -16.6830    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   25.9614  -17.5096    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n   27.2299  -15.1240    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n   28.8789  -18.9601    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   30.4153  -19.2310    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   31.1477  -17.8536    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n  1  2  1  0  0  0  0\n  2  3  1  0  0  0  0\n  3  4  1  0  0  0  0\n  4  5  2  0  0  0  0\n  5  6  1  0  0  0  0\n  6  7  1  0  0  0  0\n  8  7  2  0  0  0  0\n  4  8  1  0  0  0  0\n  2  9  1  6  0  0  0\n  9 10  1  0  0  0  0\n 10 11  1  0  0  0  0\n 11 12  1  0  0  0  0\n 12 13  1  0  0  0  0\n 13 14  1  0  0  0  0\n 14 15  1  0  0  0  0\n 15 16  1  0  0  0  0\n 16 17  2  0  0  0  0\n 17 18  1  0  0  0  0\n 18 19  2  0  0  0  0\n 20 19  1  0  0  0  0\n 21 20  2  0  0  0  0\n 16 21  1  0  0  0  0\n 19 22  1  0  0  0  0\n 14 23  1  1  0  0  0\n 23 24  1  0  0  0  0\n 24 25  2  0  0  0  0\n 26 24  1  1  0  0  0\n 26 27  1  0  0  0  0\n 27 28  1  0  0  0  0\n 28 29  1  0  0  0  0\n 29 30  1  1  0  0  0\n 30 31  1  0  0  0  0\n 31 32  1  0  0  0  0\n 32 33  1  0  0  0  0\n 33 34  1  0  0  0  0\n 34 35  1  0  0  0  0\n 34 36  2  0  0  0  0\n 32 37  1  6  0  0  0\n 31 38  2  0  0  0  0\n 29 39  1  0  0  0  0\n 39 40  1  0  0  0  0\n 40 41  1  0  0  0  0\n 41 42  1  0  0  0  0\n 42 43  1  0  0  0  0\n 43 44  2  0  0  0  0\n 43 45  1  0  0  0  0\n 28 46  2  0  0  0  0\n 26 47  1  0  0  0  0\n 47 48  1  0  0  0  0\n 48 49  1  0  0  0  0\n 49 50  1  0  0  0  0\n 13 51  2  0  0  0  0\n 11 52  1  0  0  0  0\n 52 53  1  0  0  0  0\n 53 54  1  0  0  0  0\n 52 55  1  6  0  0  0\n 11 56  1  6  0  0  0\n 10 57  2  0  0  0  0\n  1 58  2  0  0  0  0\n  1 59  1  0  0  0  0\n 59 60  1  0  0  0  0\n 60 61  1  1  0  0  0\n 61 62  1  0  0  0  0\n 61 63  2  0  0  0  0\n 60 64  1  0  0  0  0\n 64 65  1  0  0  0  0\n 66 65  1  0  0  0  0\n 59 66  1  0  0  0  0\nM  END";
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        Structure structure = structureProcessor.instrument(molfile);
        GinasChemicalStructure ginasChemicalStructure = new GinasChemicalStructure(structure);
        builder.setStructure(ginasChemicalStructure);
        builder.addName("ALTERNATIVE DEFINITION for [ACLERASTIDE] duplicate");
        ChemicalSubstance chemicalSubstance = builder.build();
        ChemicalUniquenessValidator validator = new ChemicalUniquenessValidator();
        AutowireHelper.getInstance().autowireAndProxy(validator);
        Method uniquenessCheckMethod = validator.getClass().getDeclaredMethod("handleDuplicateCheck", ChemicalSubstance.class);
        uniquenessCheckMethod.setAccessible(true);
        List<ValidationMessage> messages = (List<ValidationMessage>) uniquenessCheckMethod.invoke(validator, chemicalSubstance);
        Assertions.assertEquals(1, messages.size());
        Assertions.assertTrue(messages.stream().anyMatch(m->m.getMessage().contains("appears to be a duplicate")));
    }

    @Test
    void testFindDuplicates0() throws Exception {
        String molfile = "\\n  ACCLDraw09292322002D\\n\\n  6  6  0  0  0  0  0  0  0  0999 V2000\\n    4.2726   -6.3226    0.0000 P   0  0  3  0  0  0  0  0  0  0  0  0\\n    5.5000   -5.6139    0.0000 P   0  0  3  0  0  0  0  0  0  0  0  0\\n    6.7274   -6.3226    0.0000 P   0  0  3  0  0  0  0  0  0  0  0  0\\n    6.7274   -7.7399    0.0000 P   0  0  3  0  0  0  0  0  0  0  0  0\\n    5.5000   -8.4486    0.0000 P   0  0  3  0  0  0  0  0  0  0  0  0\\n    4.2726   -7.7399    0.0000 P   0  0  3  0  0  0  0  0  0  0  0  0\\n  1  2  1  0  0  0  0\\n  3  2  1  0  0  0  0\\n  4  3  1  0  0  0  0\\n  5  4  1  0  0  0  0\\n  1  6  1  0  0  0  0\\n  6  5  1  0  0  0  0\\nM  END\\n";
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        Structure structure = structureProcessor.instrument(molfile);
        GinasChemicalStructure ginasChemicalStructure = new GinasChemicalStructure(structure);
        builder.setStructure(ginasChemicalStructure);
        builder.addName("hexaphosphorcyclohexane");
        ChemicalSubstance chemicalSubstance = builder.build();
        ChemicalUniquenessValidator validator = new ChemicalUniquenessValidator();
        AutowireHelper.getInstance().autowireAndProxy(validator);
        Method uniquenessCheckMethod = validator.getClass().getDeclaredMethod("handleDuplicateCheck", ChemicalSubstance.class);
        uniquenessCheckMethod.setAccessible(true);
        List<ValidationMessage> messages = (List<ValidationMessage>) uniquenessCheckMethod.invoke(validator, chemicalSubstance);
        Assertions.assertEquals(1, messages.size());
        Assertions.assertTrue(messages.stream().anyMatch(m->m.getMessageType().equals(ValidationMessage.MESSAGE_TYPE.SUCCESS)));
    }

    @Test
    void testFindDuplicatesError() throws Exception {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("no structure");
        ChemicalSubstance chemicalSubstance = builder.build();
        ChemicalUniquenessValidator validator = new ChemicalUniquenessValidator();
        AutowireHelper.getInstance().autowireAndProxy(validator);
        Method uniquenessCheckMethod = validator.getClass().getDeclaredMethod("handleDuplicateCheck", ChemicalSubstance.class);
        uniquenessCheckMethod.setAccessible(true);
        List<ValidationMessage> messages = (List<ValidationMessage>) uniquenessCheckMethod.invoke(validator, chemicalSubstance);
        Assertions.assertEquals(1, messages.size());
        Assertions.assertTrue(messages.stream().anyMatch(m->m.getMessageType().equals(ValidationMessage.MESSAGE_TYPE.ERROR)));
    }

}
