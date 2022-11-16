package example.exports.scrubbers;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;
import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.scrubbers.basic.BasicSubstanceScrubber;
import gsrs.module.substance.scrubbers.basic.BasicSubstanceScrubberParameters;
import gsrs.substances.tests.SubstanceTestUtil;
import ix.core.models.Group;
import ix.core.models.Keyword;
import ix.core.models.Principal;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.models.v1.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.*;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
@Slf4j
public class BasicSubstanceScrubberTests {

    @Test
    public void testScrubSubstanceSpecificCodeRemoved() {
        //verify that expected code is removed
        String currentSmiles="C1CCCCCCCC1";
        ChemicalSubstance chemical = SubstanceTestUtil.makeChemicalSubstance(currentSmiles);
        chemical.names.clear();//remove bogus name added by makeChemicalSubstance

        Name name1 = new Name();
        name1.name = "SECRET NAME 1";
        Group groupWho = new Group("WHO");
        Group groupEma = new Group("EMA");
        Set<Group> groups = new HashSet<>();
        groups.add(groupEma);
        groups.add(groupWho);
        name1.setAccess(groups);
        chemical.names.add(name1);

        Name name2 = new Name();
        name2.name = "SECRET NAME 2";
        Set<Group> groups2 = new HashSet<>();
        groups2.add(groupEma);
        name2.setAccess(groups2);
        chemical.names.add(name2);

        Name name3 = new Name();
        name3.name = "SECRET NAME 3";
        Set<Group> groups3 = new HashSet<>();
        groups2.add(groupWho);
        name3.setAccess(groups3);
        chemical.names.add(name3);

        Group groupProtected = new Group("protected");
        Name name4 = new Name();
        name4.name = "SECRET NAME 4";
        Set<Group> groups4 = new HashSet<>();
        groups4.add(groupProtected);
        name4.setAccess(groups4);
        chemical.names.add(name4);

        Name namePublic = new Name();
        namePublic.name = "public name";
        chemical.names.add(namePublic);
        chemical.created = new Date();

        Code bogusBdnum = new Code();
        bogusBdnum.type="PRIMARY";
        bogusBdnum.codeSystem="BDNUM";
        bogusBdnum.code="AA1234567890";
        chemical.codes.add(bogusBdnum);

        Code cas= new Code();
        cas.codeSystem="CAS";
        cas.type="PRIMARY";
        cas.code="293-55-0";
        chemical.codes.add(cas);

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLockedAccessGroupsToInclude( Collections.singletonList("WHO"));
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setRemoveNotes(true);
        scrubberSettings.setRemoveChangeReason(false);
        scrubberSettings.setRemoveDates(false);
        scrubberSettings.setApprovalIdCleanup(false);
        scrubberSettings.setRemoveCodesBySystem(true);
        scrubberSettings.setRemoveCodesBySystemCodeSystemsToRemove(Collections.singletonList("BDNUM"));
        scrubberSettings.setRemoveAllLockedRemoveElementsIfNoExportablePublicRef(false);
        scrubberSettings.setAuditInformationCleanup(false);
        scrubberSettings.setSubstanceReferenceCleanup(false);
        scrubberSettings.setApprovalIdCleanup(false);
        scrubberSettings.setRegenerateUUIDs(false);
        scrubberSettings.setChangeAllStatuses(false);

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        chemical.addNote("This is a note");
        String reasonToChange = "change is inevitable";
        chemical.changeReason = reasonToChange;
        Optional<Substance> cleaned = scrubber.scrub(chemical);
        ChemicalSubstance cleanedChemical = (ChemicalSubstance) cleaned.get();
        Name cleanedName1 = cleanedChemical.names.stream().filter(n -> n.name.equals(name1.name)).findFirst().get();
        Assertions.assertEquals(1, cleanedName1.getAccess().size());
        Assertions.assertEquals("WHO", ((Group) cleanedName1.getAccess().toArray()[0]).name);

        Assertions.assertEquals(0, cleanedChemical.notes.size());
        Assertions.assertEquals(reasonToChange, cleanedChemical.changeReason);
        Assertions.assertNotNull(cleanedChemical.created);
        Assertions.assertTrue(cleanedChemical.codes.stream().noneMatch(c->c.codeSystem.equals("BDNUM")));
        Assertions.assertTrue(cleanedChemical.codes.stream().anyMatch(c->c.codeSystem.equals("CAS")));
    }

    @Test
    public void testScrubSubstanceStdNamePreserved() {
        log.trace("in testScrubSubstanceStdNamePreserved");
        //verify that an stdName is preserved
        String currentSmiles="C1CCCCCCCC1";
        ChemicalSubstance chemical = SubstanceTestUtil.makeChemicalSubstance(currentSmiles);
        chemical.names.clear();//remove bogus name added by makeChemicalSubstance

        String publicStdName= "PUBLIC NAME";
        Name namePublic = new Name();
        namePublic.name = "public name";
        namePublic.stdName= publicStdName;
        namePublic.setAccess(new HashSet<Group>());
        chemical.names.add(namePublic);
        chemical.created = new Date();

        Code bogusBdnum = new Code();
        bogusBdnum.type="PRIMARY";
        bogusBdnum.codeSystem="BDNUM";
        bogusBdnum.code="AA1234567890";
        chemical.codes.add(bogusBdnum);

        Code cas= new Code();
        cas.codeSystem="CAS";
        cas.type="PRIMARY";
        cas.code="293-55-0";
        chemical.codes.add(cas);

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setRemoveChangeReason(false);
        scrubberSettings.setRemoveDates(false);
        scrubberSettings.setApprovalIdCleanup(false);
        scrubberSettings.setRemoveAllLockedRemoveElementsIfNoExportablePublicRef(false);
        scrubberSettings.setAuditInformationCleanup(false);
        scrubberSettings.setSubstanceReferenceCleanup(false);
        scrubberSettings.setApprovalIdCleanup(false);
        scrubberSettings.setRegenerateUUIDs(false);
        scrubberSettings.setChangeAllStatuses(false);

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        chemical.addNote("This is a note");
        String reasonToChange = "change is inevitable";
        chemical.changeReason = reasonToChange;
        Optional<Substance> cleaned = scrubber.scrub(chemical);
        ChemicalSubstance cleanedChemical = (ChemicalSubstance) cleaned.get();
        Assertions.assertEquals(publicStdName, cleanedChemical.names.get(0).stdName);
        log.trace("completed");
    }

    @Test
    public void testScrubSubstance2() {
        String currentSmiles="c1ccccc1NCC";
        ChemicalSubstance chemical = SubstanceTestUtil.makeChemicalSubstance(currentSmiles);
        chemical.names.clear();//remove any bogus names

        Name name1 = new Name();
        name1.name = "SECRET NAME 1";
        Group groupWho = new Group("WHO");
        Group groupEma = new Group("EMA");
        Set<Group> groups = new HashSet<>();
        groups.add(groupEma);
        groups.add(groupWho);
        name1.setAccess(groups);
        chemical.names.add(name1);

        Name name2 = new Name();
        name2.name = "SECRET NAME 2";
        Set<Group> groups2 = new HashSet<>();
        groups2.add(groupEma);
        name2.setAccess(groups2);
        chemical.names.add(name2);

        Name name3 = new Name();
        name3.name = "SECRET NAME 3";
        Set<Group> groups3 = new HashSet<>();
        groups2.add(groupWho);
        name3.setAccess(groups3);
        chemical.names.add(name3);

        Group groupProtected = new Group("protected");
        Name name4 = new Name();
        name4.name = "SECRET NAME 4";
        Set<Group> groups4 = new HashSet<>();
        groups4.add(groupProtected);
        name4.setAccess(groups4);
        chemical.names.add(name4);

        Name namePublic = new Name();
        namePublic.name = "phenyl ethyl amine";
        chemical.names.add(namePublic);
        chemical.created = new Date();

        Code bogusBdnum = new Code();
        bogusBdnum.type="PRIMARY";
        bogusBdnum.codeSystem="BDNUM";
        bogusBdnum.code="AA1234567890";
        chemical.codes.add(bogusBdnum);

        Code cas= new Code();
        cas.codeSystem="CAS";
        cas.type="PRIMARY";
        cas.code="293-55-0";
        chemical.codes.add(cas);

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLockedAccessGroupsToInclude(Collections.singletonList("WHO"));
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setRemoveNotes(false);
        scrubberSettings.setRemoveChangeReason(false);
        scrubberSettings.setRemoveDates(false);
        scrubberSettings.setApprovalIdCleanup(false);
        scrubberSettings.setRemoveCodesBySystem(true);
        scrubberSettings.setRemoveCodesBySystemCodeSystemsToRemove(Collections.singletonList("BDNUM"));

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        chemical.addNote("This is a note");
        String reasonToChange = "change is inevitable";
        chemical.changeReason = reasonToChange;
        Optional<Substance> cleaned = scrubber.scrub(chemical);
        ChemicalSubstance cleanedChemical = (ChemicalSubstance) cleaned.get();
        Name cleanedName1 = cleanedChemical.names.stream().filter(n -> n.name.equals(name1.name)).findFirst().get();
        Assertions.assertEquals(1, cleanedName1.getAccess().size());
        Assertions.assertEquals("WHO", ((Group) cleanedName1.getAccess().toArray()[0]).name);

        Assertions.assertEquals(1, cleanedChemical.notes.size());
        Assertions.assertEquals(reasonToChange, cleanedChemical.changeReason);
        Assertions.assertNotNull(cleanedChemical.created);
        Assertions.assertTrue(cleanedChemical.codes.stream().noneMatch(c->c.codeSystem.equals("BDNUM")));
        Assertions.assertTrue(cleanedChemical.codes.stream().anyMatch(c->c.codeSystem.equals("CAS")));
    }

    @Test
    public void testScrubSubstanceDelElementNoNoPublicRef() {
        String currentSmiles="C1CCCCCCCC1";
        ChemicalSubstance chemical = SubstanceTestUtil.makeChemicalSubstance(currentSmiles);
        chemical.names.clear();//remove any bogus names

        Name name1 = new Name();
        name1.name = "SECRET NAME 1";
        Group groupWho = new Group("WHO");
        Group groupEma = new Group("EMA");
        Set<Group> groups = new HashSet<>();
        groups.add(groupEma);
        groups.add(groupWho);
        name1.setAccess(groups);
        chemical.names.add(name1);

        Name name2 = new Name();
        name2.name = "SECRET NAME 2";
        Set<Group> groups2 = new HashSet<>();
        groups2.add(groupEma);
        name2.setAccess(groups2);
        chemical.names.add(name2);

        Name name3 = new Name();
        name3.name = "SECRET NAME 3";
        Set<Group> groups3 = new HashSet<>();
        groups2.add(groupWho);
        name3.setAccess(groups3);
        chemical.names.add(name3);

        Group groupProtected = new Group("protected");
        Name name4 = new Name();
        name4.name = "SECRET NAME 4";
        Set<Group> groups4 = new HashSet<>();
        groups4.add(groupProtected);
        name4.setAccess(groups4);
        chemical.names.add(name4);

        Reference publicRef = new Reference();
        publicRef.publicDomain=true;
        publicRef.docType="Wikipedia";
        publicRef.citation="Some public chemical";

        Name namePublic = new Name();
        namePublic.name = "public name";
        namePublic.addReference(publicRef);
        chemical.addReference(publicRef);
        chemical.names.add(namePublic);
        chemical.created = new Date();

        Code bogusBdnum = new Code();
        bogusBdnum.type="PRIMARY";
        bogusBdnum.codeSystem="BDNUM";
        bogusBdnum.code="AA1234567890";
        chemical.codes.add(bogusBdnum);

        Code cas= new Code();
        cas.codeSystem="CAS";
        cas.type="PRIMARY";
        cas.code="293-55-0";
        chemical.codes.add(cas);

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLockedAccessGroupsToInclude( Collections.singletonList("WHO"));
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setRemoveNotes(true);
        scrubberSettings.setRemoveChangeReason(false);
        scrubberSettings.setRemoveDates(false);
        scrubberSettings.setApprovalIdCleanup(false);
        scrubberSettings.setRemoveCodesBySystem(true);
        scrubberSettings.setRemoveCodesBySystemCodeSystemsToRemove(Collections.singletonList("BDNUM"));
        scrubberSettings.setRemoveAllLockedRemoveElementsIfNoExportablePublicRef(true);

        scrubberSettings.setRemoveElementsIfNoExportablePublicRefElementsToRemove(Collections.singletonList("Names"));
        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        chemical.addNote("This is a note");
        String reasonToChange = "change is inevitable";
        chemical.changeReason = reasonToChange;
        Optional<Substance> cleaned = scrubber.scrub(chemical);
        ChemicalSubstance cleanedChemical = (ChemicalSubstance) cleaned.get();
        Name cleanedName1 = cleanedChemical.names.get(0);
        Assertions.assertEquals(1, cleanedChemical.names.size());
        Assertions.assertEquals("public name", cleanedName1.name);
    }

    @Test
    public void testScrubSubstanceDefNoNoPublicRef() {
        String currentSmiles="C1CCCCCCCC1";
        ChemicalSubstance chemical = SubstanceTestUtil.makeChemicalSubstance(currentSmiles);
        chemical.names.clear();//remove any bogus names
        Reference privateRef = new Reference();
        privateRef.publicDomain=false;
        privateRef.docType="Private Document Type";
        privateRef.citation="000000";

        chemical.getStructure().addReference(privateRef);
        chemical.addReference(privateRef);

        Reference publicRef = new Reference();
        publicRef.publicDomain=true;
        publicRef.docType="Wikipedia";
        publicRef.citation="Some public chemical";

        Name namePublic = new Name();
        namePublic.name = "public name";
        namePublic.addReference(publicRef);
        chemical.addReference(publicRef);
        chemical.names.add(namePublic);
        chemical.created = new Date();

        Code bogusBdnum = new Code();
        bogusBdnum.type="PRIMARY";
        bogusBdnum.codeSystem="BDNUM";
        bogusBdnum.code="AA1234567890";
        chemical.codes.add(bogusBdnum);

        Code cas= new Code();
        cas.codeSystem="CAS";
        cas.type="PRIMARY";
        cas.code="293-55-0";
        chemical.codes.add(cas);

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLockedAccessGroupsToInclude( Collections.singletonList("WHO"));
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setRemoveNotes(true);
        scrubberSettings.setRemoveChangeReason(false);
        scrubberSettings.setRemoveDates(false);
        scrubberSettings.setApprovalIdCleanup(false);
        scrubberSettings.setRemoveCodesBySystem(true);
        scrubberSettings.setRemoveCodesBySystemCodeSystemsToRemove(Collections.singletonList("BDNUM"));
        scrubberSettings.setRemoveAllLockedRemoveElementsIfNoExportablePublicRef(true);
        scrubberSettings.setRemoveElementsIfNoExportablePublicRefElementsToRemove(Collections.singletonList("Definition"));

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        chemical.addNote("This is a note");
        Optional<Substance> cleaned = scrubber.scrub(chemical);
        ChemicalSubstance cleanedChemical = (ChemicalSubstance) cleaned.get();
        Name cleanedName1 = cleanedChemical.names.get(0);
        //todo: add criteria

        Assertions.assertEquals(1, cleanedChemical.names.size());
        Assertions.assertEquals("public name", cleanedName1.name);
    }

    @Test
    public void testJsonPathPredicate() {
        String t = "{\n"
                + "    \"book\": \n"
                + "    [\n"
                + "        {\n"
                + "            \"title\": \"Beginning JSON\",\n"
                + "            \"author\": \"Ben Smith\",\n"
                + "            \"price\": 49.99\n"
                + "        },\n"
                + "\n"
                + "        {\n"
                + "            \"title\": \"JSON at Work\",\n"
                + "            \"author\": \"Tom Marrs\",\n"
                + "            \"price\": 29.99\n"
                + "        },\n"
                + "\n"
                + "        {\n"
                + "            \"title\": \"Learn JSON in a DAY\",\n"
                + "            \"author\": \"Acodemy\",\n"
                + "            \"price\": 8.99\n"
                + "        },\n"
                + "\n"
                + "        {\n"
                + "            \"title\": \"JSON: Questions and Answers\",\n"
                + "            \"author\": \"George Duckett\",\n"
                + "            \"price\": 6.00\n"
                + "        }\n"
                + "    ],\n"
                + "\n"
                + "    \"price range\": \n"
                + "    {\n"
                + "        \"cheap\": 10.00,\n"
                + "        \"medium\": 20.00\n"
                + "    }\n"
                + "}";
        Predicate expensivePredicate = new Predicate() {
            public boolean apply(PredicateContext context) {
                System.out.println("INSIDE THE PREDICATE");
                String value = context.item(Map.class).get("price").toString();
                return Float.valueOf(value) > 20.00;
            }
        };
        List<Map<String, Object>> expensive = JsonPath.parse(t)
                .read("$['book'][?]", expensivePredicate);
        predicateUsageAssertionHelper(expensive);
    }

    @Test
    public void testJson() {
        String currentSmiles="c1ccccc1";
        ChemicalSubstance chemical = SubstanceTestUtil.makeChemicalSubstance(currentSmiles);
        String  json = chemical.toFullJsonNode().toString();
        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        String result=scrubber.testAll(json);
        System.out.println(result);
        Assertions.assertTrue(1==1);

    }

    @Test
    public void testScrubSubstance3() {
        Subunit proteinSubunit = new Subunit();
        proteinSubunit.sequence="MAMVTGGWGGPGGDTNGVDKAGGYPRAAEDDSASPPGAASDAEPGDEERPGLQVDCVVCGDKSSGKHYGVFTCEGCKSFFKRSIRRNLSYTCRSNRDCQDQHHRNQCQYCRLKKCFRVGMRKEAVQRGRIPHSLPGAVAASSGSPPGSALAAVASGGDLFPGQPVSELIAQLLRAEPYPAAAGRFGAGGGAAGAVLGIDNVCELAARLLFSTVEWARHAPFFPELPVADQVALLRLSWSELFVLNAAQAALPLHTAPLLAAAGLHAAPMAAERAVAFMDQVRAFQEQVDKLGRLQVDSAEYGCLKAIALFTPDACGLSDPAHVESLQEKAQVALTEYVRAQYPSQPQRFGRLLLRLPALRAVPASLISQLFFMRLVGKTPIETLIRDMLLSGSTFNWPYGSGQ";
        //from https://gsrs.ncats.nih.gov/ginas/app/beta/substances/bf728207-d741-4363-937f-bcc891956499
        proteinSubunit.subunitIndex=1;

        Name name1= new Name("NUCLEAR RECEPTOR SUBFAMILY 2 GROUP F MEMBER 6");
        name1.displayName=true;

        Code codeUni= new Code();
        codeUni.code="P10588";
        codeUni.codeSystem="UNIPROT";
        codeUni.url="https://www.uniprot.org/uniprot/P10588";
        codeUni.type="PRIMARY";

        Code codeCas = new Code();
        codeCas.code="1102-21-2";
        codeCas.codeSystem="CAS";
        codeCas.type="PRIMARY";

        ProteinSubstance proteinSubstance = new ProteinSubstanceBuilder()
                .addSubUnit(proteinSubunit)
                .addName(name1)
                .addCode(codeUni)
                .addCode(codeCas)
                .build();

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLockedAccessGroupsToInclude( Collections.singletonList("WHO"));
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setRemoveNotes(true);
        scrubberSettings.setRemoveChangeReason(false);
        scrubberSettings.setRemoveDates(false);
        scrubberSettings.setApprovalIdCleanup(false);
        scrubberSettings.setRemoveCodesBySystem(true);
        scrubberSettings.setRemoveCodesBySystemCodeSystemsToRemove(Arrays.asList("BDNUM", "CAS"));

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        ProteinSubstance cleanedProtein= (ProteinSubstance) scrubber.scrub(proteinSubstance).get();

        Assertions.assertEquals(1, cleanedProtein.codes.size());
        Code cleanedCode=cleanedProtein.codes.get(0);
        Assertions.assertEquals("UNIPROT", cleanedCode.codeSystem);
    }

    @Test
    public void testScrubSubstanceCodesKeep() {
        Subunit proteinSubunit = new Subunit();
        proteinSubunit.sequence="MAMVTGGWGGPGGDTNGVDKAGGYPRAAEDDSASPPGAASDAEPGDEERPGLQVDCVVCGDKSSGKHYGVFTCEGCKSFFKRSIRRNLSYTCRSNRDCQDQHHRNQCQYCRLKKCFRVGMRKEAVQRGRIPHSLPGAVAASSGSPPGSALAAVASGGDLFPGQPVSELIAQLLRAEPYPAAAGRFGAGGGAAGAVLGIDNVCELAARLLFSTVEWARHAPFFPELPVADQVALLRLSWSELFVLNAAQAALPLHTAPLLAAAGLHAAPMAAERAVAFMDQVRAFQEQVDKLGRLQVDSAEYGCLKAIALFTPDACGLSDPAHVESLQEKAQVALTEYVRAQYPSQPQRFGRLLLRLPALRAVPASLISQLFFMRLVGKTPIETLIRDMLLSGSTFNWPYGSGQ";
        //from https://gsrs.ncats.nih.gov/ginas/app/beta/substances/bf728207-d741-4363-937f-bcc891956499
        proteinSubunit.subunitIndex=1;

        Name name1= new Name("NUCLEAR RECEPTOR SUBFAMILY 2 GROUP F MEMBER 6");
        name1.displayName=true;

        Code codeUni= new Code();
        codeUni.code="P10588";
        codeUni.codeSystem="UNIPROT";
        codeUni.url="https://www.uniprot.org/uniprot/P10588";
        codeUni.type="PRIMARY";

        Code codeCas = new Code();
        codeCas.code="1102-21-2";
        codeCas.codeSystem="CAS";
        codeCas.type="PRIMARY";

        ProteinSubstance proteinSubstance = new ProteinSubstanceBuilder()
                .addSubUnit(proteinSubunit)
                .addName(name1)
                .addCode(codeUni)
                .addCode(codeCas)
                .build();

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLockedAccessGroupsToInclude( Collections.singletonList("WHO"));
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setRemoveNotes(true);
        scrubberSettings.setRemoveChangeReason(false);
        scrubberSettings.setRemoveDates(false);
        scrubberSettings.setApprovalIdCleanup(false);
        scrubberSettings.setRemoveCodesBySystem(true);
        scrubberSettings.setRemoveCodesBySystemCodeSystemsToKeep(Collections.singletonList("UNIPROT"));

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        ProteinSubstance cleanedProtein= (ProteinSubstance) scrubber.scrub(proteinSubstance).get();

        Assertions.assertEquals(1, cleanedProtein.codes.size());
        Code cleanedCode=cleanedProtein.codes.get(0);
        Assertions.assertEquals("UNIPROT", cleanedCode.codeSystem);
    }

    @Test
    public void testScrubSubstanceCodesDeidentify() {
        Subunit proteinSubunit = new Subunit();
        proteinSubunit.sequence="MAMVTGGWGGPGGDTNGVDKAGGYPRAAEDDSASPPGAASDAEPGDEERPGLQVDCVVCGDKSSGKHYGVFTCEGCKSFFKRSIRRNLSYTCRSNRDCQDQHHRNQCQYCRLKKCFRVGMRKEAVQRGRIPHSLPGAVAASSGSPPGSALAAVASGGDLFPGQPVSELIAQLLRAEPYPAAAGRFGAGGGAAGAVLGIDNVCELAARLLFSTVEWARHAPFFPELPVADQVALLRLSWSELFVLNAAQAALPLHTAPLLAAAGLHAAPMAAERAVAFMDQVRAFQEQVDKLGRLQVDSAEYGCLKAIALFTPDACGLSDPAHVESLQEKAQVALTEYVRAQYPSQPQRFGRLLLRLPALRAVPASLISQLFFMRLVGKTPIETLIRDMLLSGSTFNWPYGSGQ";
        //from https://gsrs.ncats.nih.gov/ginas/app/beta/substances/bf728207-d741-4363-937f-bcc891956499
        proteinSubunit.subunitIndex=1;

        Principal user1 = new Principal();
        user1.username="Scientific User";
        user1.email="scientific@company.com";
        Name name1= new Name("NUCLEAR RECEPTOR SUBFAMILY 2 GROUP F MEMBER 6");
        name1.createdBy= user1;
        name1.displayName=true;

        Code codeUni= new Code();
        codeUni.code="P10588";
        codeUni.codeSystem="UNIPROT";
        codeUni.url="https://www.uniprot.org/uniprot/P10588";
        codeUni.type="PRIMARY";

        Code codeCas = new Code();
        codeCas.code="1102-21-2";
        codeCas.codeSystem="CAS";
        codeCas.type="PRIMARY";

        ProteinSubstance proteinSubstance = new ProteinSubstanceBuilder()
                .addSubUnit(proteinSubunit)
                .addName(name1)
                .addCode(codeUni)
                .addCode(codeCas)
                .build();

        proteinSubstance.createdBy=user1;
        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLockedAccessGroupsToInclude( Collections.singletonList("WHO"));
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setRemoveNotes(true);
        scrubberSettings.setRemoveChangeReason(false);
        scrubberSettings.setRemoveDates(false);
        scrubberSettings.setApprovalIdCleanup(false);
        scrubberSettings.setAuditInformationCleanup(true);
        scrubberSettings.setAuditInformationCleanupDeidentifyAuditUser(true);

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        ProteinSubstance cleanedProtein= (ProteinSubstance) scrubber.scrub(proteinSubstance).get();

        Assertions.assertNull(cleanedProtein.createdBy);
    }

    @Test
    public void testScrubSubstanceCodesChangeUser() {
        Subunit proteinSubunit = new Subunit();
        proteinSubunit.sequence="MAMVTGGWGGPGGDTNGVDKAGGYPRAAEDDSASPPGAASDAEPGDEERPGLQVDCVVCGDKSSGKHYGVFTCEGCKSFFKRSIRRNLSYTCRSNRDCQDQHHRNQCQYCRLKKCFRVGMRKEAVQRGRIPHSLPGAVAASSGSPPGSALAAVASGGDLFPGQPVSELIAQLLRAEPYPAAAGRFGAGGGAAGAVLGIDNVCELAARLLFSTVEWARHAPFFPELPVADQVALLRLSWSELFVLNAAQAALPLHTAPLLAAAGLHAAPMAAERAVAFMDQVRAFQEQVDKLGRLQVDSAEYGCLKAIALFTPDACGLSDPAHVESLQEKAQVALTEYVRAQYPSQPQRFGRLLLRLPALRAVPASLISQLFFMRLVGKTPIETLIRDMLLSGSTFNWPYGSGQ";
        //from https://gsrs.ncats.nih.gov/ginas/app/beta/substances/bf728207-d741-4363-937f-bcc891956499
        proteinSubunit.subunitIndex=1;

        Principal user1 = new Principal();
        user1.username="Scientific User";
        user1.email="scientific@company.com";
        Name name1= new Name("NUCLEAR RECEPTOR SUBFAMILY 2 GROUP F MEMBER 6");
        name1.createdBy= user1;
        name1.displayName=true;

        String newUserName ="Technical User";

        Code codeUni= new Code();
        codeUni.code="P10588";
        codeUni.codeSystem="UNIPROT";
        codeUni.url="https://www.uniprot.org/uniprot/P10588";
        codeUni.type="PRIMARY";

        Code codeCas = new Code();
        codeCas.code="1102-21-2";
        codeCas.codeSystem="CAS";
        codeCas.type="PRIMARY";

        ProteinSubstance proteinSubstance = new ProteinSubstanceBuilder()
                .addSubUnit(proteinSubunit)
                .addName(name1)
                .addCode(codeUni)
                .addCode(codeCas)
                .build();

        proteinSubstance.createdBy=user1;
        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLockedAccessGroupsToInclude( Collections.singletonList("WHO"));
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setRemoveNotes(true);
        scrubberSettings.setRemoveChangeReason(false);
        scrubberSettings.setRemoveDates(false);
        scrubberSettings.setApprovalIdCleanup(false);
        scrubberSettings.setAuditInformationCleanup(true);
        scrubberSettings.setAuditInformationCleanupNewAuditorValue(newUserName);

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        ProteinSubstance cleanedProtein= (ProteinSubstance) scrubber.scrub(proteinSubstance).get();

        Assertions.assertEquals(newUserName, cleanedProtein.createdBy.username);
    }

    @Test
    public void testScrubSubstanceReferenceCleanupByType() {
        String currentSmiles="C1CCCCCCCC1";
        ChemicalSubstance chemical = SubstanceTestUtil.makeChemicalSubstance(currentSmiles);
        chemical.names.clear();//remove any bogus names

        Reference publicRef = new Reference();
        publicRef.publicDomain=true;
        publicRef.docType="Wikipedia";
        publicRef.citation="Some public chemical";

        Reference publicRef2 = new Reference();
        publicRef2.publicDomain=true;
        publicRef2.docType="UNIPROT";
        publicRef2.citation="Some public protein ref";

        Reference publicRef3 = new Reference();
        publicRef3.publicDomain=true;
        publicRef3.docType="KEW GARDENS (WCPS)";
        publicRef3.citation="KEW GARDENS (WCPS)";

        Name name2 = new Name();
        name2.name = "SECRET NAME 2";
        chemical.names.add(name2);

        Group groupProtected = new Group("protected");
        Name name4 = new Name();
        name4.name = "Alternate name 4";
        Set<Group> groups4 = new HashSet<>();
        groups4.add(groupProtected);
        name4.setAccess(groups4);
        name4.addReference(publicRef3);
        chemical.names.add(name4);
        chemical.addReference(publicRef3);

        Name namePublic = new Name();
        namePublic.name = "public name";
        namePublic.addReference(publicRef);
        chemical.addReference(publicRef);
        chemical.names.add(namePublic);
        chemical.created = new Date();

        Code bogusBdnum = new Code();
        bogusBdnum.type="PRIMARY";
        bogusBdnum.codeSystem="BDNUM";
        bogusBdnum.code="AA1234567890";
        chemical.codes.add(bogusBdnum);

        Code cas= new Code();
        cas.codeSystem="CAS";
        cas.type="PRIMARY";
        cas.code="293-55-0";
        cas.addReference(publicRef2);
        chemical.codes.add(cas);
        chemical.addReference(publicRef2);

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLockedAccessGroupsToInclude( Collections.singletonList("WHO"));
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setRemoveNotes(true);
        scrubberSettings.setRemoveChangeReason(false);
        scrubberSettings.setRemoveDates(false);
        scrubberSettings.setApprovalIdCleanup(false);
        scrubberSettings.setRemoveCodesBySystem(true);
        scrubberSettings.setRemoveCodesBySystemCodeSystemsToRemove(Collections.singletonList("BDNUM"));
        scrubberSettings.setSubstanceReferenceCleanup(true);
        scrubberSettings.setRemoveReferencesByCriteria(true);
        scrubberSettings.setRemoveReferencesByCriteriaReferenceTypesToRemove(Collections.singletonList("Wikipedia"));

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        chemical.addNote("This is a note");
        String reasonToChange = "change is inevitable";
        chemical.changeReason = reasonToChange;
        Optional<Substance> cleaned = scrubber.scrub(chemical);
        ChemicalSubstance cleanedChemical = (ChemicalSubstance) cleaned.get();
        Assertions.assertFalse(cleanedChemical.references.stream().anyMatch(r->r.docType.equalsIgnoreCase("wikipedia")));
    }

    @Test
    public void testScrubSubstanceReferenceCleanupByPattern() {
        String currentSmiles="C1CCCCCCCC1";
        ChemicalSubstance chemical = SubstanceTestUtil.makeChemicalSubstance(currentSmiles);
        chemical.names.clear();//remove any bogus names

        Reference publicRef = new Reference();
        publicRef.publicDomain=true;
        publicRef.docType="Wikipedia";
        publicRef.citation="Some public chemical";

        Reference publicRef2 = new Reference();
        publicRef2.publicDomain=true;
        publicRef2.docType="UNIPROT";
        publicRef2.citation="Some public protein ref";

        Reference publicRef2a = new Reference();
        publicRef2a.publicDomain=true;
        publicRef2a.docType="UNIPROT";
        publicRef2a.citation="Some public protein ref 2";

        Reference publicRef3 = new Reference();
        publicRef3.publicDomain=true;
        publicRef3.docType="KEW GARDENS (WCPS)";
        publicRef3.citation="KEW GARDENS (WCPS)";

        Name name2 = new Name();
        name2.name = "SECRET NAME 2";
        chemical.names.add(name2);

        Name name4 = new Name();
        name4.name = "Alternate name 4";
        name4.addReference(publicRef3);
        name4.addReference(publicRef2a);
        chemical.names.add(name4);
        chemical.addReference(publicRef3);
        chemical.addReference(publicRef2a);

        Name namePublic = new Name();
        namePublic.name = "public name";
        namePublic.addReference(publicRef);
        chemical.addReference(publicRef);
        chemical.names.add(namePublic);
        chemical.created = new Date();

        Reference poeRef = new Reference();
        poeRef.citation="nevermore";
        poeRef.docType="OTHER";
        chemical.addReference(poeRef);
        Code bogusBdnum = new Code();
        bogusBdnum.type="PRIMARY";
        bogusBdnum.codeSystem="BDNUM";
        bogusBdnum.code="AA1234567890";
        bogusBdnum.addReference(poeRef);
        chemical.codes.add(bogusBdnum);

        Code cas= new Code();
        cas.codeSystem="CAS";
        cas.type="PRIMARY";
        cas.code="293-55-0";
        cas.addReference(publicRef2);
        chemical.codes.add(cas);
        chemical.addReference(publicRef2);

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLockedAccessGroupsToInclude( Collections.singletonList("WHO"));
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setRemoveNotes(true);
        scrubberSettings.setRemoveChangeReason(false);
        scrubberSettings.setRemoveDates(false);
        scrubberSettings.setApprovalIdCleanup(false);
        scrubberSettings.setSubstanceReferenceCleanup(true);
        scrubberSettings.setRemoveReferencesByCriteria(true);
        scrubberSettings.setRemoveReferencesByCriteriaExcludeReferenceByPattern(true);
        scrubberSettings.setRemoveReferencesByCriteriaCitationPatternsToRemove(".*GARDENS.*\nnevermore");

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        chemical.addNote("This is a note");
        String reasonToChange = "change is inevitable";
        chemical.changeReason = reasonToChange;
        Optional<Substance> cleaned = scrubber.scrub(chemical);
        ChemicalSubstance cleanedChemical = (ChemicalSubstance) cleaned.get();
        Assertions.assertTrue(cleanedChemical.references.stream().noneMatch(r->r.docType.equalsIgnoreCase("KEW GARDENS (WCPS)")));
        Assertions.assertTrue(cleanedChemical.references.stream().noneMatch(r->r.citation.equalsIgnoreCase("nevermore")));
        Name testName = cleanedChemical.names.stream().filter(n->n.name.equals("Alternate name 4")).findFirst().get();
        Assertions.assertEquals(1, testName.getReferences().size());
    }

    @Test
    public void testScrubSubstanceRemoveApprovalId() {
        String currentSmiles="C1CCCCCCC1N";
        ChemicalSubstance chemical = SubstanceTestUtil.makeChemicalSubstance(currentSmiles);
        chemical.names.clear();//remove any bogus names

        Reference publicRef = new Reference();
        publicRef.publicDomain=true;
        publicRef.docType="Wikipedia";
        publicRef.citation="Some public chemical";

        Reference publicRef2 = new Reference();
        publicRef2.publicDomain=true;
        publicRef2.docType="UNIPROT";
        publicRef2.citation="Some public protein ref";

        Reference publicRef2a = new Reference();
        publicRef2a.publicDomain=true;
        publicRef2a.docType="UNIPROT";
        publicRef2a.citation="Some public protein ref 2";

        Reference publicRef3 = new Reference();
        publicRef3.publicDomain=true;
        publicRef3.docType="KEW GARDENS (WCPS)";
        publicRef3.citation="KEW GARDENS (WCPS)";
        String approvalIdValue="Approved12";
        String approvalIDCodeSystem="Approval ID Code";

        Name namePublic = new Name();
        namePublic.name = "public name";
        namePublic.addReference(publicRef);
        chemical.addReference(publicRef);
        chemical.names.add(namePublic);
        chemical.created = new Date();

        Reference poeRef = new Reference();
        poeRef.citation="nevermore";
        poeRef.docType="OTHER";
        chemical.addReference(poeRef);
        chemical.status="approved";
        chemical.approvalID=approvalIdValue;

        Code cas= new Code();
        cas.codeSystem="CAS";
        cas.type="PRIMARY";
        cas.code="293-55-0";
        cas.addReference(publicRef2);
        chemical.codes.add(cas);
        chemical.addReference(publicRef2);

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLockedAccessGroupsToInclude( Collections.singletonList("WHO"));
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setRemoveNotes(true);
        scrubberSettings.setRemoveChangeReason(false);
        scrubberSettings.setRemoveDates(false);
        scrubberSettings.setApprovalIdCleanup(true);
        scrubberSettings.setApprovalIdCleanupRemoveApprovalId(true);
        scrubberSettings.setSubstanceReferenceCleanup(false);

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        chemical.addNote("This is a note");
        String reasonToChange = "change is inevitable";
        chemical.changeReason = reasonToChange;
        Optional<Substance> cleaned = scrubber.scrub(chemical);
        ChemicalSubstance cleanedChemical = (ChemicalSubstance) cleaned.get();
        Assertions.assertNull(cleanedChemical.approvalID);
    }

    @Test
    public void testScrubSubstanceCopyApprovalIdToCode() {
        String currentSmiles="C1CCCCCCC1N";
        ChemicalSubstance chemical = SubstanceTestUtil.makeChemicalSubstance(currentSmiles);
        chemical.names.clear();//remove any bogus names

        Reference publicRef = new Reference();
        publicRef.publicDomain=true;
        publicRef.docType="Wikipedia";
        publicRef.citation="Some public chemical";

        Reference publicRef2 = new Reference();
        publicRef2.publicDomain=true;
        publicRef2.docType="UNIPROT";
        publicRef2.citation="Some public protein ref";

        Reference publicRef2a = new Reference();
        publicRef2a.publicDomain=true;
        publicRef2a.docType="UNIPROT";
        publicRef2a.citation="Some public protein ref 2";

        Reference publicRef3 = new Reference();
        publicRef3.publicDomain=true;
        publicRef3.docType="KEW GARDENS (WCPS)";
        publicRef3.citation="KEW GARDENS (WCPS)";
        String approvalIdValue="Approved12";
        String approvalIDCodeSystem="Approval ID Code";

        Name namePublic = new Name();
        namePublic.name = "public name";
        namePublic.addReference(publicRef);
        chemical.addReference(publicRef);
        chemical.names.add(namePublic);
        chemical.created = new Date();

        Reference poeRef = new Reference();
        poeRef.citation="nevermore";
        poeRef.docType="OTHER";
        chemical.addReference(poeRef);
        chemical.status="approved";
        chemical.approvalID=approvalIdValue;

        Code cas= new Code();
        cas.codeSystem="CAS";
        cas.type="PRIMARY";
        cas.code="293-55-0";
        cas.addReference(publicRef2);
        chemical.codes.add(cas);
        chemical.addReference(publicRef2);

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setRemoveAllLockedAccessGroupsToInclude( Collections.singletonList("WHO"));
        scrubberSettings.setRemoveAllLocked(true);
        scrubberSettings.setRemoveNotes(true);
        scrubberSettings.setRemoveChangeReason(false);
        scrubberSettings.setRemoveDates(false);
        scrubberSettings.setApprovalIdCleanup(true);
        scrubberSettings.setApprovalIdCleanupRemoveApprovalId(true);
        scrubberSettings.setSubstanceReferenceCleanup(false);
        scrubberSettings.setApprovalIdCleanupCopyApprovalIdToCode(true);
        scrubberSettings.setApprovalIdCleanupApprovalIdCodeSystem(approvalIDCodeSystem);

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        chemical.addNote("This is a note");
        String reasonToChange = "change is inevitable";
        chemical.changeReason = reasonToChange;
        Optional<Substance> cleaned = scrubber.scrub(chemical);
        ChemicalSubstance cleanedChemical = (ChemicalSubstance) cleaned.get();
        Assertions.assertNull(cleanedChemical.approvalID);
        Code approvalIdCode= cleanedChemical.codes.stream().filter(c->c.codeSystem.equals(approvalIDCodeSystem)).findFirst().get();
        Assertions.assertEquals(approvalIdValue, approvalIdCode.code);
    }

    @Test
    public void testScrubSubstanceUuids() {
        String currentSmiles="C1CCCCCCC1N";
        ChemicalSubstance chemical = SubstanceTestUtil.makeChemicalSubstance(currentSmiles);
        chemical.names.clear();//remove any bogus names
        UUID mainUuid=UUID.randomUUID();
        chemical.uuid=mainUuid;
        int standardUuidLength=36;

        Reference publicRef = new Reference();
        publicRef.publicDomain=true;
        publicRef.docType="Wikipedia";
        publicRef.citation="Some public chemical";

        Reference publicRef2 = new Reference();
        publicRef2.publicDomain=true;
        publicRef2.docType="UNIPROT";
        publicRef2.citation="Some public protein ref";

        Reference publicRef2a = new Reference();
        publicRef2a.publicDomain=true;
        publicRef2a.docType="UNIPROT";
        publicRef2a.citation="Some public protein ref 2";
        UUID refUuid= UUID.randomUUID();
        publicRef2a.uuid= refUuid;

        Reference publicRef3 = new Reference();
        publicRef3.publicDomain=true;
        publicRef3.docType="KEW GARDENS (WCPS)";
        publicRef3.citation="KEW GARDENS (WCPS)";
        String approvalIdValue="Approved12";
        String approvalIDCodeSystem="Approval ID Code";

        Name namePublic = new Name();
        namePublic.name = "public name";
        namePublic.addReference(publicRef);
        namePublic.addReference(publicRef2a);
        UUID publicNameUuid=UUID.randomUUID();
        namePublic.uuid=publicNameUuid;
        chemical.addReference(publicRef);
        chemical.addReference(publicRef2a);
        chemical.names.add(namePublic);
        chemical.created = new Date();

        Reference poeRef = new Reference();
        poeRef.citation="nevermore";
        poeRef.docType="OTHER";
        chemical.addReference(poeRef);
        chemical.status="approved";
        chemical.approvalID=approvalIdValue;

        Code cas= new Code();
        cas.codeSystem="CAS";
        cas.type="PRIMARY";
        cas.code="293-55-0";
        cas.addReference(publicRef2);
        UUID casCodeUuid=UUID.randomUUID();
        cas.uuid=casCodeUuid;
        chemical.codes.add(cas);
        chemical.addReference(publicRef2);

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setSubstanceReferenceCleanup(false);
        scrubberSettings.setRegenerateUUIDs(true);

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        chemical.addNote("This is a note");
        String reasonToChange = "change is inevitable";
        chemical.changeReason = reasonToChange;
        Optional<Substance> cleaned = scrubber.scrub(chemical);
        ChemicalSubstance cleanedChemical = (ChemicalSubstance) cleaned.get();
        Assertions.assertNotEquals(mainUuid, cleanedChemical.uuid);
        Assertions.assertEquals(standardUuidLength, cleanedChemical.uuid.toString().length());

        Name cleanedPublicName= cleanedChemical.names.stream()
                .filter(n->n.name.equals("public name"))
                .findFirst().get();
        Assertions.assertNotEquals(publicNameUuid, cleanedPublicName.uuid);
        Code cleanedCasCode = cleanedChemical.codes.stream()
                .filter(c->c.codeSystem.equals("CAS"))
                .findFirst().get();
        Assertions.assertNotEquals(casCodeUuid, cleanedCasCode.uuid);

        Reference cleanedUnipRef = cleanedChemical.references.stream()
                .filter(r->r.docType.equals(publicRef2a.docType) && r.citation.equals(publicRef2a.citation))
                .findFirst().get();
        System.out.println(cleanedUnipRef.uuid.equals( refUuid) ? "reference UUIDs unchanged" : "reference UUIDs changed");
    }

    @Test
    public void testScrubSubstanceUuidsUnchanged() {
        String currentSmiles="C1CCCCCCC1N";
        ChemicalSubstance chemical = SubstanceTestUtil.makeChemicalSubstance(currentSmiles);
        chemical.names.clear();//remove any bogus names
        UUID mainUuid=UUID.randomUUID();
        chemical.uuid=mainUuid;
        int standardUuidLength=36;

        Reference publicRef = new Reference();
        publicRef.publicDomain=true;
        publicRef.docType="Wikipedia";
        publicRef.citation="Some public chemical";

        Reference publicRef2 = new Reference();
        publicRef2.publicDomain=true;
        publicRef2.docType="UNIPROT";
        publicRef2.citation="Some public protein ref";

        Reference publicRef2a = new Reference();
        publicRef2a.publicDomain=true;
        publicRef2a.docType="UNIPROT";
        publicRef2a.citation="Some public protein ref 2";
        UUID refUuid= UUID.randomUUID();
        publicRef2a.uuid= refUuid;

        Reference publicRef3 = new Reference();
        publicRef3.publicDomain=true;
        publicRef3.docType="KEW GARDENS (WCPS)";
        publicRef3.citation="KEW GARDENS (WCPS)";
        String approvalIdValue="Approved12";
        String approvalIDCodeSystem="Approval ID Code";

        Name namePublic = new Name();
        namePublic.name = "public name";
        namePublic.addReference(publicRef);
        namePublic.addReference(publicRef2a);
        UUID publicNameUuid=UUID.randomUUID();
        namePublic.uuid=publicNameUuid;
        chemical.addReference(publicRef);
        chemical.addReference(publicRef2a);
        chemical.names.add(namePublic);
        chemical.created = new Date();

        Reference poeRef = new Reference();
        poeRef.citation="nevermore";
        poeRef.docType="OTHER";
        chemical.addReference(poeRef);
        chemical.status="approved";
        chemical.approvalID=approvalIdValue;

        Code cas= new Code();
        cas.codeSystem="CAS";
        cas.type="PRIMARY";
        cas.code="293-55-0";
        cas.addReference(publicRef2);
        UUID casCodeUuid=UUID.randomUUID();
        cas.uuid=casCodeUuid;
        chemical.codes.add(cas);
        chemical.addReference(publicRef2);

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setSubstanceReferenceCleanup(false);
        scrubberSettings.setRegenerateUUIDs(false);

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        chemical.addNote("This is a note");
        String reasonToChange = "change is inevitable";
        chemical.changeReason = reasonToChange;
        Optional<Substance> cleaned = scrubber.scrub(chemical);
        ChemicalSubstance cleanedChemical = (ChemicalSubstance) cleaned.get();
        Assertions.assertEquals(mainUuid, cleanedChemical.uuid);
        Assertions.assertEquals(standardUuidLength, cleanedChemical.uuid.toString().length());

        Name cleanedPublicName= cleanedChemical.names.stream()
                .filter(n->n.name.equals("public name"))
                .findFirst().get();
        Assertions.assertEquals(publicNameUuid, cleanedPublicName.uuid);
        Code cleanedCasCode = cleanedChemical.codes.stream()
                .filter(c->c.codeSystem.equals("CAS"))
                .findFirst().get();
        Assertions.assertEquals(casCodeUuid, cleanedCasCode.uuid);
    }

    @Test
    public void testScrubSubstanceStatus1() {
        String currentSmiles="C1CCCCCCC1N";
        ChemicalSubstance chemical = SubstanceTestUtil.makeChemicalSubstance(currentSmiles);
        chemical.names.clear();//remove any bogus names
        UUID mainUuid=UUID.randomUUID();
        chemical.uuid=mainUuid;
        chemical.status="approved";

        Reference publicRef = new Reference();
        publicRef.publicDomain=true;
        publicRef.docType="Wikipedia";
        publicRef.citation="Some public chemical";

        Reference publicRef2 = new Reference();
        publicRef2.publicDomain=true;
        publicRef2.docType="UNIPROT";
        publicRef2.citation="Some public protein ref";


        Reference publicRef3 = new Reference();
        publicRef3.publicDomain=true;
        publicRef3.docType="KEW GARDENS (WCPS)";
        publicRef3.citation="KEW GARDENS (WCPS)";
        String approvalIdValue="Approved12";
        String approvalIDCodeSystem="Approval ID Code";

        Name namePublic = new Name();
        namePublic.name = "public name";
        namePublic.addReference(publicRef);
        UUID publicNameUuid=UUID.randomUUID();
        namePublic.uuid=publicNameUuid;
        chemical.addReference(publicRef);
        chemical.names.add(namePublic);
        chemical.created = new Date();

        Reference poeRef = new Reference();
        poeRef.citation="nevermore";
        poeRef.docType="OTHER";
        chemical.addReference(poeRef);
        chemical.status="approved";
        chemical.approvalID=approvalIdValue;

        Code cas= new Code();
        cas.codeSystem="CAS";
        cas.type="PRIMARY";
        cas.code="293-55-0";
        cas.addReference(publicRef2);
        UUID casCodeUuid=UUID.randomUUID();
        cas.uuid=casCodeUuid;
        chemical.codes.add(cas);
        chemical.addReference(publicRef2);

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setSubstanceReferenceCleanup(false);
        scrubberSettings.setRegenerateUUIDs(true);
        scrubberSettings.setChangeAllStatuses(true);
        scrubberSettings.setChangeAllStatusesNewStatusValue("pending");

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        chemical.addNote("This is a note");
        String reasonToChange = "change is inevitable";
        chemical.changeReason = reasonToChange;
        Optional<Substance> cleaned = scrubber.scrub(chemical);
        ChemicalSubstance cleanedChemical = (ChemicalSubstance) cleaned.get();
        Assertions.assertEquals("pending", cleanedChemical.status);
    }

    @Test
    public void testScrubSubstanceStatus2() {
        String currentSmiles="C1CCCCCCC1N";
        ChemicalSubstance chemical = SubstanceTestUtil.makeChemicalSubstance(currentSmiles);
        chemical.names.clear();//remove any bogus names
        UUID mainUuid=UUID.randomUUID();
        chemical.uuid=mainUuid;
        chemical.status="approved";

        Reference publicRef = new Reference();
        publicRef.publicDomain=true;
        publicRef.docType="Wikipedia";
        publicRef.citation="Some public chemical";

        Reference publicRef2 = new Reference();
        publicRef2.publicDomain=true;
        publicRef2.docType="UNIPROT";
        publicRef2.citation="Some public protein ref";

        Reference publicRef3 = new Reference();
        publicRef3.publicDomain=true;
        publicRef3.docType="KEW GARDENS (WCPS)";
        publicRef3.citation="KEW GARDENS (WCPS)";
        String approvalIdValue="Approved12";
        String approvalIDCodeSystem="Approval ID Code";

        Name namePublic = new Name();
        namePublic.name = "public name";
        namePublic.addReference(publicRef);
        UUID publicNameUuid=UUID.randomUUID();
        namePublic.uuid=publicNameUuid;
        chemical.addReference(publicRef);
        chemical.names.add(namePublic);
        chemical.created = new Date();

        Reference poeRef = new Reference();
        poeRef.citation="nevermore";
        poeRef.docType="OTHER";
        chemical.addReference(poeRef);
        chemical.status="approved";
        chemical.approvalID=approvalIdValue;

        Code cas= new Code();
        cas.codeSystem="CAS";
        cas.type="PRIMARY";
        cas.code="293-55-0";
        cas.addReference(publicRef2);
        UUID casCodeUuid=UUID.randomUUID();
        cas.uuid=casCodeUuid;
        chemical.codes.add(cas);
        chemical.addReference(publicRef2);

        BasicSubstanceScrubberParameters scrubberSettings = new BasicSubstanceScrubberParameters();
        scrubberSettings.setSubstanceReferenceCleanup(false);
        scrubberSettings.setRegenerateUUIDs(true);
        scrubberSettings.setChangeAllStatuses(false);
        scrubberSettings.setChangeAllStatusesNewStatusValue("pending");

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberSettings);
        chemical.addNote("This is a note");
        String reasonToChange = "change is inevitable";
        chemical.changeReason = reasonToChange;
        Optional<Substance> cleaned = scrubber.scrub(chemical);
        ChemicalSubstance cleanedChemical = (ChemicalSubstance) cleaned.get();
        Assertions.assertEquals("approved", cleanedChemical.status);
    }

    @Test
    public void testReferencedSubstanceCleanup() {
        //building up a structurally diverse substance where the parent is protected.
        StructurallyDiverseSubstance parent = new StructurallyDiverseSubstance();
        StructurallyDiverse parentCore= new StructurallyDiverse();
        parentCore.part.add(new Keyword("WHOLE"));
        parentCore.sourceMaterialClass="ORGANISM";
        parentCore.sourceMaterialType="PLANT";
        parentCore.organismFamily= "Myrtaceae";
        parentCore.organismGenus="Taxandria";
        parentCore.organismSpecies="fragrans";
        parentCore.organismAuthor="(J. R. Wheeler & N. G. Marchant) J. R. Wheeler & N. G. Marchant";
        parent.structurallyDiverse=parentCore;
        parent.names.add(new Name("Taxandria fragrans whole"));
        Group protectedGroup = new Group("protected");
        parent.getAccess().add(protectedGroup);
        UUID parentUuid= parent.getOrGenerateUUID();

        StructurallyDiverseSubstance child = new StructurallyDiverseSubstance();
        StructurallyDiverse childCore = new StructurallyDiverse();
        childCore.parentSubstance = new SubstanceReference();
        childCore.parentSubstance = SubstanceReference.newReferenceFor(parent);
        childCore.part.add(new Keyword("FLOWER"));
        childCore.part.add(new Keyword("LEAF"));
        childCore.part.add(new Keyword("STEM"));

        child.structurallyDiverse=childCore;
        child.structurallyDiverse=childCore;

        Assertions.assertEquals(parentUuid.toString(), child.structurallyDiverse.parentSubstance.refuuid);
    }
    
    private void predicateUsageAssertionHelper(List<?> predicate) {
        System.out.println(predicate.toString());
    }
}
