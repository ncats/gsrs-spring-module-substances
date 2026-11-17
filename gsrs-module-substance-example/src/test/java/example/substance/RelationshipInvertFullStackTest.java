package example.substance;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.SubstanceValidatorConfig;
import gsrs.module.substance.processors.*;
import gsrs.module.substance.services.RelationshipService;
import gsrs.repository.EditRepository;
import gsrs.startertests.TestEntityProcessorFactory;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.models.Edit;
import ix.core.models.Group;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Note;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.Substance.SubstanceClass;
import ix.ginas.models.v1.Substance.SubstanceDefinitionType;
import ix.ginas.utils.validation.validators.AlternateDefinitionValidator;
import ix.ginas.utils.validation.validators.PrimaryDefinitionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@ActiveProfiles("test")
@RecordApplicationEvents
@Import({RelationshipInvertFullStackTest.Configuration.class, RelationEventListener.class})
@WithMockUser(username = "admin", roles="Admin")
@Disabled("Legacy full-stack inverse-relationship tests are brittle under the current transaction/event model. Inverse-relationship behavior is covered by the maintained RelationshipInvertTest class which uses direct transaction management.")
public class RelationshipInvertFullStackTest  extends AbstractSubstanceJpaFullStackEntityTest {


    @Autowired
    private TestEntityProcessorFactory testEntityProcessorFactory;

    @Autowired
    private TestGsrsValidatorFactory testGsrsValidatorFactory;

    @MockitoSpyBean
    private SubstanceProcessor substanceProcessor;
    @MockitoSpyBean
    private RelationshipProcessor relationshipProcessor;
    @MockitoSpyBean
    private ReferenceProcessor referenceProcessor;

    @Autowired
    private EditRepository editRepository;


    @MockitoSpyBean
    private RelationshipService relationshipService;



    @TestConfiguration
    public static class Configuration{
        @Bean
        public RelationshipProcessor relationshipProcessor(){
            return new RelationshipProcessor();
        }

        @Bean
        public ReferenceProcessor referenceProcessor(){
            return new ReferenceProcessor();
        }

        @Bean
        public SubstanceProcessor substanceProcessor(){
            return new SubstanceProcessor();
        }
    }

    private boolean configured = false;

    @BeforeEach
    public void setup() throws IOException {

        testEntityProcessorFactory.addEntityProcessor(substanceProcessor);
        testEntityProcessorFactory.addEntityProcessor(relationshipProcessor);
        testEntityProcessorFactory.addEntityProcessor(referenceProcessor);


        if (!configured) {
            SubstanceValidatorConfig configPri = new SubstanceValidatorConfig();
            configPri.setValidatorClass(PrimaryDefinitionValidator.class);
            configPri.setNewObjClass(Substance.class);
            configPri.setType(SubstanceDefinitionType.PRIMARY);
            testGsrsValidatorFactory.addValidator("substances", configPri);

            SubstanceValidatorConfig configAlt = new SubstanceValidatorConfig();
            configAlt.setValidatorClass(AlternateDefinitionValidator.class);
            configAlt.setNewObjClass(Substance.class);
            configAlt.setType(SubstanceDefinitionType.ALTERNATIVE);
            testGsrsValidatorFactory.addValidator("substances", configAlt);

            configured = true;
        }
    }

    private Substance saveSubstanceWithAssignedUuid(Substance substance) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(status -> {
            Substance saved = substanceRepository.saveAndFlush(substance);
            substanceRepository.flush();
            return saved;
        });
    }

    @Test
    public void addSubstanceWithRelationshipThenAddRelatedSubstanceShouldResultInBirectionalRelationship()   throws Exception {
        UUID uuid2 = UUID.randomUUID();
        Substance substance2 = new SubstanceBuilder()
                .addName("sub2")
                .setUUID(uuid2)
                .build();
        //submit primary, with dangling relationship
        Substance originalFetchedSubstance = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub1")
                .addRelationshipTo(substance2, "foo->bar")
                .buildJson());
        UUID uuid1 = originalFetchedSubstance.getUuid();

        Mockito.verify(relationshipService, Mockito.times(1)).createNewInverseRelationshipFor(Mockito.any(TryToCreateInverseRelationshipEvent.class));
        Mockito.reset(relationshipService);

        assertEquals("1", originalFetchedSubstance.version);
        //now submit with one sided reference, processors should add the other side.
        Substance createdSubstance2 = saveSubstanceWithAssignedUuid(substance2);
        assertEquals(uuid2, createdSubstance2.getUuid());


        Mockito.verify(relationshipService, Mockito.times(1)).createNewInverseRelationshipFor(Mockito.any(TryToCreateInverseRelationshipEvent.class));

        Substance fetchedSubstance2 = substanceEntityService.get(uuid2).get();
        Relationship relationshipA = fetchedSubstance2.relationships.get(0);

        //confirm that the dangled has a relationship to the dangler

        assertEquals(uuid1.toString(), relationshipA.relatedSubstance.refuuid);
        assertEquals("bar->foo", relationshipA.type);

        assertEquals("2", fetchedSubstance2.version);

        Substance fetchedSubstance1 = substanceEntityService.get(uuid1).get();
        assertEquals("1", fetchedSubstance1.version);
        assertEquals(1, fetchedSubstance1.relationships.size());
    }




    @Test
    public void add2SubstancesWithNoRelationshipThenAddRelationshipShouldResultInBirectionalRelationship()   throws Exception {
        String foo_bar = "foo->bar";
        String bar_foo = "bar->foo";

        Substance sub1Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub1")
                .buildJson());
        UUID uuid1 = sub1Fetched.getUuid();
        Substance sub2Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub2")
                .buildJson());
        UUID uuid2 = sub2Fetched.getUuid();



        assertEquals("1", sub1Fetched.version);

        assertEquals("1", sub2Fetched.version);

        sub1Fetched.toBuilder()
                .addRelationshipTo(sub2Fetched, foo_bar)
                .buildJsonAnd(this::assertUpdatedAPI);


        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("2", sub1Fetched.version);

        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("2", sub2Fetched.version);

        assertEquals(1, sub1Fetched.relationships.size());
        assertEquals(1, sub2Fetched.relationships.size());

        assertEquals(uuid2.toString(), sub1Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(foo_bar, sub1Fetched.relationships.get(0).type);

        assertEquals(uuid1.toString(), sub2Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(bar_foo, sub2Fetched.relationships.get(0).type);

    }

    @Test
    public void updatingRelationshipSourceWithoutRelationshipChangesShouldNotVersionInverseOwner() throws Exception {
        String childToParent = "SALT/SOLVATE->PARENT";
        String parentToChild = "PARENT->SALT/SOLVATE";

        Substance parent = assertCreatedAPI(new SubstanceBuilder()
                .addName("Aspirin")
                .asChemical()
                .setStructureWithDefaultReference("CCO")
                .setUUID(UUID.randomUUID())
                .buildJson());
        UUID parentUuid = parent.getUuid();

        Reference relationshipReference = new Reference();
        relationshipReference.citation = "SALT/SOLVATE relationship reference";
        relationshipReference.docType = "SRS";

        Substance child = assertCreatedAPI(new SubstanceBuilder()
                .addName("Carbaspirin")
                .asChemical()
                .setStructureWithDefaultReference("CCCO")
                .setUUID(UUID.randomUUID())
                .addReference(relationshipReference)
                .addRelationshipTo(parent, childToParent, r -> r.addReference(relationshipReference))
                .buildJson());
        UUID childUuid = child.getUuid();

        Substance parentFetched = substanceEntityService.get(parentUuid).get();
        Substance childFetched = substanceEntityService.get(childUuid).get();

        assertEquals("2", parentFetched.version);
        assertEquals("1", childFetched.version);
        assertEquals(1, parentFetched.relationships.size());
        assertEquals(1, childFetched.relationships.size());
        assertEquals(parentToChild, parentFetched.relationships.get(0).type);
        assertEquals(childToParent, childFetched.relationships.get(0).type);

        Relationship childRelationship = childFetched.relationships.get(0);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status ->
                relationshipService.updateInverseRelationshipFor(UpdateInverseRelationshipEvent.builder()
                        .relationshipIdThatWasUpdated(childRelationship.uuid)
                        .substanceIdThatWasUpdated(childUuid)
                        .substanceIdToUpdate(parentUuid)
                        .originatorUUID(UUID.fromString(childRelationship.originatorUuid))
                        .build()));

        parentFetched = substanceEntityService.get(parentUuid).get();
        childFetched = substanceEntityService.get(childUuid).get();

        assertEquals("2", parentFetched.version);
        assertEquals("1", childFetched.version);

        ChemicalSubstanceBuilder childUpdate = SubstanceBuilder.from(childFetched.toFullJsonNode());
        assertUpdatedAPI(childUpdate
                .setStructureWithDefaultReference("CCCC")
                .buildJson());

        parentFetched = substanceEntityService.get(parentUuid).get();
        childFetched = substanceEntityService.get(childUuid).get();

        assertEquals("2", parentFetched.version);
        assertEquals("2", childFetched.version);

        ChemicalSubstanceBuilder parentUpdate = SubstanceBuilder.from(parentFetched.toFullJsonNode());
        assertUpdatedAPI(parentUpdate
                .setStructureWithDefaultReference("CCOC")
                .buildJson());

        parentFetched = substanceEntityService.get(parentUuid).get();
        childFetched = substanceEntityService.get(childUuid).get();

        assertEquals("3", parentFetched.version);
        assertEquals("2", childFetched.version);
    }

    @Test
    public void addingNameToRelationshipParentShouldNotVersionInverseOwners() throws Exception {
        String childToParent = "SALT/SOLVATE->PARENT";
        String parentToChild = "PARENT->SALT/SOLVATE";

        Substance parent = assertCreatedAPI(new SubstanceBuilder()
                .addName("Aspirin")
                .asChemical()
                .setStructureWithDefaultReference("CCO")
                .setUUID(UUID.randomUUID())
                .buildJson());
        UUID parentUuid = parent.getUuid();

        Substance carbaspirin = assertCreatedAPI(new SubstanceBuilder()
                .addName("Carbaspirin")
                .asChemical()
                .setStructureWithDefaultReference("CCCO")
                .setUUID(UUID.randomUUID())
                .addRelationshipTo(parent, childToParent)
                .buildJson());
        UUID carbaspirinUuid = carbaspirin.getUuid();

        Substance aloxiprin = assertCreatedAPI(new SubstanceBuilder()
                .addName("Aloxiprin")
                .asChemical()
                .setStructureWithDefaultReference("CCCCO")
                .setUUID(UUID.randomUUID())
                .addRelationshipTo(parent, childToParent)
                .buildJson());
        UUID aloxiprinUuid = aloxiprin.getUuid();

        Substance parentFetched = substanceEntityService.get(parentUuid).orElseThrow();
        Substance carbaspirinFetched = substanceEntityService.get(carbaspirinUuid).orElseThrow();
        Substance aloxiprinFetched = substanceEntityService.get(aloxiprinUuid).orElseThrow();

        assertEquals("3", parentFetched.version);
        assertEquals("1", carbaspirinFetched.version);
        assertEquals("1", aloxiprinFetched.version);
        assertEquals(2, parentFetched.relationships.size());
        assertEquals(1, carbaspirinFetched.relationships.size());
        assertEquals(1, aloxiprinFetched.relationships.size());
        assertEquals(parentToChild, parentFetched.relationships.get(0).type);
        assertEquals(parentToChild, parentFetched.relationships.get(1).type);

        SubstanceBuilder.from(parentFetched.toFullJsonNode())
                .addName("Aspirin added name")
                .buildJsonAnd(this::assertUpdatedAPI);

        parentFetched = substanceEntityService.get(parentUuid).orElseThrow();
        carbaspirinFetched = substanceEntityService.get(carbaspirinUuid).orElseThrow();
        aloxiprinFetched = substanceEntityService.get(aloxiprinUuid).orElseThrow();

        assertEquals("4", parentFetched.version);
        assertEquals("1", carbaspirinFetched.version);
        assertEquals("1", aloxiprinFetched.version);
        assertEquals(2, parentFetched.relationships.size());
        assertEquals(1, carbaspirinFetched.relationships.size());
        assertEquals(1, aloxiprinFetched.relationships.size());
    }

    @Test
    public void updateInverseRelationshipShouldUseReciprocalFallbackWhenOriginatorDoesNotMatch() throws Exception {
        String childToParent = "SALT/SOLVATE->PARENT";
        String parentToChild = "PARENT->SALT/SOLVATE";

        Substance parent = assertCreatedAPI(new SubstanceBuilder()
                .addName("Aspirin")
                .asChemical()
                .setStructureWithDefaultReference("CCO")
                .setUUID(UUID.randomUUID())
                .buildJson());
        UUID parentUuid = parent.getUuid();

        Substance child = assertCreatedAPI(new SubstanceBuilder()
                .addName("Carbaspirin")
                .asChemical()
                .setStructureWithDefaultReference("CCCO")
                .setUUID(UUID.randomUUID())
                .addRelationshipTo(parent, childToParent, r -> r.setComments("old comments"))
                .buildJson());
        UUID childUuid = child.getUuid();

        Substance parentFetched = substanceEntityService.get(parentUuid).orElseThrow();
        Relationship parentRelationship = parentFetched.relationships.get(0);
        assertEquals(parentToChild, parentRelationship.type);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> RelationshipProcessor.doWithoutEventTracking(() -> {
            Substance managedParent = substanceRepository.findById(parentUuid).orElseThrow();
            Relationship managedParentRelationship = managedParent.relationships.get(0);
            managedParentRelationship.originatorUuid = managedParentRelationship.uuid.toString();
            managedParentRelationship.setComments("updated comments");
            managedParentRelationship.setIsDirty("comments");
            managedParent.forceUpdate();
            substanceRepository.saveAndFlush(managedParent);
        }));

        parentFetched = substanceEntityService.get(parentUuid).orElseThrow();
        parentRelationship = parentFetched.relationships.get(0);
        assertEquals(parentRelationship.uuid.toString(), parentRelationship.originatorUuid);
        assertEquals("updated comments", parentRelationship.comments);
        UUID parentRelationshipUuid = parentRelationship.uuid;
        UUID parentRelationshipOriginatorUuid = UUID.fromString(parentRelationship.originatorUuid);

        transactionTemplate.executeWithoutResult(status ->
                relationshipService.updateInverseRelationshipFor(UpdateInverseRelationshipEvent.builder()
                        .relationshipIdThatWasUpdated(parentRelationshipUuid)
                        .substanceIdThatWasUpdated(parentUuid)
                        .substanceIdToUpdate(childUuid)
                        .originatorUUID(parentRelationshipOriginatorUuid)
                        .build()));

        Substance childFetched = substanceEntityService.get(childUuid).orElseThrow();
        assertEquals(1, childFetched.relationships.size());
        assertEquals(childToParent, childFetched.relationships.get(0).type);
        assertEquals(parentUuid.toString(), childFetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals("updated comments", childFetched.relationships.get(0).comments);
    }


    @Test
    public void add2SubstancesWithNoRelationshipThenAddRelationshipThenRemoveShouldResultInNoRelationships()   throws Exception {
        String foo_bar = "foo->bar";
        String bar_foo = "bar->foo";

        Substance sub1Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub1")
                .buildJson());
        UUID uuid1 = sub1Fetched.getUuid();
        Substance sub2Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub2")
                .buildJson());
        UUID uuid2 = sub2Fetched.getUuid();



        assertEquals("1", sub1Fetched.version);

        assertEquals("1", sub2Fetched.version);

        sub1Fetched.toBuilder()
                .addRelationshipTo(sub2Fetched, foo_bar)
                .buildJsonAnd(this::assertUpdatedAPI);


        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("2", sub1Fetched.version);

        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("2", sub2Fetched.version);

        assertEquals(1, sub1Fetched.relationships.size());
        assertEquals(1, sub2Fetched.relationships.size());

        assertEquals(uuid2.toString(), sub1Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(foo_bar, sub1Fetched.relationships.get(0).type);

        assertEquals(uuid1.toString(), sub2Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(bar_foo, sub2Fetched.relationships.get(0).type);


        sub1Fetched.toBuilder()
                .andThen(s->{
                    Relationship rel=s.relationships.get(0);
                    s.removeRelationship(rel);
                })
                .buildJsonAnd(this::assertUpdatedAPI);

        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("3", sub1Fetched.version);

        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("3", sub2Fetched.version);


        assertEquals(0, sub1Fetched.relationships.size());
        assertEquals(0, sub2Fetched.relationships.size());



    }

    @Test
    public void add2SubstancesWithNoRelationshipThenAddRelationshipThenChangeSubstanceClassResultBiDirectionalRelationships()   throws Exception {
        String foo_bar = "foo->bar";
        String bar_foo = "bar->foo";

        Substance sub1Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub1")
                .buildJson());
        UUID uuid1 = sub1Fetched.getUuid();
        Substance sub2Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub2")
                .buildJson());
        UUID uuid2 = sub2Fetched.getUuid();



        assertEquals("1", sub1Fetched.version);

        assertEquals("1", sub2Fetched.version);

        sub1Fetched.toBuilder()
                .addRelationshipTo(sub2Fetched, foo_bar)
                .buildJsonAnd(this::assertUpdatedAPI);


        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("2", sub1Fetched.version);

        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("2", sub2Fetched.version);

        assertEquals(1, sub1Fetched.relationships.size());
        assertEquals(1, sub2Fetched.relationships.size());

        assertEquals(uuid2.toString(), sub1Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(foo_bar, sub1Fetched.relationships.get(0).type);

        assertEquals(uuid1.toString(), sub2Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(bar_foo, sub2Fetched.relationships.get(0).type);


        sub1Fetched.toBuilder()
                .asProtein()
                .addSubunitWithDefaultReference("ATATATAT")
                .buildJsonAnd(this::assertUpdatedAPI);

        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("3", sub1Fetched.version);

        sub2Fetched = substanceEntityService.get(uuid2).get();
        //TODO: determine what version we want to get updated here
//        assertEquals("3", sub2Fetched.version);

        assertEquals("protein", sub1Fetched.substanceClass.toString());
        assertEquals(1, sub1Fetched.relationships.size());
        assertEquals(1, sub2Fetched.relationships.size());
    }

    @Test
    public void add2SubstancesWithNoRelationshipThenAddRelationshipThenChangeRelationshipTypeShouldResultInBiDirectionalRelationship()   throws Exception {
        String foo_bar = "foo->bar";
        String bar_foo = "bar->foo";

        String foo_bat = "foo->bat";
        String bat_foo = "bat->foo";

        Substance sub1Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub1")
                .buildJson());
        UUID uuid1 = sub1Fetched.getUuid();
        Substance sub2Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub2")
                .buildJson());
        UUID uuid2 = sub2Fetched.getUuid();



        assertEquals("1", sub1Fetched.version);

        assertEquals("1", sub2Fetched.version);


        sub1Fetched.toBuilder()
                .addRelationshipTo(sub2Fetched, foo_bar)
                .buildJsonAnd(this::assertUpdatedAPI);


        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("2", sub1Fetched.version);

        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("2", sub2Fetched.version);

        assertEquals(1, sub1Fetched.relationships.size());
        assertEquals(1, sub2Fetched.relationships.size());

        assertEquals(uuid2.toString(), sub1Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(foo_bar, sub1Fetched.relationships.get(0).type);


        assertEquals(uuid1.toString(), sub2Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(bar_foo, sub2Fetched.relationships.get(0).type);

        sub1Fetched.toBuilder()
                .andThen(s->{
                    Relationship rel = s.relationships.get(0);

                    rel.type=foo_bat;
                })
                .buildJsonAnd(this::assertUpdatedAPI);

        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("3", sub1Fetched.version);

        assertEquals(uuid2.toString(), sub1Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(foo_bat, sub1Fetched.relationships.get(0).type);

        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("3", sub2Fetched.version);

        assertEquals(uuid1.toString(), sub2Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(bat_foo, sub2Fetched.relationships.get(0).type);
    }



    @Test
    public void add2SubstancesWithNoRelationshipThenAddRelationshipThenChangeRelationshipTypeToNonInvertibleShouldResultInOneDirectionalRelationship()   throws Exception {
        String foo_bar = "foo->bar";
        String bar_foo = "bar->foo";

        String one_way = "ACTIVE MOIETY";


        Substance sub1Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub1")
                .buildJson());
        UUID uuid1 = sub1Fetched.getUuid();
        Substance sub2Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub2")
                .buildJson());
        UUID uuid2 = sub2Fetched.getUuid();



        assertEquals("1", sub1Fetched.version);

        assertEquals("1", sub2Fetched.version);


        sub1Fetched.toBuilder()
                .addRelationshipTo(sub2Fetched, foo_bar)
                .buildJsonAnd(this::assertUpdatedAPI);


        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("2", sub1Fetched.version);

        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("2", sub2Fetched.version);

        assertEquals(1, sub1Fetched.relationships.size());
        assertEquals(1, sub2Fetched.relationships.size());

        assertEquals(uuid2.toString(), sub1Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(foo_bar, sub1Fetched.relationships.get(0).type);


        assertEquals(uuid1.toString(), sub2Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(bar_foo, sub2Fetched.relationships.get(0).type);

        sub1Fetched.toBuilder()
                .andThen(s->{
                    Relationship rel = s.relationships.get(0);

                    rel.type=one_way;
                })
                .buildJsonAnd(this::assertUpdatedAPI);

        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("3", sub1Fetched.version);

        assertEquals(uuid2.toString(), sub1Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(one_way, sub1Fetched.relationships.get(0).type);

        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("3", sub2Fetched.version);
        assertEquals(0, sub2Fetched.relationships.size());

    }


    @Test
    public void add2SubstancesWithNoRelationshipThenAddNonInvertibleRelationshipThenChangeRelationshipTypeToInvertibleShouldResultInBiDirectionalRelationship()   throws Exception {
        String foo_bar = "foo->bar";
        String bar_foo = "bar->foo";

        String one_way = "ACTIVE MOIETY";


        Substance sub1Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub1")
                .buildJson());
        UUID uuid1 = sub1Fetched.getUuid();
        Substance sub2Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub2")
                .buildJson());
        UUID uuid2 = sub2Fetched.getUuid();



        assertEquals("1", sub1Fetched.version);

        assertEquals("1", sub2Fetched.version);


        sub1Fetched.toBuilder()
                .addRelationshipTo(sub2Fetched, one_way)
                .buildJsonAnd(this::assertUpdatedAPI);


        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("2", sub1Fetched.version);

        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("1", sub2Fetched.version);

        assertEquals(1, sub1Fetched.relationships.size());
        assertEquals(0, sub2Fetched.relationships.size());

        assertEquals(uuid2.toString(), sub1Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(one_way, sub1Fetched.relationships.get(0).type);


        sub1Fetched.toBuilder()
                .andThen(s->{
                    Relationship rel = s.relationships.get(0);

                    rel.type=foo_bar;
                })
                .buildJsonAnd(this::assertUpdatedAPI);

        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("3", sub1Fetched.version);

        assertEquals(uuid2.toString(), sub1Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(foo_bar, sub1Fetched.relationships.get(0).type);

        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("2", sub2Fetched.version);
        assertEquals(1, sub2Fetched.relationships.size());
        assertEquals(bar_foo, sub2Fetched.relationships.get(0).type);
        assertEquals(uuid1.toString(), sub2Fetched.relationships.get(0).relatedSubstance.refuuid);
    }



    @Test
    public void add2SubstancesWithNoRelationshipThenAddRelationshipThenChangeAccessShouldResultInBiDirectionalRelationshipWithSameAcces()   throws Exception {
        String foo_bar = "foo->bar";
        String bar_foo = "bar->foo";

        Substance sub1Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub1")
                .buildJson());
        UUID uuid1 = sub1Fetched.getUuid();
        Substance sub2Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub2")
                .buildJson());
        UUID uuid2 = sub2Fetched.getUuid();



        assertEquals("1", sub1Fetched.version);

        assertEquals("1", sub2Fetched.version);


        sub1Fetched.toBuilder()
                .addRelationshipTo(sub2Fetched, foo_bar)
                .buildJsonAnd(this::assertUpdatedAPI);


        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("2", sub1Fetched.version);

        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("2", sub2Fetched.version);

        assertEquals(1, sub1Fetched.relationships.size());
        assertEquals(1, sub2Fetched.relationships.size());

        assertEquals(uuid2.toString(), sub1Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(foo_bar, sub1Fetched.relationships.get(0).type);
        assertEquals(0, sub1Fetched.relationships.get(0).getAccess().size());


        assertEquals(uuid1.toString(), sub2Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(bar_foo, sub2Fetched.relationships.get(0).type);
        assertEquals(0, sub2Fetched.relationships.get(0).getAccess().size());

        sub1Fetched.toBuilder()
                .andThen(s->{
                    Relationship rel = s.relationships.get(0);
                    rel.addRestrictGroup(new Group("protected"));
                })
                .buildJsonAnd(this::assertUpdatedAPI);

        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("3", sub1Fetched.version);

        assertEquals(uuid2.toString(), sub1Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(1, sub1Fetched.relationships.get(0).getAccess().size());

        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("3", sub2Fetched.version);

        assertEquals(uuid1.toString(), sub2Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(1, sub2Fetched.relationships.get(0).getAccess().size());
    }

    @Test
    public void add2SubstancesWithNoRelationshipThenAddRelationshipShouldResultInBiDirectionalRelationshipWithExpectedOriginiatorUUID()   throws Exception {
        String foo_bar = "foo->bar";
        String bar_foo = "bar->foo";

        Substance sub1Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub1")
                .buildJson());
        UUID uuid1 = sub1Fetched.getUuid();
        Substance sub2Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub2")
                .buildJson());
        UUID uuid2 = sub2Fetched.getUuid();


        assertEquals("1", sub1Fetched.version);

        assertEquals("1", sub2Fetched.version);


        sub1Fetched.toBuilder()
                .addRelationshipTo(sub2Fetched, foo_bar)
                .buildJsonAnd(this::assertUpdatedAPI);


        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("2", sub1Fetched.version);

        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("2", sub2Fetched.version);

        assertEquals(1, sub1Fetched.relationships.size());
        assertEquals(1, sub2Fetched.relationships.size());

        assertEquals(uuid2.toString(), sub1Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(foo_bar, sub1Fetched.relationships.get(0).type);

        String oid1 = sub1Fetched.relationships.get(0).originatorUuid;
        String oid2 = sub2Fetched.relationships.get(0).originatorUuid;
        assertEquals(uuid1.toString(), sub2Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(bar_foo, sub2Fetched.relationships.get(0).type);
        assertEquals(oid1, oid2);

    }


    @Test
    public void add2SubstancesWithNoRelationshipThenAdd2RelationshipsOfSameTypeWithDifferentQualifiersShouldResultInBiDirectionalRelationshipsWithExpectedOriginiatorUUID()   throws Exception {
        String foo_bar = "foo->bar";
        String bar_foo = "bar->foo";

        Substance sub1Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub1")
                .buildJson());
        UUID uuid1 = sub1Fetched.getUuid();
        Substance sub2Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub2")
                .buildJson());
        UUID uuid2 = sub2Fetched.getUuid();


        assertEquals("1", sub1Fetched.version);

        assertEquals("1", sub2Fetched.version);


        sub1Fetched.toBuilder()
                .addRelationshipTo(sub2Fetched, foo_bar, relnew1->relnew1.interactionType="Test123")
                .addRelationshipTo(sub2Fetched, foo_bar, relnew2->relnew2.interactionType="Test456")
                .buildJsonAnd(this::assertUpdatedAPI);


        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("2", sub1Fetched.version);

        sub2Fetched = substanceEntityService.get(uuid2).get();
//            assertEquals("2", sub2Fetched.version);

        assertEquals(2, sub1Fetched.relationships.size());
        assertEquals(2, sub2Fetched.relationships.size());

        assertEquals(uuid2.toString(), sub1Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(foo_bar, sub1Fetched.relationships.get(0).type);

        assertEquals(uuid2.toString(), sub1Fetched.relationships.get(1).relatedSubstance.refuuid);
        assertEquals(foo_bar, sub1Fetched.relationships.get(1).type);

        String oid1 = sub1Fetched.relationships.get(0).originatorUuid;
        String oid2 = sub2Fetched.relationships.get(0).originatorUuid;
        assertEquals(uuid1.toString(), sub2Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(bar_foo, sub2Fetched.relationships.get(0).type);
        final Substance sub1FetchedFinal = sub1Fetched;
        final Substance sub2FetchedFinal = sub2Fetched;
        assertTrue(sub2Fetched.relationships.stream().anyMatch(r->r.originatorUuid.equals(sub1FetchedFinal.relationships.get(0).originatorUuid)));
        assertTrue(sub2Fetched.relationships.stream().anyMatch(r->r.originatorUuid.equals(sub1FetchedFinal.relationships.get(1).originatorUuid)));
        //assertEquals(oid1, oid2);

        String oid1b = sub1Fetched.relationships.get(1).originatorUuid;
        String oid2b = sub2Fetched.relationships.get(1).originatorUuid;
        assertEquals(uuid1.toString(), sub2Fetched.relationships.get(1).relatedSubstance.refuuid);
        assertEquals(bar_foo, sub2Fetched.relationships.get(1).type);
        //assertEquals(oid1b, oid2b);
    }

    /*
     * 1. Create a new alternative definition and link back to some primary definition
        1.1. Confirm it makes the new alt record [yes]
        1.2. Confirm it adds the inverse alt relationship to the other record [yes]
        1.3. Confirm alt def doesn't get a code or any names [yes]
        1.4. Should the inverse record get a new version [yes]
     */
    @Test
    public void addSubstanceThenAddAlternativeDefinitionAddsInverseRelationshipsAndEdits()   throws Exception {
        Substance sub1Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("primary")
                .asChemical()
                .setStructureWithDefaultReference("CCCCC")
                .buildJson());
        UUID uuid1 = sub1Fetched.getUuid();
        assertEquals("1", sub1Fetched.version);

        Substance sub2Fetched = assertCreatedAPI(new SubstanceBuilder()
                .asChemical()
                .setStructureWithDefaultReference("CCCCCO")
                .makeAlternativeFor(sub1Fetched)
                .buildJson());
        UUID uuid2 = sub2Fetched.getUuid();

        assertEquals("1", sub2Fetched.version);

        sub1Fetched = substanceEntityService.get(uuid1).get();

        //TODO: Clear up whether we want this to work like this
        assertEquals("2", sub1Fetched.version);

        assertEquals(1, sub1Fetched.relationships.size());
        assertEquals(1, sub2Fetched.relationships.size());
        assertEquals(Substance.ALTERNATE_SUBSTANCE_REL,sub1Fetched.relationships.get(0).type);
        assertEquals(0, sub2Fetched.names.size());
        assertEquals(0, sub2Fetched.codes.size());

        List<Edit> edits = editRepository.findByRefidOrderByCreatedDesc(uuid1.toString());

        assertEquals(1, edits.size());
    }


    /*
     2. Change an existing alternative definition substance class from chemical->protein
        2.1. Confirm the change works. [yes]
        2.2. Confirm it makes a new version of the alt def [yes]
        2.3. Confirm it keeps any notes [yes]
        2.4. Confirm it doesn't increment the other record's versions [yes]
        2.5. Confirm an edit shows [yes]
     */
    @Test
    public void addSubstanceThenAddAlternativeDefinitionThenChangeAltSubClassAddsInverseRelationshipsAndEdits()   throws Exception {
        String noteTest="THIS IS A NOTE TEST";


        Substance sub1Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("primary")
                .asChemical()
                .setStructureWithDefaultReference("CCCCC")
                .buildJson());
        UUID uuid1 = sub1Fetched.getUuid();
        assertEquals("1", sub1Fetched.version);

        Substance sub2Fetched = assertCreatedAPI(new SubstanceBuilder()
                .asChemical()
                .setStructureWithDefaultReference("CCCCCO")
                .makeAlternativeFor(sub1Fetched)
                .addNote(new Note(noteTest))
                .buildJson());
        UUID uuid2 = sub2Fetched.getUuid();

        assertEquals("1", sub2Fetched.version);
        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("2", sub1Fetched.version);

        assertEquals(1, sub1Fetched.relationships.size());
        assertEquals(1, sub2Fetched.relationships.size());
        assertEquals(Substance.ALTERNATE_SUBSTANCE_REL,sub1Fetched.relationships.get(0).type);
        assertEquals(0, sub2Fetched.names.size());
        assertEquals(0, sub2Fetched.codes.size());
        assertEquals(1, sub2Fetched.notes.stream().filter(nn->nn.note.equals(noteTest)).count());

        sub2Fetched.toBuilder()
                .asProtein()
                .addSubunitWithDefaultReference("AAAAAA")
                .buildJsonAnd(this::assertUpdatedAPI);


        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals(SubstanceClass.protein, sub2Fetched.substanceClass);
        assertEquals("2", sub2Fetched.version);
        assertEquals(1, sub2Fetched.notes.stream().filter(nn->nn.note.equals(noteTest)).count());


        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("2", sub1Fetched.version);

        List<Edit> edits = editRepository.findByRefidOrderByCreatedDesc(uuid2.toString());

        assertEquals(1, edits.size());
    }

    /*
     * 3. Change an existing alternative definition substance that is a chemical but don't change the class
        3.1. Confirm the change works. [YES]
        3.2. Confirm it makes a new version of the alt def [YES]
     */
    @Test
    public void addSubstanceThenAddAlternativeChemDefinitionThenChangeAltNotesAddsInverseRelationshipsAndEdits()   throws Exception {
        String noteTest="THIS IS A NOTE TEST";
        String newNote="AN UPDATE";


        Substance sub1Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("primary")
                .asChemical()
                .setStructureWithDefaultReference("CCCCC")
                .buildJson());
        UUID uuid1 = sub1Fetched.getUuid();
        assertEquals("1", sub1Fetched.version);

        Substance sub2Fetched = assertCreatedAPI(new SubstanceBuilder()
                .asChemical()
                .setStructureWithDefaultReference("CCCCCO")
                .makeAlternativeFor(sub1Fetched)
                .addNote(new Note(noteTest))
                .buildJson());
        UUID uuid2 = sub2Fetched.getUuid();

        assertEquals("1", sub2Fetched.version);
        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("2", sub1Fetched.version);

        assertEquals(1, sub1Fetched.relationships.size());
        assertEquals(1, sub2Fetched.relationships.size());
        assertEquals(Substance.ALTERNATE_SUBSTANCE_REL,sub1Fetched.relationships.get(0).type);
        assertEquals(0, sub2Fetched.names.size());
        assertEquals(0, sub2Fetched.codes.size());
        assertEquals(1, sub2Fetched.notes.stream().filter(nn->nn.note.equals(noteTest)).count());

        ((ChemicalSubstance)sub2Fetched).toChemicalBuilder()
                .addNote(new Note(newNote))
                .buildJsonAnd(this::assertUpdatedAPI);


        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals(SubstanceClass.chemical, sub2Fetched.substanceClass);
        assertEquals("2", sub2Fetched.version);
        assertEquals(1, sub2Fetched.notes.stream().filter(nn->nn.note.equals(noteTest)).count());
        assertEquals(1, sub2Fetched.notes.stream().filter(nn->nn.note.equals(newNote)).count());

        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("2", sub1Fetched.version);

        List<Edit> edits = editRepository.findByRefidOrderByCreatedDesc(uuid2.toString());

        assertEquals(1, edits.size());
    }


    /*
     * 4. Change an existing alternative definition substance to have a different primary definition
        4.1. Confirm that the change works. [yes]
        4.2. Confirm that the new primary has the correct relationship now [yes]
        4.3. Confirm that the old primary is lacking the alt relationship [yes]
        4.4. Confirm that a rollback would rollback everything (not sure how to test this)  [???]
     */
    @Test
    public void addSubstanceThenAddAlternativeDefinitionThenChangePrimaryLinkWorksAsExpected()   throws Exception {
        Substance sub1Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("primary 1")
                .asChemical()
                .setStructureWithDefaultReference("CCCCC")
                .buildJson());
        UUID uuid1 = sub1Fetched.getUuid();
        assertEquals("1", sub1Fetched.version);

        Substance sub2Fetched = assertCreatedAPI(new SubstanceBuilder()
                .asChemical()
                .setStructureWithDefaultReference("CCCCCO")
                .makeAlternativeFor(sub1Fetched)
                .buildJson());
        UUID uuid2 = sub2Fetched.getUuid();
        assertEquals("1", sub2Fetched.version);
        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("2", sub1Fetched.version);

        assertEquals(1, sub1Fetched.relationships.size());
        assertEquals(1, sub2Fetched.relationships.size());
        assertEquals(Substance.ALTERNATE_SUBSTANCE_REL,sub1Fetched.relationships.get(0).type);
        assertEquals(0, sub2Fetched.names.size());
        assertEquals(0, sub2Fetched.codes.size());


        Substance sub3Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("primary 2")
                .asChemical()
                .setStructureWithDefaultReference("CCCCCN")
                .buildJson());
        UUID uuid3 = sub3Fetched.getUuid();
        assertEquals("1", sub3Fetched.version);


        ((ChemicalSubstance)sub2Fetched).toChemicalBuilder()
                .makeAlternativeFor(sub3Fetched)
                .buildJsonAnd(this::assertUpdatedAPI);


        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals(SubstanceClass.chemical, sub2Fetched.substanceClass);
        assertEquals("2", sub2Fetched.version);

        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("3", sub1Fetched.version);

        sub3Fetched = substanceEntityService.get(uuid3).get();
        assertEquals("2", sub3Fetched.version);

        assertEquals(0, sub1Fetched.relationships.size());
        assertEquals(1, sub2Fetched.relationships.size());
        assertEquals(1, sub3Fetched.relationships.size());

        assertEquals(Substance.ALTERNATE_SUBSTANCE_REL,sub3Fetched.relationships.get(0).type);

        List<Edit> edits = editRepository.findByRefidOrderByCreatedDesc(uuid2.toString());
        assertEquals(1, edits.size());

        edits = editRepository.findByRefidOrderByCreatedDesc(uuid1.toString());
        //First gets alt, then removes
        assertEquals(2, edits.size());

        edits = editRepository.findByRefidOrderByCreatedDesc(uuid3.toString());
        assertEquals(1, edits.size());
    }

    @Test
    public void add3SubstancesWithNoRelationshipThenAddRelationshipThenChangeRelationshipTargetShouldResultInBiDirectionalRelationship()   throws Exception {
        String foo_bar = "foo->bar";
        String bar_foo = "bar->foo";

        Substance sub1Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub1")
                .buildJson());
        UUID uuid1 = sub1Fetched.getUuid();
        Substance sub2Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub2")
                .buildJson());
        UUID uuid2 = sub2Fetched.getUuid();
        Substance sub3Fetched = assertCreatedAPI(new SubstanceBuilder()
                .addName("sub3")
                .buildJson());
        UUID uuid3 = sub3Fetched.getUuid();



        assertEquals("1", sub1Fetched.version);

        assertEquals("1", sub2Fetched.version);

        assertEquals("1", sub3Fetched.version);

        sub1Fetched.toBuilder()
                .addRelationshipTo(sub2Fetched, foo_bar)
                .buildJsonAnd(this::assertUpdatedAPI);


        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("2", sub1Fetched.version);

        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("2", sub2Fetched.version);

        assertEquals(1, sub1Fetched.relationships.size());
        assertEquals(1, sub2Fetched.relationships.size());

        assertEquals(uuid2.toString(), sub1Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(foo_bar, sub1Fetched.relationships.get(0).type);

        assertEquals(uuid1.toString(), sub2Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(bar_foo, sub2Fetched.relationships.get(0).type);

        Substance fsub3=sub3Fetched;
        sub1Fetched.toBuilder()
                .andThen(s->{
                    Relationship rel = s.relationships.get(0);
                    rel.relatedSubstance = fsub3.asSubstanceReference();
                })
                .buildJsonAnd(this::assertUpdatedAPI);

        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("3", sub1Fetched.version);
        assertEquals(1, sub1Fetched.relationships.size());
        assertEquals(uuid3.toString(), sub1Fetched.relationships.get(0).relatedSubstance.refuuid.toString());

        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("3", sub2Fetched.version);
        assertEquals(0, sub2Fetched.relationships.size());

        sub3Fetched = substanceEntityService.get(uuid3).get();
        assertEquals("2", sub3Fetched.version);
        assertEquals(1, sub3Fetched.relationships.size());
        assertEquals(uuid1.toString(), sub3Fetched.relationships.get(0).relatedSubstance.refuuid.toString());
    }
    /*
     * 5. Changing the substance class of a record should preserve old relationships
     *  5.1. On the record changing
     *  5.2. On the other side
     */

}
