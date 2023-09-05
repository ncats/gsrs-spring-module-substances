package example.substance.chemical;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.service.GsrsEntityService;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.EntityFetcher;
import ix.core.models.Keyword;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.UUID;

@WithMockUser(username = "admin", roles="Admin")
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
public class ChemicalChangesTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    protected SubstanceEntityService substanceEntityService;

    @BeforeEach
    public void setup(){
        AutowireHelper.getInstance().autowire(this);
    }

    @Test
    public void testStructureChange() throws Exception {
        UUID substanceId= UUID.randomUUID();
        ChemicalSubstanceBuilder builder= new ChemicalSubstanceBuilder();
        builder.setUUID(substanceId);
        builder.setStructureWithDefaultReference("NCCCCN");
        Name name1 = new Name();
        name1.name="putrecine";
        name1.languages.add(new Keyword("en"));
        name1.displayName=true;
        Reference substanceOneReference = new Reference();
        substanceOneReference.docType="CATALOG";
        substanceOneReference.citation="Value specific to substance 1";
        name1.addReference(substanceOneReference);
        builder.addName(name1);
        ChemicalSubstance chemicalSubstance = builder.build();
        GsrsEntityService.CreationResult<Substance> result= substanceEntityService.createEntity(chemicalSubstance.toFullJsonNode());
        Assertions.assertTrue(result.isCreated());

        //retrieve
        ChemicalSubstance retrieved = (ChemicalSubstance) EntityFetcher.of(EntityUtils.Key.of(Substance.class, UUID.fromString(substanceId.toString()))).call();
        retrieved.getStructure().smiles="NCCCCCN";//added one methylene
        GsrsEntityService.UpdateResult<Substance> updateResult = substanceEntityService.updateEntity(retrieved.toFullJsonNode());
        Assertions.assertNull(updateResult.getThrowable());
    }

}
