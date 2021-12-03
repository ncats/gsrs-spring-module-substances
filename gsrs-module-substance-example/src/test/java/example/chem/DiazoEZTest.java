package example.chem;

import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import gov.nih.ncats.molwitch.Chemical;
import gsrs.module.substance.StructureProcessingConfiguration;
import ix.core.chem.StructureProcessor;
import ix.core.chem.StructureStandardizer;
import ix.core.models.Structure;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import gsrs.springUtils.AutowireHelper;
import org.springframework.security.test.context.support.WithMockUser;

/**
 *
 * @author mitch
 */
@WithMockUser(username = "admin", roles = "Admin")
public class DiazoEZTest extends AbstractSubstanceJpaFullStackEntityTest{

    @Autowired
    static StructureStandardizer standardizer;

    @Autowired
    static StructureProcessingConfiguration structureProcessingConfiguration;

    @BeforeAll
    public static void before() {
                
       AutowireHelper.getInstance().autowire(standardizer);
       AutowireHelper.getInstance().autowire(structureProcessingConfiguration);

    }
  
    @Test
    public void testDiazo() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        String molfile = "\n   JSDraw212022119472D\n"
                + "\n"
                + " 10 10  0  0  0  0              0 V2000\n"
                + "   23.2130   -6.3552    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   23.9930   -7.6991    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   26.2379   -9.0368    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   27.9028   -9.0368    0.0000 N   0  3  0  0  0  0  0  0  0  0  0  0\n"
                + "   29.4630   -9.0368    0.0000 N   0  5  0  0  0  0  0  0  0  0  0  0\n"
                + "   21.6530   -6.3552    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   20.8730   -7.6991    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   21.6530   -9.0368    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   23.2130   -9.0368    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   25.5532   -7.6991    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "  6  7  2  0  0  0  0\n"
                + "  7  8  1  0  0  0  0\n"
                + "  3 10  1  0  0  0  0\n"
                + "  2  9  1  0  0  0  0\n"
                + "  2 10  1  0  0  0  0\n"
                + "  1  2  2  0  0  0  0\n"
                + "  1  6  1  0  0  0  0\n"
                + "  3  4  2  0  0  0  0\n"
                + "  4  5  2  0  0  0  0\n"
                + "  8  9  2  0  0  0  0\n"
                + "M  CHG  2   4   1   5  -1\n"
                + "M  END";

        Chemical chem = Chemical.parseMol(molfile);

        StructureProcessor processor = structureProcessingConfiguration.structureProcessor(standardizer, structureProcessingConfiguration.structureHashserInstance());
        Structure struct = processor.instrument(chem);
        int actualEz = struct.ezCenters;
        int expectedEz = 0;
        Assertions.assertEquals(expectedEz, actualEz, "a diazo compound has 0 E/Z centers");
    }

}
