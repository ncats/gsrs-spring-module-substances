package example.substance;

import ix.core.models.Keyword;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;
import static org.junit.Assert.assertEquals;

@SpringBootTest
@Slf4j
public class TagsTest {
    final static boolean logBeforeAfterClass = true;

    @BeforeAll
    static void beforeUnits() throws Exception {
        if(logBeforeAfterClass) log.info("\n\n==== Starting TagsTest Class ====\n\n");
    }
    @AfterAll
    static void afterUnits() throws Exception {
        if(logBeforeAfterClass) log.info("\n\n==== Finished TagsTest Class ====\n\n");
    }

    Substance createOldSubstance() {
        Substance oldSubstance = new Substance();
        Name oldName1 = new Name();
        Name oldName2 = new Name();
        Name oldName3 = new Name();
        Name oldName4 = new Name();
        Name oldName5 = new Name();
        Name oldName6 = new Name();
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
        oldSubstance.names.add(oldName6);
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
        log.info("Testing testCanDeleteTag");
        Substance oldSubstance = this.createOldSubstance();
        oldSubstance.removeTagString("VANDF");
    }

    @Test
    void testMiscTags() throws Exception {
        log.info("Testing testMiscTags");
        Substance oldSubstance = this.createOldSubstance();
        Substance newSubstance = this.createNewSubstance();
        List<String> oldTagTermsFromNames = oldSubstance.extractTagTermsFromNames();
        List<String> newTagTermsFromNames = newSubstance.extractTagTermsFromNames();

        log.info(oldTagTermsFromNames.toString());
        log.info(newTagTermsFromNames.toString());

        List<String> oldTags = oldSubstance.grabTagTerms();
        List<String> newTags = newSubstance.grabTagTerms();

        List<String> addedTags = new ArrayList<>((CollectionUtils.removeAll(newTags, oldTags)));
        List<String> deletedTags = new ArrayList<>((CollectionUtils.removeAll(oldTags, newTags)));

        log.info("Added/Deleted tagTerms from tags object");
        log.info(addedTags.toString());
        log.info(deletedTags.toString());

    }

    @Test
    void testExtractTagTermFromName() throws Exception {
        log.info("Testing testExtractTagTermFromName");
        assert (Name.extractTagTermFromName("ABC [USP]").equals("USP"));
        assert (Name.extractTagTermFromName("ABC [USP    ]").equals("USP    "));
        assert (Name.extractTagTermFromName("ABC [USP]    ").equals("USP"));
        Assertions.assertNull(Name.extractTagTermFromName("ABC"));
        Assertions.assertNull(Name.extractTagTermFromName("ABC USP]"));
        Assertions.assertNull(Name.extractTagTermFromName("[USP] ABC"));
    }

    @Test
    void testCompareTagTermsToNamesTagTermsOldSubstance() throws Exception {
        log.info("Testing testCompareTagTermsToNamesTagTermsOldSubstance");
        Substance oldSubstance = this.createOldSubstance();
        assertEquals(
                oldSubstance.compareTagTermsInNamesMissingFromTags(
                        oldSubstance.extractTagTermsFromNames(),
                        oldSubstance.grabTagTerms()
                ),
                Arrays.asList()
        );
        assertEquals(
                oldSubstance.compareTagTermsInTagsMissingFromNames(
                        oldSubstance.grabTagTerms(),
                        oldSubstance.extractTagTermsFromNames()
                ),
                Arrays.asList()
        );

    }

    @Test
    void testCompareTagTermsToNamesTagTermsNewSubstance() throws Exception {
        log.info("Testing compareTagTermsToNamesTagTermsOldSubstance");
        Substance newSubstance = this.createNewSubstance();
        assertEquals(
                newSubstance.compareTagTermsInNamesMissingFromTags(
                        newSubstance.extractTagTermsFromNames(),
                        newSubstance.grabTagTerms()
                ),
                Arrays.asList()
        );
        assertEquals(
                newSubstance.compareTagTermsInTagsMissingFromNames(
                        newSubstance.grabTagTerms(),
                        newSubstance.extractTagTermsFromNames()
                ),
                Arrays.asList("INN")
        );

    }

    @Test
    void testCompareTagTermsToNamesTagTermsOldSubstanceIfNamesEmpty() throws Exception {
        log.info("Testing testCompareTagTermsToNamesTagTermsOldSubstanceIfNamesNull");
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
                oldSubstance.compareTagTermsInNamesMissingFromTags(
                        oldSubstance.extractTagTermsFromNames(),
                        oldSubstance.grabTagTerms()
                ),
                Arrays.asList()
        );
        assertEquals(
                oldSubstance.compareTagTermsInTagsMissingFromNames(
                        oldSubstance.grabTagTerms(),
                        oldSubstance.extractTagTermsFromNames()
                ),
                Arrays.asList("USP", "VANDF")
        );

    }

    @Test
    void testCompareTagTermsToNamesTagTermsOldSubstanceIfTagsEmpty() throws Exception {
        log.info("Testing testCompareTagTermsToNamesTagTermsOldSubstanceIfNamesNull");
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
                oldSubstance.compareTagTermsInNamesMissingFromTags(
                        oldSubstance.extractTagTermsFromNames(),
                        oldSubstance.grabTagTerms()
                ),
                Arrays.asList("USP", "VANDF")
        );
        assertEquals(
                oldSubstance.compareTagTermsInTagsMissingFromNames(
                        oldSubstance.grabTagTerms(),
                        oldSubstance.extractTagTermsFromNames()
                ),
                Arrays.asList()
        );

    }


}