package example.imports;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.standardizer.SubstanceSynchronizer;
import gsrs.service.GsrsEntityService;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.EntityFetcher;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.MixtureSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.MixtureSubstance;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
public class SubstanceSynchronizerTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    SubstanceSynchronizer substanceSynchronizer;

    @Autowired
    protected SubstanceEntityService substanceEntityService;

    private final String uuidCodeSystem ="Code for UUID";
    private final String approvalIdCodeSystem ="Code for ApprovalID";

    @WithMockUser(username = "admin", roles = "Admin")
    @Test
    public void testRelationshipResolution() throws Exception {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder = new ChemicalSubstanceBuilder();
        String mXyleneName ="m xylene";
        chemicalSubstanceBuilder.addName(mXyleneName);
        // Use simple aliphatic SMILES that are guaranteed not to trigger CDK aromatic-bond
        // serialization errors inside PojoDiff.getEnhancedJsonDiff (CCCCCCN causes that issue).
        chemicalSubstanceBuilder.setStructureWithDefaultReference("CCO");
        UUID initialmXyleneUuid= UUID.randomUUID();
        chemicalSubstanceBuilder.setUUID(initialmXyleneUuid);
        ChemicalSubstance mXylene = chemicalSubstanceBuilder.build();

        GsrsEntityService.CreationResult<Substance> result= substanceEntityService.createEntity(mXylene.toFullJsonNode());
        Assertions.assertTrue(result.isCreated());

        ChemicalSubstanceBuilder pXyleneBuilder = new ChemicalSubstanceBuilder();
        String pXyleneName="p xylene";
        pXyleneBuilder.addName(pXyleneName);
        UUID initialpXyleneUuid= UUID.randomUUID();
        pXyleneBuilder.setStructureWithDefaultReference("CCCO");
        pXyleneBuilder.setUUID(initialpXyleneUuid);
        String relationshipType = "STRUCTURAL ISOMER->OTHER STRUCTURAL ISOMER";
        pXyleneBuilder.addRelationshipTo(mXylene, relationshipType);

        ChemicalSubstance pXylene = pXyleneBuilder.build();
        GsrsEntityService.CreationResult<Substance> result2= substanceEntityService.createEntity(pXylene.toFullJsonNode());
        Assertions.assertTrue(result2.isCreated());

        UUID legacyRelationshipUuid = UUID.randomUUID();
        pXylene.relationships.get(0).relatedSubstance.refuuid = legacyRelationshipUuid.toString();

        UUID changedmXyleneUuid =UUID.randomUUID();
        ChemicalSubstanceBuilder updatedMXyleneBuilder = new ChemicalSubstanceBuilder();
        updatedMXyleneBuilder.addName(mXyleneName + " (legacy)");
        updatedMXyleneBuilder.addCode(uuidCodeSystem, legacyRelationshipUuid.toString());
        updatedMXyleneBuilder.setUUID(changedmXyleneUuid);
        ChemicalSubstance updatedMXylene = updatedMXyleneBuilder.build();
        GsrsEntityService.CreationResult<Substance> createResult= substanceEntityService.createEntity(updatedMXylene.toFullJsonNode());
        Assertions.assertTrue(createResult.isCreated());

        StringBuilder notations = new StringBuilder();
        TransactionTemplate relationshipFixTransaction = newTransactionTemplate();
        relationshipFixTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        relationshipFixTransaction.executeWithoutResult(status ->
                substanceSynchronizer.fixSubstanceReferences(pXylene, notations::append, uuidCodeSystem, approvalIdCodeSystem)
        );

        EntityUtils.EntityWrapper<Substance> substanceWrapper = EntityUtils.EntityWrapper.of(pXylene);
        EntityFetcher<?> updatedXyleneFetcher = EntityFetcher.of( EntityUtils.Key.of(substanceWrapper));
        System.out.println(notations);
        ChemicalSubstance fetchedPXylene = (ChemicalSubstance) updatedXyleneFetcher.getIfPossible().get();

        Assertions.assertEquals(fetchedPXylene.relationships.get(0).relatedSubstance.refuuid, changedmXyleneUuid.toString());
    }

    @WithMockUser(username = "admin", roles = "Admin")
    @Test
    public void testMixtureComponentResolution() throws Exception {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder = new ChemicalSubstanceBuilder();
        String mXyleneName ="m xylene";
        chemicalSubstanceBuilder.addName(mXyleneName);
        // Use simple aliphatic SMILES that are guaranteed not to trigger CDK aromatic-bond
        // serialization errors inside PojoDiff.getEnhancedJsonDiff.
        chemicalSubstanceBuilder.setStructureWithDefaultReference("CCO");
        UUID initialmXyleneUuid= UUID.randomUUID();
        chemicalSubstanceBuilder.setUUID(initialmXyleneUuid);
        ChemicalSubstance mXylene = chemicalSubstanceBuilder.build();

        GsrsEntityService.CreationResult<Substance> result= substanceEntityService.createEntity(mXylene.toFullJsonNode());
        Assertions.assertTrue(result.isCreated());

        ChemicalSubstanceBuilder pXyleneBuilder = new ChemicalSubstanceBuilder();
        String pXyleneName="p xylene";
        pXyleneBuilder.addName(pXyleneName);
        UUID initialpXyleneUuid= UUID.randomUUID();
        pXyleneBuilder.setStructureWithDefaultReference("CCCO");
        pXyleneBuilder.setUUID(initialpXyleneUuid);

        ChemicalSubstance pXylene = pXyleneBuilder.build();
        GsrsEntityService.CreationResult<Substance> result2= substanceEntityService.createEntity(pXylene.toFullJsonNode());
        Assertions.assertTrue(result2.isCreated());

        MixtureSubstanceBuilder mixtureSubstanceBuilder = new MixtureSubstanceBuilder();
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", pXylene, mXylene);
        mixtureSubstanceBuilder.addName("pm xylenes");
        mixtureSubstanceBuilder.setUUID(UUID.randomUUID());
        MixtureSubstance pmXylenes = mixtureSubstanceBuilder.build();
        GsrsEntityService.CreationResult<Substance> result3= substanceEntityService.createEntity(pmXylenes.toFullJsonNode());
        Assertions.assertTrue(result3.isCreated());

        UUID legacyMixtureComponentUuid = UUID.randomUUID();
        pmXylenes.mixture.components.stream()
                .filter(p -> p.substance.refPname.equals(pXyleneName))
                .forEach(p -> p.substance.refuuid = legacyMixtureComponentUuid.toString());

        UUID changedpXyleneUuid =UUID.randomUUID();
        ChemicalSubstanceBuilder updatedPXyleneBuilder = new ChemicalSubstanceBuilder();
        updatedPXyleneBuilder.addName(pXyleneName + " (legacy)");
        updatedPXyleneBuilder.addCode(uuidCodeSystem, legacyMixtureComponentUuid.toString());
        updatedPXyleneBuilder.setUUID(changedpXyleneUuid);
        ChemicalSubstance updatedPXylene = updatedPXyleneBuilder.build();
        GsrsEntityService.CreationResult<Substance> createResult= substanceEntityService.createEntity(updatedPXylene.toFullJsonNode());
        Assertions.assertTrue(createResult.isCreated());

        StringBuilder notations = new StringBuilder();
        TransactionTemplate componentFixTransaction = newTransactionTemplate();
        componentFixTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        componentFixTransaction.executeWithoutResult(status ->
                substanceSynchronizer.fixSubstanceReferences(pmXylenes, notations::append, uuidCodeSystem, approvalIdCodeSystem)
        );

        EntityUtils.EntityWrapper<?> mixtureWrapper = EntityUtils.EntityWrapper.of(pmXylenes);
        EntityFetcher<?> mixtureFetcher = EntityFetcher.of( EntityUtils.Key.of(mixtureWrapper));
        System.out.println(notations);
        MixtureSubstance fetchedMixture = (MixtureSubstance) mixtureFetcher.getIfPossible().get();

        Assertions.assertTrue( fetchedMixture.mixture.components.stream().filter(p->p.substance.refPname.equals(pXyleneName)).allMatch(p->p.substance.refuuid.equals(changedpXyleneUuid.toString())));

    }

}
