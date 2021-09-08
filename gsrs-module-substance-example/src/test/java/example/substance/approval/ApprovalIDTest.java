package example.substance.approval;

import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.springUtils.AutowireHelper;
import ix.core.chem.StructureProcessor;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.utils.DefaultApprovalIDGenerator;
import ix.ginas.utils.SubstanceApprovalIdGenerator;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author mitch
 */
@Slf4j
public class ApprovalIDTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    StructureProcessor structureProcessor;

    public ApprovalIDTest() {
    }

    @Test
    public void testIDGenerator() {
        SubstanceApprovalIdGenerator idGenerator = new DefaultApprovalIDGenerator("GID", 8, true, null);
        ChemicalSubstance sub = getChemicalFromFile("invrelate2");
        AutowireHelper.getInstance().autowire(idGenerator);
        try {
            String id = idGenerator.generateId(sub);
            log.debug("approval id: " + id);
            assertTrue(id.length() > 0);
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }
    }

    @Test
    public void testIDGeneratorNoPad() {
        log.debug("testIDGeneratorNoPad");
        SubstanceApprovalIdGenerator idGenerator = new DefaultApprovalIDGenerator("GID", 8, false, null);
        ChemicalSubstance sub = getChemicalFromFile("invrelate2");
        AutowireHelper.getInstance().autowire(idGenerator);
        try {
            String id = idGenerator.generateId(sub);
            log.debug("approval id: " + id);
            assertTrue(id.length() > 0);
            Assertions.assertFalse(id.startsWith("0"));
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }
    }
    private ChemicalSubstance getChemicalFromFile(String name) {
        try {
            File chemicalFile = new ClassPathResource("testJSON/" + name + ".json").getFile();
            ChemicalSubstanceBuilder builder = SubstanceBuilder.from(chemicalFile);
            ChemicalSubstance s = builder.build();

            //'validation' here is a way of setting properties required for salt validator's duplicate check
            ChemicalValidator chemicalValidator = new ChemicalValidator();
            chemicalValidator.setStructureProcessor(structureProcessor);
            chemicalValidator.validate(s, null);
            return s;
        } catch (IOException ex) {
            log.error("Error reading chemical file", ex);
        }
        return null;
    }
}
