package example.exports.scrubbers;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.scrubbers.basic.BasicSubstanceScrubber;
import gsrs.module.substance.scrubbers.basic.BasicSubstanceScrubberParameters;
import ix.core.models.Group;
import ix.core.models.Keyword;
import ix.core.models.Principal;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.NucleicAcidSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.BufferedReader;
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

    private NucleicAcidSubstance createApprovedNA(String newApprovalId) {
        NucleicAcidSubstanceBuilder builder = new NucleicAcidSubstanceBuilder();
        builder.setStatus("approved");
        builder.addDnaSubunit("AAATTTCCC");
        Principal approver = new Principal("mitch@contoso.com");
        builder.setApproval(approver, new Date(), newApprovalId);
        return builder.build();
    }
}
