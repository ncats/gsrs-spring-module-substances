package example.substance;

import ix.core.models.Keyword;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;
import ix.ginas.utils.validation.validators.tags.TagUtilities;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Slf4j
public class TagsTest {

    Substance createOldSubstance() {
        Substance oldSubstance = new Substance();
        Name oldName1 = new Name();
        Name oldName2 = new Name();
        Name oldName3 = new Name();
        Name oldName4 = new Name();
        Name oldName5 = new Name();
        oldName1.setName("A");
        oldName2.setName("B");
        oldName3.setName("C [USP]");
        oldName4.setName("D");
        oldName5.setName("E [VANDF]");
        oldSubstance.names.add(oldName1);
        oldSubstance.names.add(oldName2);
        oldSubstance.names.add(oldName3);
        oldSubstance.names.add(oldName4);
        oldSubstance.names.add(oldName5);
        oldSubstance.addTagString("USP");
        oldSubstance.addTagString("VANDF");
        return oldSubstance;
    }

    Substance createNewSubstance() {
        Substance newSubstance = new Substance();
        Name newName1 = new Name();
        Name newName2 = new Name();
        Name newName3 = new Name();
        Name newName4 = new Name();
        newName1.setName("D");
        newName2.setName("E [VANDF]");
        newName3.setName("F");
        newName4.setName("G");
        newSubstance.names.add(newName1);
        newSubstance.names.add(newName2);
        newSubstance.names.add(newName3);
        newSubstance.names.add(newName4);
        newSubstance.addTagString("INN");
        newSubstance.addTagString("VANDF");
        return newSubstance;
    }

    @Test
    void testCanDeleteTag() throws Exception {
        Substance oldSubstance = this.createOldSubstance();
        oldSubstance.removeTagString("VANDF");
    }

    @Test
    void testExtractBracketNameTags() throws Exception {
        Substance s = new Substance();
        s.names = new ArrayList<>();
        s.tags.add(new Keyword("USP"));
        s.tags.add(new Keyword("INN"));
        s.tags.add(new Keyword("GREEN BOOK"));
        s.names.add(new Name("ABC [USP]"));
        s.names.add(new Name("CED [USP]"));
        s.names.add(new Name("PED [INN]"));
        s.names.add(new Name("QAK [INN]"));
        s.names.add(new Name("VAD [VANDF]"));
        s.names.add(new Name("RAGDOLL [NOT][FOOT]"));
        s.names.add(new Name("SPEAK [ZEEL][SPELT]"));
        Set<String> bracketNameTags = TagUtilities.extractBracketNameTags(s);
        assert(bracketNameTags.equals(new HashSet<>(Arrays.asList("USP","INN","VANDF", "ZEEL", "SPELT", "NOT", "FOOT"))));
    }

    @Test
    void testCompareTagTermsToNamesTagTermsOldSubstance() throws Exception {
            Substance oldSubstance = createOldSubstance();

        assertEquals(
                TagUtilities.getSetAExcludesB(
                        TagUtilities.extractBracketNameTags(oldSubstance),
                        TagUtilities.extractExplicitTags(oldSubstance)
                ),
                new HashSet<>(Arrays.asList())
        );
        assertEquals(
                TagUtilities.getSetAExcludesB(
                        TagUtilities.extractExplicitTags(oldSubstance),
                        TagUtilities.extractBracketNameTags(oldSubstance)
                ),
                new HashSet<>(Arrays.asList())
        );
    }

    @Test
    void testGetBracketTermWhereNameStringNull() throws Exception {
        Substance substance = new Substance();
        Name oldName1 = new Name();
        substance.names.add(oldName1);
        substance.addTagString("USP");
        boolean npeThrown = false;
        try {
            List<String> s = TagUtilities.getBracketTerms((String) null);
        }catch (NullPointerException npe){
            npeThrown = true;
        }
        assertTrue(npeThrown);
    }

    @Test
    void testCompareTagTermsToNamesTagTermsNewSubstance() throws Exception {
        Substance newSubstance = this.createNewSubstance();
        assertEquals(
                TagUtilities.getSetAExcludesB(
                        TagUtilities.extractBracketNameTags(newSubstance),
                        TagUtilities.extractExplicitTags(newSubstance)
                ),
                new HashSet<>(Arrays.asList())
        );
        assertEquals(
                TagUtilities.getSetAExcludesB(
                        TagUtilities.extractExplicitTags(newSubstance),
                        TagUtilities.extractBracketNameTags(newSubstance)
                ),
                new HashSet<>(Arrays.asList("INN"))
        );
    }

    @Test
    void testCompareTagTermsToNamesTagTermsOldSubstanceIfNamesEmpty() throws Exception {
        Substance oldSubstance = this.createOldSubstance();
        oldSubstance.names = new ArrayList<Name>();
        oldSubstance.tags = new ArrayList<Keyword>();
        oldSubstance.addTagString("USP");
        oldSubstance.addTagString("VANDF");
        // names []
        // tags [a b]
        // in tags missing from names a b
        // in names missing from tags []
        assertEquals(
                TagUtilities.getSetAExcludesB(
                        TagUtilities.extractBracketNameTags(oldSubstance),
                        TagUtilities.extractExplicitTags(oldSubstance)
                ),
                new HashSet<>(Arrays.asList())
        );
        assertEquals(
                TagUtilities.getSetAExcludesB(
                        TagUtilities.extractExplicitTags(oldSubstance),
                        TagUtilities.extractBracketNameTags(oldSubstance)
                ),
                new HashSet<>(Arrays.asList("USP", "VANDF"))
        );
    }

    @Test
    void testCompareTagTermsToNamesTagTermsOldSubstanceIfTagsEmpty() throws Exception {
        Substance oldSubstance = this.createOldSubstance();
        oldSubstance.tags = new ArrayList<Keyword>();
        oldSubstance.names = new ArrayList<Name>();
        Name oldName1 = new Name();
        Name oldName2 = new Name();
        oldName1.setName("C [USP]");
        oldName2.setName("D [VANDF]");
        oldSubstance.names.add(oldName1);
        oldSubstance.names.add(oldName2);
        // names [c d]
        // tags []
        // in tags missing from names []
        // in names missing from tags [c d]

        assertEquals(
                TagUtilities.getSetAExcludesB(
                        TagUtilities.extractBracketNameTags(oldSubstance),
                        TagUtilities.extractExplicitTags(oldSubstance)
                ),
                new HashSet<>(Arrays.asList("USP", "VANDF"))
        );
        assertEquals(
                TagUtilities.getSetAExcludesB(
                        TagUtilities.extractExplicitTags(oldSubstance),
                        TagUtilities.extractBracketNameTags(oldSubstance)
                ),
                new HashSet<>(Arrays.asList())
        );
    }
    @Test
    void testGetBracketTermsTemp() {
        Assertions.assertEquals(TagUtilities.getBracketTerms("Hello [GREEN BOOK]    [BASKET  ]"), new ArrayList<>(Arrays.asList("GREEN BOOK", "BASKET")));
        Assertions.assertEquals(TagUtilities.getBracketTerms("Halo Baby     [GREEN BOOK]    [BASKET  ]"), new ArrayList<>(Arrays.asList("GREEN BOOK", "BASKET")));
        Assertions.assertEquals(TagUtilities.getBracketTerms("Halo [Baby     [GREEN BOOK]    [BASKET  ]"), new ArrayList<>(Arrays.asList("GREEN BOOK", "BASKET")));
        Assertions.assertEquals(TagUtilities.getBracketTerms("Halo[Baby]     [GREEN BOOK]    [BASKET  ]"), new ArrayList<>(Arrays.asList("GREEN BOOK", "BASKET")));
    }

    @Test
    void testGetBracketTerms() {

        // Fixes problematic issue seen above
        Assertions.assertEquals(TagUtilities.getBracketTerms("XYZ [USP] ABC"), new ArrayList<>(Arrays.asList()));
        // Handles colon delimited tags.
        Assertions.assertEquals(TagUtilities.getBracketTerms("ASPIRIN1,23[asguyasgda]asgduytqwqd [INN:USAN][ABC]"), new ArrayList<>(Arrays.asList("INN", "USAN", "ABC")));
        Assertions.assertEquals(TagUtilities.getBracketTerms("ASPIRIN1,23[asguyasgda]asgduytqwqd [INN][USAN][ABC]"), new ArrayList<>(Arrays.asList("INN", "USAN", "ABC")));
        Assertions.assertEquals(TagUtilities.getBracketTerms("ABC [USP]"), new ArrayList<>(Arrays.asList("USP")));
        Assertions.assertEquals(TagUtilities.getBracketTerms("ABC"), new ArrayList<>(Arrays.asList()));
        Assertions.assertEquals(TagUtilities.getBracketTerms("[USP] ABC"), new ArrayList<>(Arrays.asList()));
        Assertions.assertEquals(TagUtilities.getBracketTerms("ibuprofen [INN]"), new ArrayList<>(Arrays.asList("INN")));
        Assertions.assertEquals(TagUtilities.getBracketTerms("1,2-dimethyl[something-or-other]"), new ArrayList<>(Arrays.asList()));
        Assertions.assertEquals(TagUtilities.getBracketTerms("ibuprofen [WHO-DD]"), new ArrayList<>(Arrays.asList("WHO-DD")));
        Assertions.assertEquals(TagUtilities.getBracketTerms("1,2-dimethyl[something-or-other] [INN]"), new ArrayList<>(Arrays.asList("INN")));
        Assertions.assertEquals(TagUtilities.getBracketTerms("ibuprofen[INN][USAN]"), new ArrayList<>(Arrays.asList()));
        Assertions.assertEquals(TagUtilities.getBracketTerms("Hello [GREEN BOOK]"), new ArrayList<>(Arrays.asList("GREEN BOOK")));

        // Make sure tag cleaning works
        Assertions.assertEquals(TagUtilities.getBracketTerms("ABC [USP    ]"), new ArrayList<>(Arrays.asList("USP")));
        Assertions.assertEquals(TagUtilities.getBracketTerms("ABC [    USP    ]"), new ArrayList<>(Arrays.asList("USP")));
        Assertions.assertEquals(TagUtilities.getBracketTerms("ABC [USP :  UK  :  ST    EP  ]"), new ArrayList<>(Arrays.asList("USP", "UK", "ST EP")));
        Assertions.assertEquals(TagUtilities.getBracketTerms("ABC [USP :::]"), new ArrayList<>(Arrays.asList("USP")));
        Assertions.assertEquals(TagUtilities.getBracketTerms("ABC [USP:]"), new ArrayList<>(Arrays.asList("USP")));
        Assertions.assertEquals(TagUtilities.getBracketTerms("ABC []"), new ArrayList<>(Arrays.asList()));
        Assertions.assertEquals(TagUtilities.getBracketTerms("ABC [:]"), new ArrayList<>(Arrays.asList()));
        Assertions.assertEquals(TagUtilities.getBracketTerms("ABC [    ]"), new ArrayList<>(Arrays.asList()));

        // Make sure namePart cleaning works
        if (TagUtilities.CLEAN_NAME_PART) {
            TagUtilities.BracketExtraction be1 = TagUtilities.getBracketExtraction("   ABC I am   Messy [USP    ]");
            Assertions.assertEquals(be1.getNamePart(), "ABC I am Messy");
            Assertions.assertEquals(be1.getTagTerms(), new ArrayList<>(Arrays.asList("USP")));
        }
    }

    @Test
    void testGetBracketTermsWhenNameStringNull(){
        List<String> list;
        boolean npeThrown = false;
        try {
            list = TagUtilities.getBracketTerms((String) null);
        } catch(NullPointerException npe) {
            npeThrown = true;
        }
        assertTrue(true);
    }

    @Test
    void testGetBracketExtraction() throws Exception {
        // This uses the 2-step regex
        TagUtilities.BracketExtraction be1 = TagUtilities.getBracketExtraction("ABC [USP]");
        assert(be1.getNamePart().equals("ABC"));
        assert(be1.getTagTerms().equals(Arrays.asList("USP")));
        TagUtilities.BracketExtraction be2 = TagUtilities.getBracketExtraction("NAME BOY [BEEP][GREEN BOOK]");
        assert(be2.getNamePart().equals("NAME BOY"));
        assert(be2.getTagTerms().equals(Arrays.asList("BEEP", "GREEN BOOK")));
        TagUtilities.BracketExtraction be3 = TagUtilities.getBracketExtraction("NAME BOY [     ]");
        assert(be3.getNamePart().equals("NAME BOY"));
        assert(be3.getTagTerms().equals(Arrays.asList()));
        TagUtilities.BracketExtraction be4 = TagUtilities.getBracketExtraction("NAME     BOY     [USP   : : : : ]");
        assert(be4.getNamePart().equals("NAME BOY"));
        assert(be4.getTagTerms().equals(Arrays.asList("USP")));
    }

}
