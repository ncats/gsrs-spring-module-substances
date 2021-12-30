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
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Note;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.Substance.SubstanceClass;
import ix.ginas.models.v1.Substance.SubstanceDefinitionType;
import ix.ginas.utils.validation.validators.AlternateDefinitionValidator;
import ix.ginas.utils.validation.validators.PrimaryDefinitionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@ActiveProfiles("test")
@RecordApplicationEvents
@Import({RelationshipInvertFullStackTest.Configuration.class, RelationEventListener.class})
@WithMockUser(username = "admin", roles="Admin")
public class RelationshipInvertFullStackTest  extends AbstractSubstanceJpaFullStackEntityTest {


    @Autowired
    private TestEntityProcessorFactory testEntityProcessorFactory;

    @Autowired
    private TestGsrsValidatorFactory testGsrsValidatorFactory;

    @SpyBean
    private SubstanceProcessor substanceProcessor;
    @SpyBean
    private RelationshipProcessor relationshipProcessor;
    @SpyBean
    private ReferenceProcessor referenceProcessor;

    @Autowired
    private EditRepository editRepository;


    @SpyBean
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

    @Test
    public void addSubstanceWithRelationshipThenAddRelatedSubstanceShouldResultInBirectionalRelationship()   throws Exception {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        Substance substance2 = new SubstanceBuilder()
                .addName("sub2")
                .setUUID(uuid2)
                .build();
        //submit primary, with dangling relationship
        new SubstanceBuilder()
                .addName("sub1")
                .setUUID(uuid1)
                .addRelationshipTo(substance2, "foo->bar")
                .buildJsonAnd(this::assertCreatedAPI);

        Mockito.verify(relationshipService, Mockito.times(1)).createNewInverseRelationshipFor(Mockito.any(TryToCreateInverseRelationshipEvent.class));
        Mockito.reset(relationshipService);

        Substance originalFetchedSubstance = substanceEntityService.get(uuid1).get();
        assertEquals("1", originalFetchedSubstance.version);
        //now submit with one sided reference, processors should add the other side.
        assertCreatedAPI(substance2.toFullJsonNode());


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
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        String foo_bar = "foo->bar";
        String bar_foo = "bar->foo";
        
        new SubstanceBuilder()
                .addName("sub1")
                .setUUID(uuid1)
                .buildJsonAnd(this::assertCreatedAPI);
        new SubstanceBuilder()
            .addName("sub2")
            .setUUID(uuid2)
            .buildJsonAnd(this::assertCreatedAPI);
       
       

        Substance sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("1", sub1Fetched.version);
        
        Substance sub2Fetched = substanceEntityService.get(uuid2).get();
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
    public void add2SubstancesWithNoRelationshipThenAddRelationshipThenRemoveShouldResultInNoRelationships()   throws Exception {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        String foo_bar = "foo->bar";
        String bar_foo = "bar->foo";
        
        new SubstanceBuilder()
                .addName("sub1")
                .setUUID(uuid1)
                .buildJsonAnd(this::assertCreatedAPI);
        new SubstanceBuilder()
            .addName("sub2")
            .setUUID(uuid2)
            .buildJsonAnd(this::assertCreatedAPI);
       
       

        Substance sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("1", sub1Fetched.version);
        
        Substance sub2Fetched = substanceEntityService.get(uuid2).get();
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
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        String foo_bar = "foo->bar";
        String bar_foo = "bar->foo";
        
        new SubstanceBuilder()
                .addName("sub1")
                .setUUID(uuid1)
                .buildJsonAnd(this::assertCreatedAPI);
        new SubstanceBuilder()
            .addName("sub2")
            .setUUID(uuid2)
            .buildJsonAnd(this::assertCreatedAPI);
       
       

        Substance sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("1", sub1Fetched.version);
        
        Substance sub2Fetched = substanceEntityService.get(uuid2).get();
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
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        String foo_bar = "foo->bar";
        String bar_foo = "bar->foo";
        
        String foo_bat = "foo->bat";
        String bat_foo = "bat->foo";
        
        new SubstanceBuilder()
                .addName("sub1")
                .setUUID(uuid1)
                .buildJsonAnd(this::assertCreatedAPI);
        new SubstanceBuilder()
            .addName("sub2")
            .setUUID(uuid2)
            .buildJsonAnd(this::assertCreatedAPI);
       
       

        Substance sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("1", sub1Fetched.version);
        
        Substance sub2Fetched = substanceEntityService.get(uuid2).get();
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
    public void add2SubstancesWithNoRelationshipThenAddRelationshipThenChangeAccessShouldResultInBiDirectionalRelationshipWithSameAcces()   throws Exception {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        String foo_bar = "foo->bar";
        String bar_foo = "bar->foo";
        
        new SubstanceBuilder()
                .addName("sub1")
                .setUUID(uuid1)
                .buildJsonAnd(this::assertCreatedAPI);
        new SubstanceBuilder()
            .addName("sub2")
            .setUUID(uuid2)
            .buildJsonAnd(this::assertCreatedAPI);
       
       

        Substance sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("1", sub1Fetched.version);
        
        Substance sub2Fetched = substanceEntityService.get(uuid2).get();
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
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        String foo_bar = "foo->bar";
        String bar_foo = "bar->foo";
        
        new SubstanceBuilder()
                .addName("sub1")
                .setUUID(uuid1)
                .buildJsonAnd(this::assertCreatedAPI);
        new SubstanceBuilder()
            .addName("sub2")
            .setUUID(uuid2)
            .buildJsonAnd(this::assertCreatedAPI);
       
       

        Substance sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("1", sub1Fetched.version);
        
        Substance sub2Fetched = substanceEntityService.get(uuid2).get();
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
        
        String oid1=sub1Fetched.relationships.get(0).originatorUuid;
        String oid2=sub2Fetched.relationships.get(0).originatorUuid;
        assertEquals(uuid1.toString(), sub2Fetched.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(bar_foo, sub2Fetched.relationships.get(0).type);
        assertEquals(oid1, oid2);
        
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
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        
        new SubstanceBuilder()
                .addName("primary")
                .asChemical()
                .setStructureWithDefaultReference("CCCCC")
                .setUUID(uuid1)
                .buildJsonAnd(this::assertCreatedAPI);
        Substance sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("1", sub1Fetched.version);
        
        new SubstanceBuilder()
            .setUUID(uuid2)
            .asChemical()
            .setStructureWithDefaultReference("CCCCCO")
            .makeAlternativeFor(sub1Fetched)
            .buildJsonAnd(this::assertCreatedAPI);       
        
        Substance sub2Fetched = substanceEntityService.get(uuid2).get();
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
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        String noteTest="THIS IS A NOTE TEST";
        
        
        new SubstanceBuilder()
                .addName("primary")
                .asChemical()
                .setStructureWithDefaultReference("CCCCC")
                .setUUID(uuid1)
                .buildJsonAnd(this::assertCreatedAPI);
        Substance sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("1", sub1Fetched.version);
        
        new SubstanceBuilder()
            .setUUID(uuid2)
            .asChemical()
            .setStructureWithDefaultReference("CCCCCO")
            .makeAlternativeFor(sub1Fetched)
            .addNote(new Note(noteTest))
            .buildJsonAnd(this::assertCreatedAPI);       
        
        Substance sub2Fetched = substanceEntityService.get(uuid2).get();
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
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        String noteTest="THIS IS A NOTE TEST";
        String newNote="AN UPDATE";
        
        
        new SubstanceBuilder()
                .addName("primary")
                .asChemical()
                .setStructureWithDefaultReference("CCCCC")
                .setUUID(uuid1)
                .buildJsonAnd(this::assertCreatedAPI);
        Substance sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("1", sub1Fetched.version);
        
        new SubstanceBuilder()
            .setUUID(uuid2)
            .asChemical()
            .setStructureWithDefaultReference("CCCCCO")
            .makeAlternativeFor(sub1Fetched)
            .addNote(new Note(noteTest))
            .buildJsonAnd(this::assertCreatedAPI);       
        
        Substance sub2Fetched = substanceEntityService.get(uuid2).get();
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
        UUID uuid1 = UUID.fromString("38026004-e3da-4a2b-be2b-0aaf3d674743");
        UUID uuid2 = UUID.fromString("6bbb7f58-bf3d-454c-880a-08ded7d48694");
        UUID uuid3 = UUID.fromString("3647b733-bb09-45ef-925b-6d6744883e33");
        
        
        
        new SubstanceBuilder()
                .addName("primary 1")
                .asChemical()
                .setStructureWithDefaultReference("CCCCC")
                .setUUID(uuid1)
                .buildJsonAnd(this::assertCreatedAPI);
        Substance sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("1", sub1Fetched.version);
        
        new SubstanceBuilder()
            .setUUID(uuid2)
            .asChemical()
            .setStructureWithDefaultReference("CCCCCO")
            .makeAlternativeFor(sub1Fetched)
            .buildJsonAnd(this::assertCreatedAPI);

        Substance sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("1", sub2Fetched.version);
        sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("2", sub1Fetched.version);
        
        assertEquals(1, sub1Fetched.relationships.size());
        assertEquals(1, sub2Fetched.relationships.size());
        assertEquals(Substance.ALTERNATE_SUBSTANCE_REL,sub1Fetched.relationships.get(0).type);
        assertEquals(0, sub2Fetched.names.size());
        assertEquals(0, sub2Fetched.codes.size());
        
        
        new SubstanceBuilder()
        .addName("primary 2")
        .asChemical()
        .setStructureWithDefaultReference("CCCCCN")
        .setUUID(uuid3)
        .buildJsonAnd(this::assertCreatedAPI);
        
        Substance sub3Fetched = substanceEntityService.get(uuid3).get();
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
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        String foo_bar = "foo->bar";
        String bar_foo = "bar->foo";
        
        new SubstanceBuilder()
                .addName("sub1")
                .setUUID(uuid1)
                .buildJsonAnd(this::assertCreatedAPI);
        new SubstanceBuilder()
            .addName("sub2")
            .setUUID(uuid2)
            .buildJsonAnd(this::assertCreatedAPI);
        new SubstanceBuilder()
        .addName("sub3")
        .setUUID(uuid3)
        .buildJsonAnd(this::assertCreatedAPI);
       
       

        Substance sub1Fetched = substanceEntityService.get(uuid1).get();
        assertEquals("1", sub1Fetched.version);
        
        Substance sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("1", sub2Fetched.version);
        
        Substance sub3Fetched = substanceEntityService.get(uuid3).get();
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
        
        sub2Fetched = substanceEntityService.get(uuid2).get();
        assertEquals("3", sub2Fetched.version);
        assertEquals(0, sub2Fetched.relationships.size());
        
        sub3Fetched = substanceEntityService.get(uuid3).get();
        assertEquals("2", sub3Fetched.version);
        assertEquals(1, sub3Fetched.relationships.size());
    }
    /*
     * 5. Changing the substance class of a record should preserve old relationships
     *  5.1. On the record changing
     *  5.2. On the other side
     */
    
}
