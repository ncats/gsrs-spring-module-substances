package example.imports;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.standardizer.SubstanceSynchronizer;
import gsrs.repository.PrincipalRepository;
import gsrs.service.GsrsEntityService;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.models.Principal;
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

import java.util.Date;
import java.util.UUID;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
public class SubstanceSynchronizerTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    SubstanceSynchronizer substanceSynchronizer;

    @Autowired
    protected SubstanceEntityService substanceEntityService;

    @Autowired
    PrincipalRepository principalRepository;

    private final String uuidCodeSystem ="Code for UUID";
    private final String approvalIdCodeSystem ="Code for ApprovalID";

    @WithMockUser(username = "admin", roles = "Admin")
    @Test
    public void testRelationshipResolution() throws Exception {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder = new ChemicalSubstanceBuilder();
        String uniqueToken = UUID.randomUUID().toString().substring(0, 8);
        String mXyleneName ="m xylene " + uniqueToken;
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
        String pXyleneName="p xylene " + uniqueToken;
        pXyleneBuilder.addName(pXyleneName);
        UUID initialpXyleneUuid= UUID.randomUUID();
        pXyleneBuilder.setStructureWithDefaultReference("CCCO");
        pXyleneBuilder.setUUID(initialpXyleneUuid);
        String relationshipType = "STRUCTURAL ISOMER->OTHER STRUCTURAL ISOMER";
        pXyleneBuilder.addRelationshipTo(mXylene, relationshipType);

        ChemicalSubstance pXylene = pXyleneBuilder.build();
        GsrsEntityService.CreationResult<Substance> result2= substanceEntityService.createEntity(pXylene.toFullJsonNode());
        Assertions.assertTrue(result2.isCreated());

        // Use the persisted entity returned by createEntity to avoid stale builder state.
        pXylene = (ChemicalSubstance) result2.getCreatedEntity();
        Assertions.assertNotNull(pXylene);

        pXylene.relationships.get(0).relatedSubstance.refuuid = null;
        pXylene.relationships.get(0).relatedSubstance.wrappedSubstance = null;
        String legacyRelationshipApprovalId = "REL-" + uniqueToken;
        pXylene.relationships.get(0).relatedSubstance.approvalID = legacyRelationshipApprovalId;

        UUID changedmXyleneUuid =UUID.randomUUID();
        ChemicalSubstanceBuilder updatedMXyleneBuilder = new ChemicalSubstanceBuilder();
        updatedMXyleneBuilder.addName(mXyleneName + " (legacy)");
        updatedMXyleneBuilder.setStructureWithDefaultReference("CCN");
        updatedMXyleneBuilder.addCode(uuidCodeSystem, legacyRelationshipApprovalId);
        Principal admin = principalRepository.findDistinctByUsernameIgnoreCase("admin");
        updatedMXyleneBuilder.setApproval(admin, new Date(), legacyRelationshipApprovalId);
        updatedMXyleneBuilder.setUUID(changedmXyleneUuid);
        ChemicalSubstance updatedMXylene = updatedMXyleneBuilder.build();
        GsrsEntityService.CreationResult<Substance> createResult= substanceEntityService.createEntity(updatedMXylene.toFullJsonNode());
        Assertions.assertTrue(createResult.isCreated());

        StringBuilder notations = new StringBuilder();
        // fixSubstanceReferences creates its own REQUIRES_NEW transaction internally;
        // no outer wrapper is needed here and wrapping it can cause UnexpectedRollbackException
        // when the inner tx is silently marked rollback-only by performUpdateEntity.
        substanceSynchronizer.fixSubstanceReferences(pXylene, notations::append, uuidCodeSystem, approvalIdCodeSystem);

        System.out.println(notations);
        Assertions.assertEquals(changedmXyleneUuid.toString(), pXylene.relationships.get(0).relatedSubstance.refuuid);
        Assertions.assertTrue(notations.toString().contains("was found on substance")
                || notations.toString().contains("Resolved record UUID"));
    }

    @WithMockUser(username = "admin", roles = "Admin")
    @Test
    public void testMixtureComponentResolution() throws Exception {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder = new ChemicalSubstanceBuilder();
        String uniqueToken = UUID.randomUUID().toString().substring(0, 8);
        String mXyleneName ="m xylene " + uniqueToken;
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
        String pXyleneName="p xylene " + uniqueToken;
        pXyleneBuilder.addName(pXyleneName);
        UUID initialpXyleneUuid= UUID.randomUUID();
        pXyleneBuilder.setStructureWithDefaultReference("CCCO");
        pXyleneBuilder.setUUID(initialpXyleneUuid);

        ChemicalSubstance pXylene = pXyleneBuilder.build();
        GsrsEntityService.CreationResult<Substance> result2= substanceEntityService.createEntity(pXylene.toFullJsonNode());
        Assertions.assertTrue(result2.isCreated());

        MixtureSubstanceBuilder mixtureSubstanceBuilder = new MixtureSubstanceBuilder();
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", pXylene, mXylene);
        mixtureSubstanceBuilder.addName("pm xylenes " + uniqueToken);
        UUID pmXyleneMixtureUuid = UUID.randomUUID();
        mixtureSubstanceBuilder.setUUID(pmXyleneMixtureUuid);
        MixtureSubstance pmXylenes = mixtureSubstanceBuilder.build();
        GsrsEntityService.CreationResult<Substance> result3= substanceEntityService.createEntity(pmXylenes.toFullJsonNode());
        Assertions.assertTrue(result3.isCreated());

        // Use the persisted entity returned by createEntity to avoid lookup timing issues.
        pmXylenes = (MixtureSubstance) result3.getCreatedEntity();
        Assertions.assertNotNull(pmXylenes);

        String legacyMixtureApprovalId = "MIX-" + uniqueToken;
        pmXylenes.mixture.components.stream()
                .filter(p -> p.substance.refPname.equals(pXyleneName))
                .forEach(p -> {
                    p.substance.refuuid = null;
                    p.substance.wrappedSubstance = null;
                    p.substance.approvalID = legacyMixtureApprovalId;
                });

        UUID changedpXyleneUuid =UUID.randomUUID();
        ChemicalSubstanceBuilder updatedPXyleneBuilder = new ChemicalSubstanceBuilder();
        updatedPXyleneBuilder.addName(pXyleneName + " (legacy)");
        updatedPXyleneBuilder.setStructureWithDefaultReference("CCC");
        updatedPXyleneBuilder.addCode(uuidCodeSystem, legacyMixtureApprovalId);
        Principal admin = principalRepository.findDistinctByUsernameIgnoreCase("admin");
        updatedPXyleneBuilder.setApproval(admin, new Date(), legacyMixtureApprovalId);
        updatedPXyleneBuilder.setUUID(changedpXyleneUuid);
        ChemicalSubstance updatedPXylene = updatedPXyleneBuilder.build();
        GsrsEntityService.CreationResult<Substance> createResult= substanceEntityService.createEntity(updatedPXylene.toFullJsonNode());
        Assertions.assertTrue(createResult.isCreated());

        StringBuilder notations = new StringBuilder();
        // fixSubstanceReferences manages its own REQUIRES_NEW transaction; no outer wrapper needed.
        substanceSynchronizer.fixSubstanceReferences(pmXylenes, notations::append, uuidCodeSystem, approvalIdCodeSystem);

        System.out.println(notations);
        Assertions.assertTrue(pmXylenes.mixture.components.stream()
                .filter(p -> p.substance.refPname.equals(pXyleneName))
                .allMatch(p -> p.substance.refuuid.equals(changedpXyleneUuid.toString())));
        Assertions.assertTrue(notations.toString().contains("Resolved record UUID")
                || notations.toString().contains("was found on substance"));

    }

}
