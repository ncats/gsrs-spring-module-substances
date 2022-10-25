package example.exports.scrubbers;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.scrubbers.basic.BasicSubstanceScrubber;
import gsrs.module.substance.scrubbers.basic.BasicSubstanceScrubberParameters;
import ix.core.models.Group;
import ix.core.models.Keyword;
import ix.core.models.Principal;
import ix.ginas.exporters.RecordScrubber;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.MixtureSubstanceBuilder;
import ix.ginas.modelBuilders.NucleicAcidSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.*;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
public class ScrubberTestsSpecific {

    @Test
    /*
    Substance has 2 names; one 'locked' one open.
    Expect to remove anly the locked name
     */
    public void testRemoveLockedName(){

        SubstanceBuilder substanceBuilder = new SubstanceBuilder();
        Reference publicReference = new Reference();
        publicReference.publicDomain=true;
        publicReference.citation="something public";
        publicReference.docType="OTHER";
        publicReference.makePublicReleaseReference();
        Name lockedName = new Name();
        lockedName.name= "Locked";
        lockedName.languages.add(new Keyword("en"));
        lockedName.setAccess(Collections.singleton(new Group("protected")));

        lockedName.addReference(publicReference);
        substanceBuilder.addName(lockedName);
        Name openName = new Name();
        openName.name="Ouvert";
        openName.languages.add(new Keyword("fr"));
        openName.addReference(publicReference);
        substanceBuilder.addName(openName);
        substanceBuilder.addReference(publicReference);
        Substance testConcept = substanceBuilder.build();

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLocked(true);

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        Optional<Substance> cleaned = scrubber.scrub(testConcept);
        Assertions.assertEquals(1, cleaned.get().names.size());
        Assertions.assertEquals(openName.name, cleaned.get().names.get(0).name);
    }

    @Test
    /*
    Substance has 2 names; one 'locked' one open.
    setting turns off removal of locked
    Expect to remove neither name
     */
    public void testDoNotRemoveLockedName(){

        SubstanceBuilder substanceBuilder = new SubstanceBuilder();
        Reference publicReference = new Reference();
        publicReference.publicDomain=true;
        publicReference.citation="something public";
        publicReference.docType="OTHER";
        publicReference.makePublicReleaseReference();
        Name lockedName = new Name();
        lockedName.name= "Locked";
        lockedName.languages.add(new Keyword("en"));
        lockedName.setAccess(Collections.singleton(new Group("protected")));

        lockedName.addReference(publicReference);
        substanceBuilder.addName(lockedName);
        Name openName = new Name();
        openName.name="Ouvert";
        openName.languages.add(new Keyword("fr"));
        openName.addReference(publicReference);
        substanceBuilder.addName(openName);
        substanceBuilder.addReference(publicReference);
        Substance testConcept = substanceBuilder.build();

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLocked(false);
        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        Optional<Substance> cleaned = scrubber.scrub(testConcept);
        Assertions.assertEquals(2, cleaned.get().names.size());
    }

    @Test
    /*
    Substance has 2 names; one 'locked' one open.
    Expect to remove anly the locked name
     */
    public void testDoNotRemoveLockedNameWhenGroupAllowed(){

        SubstanceBuilder substanceBuilder = new SubstanceBuilder();
        Reference publicReference = new Reference();
        publicReference.publicDomain=true;
        publicReference.citation="something public";
        publicReference.docType="OTHER";
        publicReference.makePublicReleaseReference();
        Name lockedName = new Name();
        lockedName.name= "Locked";
        lockedName.languages.add(new Keyword("en"));
        lockedName.setAccess(Collections.singleton(new Group("confidential")));

        lockedName.addReference(publicReference);
        substanceBuilder.addName(lockedName);
        Name openName = new Name();
        openName.name="Ouvert";
        openName.languages.add(new Keyword("fr"));
        openName.addReference(publicReference);
        substanceBuilder.addName(openName);
        substanceBuilder.addReference(publicReference);
        Substance testConcept = substanceBuilder.build();

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setRemoveAllLockedAccessGroupsToInclude(Collections.singletonList("confidential"));

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        Optional<Substance> cleaned = scrubber.scrub(testConcept);
        Assertions.assertEquals(2, cleaned.get().names.size());

    }

    @Test
    /*
    Substance has 2 codes; one 'locked' one open.
    Expect to remove anly the locked code
     */
    public void testRemoveLockedCode(){

        SubstanceBuilder substanceBuilder = new SubstanceBuilder();
        Reference publicReference = new Reference();
        publicReference.publicDomain=true;
        publicReference.citation="something public";
        publicReference.docType="OTHER";
        publicReference.makePublicReleaseReference();

        Name openName = new Name();
        openName.name="Ouvert";
        openName.languages.add(new Keyword("fr"));
        openName.addReference(publicReference);
        substanceBuilder.addName(openName);
        substanceBuilder.addReference(publicReference);

        Code openCode = new Code();
        openCode.code="1";
        openCode.codeSystem="EINECS";
        openCode.type="PRIMARY";
        substanceBuilder.addCode(openCode);

        Code lockedCode = new Code();
        lockedCode.code="999";
        lockedCode.codeSystem="confidential application";
        lockedCode.type="PRIMARY";
        lockedCode.setAccess(Collections.singleton(new Group("confidential")));
        substanceBuilder.addCode(lockedCode);
        Substance testConcept = substanceBuilder.build();

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLocked(true);

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        Optional<Substance> cleaned = scrubber.scrub(testConcept);
        Assertions.assertEquals(1, cleaned.get().codes.size());
        Assertions.assertEquals(openCode.code, cleaned.get().codes.get(0).code);
    }

    @Test
    /*
    Substance has 2 codes; one 'locked' one open.
    setting: do not removed locked stuff
    Expect to remove no codes
     */
    public void testDoNotRemoveLockedCode(){

        SubstanceBuilder substanceBuilder = new SubstanceBuilder();
        Reference publicReference = new Reference();
        publicReference.publicDomain=true;
        publicReference.citation="something public";
        publicReference.docType="OTHER";
        publicReference.makePublicReleaseReference();

        Name openName = new Name();
        openName.name="Ouvert";
        openName.languages.add(new Keyword("fr"));
        openName.addReference(publicReference);
        substanceBuilder.addName(openName);
        substanceBuilder.addReference(publicReference);

        Code openCode = new Code();
        openCode.code="1";
        openCode.codeSystem="EINECS";
        openCode.type="PRIMARY";
        substanceBuilder.addCode(openCode);

        Code lockedCode = new Code();
        lockedCode.code="999";
        lockedCode.codeSystem="confidential application";
        lockedCode.type="PRIMARY";
        lockedCode.setAccess(Collections.singleton(new Group("confidential")));
        substanceBuilder.addCode(lockedCode);
        Substance testConcept = substanceBuilder.build();

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLocked(false);

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        Optional<Substance> cleaned = scrubber.scrub(testConcept);
        Assertions.assertEquals(2, cleaned.get().codes.size());
    }

    @Test
    /*
    Substance has 2 codes; one 'locked' one open.
    Expect to remove anly the locked code
     */
    public void testDoNotRemoveLockedCodeWhenGroupAllowed(){

        SubstanceBuilder substanceBuilder = new SubstanceBuilder();
        Reference publicReference = new Reference();
        publicReference.publicDomain=true;
        publicReference.citation="something public";
        publicReference.docType="OTHER";
        publicReference.makePublicReleaseReference();

        Name openName = new Name();
        openName.name="aperto";
        openName.languages.add(new Keyword("it"));
        openName.addReference(publicReference);
        substanceBuilder.addName(openName);
        substanceBuilder.addReference(publicReference);
        Code openCode = new Code();
        openCode.code="1";
        openCode.codeSystem="EINECS";
        openCode.type="PRIMARY";
        substanceBuilder.addCode(openCode);

        Code lockedCode = new Code();
        lockedCode.code="999";
        lockedCode.codeSystem="confidential application";
        lockedCode.type="PRIMARY";
        lockedCode.setAccess(Collections.singleton(new Group("confidential")));
        substanceBuilder.addCode(lockedCode);
        Substance testConcept = substanceBuilder.build();

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setRemoveAllLockedAccessGroupsToInclude(Collections.singletonList("confidential"));

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        Optional<Substance> cleaned = scrubber.scrub(testConcept);
        Assertions.assertEquals(2, cleaned.get().codes.size());
    }

    @Test
    public void TestStructureHandling(){
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.setStructureWithDefaultReference("N(CC)(CC)CC");
        Name openName = new Name();
        openName.name="triethyl amine";
        openName.languages.add(new Keyword("e"));
        Reference publicReference = new Reference();
        publicReference.publicDomain=true;
        publicReference.citation="something public";
        publicReference.docType="OTHER";
        publicReference.makePublicReleaseReference();
        openName.addReference(publicReference);
        builder.addName(openName);
        ChemicalSubstance chemicalSubstance = builder.build();
        chemicalSubstance.getStructure().setAccess(Collections.singleton( new Group("confidential")));
        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setScrubbedDefinitionHandlingSetScrubbedDefinitionalElementsIncomplete(true);
        scrubberSettings.setScrubbedDefinitionHandling(true);
        scrubberSettings.setScrubbedDefinitionHandlingRemoveScrubbedDefinitionalElementsEntirely(true);

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        ChemicalSubstance scrubbedChem = (ChemicalSubstance) scrubber.scrub(chemicalSubstance).get();
        Assertions.assertNull( scrubbedChem.getStructure());
        Assertions.assertEquals(0, scrubbedChem.getMoieties().size());
    }

    /*
    Expected approved substance not to be scrubbed away
     */
    @Test
    public void testApproved() {
        String approvalId="ABC";
        NucleicAcidSubstance approvedNA= createApprovedNA(approvalId);
        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveBasedOnStatus(true);
        scrubberSettings.setStatusesToInclude(Collections.singletonList("approved"));
        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        NucleicAcidSubstance scrubbedSubstance = (NucleicAcidSubstance) scrubber.scrub(approvedNA).get();
        Assertions.assertNotNull(scrubbedSubstance);
    }

    @Test
    public void testApprovedWithSubconcept() {
        String approvalId="ABC";
        NucleicAcidSubstance approvedNA= createApprovedNA(approvalId);
        Name mainName = new Name();
        mainName.name="Nucleic Acid 1";
        mainName.languages.add(new Keyword("en"));
        Reference publicReference = new Reference();
        publicReference.publicDomain=true;
        publicReference.citation="something public";
        publicReference.docType="OTHER";
        publicReference.makePublicReleaseReference();
        mainName.addReference(publicReference);
        approvedNA.names.add(mainName);
        approvedNA.addReference(publicReference);

        SubstanceBuilder builder = new SubstanceBuilder();
        builder.addName("Subconcept 1");
        builder.addReference(publicReference);
        builder.setStatus("pending");
        Substance subconcept = builder.build();

        Relationship relationship = new Relationship();
        relationship.type = "SUB_CONCEPT->SUBSTANCE";
        relationship.relatedSubstance = new SubstanceReference();
        relationship.relatedSubstance.wrappedSubstance= subconcept;
        approvedNA.addRelationship(relationship);

        Relationship inverseRelationship = new Relationship();
        inverseRelationship.type = "SUBSTANCE->SUB_CONCEPT";
        inverseRelationship.relatedSubstance = new SubstanceReference();
        inverseRelationship.relatedSubstance.wrappedSubstance= approvedNA;

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveBasedOnStatus(true);
        scrubberSettings.setStatusesToInclude(Collections.singletonList("approved"));
        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        NucleicAcidSubstance scrubbedSubstance = (NucleicAcidSubstance) scrubber.scrub(approvedNA).get();
        Assertions.assertNotNull(scrubbedSubstance);

        Optional<Substance> scrubbedSubconcept = scrubber.scrub(subconcept);
        Assertions.assertTrue(scrubbedSubconcept.isEmpty());
    }

    @Test
    public void testApprovedWithSubconcept2() {
        String approvalId="ABC";
        NucleicAcidSubstance approvedNA= createApprovedNA(approvalId);
        Name mainName = new Name();
        mainName.name="Nucleic Acid 1";
        mainName.languages.add(new Keyword("en"));
        Reference publicReference = new Reference();
        publicReference.publicDomain=true;
        publicReference.citation="something public";
        publicReference.docType="OTHER";
        publicReference.makePublicReleaseReference();
        mainName.addReference(publicReference);
        approvedNA.names.add(mainName);
        approvedNA.addReference(publicReference);

        SubstanceBuilder builder = new SubstanceBuilder();
        builder.addName("Subconcept 1");
        builder.addReference(publicReference);
        builder.setStatus("pending");
        Substance subconcept = builder.build();

        Relationship relationship = new Relationship();
        relationship.type = "SUB_CONCEPT->SUBSTANCE";
        relationship.relatedSubstance = new SubstanceReference();
        relationship.relatedSubstance.wrappedSubstance= subconcept;
        approvedNA.addRelationship(relationship);

        Relationship inverseRelationship = new Relationship();
        inverseRelationship.type = "SUBSTANCE->SUB_CONCEPT";
        inverseRelationship.relatedSubstance = new SubstanceReference();
        inverseRelationship.relatedSubstance.wrappedSubstance= approvedNA;

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveBasedOnStatus(true);
        scrubberSettings.setStatusesToInclude(Arrays.asList ("approved", "pending"));
        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        NucleicAcidSubstance scrubbedSubstance = (NucleicAcidSubstance) scrubber.scrub(approvedNA).get();
        Assertions.assertNotNull(scrubbedSubstance);

        Optional<Substance> scrubbedSubconcept = scrubber.scrub(subconcept);
        Assertions.assertNotNull(scrubbedSubconcept.get());
    }

    @Test
    public void testSet1(){
        /*
        5 substances with different statuses.
        Scrub based on a set of allowed status values
        Verify that the expected number of substances is returned
         */
        String approvalId="ABC";
        NucleicAcidSubstance approvedNA= createApprovedNA(approvalId);
        Name mainName = new Name();
        mainName.name="Nucleic Acid 1";
        mainName.languages.add(new Keyword("en"));
        Reference publicReference = new Reference();
        publicReference.publicDomain=true;
        publicReference.citation="something public";
        publicReference.docType="OTHER";
        publicReference.makePublicReleaseReference();
        mainName.addReference(publicReference);
        approvedNA.names.add(mainName);
        approvedNA.addReference(publicReference);

        SubstanceBuilder builder = new SubstanceBuilder();
        builder.addName("Subconcept 1");
        builder.addReference(publicReference);
        builder.setStatus("subconcept");
        Substance subconcept = builder.build();

        Relationship relationship = new Relationship();
        relationship.type = "SUB_CONCEPT->SUBSTANCE";
        relationship.relatedSubstance = new SubstanceReference();
        relationship.relatedSubstance.wrappedSubstance= subconcept;
        approvedNA.addRelationship(relationship);

        //when a substance with a relationship is persisted, the inverse relationship
        // is created automatically.  That's not the case in this unit test
        Relationship inverseRelationship = new Relationship();
        inverseRelationship.type = "SUBSTANCE->SUB_CONCEPT";
        inverseRelationship.relatedSubstance = new SubstanceReference();
        inverseRelationship.relatedSubstance.wrappedSubstance= approvedNA;

        String approvalId2="Ignore me";
        NucleicAcidSubstance pendingNA= createApprovedNA(approvalId);
        pendingNA.approvalID=null;
        pendingNA.status="pending";
        SubstanceBuilder builder2 = new SubstanceBuilder();
        builder2.addName("Subconcept 2");
        builder2.addReference(publicReference);
        builder2.setStatus("pending subconcept");
        Substance subconcept2 = builder2.build();

        Relationship relationship2 = new Relationship();
        relationship2.type = "SUB_CONCEPT->SUBSTANCE";
        relationship2.relatedSubstance = new SubstanceReference();
        relationship2.relatedSubstance.wrappedSubstance= subconcept2;
        pendingNA.addRelationship(relationship2);

        //when a substance with a relationship is persisted, the inverse relationship
        // is created automatically.  That's not the case in this unit test
        Relationship inverseRelationship2 = new Relationship();
        inverseRelationship2.type = "SUBSTANCE->SUB_CONCEPT";
        inverseRelationship2.relatedSubstance = new SubstanceReference();
        inverseRelationship2.relatedSubstance.wrappedSubstance= pendingNA;

        SubstanceBuilder builder3 = new SubstanceBuilder();
        builder3.addName("Free-floating concept");
        builder3.addReference(publicReference);
        builder3.setStatus("concept");
        Substance simpleConcept = builder3.build();
        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveBasedOnStatus(true);
        scrubberSettings.setStatusesToInclude(Arrays.asList ("approved", "pending"));
        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);

        long total =Arrays.asList(approvedNA, subconcept, pendingNA, subconcept2, simpleConcept).stream()
                .map(s->scrubber.scrub(s))
                .filter(o->o.isPresent())
                .count();
        Assertions.assertEquals(2, total);
    }

    @Test
    public void testSet2(){
        /*
        5 substances with different statuses.
        Scrub based on a set of allowed status values
        Verify that the expected number of substances is returned
         */
        String approvalId="ABC";
        NucleicAcidSubstance approvedNA= createApprovedNA(approvalId);
        Name mainName = new Name();
        mainName.name="Nucleic Acid 1";
        mainName.languages.add(new Keyword("en"));
        Reference publicReference = new Reference();
        publicReference.publicDomain=true;
        publicReference.citation="something public";
        publicReference.docType="OTHER";
        publicReference.makePublicReleaseReference();
        mainName.addReference(publicReference);
        approvedNA.names.add(mainName);
        approvedNA.addReference(publicReference);

        SubstanceBuilder builder = new SubstanceBuilder();
        builder.addName("Subconcept 1");
        builder.addReference(publicReference);
        builder.setStatus("subconcept");
        Substance subconcept = builder.build();

        Relationship relationship = new Relationship();
        relationship.type = "SUB_CONCEPT->SUBSTANCE";
        relationship.relatedSubstance = new SubstanceReference();
        relationship.relatedSubstance.wrappedSubstance= subconcept;
        approvedNA.addRelationship(relationship);

        //when a substance with a relationship is persisted, the inverse relationship
        // is created automatically.  That's not the case in this unit test
        Relationship inverseRelationship = new Relationship();
        inverseRelationship.type = "SUBSTANCE->SUB_CONCEPT";
        inverseRelationship.relatedSubstance = new SubstanceReference();
        inverseRelationship.relatedSubstance.wrappedSubstance= approvedNA;

        String approvalId2="Ignore me";
        NucleicAcidSubstance pendingNA= createApprovedNA(approvalId);
        pendingNA.approvalID=null;
        pendingNA.status="pending";
        SubstanceBuilder builder2 = new SubstanceBuilder();
        builder2.addName("Subconcept 2");
        builder2.addReference(publicReference);
        builder2.setStatus("pending subconcept");
        Substance subconcept2 = builder2.build();

        Relationship relationship2 = new Relationship();
        relationship2.type = "SUB_CONCEPT->SUBSTANCE";
        relationship2.relatedSubstance = new SubstanceReference();
        relationship2.relatedSubstance.wrappedSubstance= subconcept2;
        pendingNA.addRelationship(relationship2);

        //when a substance with a relationship is persisted, the inverse relationship
        // is created automatically.  That's not the case in this unit test
        Relationship inverseRelationship2 = new Relationship();
        inverseRelationship2.type = "SUBSTANCE->SUB_CONCEPT";
        inverseRelationship2.relatedSubstance = new SubstanceReference();
        inverseRelationship2.relatedSubstance.wrappedSubstance= pendingNA;

        SubstanceBuilder builder3 = new SubstanceBuilder();
        builder3.addName("Free-floating concept");
        builder3.addReference(publicReference);
        builder3.setStatus("concept");
        Substance simpleConcept = builder3.build();
        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveBasedOnStatus(true);
        scrubberSettings.setStatusesToInclude(Arrays.asList ("approved", "pending", "concept"));
        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);

        long total =Arrays.asList(approvedNA, subconcept, pendingNA, subconcept2, simpleConcept).stream()
                .map(s->scrubber.scrub(s))
                .filter(o->o.isPresent())
                .count();
        Assertions.assertEquals(3, total);
    }

    @Test
    public void testSet3(){
        /*
        5 substances with different statuses.
        Scrub based on a set of allowed status values
        Verify that the expected number of substances is returned
         */
        String approvalId="ABC";
        NucleicAcidSubstance approvedNA= createApprovedNA(approvalId);
        Name mainName = new Name();
        mainName.name="Nucleic Acid 1";
        mainName.languages.add(new Keyword("en"));
        Reference publicReference = new Reference();
        publicReference.publicDomain=true;
        publicReference.citation="something public";
        publicReference.docType="OTHER";
        publicReference.makePublicReleaseReference();
        mainName.addReference(publicReference);
        approvedNA.names.add(mainName);
        approvedNA.addReference(publicReference);

        SubstanceBuilder builder = new SubstanceBuilder();
        builder.addName("Subconcept 1");
        builder.addReference(publicReference);
        builder.setStatus("subconcept");
        Substance subconcept = builder.build();

        Relationship relationship = new Relationship();
        relationship.type = "SUB_CONCEPT->SUBSTANCE";
        relationship.relatedSubstance = new SubstanceReference();
        relationship.relatedSubstance.wrappedSubstance= subconcept;
        approvedNA.addRelationship(relationship);

        //when a substance with a relationship is persisted, the inverse relationship
        // is created automatically.  That's not the case in this unit test
        Relationship inverseRelationship = new Relationship();
        inverseRelationship.type = "SUBSTANCE->SUB_CONCEPT";
        inverseRelationship.relatedSubstance = new SubstanceReference();
        inverseRelationship.relatedSubstance.wrappedSubstance= approvedNA;

        String approvalId2="Ignore me";
        NucleicAcidSubstance pendingNA= createApprovedNA(approvalId);
        pendingNA.approvalID=null;
        pendingNA.status="pending";
        SubstanceBuilder builder2 = new SubstanceBuilder();
        builder2.addName("Subconcept 2");
        builder2.addReference(publicReference);
        builder2.setStatus("pending subconcept");
        Substance subconcept2 = builder2.build();

        Relationship relationship2 = new Relationship();
        relationship2.type = "SUB_CONCEPT->SUBSTANCE";
        relationship2.relatedSubstance = new SubstanceReference();
        relationship2.relatedSubstance.wrappedSubstance= subconcept2;
        pendingNA.addRelationship(relationship2);

        //when a substance with a relationship is persisted, the inverse relationship
        // is created automatically.  That's not the case in this unit test
        Relationship inverseRelationship2 = new Relationship();
        inverseRelationship2.type = "SUBSTANCE->SUB_CONCEPT";
        inverseRelationship2.relatedSubstance = new SubstanceReference();
        inverseRelationship2.relatedSubstance.wrappedSubstance= pendingNA;

        SubstanceBuilder builder3 = new SubstanceBuilder();
        builder3.addName("Free-floating concept");
        builder3.addReference(publicReference);
        builder3.setStatus("concept");
        Substance simpleConcept = builder3.build();
        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveBasedOnStatus(true);
        scrubberSettings.setStatusesToInclude(Arrays.asList ("approved", "pending", "concept", "pending subconcept"));
        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);

        long total =Arrays.asList(approvedNA, subconcept, pendingNA, subconcept2, simpleConcept).stream()
                .map(s->scrubber.scrub(s))
                .filter(o->o.isPresent())
                .count();
        Assertions.assertEquals(4, total);
    }

    @Test
    public void testSet4(){
        /*
        5 substances with different statuses.
        Scrub based on a set of allowed status values
        Verify that the expected number of substances is returned
         */
        String approvalId="ABC";
        NucleicAcidSubstance approvedNA= createApprovedNA(approvalId);
        Name mainName = new Name();
        mainName.name="Nucleic Acid 1";
        mainName.languages.add(new Keyword("en"));
        Reference publicReference = new Reference();
        publicReference.publicDomain=true;
        publicReference.citation="something public";
        publicReference.docType="OTHER";
        publicReference.makePublicReleaseReference();
        mainName.addReference(publicReference);
        approvedNA.names.add(mainName);
        approvedNA.addReference(publicReference);

        SubstanceBuilder builder = new SubstanceBuilder();
        builder.addName("Subconcept 1");
        builder.addReference(publicReference);
        builder.setStatus("subconcept");
        Substance subconcept = builder.build();

        Relationship relationship = new Relationship();
        relationship.type = "SUB_CONCEPT->SUBSTANCE";
        relationship.relatedSubstance = new SubstanceReference();
        relationship.relatedSubstance.wrappedSubstance= subconcept;
        approvedNA.addRelationship(relationship);

        //when a substance with a relationship is persisted, the inverse relationship
        // is created automatically.  That's not the case in this unit test
        Relationship inverseRelationship = new Relationship();
        inverseRelationship.type = "SUBSTANCE->SUB_CONCEPT";
        inverseRelationship.relatedSubstance = new SubstanceReference();
        inverseRelationship.relatedSubstance.wrappedSubstance= approvedNA;

        String approvalId2="Ignore me";
        NucleicAcidSubstance pendingNA= createApprovedNA(approvalId);
        pendingNA.approvalID=null;
        pendingNA.status="pending";
        SubstanceBuilder builder2 = new SubstanceBuilder();
        builder2.addName("Subconcept 2");
        builder2.addReference(publicReference);
        builder2.setStatus("pending subconcept");
        Substance subconcept2 = builder2.build();

        Relationship relationship2 = new Relationship();
        relationship2.type = "SUB_CONCEPT->SUBSTANCE";
        relationship2.relatedSubstance = new SubstanceReference();
        relationship2.relatedSubstance.wrappedSubstance= subconcept2;
        pendingNA.addRelationship(relationship2);

        //when a substance with a relationship is persisted, the inverse relationship
        // is created automatically.  That's not the case in this unit test
        Relationship inverseRelationship2 = new Relationship();
        inverseRelationship2.type = "SUBSTANCE->SUB_CONCEPT";
        inverseRelationship2.relatedSubstance = new SubstanceReference();
        inverseRelationship2.relatedSubstance.wrappedSubstance= pendingNA;

        SubstanceBuilder builder3 = new SubstanceBuilder();
        builder3.addName("Free-floating concept");
        builder3.addReference(publicReference);
        builder3.setStatus("concept");
        Substance simpleConcept = builder3.build();
        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveBasedOnStatus(true);
        scrubberSettings.setStatusesToInclude(Arrays.asList ("approved", "pending", "concept", "subconcept", "pending subconcept"));
        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);

        long total =Arrays.asList(approvedNA, subconcept, pendingNA, subconcept2, simpleConcept).stream()
                .map(s->scrubber.scrub(s))
                .filter(o->o.isPresent())
                .count();
        Assertions.assertEquals(5, total);
    }

    /*
    Expected approved substance not to be scrubbed away
    */
    @Test
    public void testPending() {
        String approvalId="ABC";
        NucleicAcidSubstance pendingNA= createApprovedNA(approvalId);
        pendingNA.approvalID=null;
        pendingNA.status="pending";

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveBasedOnStatus(true);
        scrubberSettings.setStatusesToInclude(Collections.singletonList("approved"));
        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        Optional<Substance> scrubbedSubstance = scrubber.scrub(pendingNA);
        Assertions.assertTrue(scrubbedSubstance.isEmpty());

    }

    @Test
    public void testMixtureProcessing(){
        ChemicalSubstanceBuilder chemicalSubstanceBuilder1 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder1.setStructureWithDefaultReference("CCCNCC")
            .addName("ethylpropylamine")
                .generateNewUUID()
            .setStatus("approved");
        ChemicalSubstance component1= chemicalSubstanceBuilder1.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder2 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder2.setStructureWithDefaultReference("CCCNCCC")
                .addName("dipropylamine")
                .setStatus("approved")
                .generateNewUUID()
                .setAccess(Collections.singleton(new Group("protected")));
        ChemicalSubstance component2= chemicalSubstanceBuilder2.build();

        MixtureSubstanceBuilder mixtureSubstanceBuilder = new MixtureSubstanceBuilder();
        MixtureSubstance mixture1= mixtureSubstanceBuilder
                .addName("Amines")
                .generateNewUUID()
                .setStatus("approved")
                .addComponents("MUST_BE_PRESENT", component1)
                .build();
        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveBasedOnStatus(true);
        scrubberSettings.setSubstanceReferenceCleanupActionForDefinitionalDependentScrubbedSubstanceReferences("REMOVE_SUBSTANCE_REFERENCE_AND_PARENT_IF_NECESSARY");
        RecordScrubber<Substance> scrubber = new BasicSubstanceScrubber(scrubberSettings);
        MixtureSubstance scrubbedMixture = (MixtureSubstance) scrubber.scrub(mixture1).get();
        Assertions.assertEquals(1, scrubbedMixture.mixture.components.size());
    }

    @Test
    public void testMixtureProcessing2(){
        ChemicalSubstanceBuilder chemicalSubstanceBuilder1 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder1.setStructureWithDefaultReference("CCCNCC")
                .addName("ethylpropylamine")
                .generateNewUUID()
                .setStatus("approved");
        ChemicalSubstance component1= chemicalSubstanceBuilder1.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder2 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder2.setStructureWithDefaultReference("CCCNCCC")
                .addName("dipropylamine")
                .setStatus("approved")
                .generateNewUUID()
                .setAccess(Collections.singleton(new Group("protected")));
        ChemicalSubstance component2= chemicalSubstanceBuilder2.build();

        MixtureSubstanceBuilder mixtureSubstanceBuilder = new MixtureSubstanceBuilder();
        MixtureSubstance mixture1= mixtureSubstanceBuilder
                .addName("Amines")
                .generateNewUUID()
                .setStatus("approved")
                .addComponents("MUST_BE_PRESENT", component1)
                .addComponents("MUST_BE_PRESENT", component2)
                .build();
        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveBasedOnStatus(true);
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setSubstanceReferenceCleanupActionForDefinitionalDependentScrubbedSubstanceReferences("DEIDENTIFY_SUBSTANCE_REFERENCE");
        RecordScrubber<Substance> scrubber = new BasicSubstanceScrubber(scrubberSettings);
        MixtureSubstance scrubbedMixture = (MixtureSubstance) scrubber.scrub(mixture1).get();
        Assertions.assertEquals(2, scrubbedMixture.mixture.components.size());
        String deidentifiedSubstanceName ="UNSPECIFIED_SUBSTANCE";
        Assertions.assertTrue(scrubbedMixture.mixture.components.stream().anyMatch(c->c.substance.refPname.equals(deidentifiedSubstanceName)));
    }

    @Test
    public void testMixtureProcessing3(){
        ChemicalSubstanceBuilder chemicalSubstanceBuilder1 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder1.setStructureWithDefaultReference("CCCNCC")
                .addName("ethylpropylamine")
                .generateNewUUID()
                .setStatus("approved");
        ChemicalSubstance component1= chemicalSubstanceBuilder1.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder2 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder2.setStructureWithDefaultReference("CCCNCCC")
                .addName("dipropylamine")
                .setStatus("approved")
                .generateNewUUID()
                .setAccess(Collections.singleton(new Group("protected")));
        ChemicalSubstance component2= chemicalSubstanceBuilder2.build();

        MixtureSubstanceBuilder mixtureSubstanceBuilder = new MixtureSubstanceBuilder();
        MixtureSubstance mixture1= mixtureSubstanceBuilder
                .addName("Amines")
                .generateNewUUID()
                .setStatus("approved")
                .addComponents("MUST_BE_PRESENT", component1)
                .build();
        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveBasedOnStatus(true);
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setSubstanceReferenceCleanupActionForDefinitionalDependentScrubbedSubstanceReferences("REMOVE_PARENT_SUBSTANCE_ENTIRELY");
        RecordScrubber<Substance> scrubber = new BasicSubstanceScrubber(scrubberSettings);
        Optional<Substance> scrubbedMixture = scrubber.scrub(mixture1);
        Assertions.assertTrue(scrubbedMixture.isEmpty());
    }

    @Test
    public void testMixtureProcessing4(){
        ChemicalSubstanceBuilder chemicalSubstanceBuilder1 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder1.setStructureWithDefaultReference("CCCNCC")
                .addName("ethylpropylamine")
                .generateNewUUID()
                .setStatus("approved");
        ChemicalSubstance component1= chemicalSubstanceBuilder1.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder2 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder2.setStructureWithDefaultReference("CCCNCCC")
                .addName("dipropylamine")
                .setStatus("approved")
                .generateNewUUID()
                .setAccess(Collections.singleton(new Group("protected")));
        ChemicalSubstance component2= chemicalSubstanceBuilder2.build();

        MixtureSubstanceBuilder mixtureSubstanceBuilder = new MixtureSubstanceBuilder();
        MixtureSubstance mixture1= mixtureSubstanceBuilder
                .addName("Amines")
                .generateNewUUID()
                .setStatus("approved")
                .addComponents("MUST_BE_PRESENT", component1)
                .build();
        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveBasedOnStatus(true);
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setSubstanceReferenceCleanupActionForDefinitionalDependentScrubbedSubstanceReferences("REMOVE_ONLY_SUBSTANCE_REFERENCE");
        RecordScrubber<Substance> scrubber = new BasicSubstanceScrubber(scrubberSettings);
        MixtureSubstance scrubbedMixture = (MixtureSubstance) scrubber.scrub(mixture1).get();
        Assertions.assertEquals(2, scrubbedMixture.mixture.components.size());
        Assertions.assertEquals(1, scrubbedMixture.mixture.components.stream().filter(c->c.substance==null).count());
    }

    private NucleicAcidSubstance createApprovedNA(String newApprovalId) {
        NucleicAcidSubstanceBuilder builder = new NucleicAcidSubstanceBuilder();
        builder.setStatus("approved");
        builder.addDnaSubunit("AAATTTCCC");
        Principal approver = new Principal("mitch@contoso.com");
        builder.setApproval(approver, new Date(), newApprovalId);
        return builder.build();
    }
}
