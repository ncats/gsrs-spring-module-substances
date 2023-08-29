package example.imports;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.standardizer.SubstanceSynchronizer;
import gsrs.module.substance.tasks.SubstanceReferenceState;
import gsrs.service.GsrsEntityService;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.EntityFetcher;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.MixtureSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.MixtureSubstance;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
class SubstanceSynchronizerTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    SubstanceSynchronizer substanceSynchronizer;

    @Autowired
    protected SubstanceEntityService substanceEntityService;

    private final String uuidCodeSystem ="Code for UUID";
    private final String approvalIdCodeSystem ="Code for ApprovalID";

    @WithMockUser(username = "admin", roles = "Admin")
    @Test
    void testRelationshipResolution() throws Exception {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder = new ChemicalSubstanceBuilder();
        String mXyleneName ="m xylene";
        chemicalSubstanceBuilder.addName(mXyleneName);
        chemicalSubstanceBuilder.setStructureWithDefaultReference("c1(C)cc(C)ccc1");
        UUID initialmXyleneUuid= UUID.randomUUID();
        chemicalSubstanceBuilder.setUUID(initialmXyleneUuid);
        ChemicalSubstance mXylene = chemicalSubstanceBuilder.build();

        GsrsEntityService.CreationResult<Substance> result= substanceEntityService.createEntity(mXylene.toFullJsonNode());
        Assertions.assertTrue(result.isCreated());

        ChemicalSubstanceBuilder pXyleneBuilder = new ChemicalSubstanceBuilder();
        String pXyleneName="p xylene";
        pXyleneBuilder.addName(pXyleneName);
        UUID initialpXyleneUuid= UUID.randomUUID();
        pXyleneBuilder.setStructureWithDefaultReference("c1(C)ccc(C)cc1");
        pXyleneBuilder.setUUID(initialpXyleneUuid);
        String relationshipType = "STRUCTURAL ISOMER->OTHER STRUCTURAL ISOMER";
        pXyleneBuilder.addRelationshipTo(mXylene, relationshipType);

        ChemicalSubstance pXylene = pXyleneBuilder.build();
        GsrsEntityService.CreationResult<Substance> result2= substanceEntityService.createEntity(pXylene.toFullJsonNode());
        Assertions.assertTrue(result2.isCreated());

        Code uuidCode = new Code(uuidCodeSystem, pXylene.getUuid().toString());
        pXylene.addCode(uuidCode);
        UUID changedpXyleneUuid =UUID.randomUUID();
        substanceRepository.deleteById(initialpXyleneUuid);
        ChemicalSubstanceBuilder updatedPXyleneBuilder = pXylene.toChemicalBuilder();
        updatedPXyleneBuilder.setUUID(changedpXyleneUuid);
        ChemicalSubstance updatedPXylene = updatedPXyleneBuilder.build();
        substanceEntityService.createEntity (updatedPXyleneBuilder.buildJson());

        Code uuidCode2 = new Code(uuidCodeSystem, mXylene.getUuid().toString());
        mXylene.addCode(uuidCode2);

        UUID changedmXyleneUuid =UUID.randomUUID();
        mXylene.setUuid(changedmXyleneUuid);
        ChemicalSubstanceBuilder updatedMXyleneBuilder = mXylene.toChemicalBuilder();
        updatedMXyleneBuilder.setUUID(changedmXyleneUuid);
        substanceRepository.deleteById(initialmXyleneUuid);
        GsrsEntityService.CreationResult<Substance> createResult= substanceEntityService.createEntity(updatedMXyleneBuilder.buildJson());
        Assertions.assertTrue(createResult.isCreated());

        StringBuilder notations = new StringBuilder();
        substanceSynchronizer.fixSubstanceReferences(updatedPXylene, notations::append, uuidCodeSystem, approvalIdCodeSystem);

        EntityUtils.EntityWrapper<Substance> substanceWrapper = EntityUtils.EntityWrapper.of(updatedPXylene);
        EntityFetcher<?> updatedXyleneFetcher = EntityFetcher.of( EntityUtils.Key.of(substanceWrapper));
        System.out.println(notations);
        ChemicalSubstance fetchedPXylene = (ChemicalSubstance) updatedXyleneFetcher.getIfPossible().get();

        Assertions.assertEquals(fetchedPXylene.relationships.get(0).relatedSubstance.refuuid, changedmXyleneUuid.toString());
    }

    @WithMockUser(username = "admin", roles = "Admin")
    @Test
    void testMixtureComponentResolution() throws Exception {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder = new ChemicalSubstanceBuilder();
        String mXyleneName ="m xylene";
        chemicalSubstanceBuilder.addName(mXyleneName);
        chemicalSubstanceBuilder.setStructureWithDefaultReference("c1(C)cc(C)ccc1");
        UUID initialmXyleneUuid= UUID.randomUUID();
        chemicalSubstanceBuilder.setUUID(initialmXyleneUuid);
        ChemicalSubstance mXylene = chemicalSubstanceBuilder.build();

        GsrsEntityService.CreationResult<Substance> result= substanceEntityService.createEntity(mXylene.toFullJsonNode());
        Assertions.assertTrue(result.isCreated());

        ChemicalSubstanceBuilder pXyleneBuilder = new ChemicalSubstanceBuilder();
        String pXyleneName="p xylene";
        pXyleneBuilder.addName(pXyleneName);
        UUID initialpXyleneUuid= UUID.randomUUID();
        pXyleneBuilder.setStructureWithDefaultReference("c1(C)ccc(C)cc1");
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

        Code uuidCode = new Code(uuidCodeSystem, pXylene.getUuid().toString());
        pXylene.addCode(uuidCode);
        UUID changedpXyleneUuid =UUID.randomUUID();
        substanceRepository.deleteById(initialpXyleneUuid);
        ChemicalSubstanceBuilder updatedPXyleneBuilder = pXylene.toChemicalBuilder();
        updatedPXyleneBuilder.setUUID(changedpXyleneUuid);
        substanceEntityService.createEntity (updatedPXyleneBuilder.buildJson());

        Code uuidCode2 = new Code(uuidCodeSystem, mXylene.getUuid().toString());
        mXylene.addCode(uuidCode2);

        UUID changedmXyleneUuid =UUID.randomUUID();
        mXylene.setUuid(changedmXyleneUuid);
        ChemicalSubstanceBuilder updatedMXyleneBuilder = mXylene.toChemicalBuilder();
        updatedMXyleneBuilder.setUUID(changedmXyleneUuid);
        substanceRepository.deleteById(initialmXyleneUuid);
        GsrsEntityService.CreationResult<Substance> createResult= substanceEntityService.createEntity(updatedMXyleneBuilder.buildJson());
        Assertions.assertTrue(createResult.isCreated());

        StringBuilder notations = new StringBuilder();
        substanceSynchronizer.fixSubstanceReferences(pmXylenes, notations::append, uuidCodeSystem, approvalIdCodeSystem);

        EntityUtils.EntityWrapper<?> mixtureWrapper = EntityUtils.EntityWrapper.of(pmXylenes);
        EntityFetcher<?> mixtureFetcher = EntityFetcher.of( EntityUtils.Key.of(mixtureWrapper));
        System.out.println(notations);
        MixtureSubstance fetchedMixture = (MixtureSubstance) mixtureFetcher.getIfPossible().get();

        Assertions.assertTrue( fetchedMixture.mixture.components.stream().filter(p->p.substance.refPname.equals(pXyleneName)).allMatch(p->p.substance.refuuid.equals(changedpXyleneUuid.toString())));

    }

    @WithMockUser(username = "admin", roles = "Admin")
    @Test
    void testMixtureComponentResolutionSkip() throws Exception {
        //make sure that a substance reference that is complete/accurate/saved is recognized as such
        ChemicalSubstanceBuilder chemicalSubstanceBuilder = new ChemicalSubstanceBuilder();
        String mXyleneName ="m xylene";
        chemicalSubstanceBuilder.addName(mXyleneName);
        chemicalSubstanceBuilder.setStructureWithDefaultReference("c1(C)cc(C)ccc1");
        UUID initialmXyleneUuid= UUID.randomUUID();
        chemicalSubstanceBuilder.setUUID(initialmXyleneUuid);
        ChemicalSubstance mXylene = chemicalSubstanceBuilder.build();

        GsrsEntityService.CreationResult<Substance> result= substanceEntityService.createEntity(mXylene.toFullJsonNode());
        Assertions.assertTrue(result.isCreated());

        ChemicalSubstanceBuilder pXyleneBuilder = new ChemicalSubstanceBuilder();
        String pXyleneName="p xylene";
        pXyleneBuilder.addName(pXyleneName);
        UUID initialpXyleneUuid= UUID.randomUUID();
        pXyleneBuilder.setStructureWithDefaultReference("c1(C)ccc(C)cc1");
        pXyleneBuilder.setUUID(initialpXyleneUuid);

        ChemicalSubstance pXylene = pXyleneBuilder.build();
        GsrsEntityService.CreationResult<Substance> result2= substanceEntityService.createEntity(pXylene.toFullJsonNode());
        Assertions.assertTrue(result2.isCreated());

        MixtureSubstanceBuilder mixtureSubstanceBuilder = new MixtureSubstanceBuilder();
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", pXylene, mXylene);
        mixtureSubstanceBuilder.addName("p, m xylenes");
        mixtureSubstanceBuilder.setUUID(UUID.randomUUID());
        MixtureSubstance pmXylenes = mixtureSubstanceBuilder.build();
        GsrsEntityService.CreationResult<Substance> result3= substanceEntityService.createEntity(pmXylenes.toFullJsonNode());
        Assertions.assertTrue(result3.isCreated());

        Code uuidCode = new Code(uuidCodeSystem, pXylene.getUuid().toString());
        pXylene.addCode(uuidCode);
        Code uuidCode2 = new Code(uuidCodeSystem, mXylene.getUuid().toString());
        mXylene.addCode(uuidCode2);

        StringBuilder notations = new StringBuilder();
        SubstanceReferenceState actual= substanceSynchronizer.resolveSubstanceReference(pmXylenes.mixture.components.get(0).substance, notations::append, uuidCodeSystem, approvalIdCodeSystem);
        SubstanceReferenceState expected = SubstanceReferenceState.ALREADY_RESOLVED;
        Assertions.assertEquals(expected, actual);
    }

}
