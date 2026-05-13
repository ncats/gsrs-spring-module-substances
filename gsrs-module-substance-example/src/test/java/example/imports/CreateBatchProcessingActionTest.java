package example.imports;

import example.GsrsModuleSubstanceApplication;
import gsrs.dataexchange.processingactions.CreateBatchProcessingAction;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.chem.StructureProcessor;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import java.io.IOException;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
public class CreateBatchProcessingActionTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;

    @Autowired
    StructureProcessor structureProcessor;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @BeforeEach
    public void clearIndexers() throws IOException {
        SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
        AutowireHelper.getInstance().autowire(hashIndexer);
        testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);
        {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(ChemicalValidator.class);
            config.setNewObjClass(ChemicalSubstance.class);
            factory.addValidator("substances", config);
        }
    }

    @Test
    public void getNextBatchCodeTest(){
        //create the parent
        ChemicalSubstanceBuilder chemicalSubstanceBuilder = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder.setStructureWithDefaultReference("CCCCCCN");
        chemicalSubstanceBuilder.addName("hexane amine");
        ChemicalSubstance parentSubstance = (ChemicalSubstance) assertCreated(chemicalSubstanceBuilder.buildJson());

        String BATCH_RELATIONSHIP_TYPE= "BATCH->PARENT";
        String reverseBatchRelationshipType = "PARENT->BATCH";
        String BATCH_CODE_SYSTEM = "Batch Code";

        SubstanceBuilder batch1Builder = new SubstanceBuilder();
        batch1Builder.addName("hexane batch1");
        batch1Builder.addCode(BATCH_CODE_SYSTEM, "0001");
        Relationship batch1Relationship = new Relationship();
        batch1Relationship.type=reverseBatchRelationshipType;
        batch1Relationship.relatedSubstance= parentSubstance.asSubstanceReference();
        batch1Builder.addRelationship(batch1Relationship);
        batch1Builder.buildJsonAnd(this::assertCreated);

        SubstanceBuilder batch2Builder = new SubstanceBuilder();
        batch2Builder.addName("hexane batch2");
        batch2Builder.addCode(BATCH_CODE_SYSTEM, "0002");
        Relationship batch2Relationship = new Relationship();
        batch2Relationship.type=reverseBatchRelationshipType;
        batch2Relationship.relatedSubstance= parentSubstance.asSubstanceReference();
        batch2Builder.addRelationship(batch2Relationship);
        batch2Builder.buildJsonAnd(this::assertCreated);

        CreateBatchProcessingAction action = new CreateBatchProcessingAction();
        AutowireHelper.getInstance().autowireAndProxy(action);
        String batchCode= action.getNextBatchCode(parentSubstance, reverseBatchRelationshipType, BATCH_CODE_SYSTEM);
        String expectedCode="0003";
        Assertions.assertEquals(expectedCode, batchCode);
    }
}
