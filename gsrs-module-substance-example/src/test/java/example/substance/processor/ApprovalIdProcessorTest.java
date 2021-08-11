package example.substance.processor;

import example.prot.ProtCalculationTest;
import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.startertests.TestEntityProcessorFactory;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Substance;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 *
 * @author mitch miller
 */
public class ApprovalIdProcessorTest extends AbstractSubstanceJpaEntityTest {
    
    public ApprovalIdProcessorTest() {
    }
    
    @Autowired
    TestEntityProcessorFactory processorFactory;
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testApprovalIdProcessing() {
        Substance approvedSubstance = getProteinFromFile();
        substanceRepository.saveAndFlush(approvedSubstance);
        
    }

    

   private ProteinSubstance getProteinFromFile() {
        try {
            File proteinFile =new ClassPathResource("testJSON/88ECG9H7RA.json").getFile();
            ProteinSubstanceBuilder builder =SubstanceBuilder.from(proteinFile);
            return builder.build();
        } catch (IOException ex) {
            Logger.getLogger(ProtCalculationTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
