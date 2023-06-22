package example.substance.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import example.GsrsModuleSubstanceApplication;
import gsrs.cache.GsrsCache;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.service.GsrsEntityService;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.models.Keyword;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.StandardNameDuplicateValidator;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
public class StandardNameDuplicateValidatorTest extends AbstractSubstanceJpaFullStackEntityTest {

        @Autowired
        private SubstanceRepository substanceRepository;

        @Autowired
        private SubstanceLegacySearchService searchService;

        @Autowired
        private TestIndexValueMakerFactory testIndexValueMakerFactory;

        @Autowired
        private TestGsrsValidatorFactory factory;

        @Autowired
        private GsrsCache cache;

        
        @BeforeEach
        public void clearIndexers() throws IOException {
        	ValidatorConfig config = new DefaultValidatorConfig();
        	config.setNewObjClass(Substance.class);
        	factory.addValidator("substances", config);
        }

        @Test
        public void testCheckStdNameForDuplicateInOtherRecordsViaIndexer() {
                StandardNameDuplicateValidator validator = AutowireHelper.getInstance().autowireAndProxy(new StandardNameDuplicateValidator());

                String template = "{\"uuid\": \"__UUID__\", \"substanceClass\": \"concept\", \"names\": [{\"name\": \"__NAME__\", \"stdName\": \"__STDNAME1__\", \"references\": [\"__REFERENCE_ID1__\"]}], \"references\": [{\"uuid\": \"__REFERENCE_ID1__\", \"citation\": \"Some Citatation __NAME1__\", \"docType\": \"WEBSITE\", \"publicDomain\": true}], \"access\": [\"protected\"]}";

                Substance s1 = null;
                Substance s2 = null;

                String j1 = template;
                String subId_a = "25cc4754-ccf1-4db2-bb6a-367581fa17ea";
                String refId1_a = "a7dcc059-7f47-4815-8444-2157381b8f17";
                String name1_a = "Test1";
                String stdName1_a = "Test1 Std";
                j1 = j1.replaceAll("__UUID__", subId_a);
                j1 = j1.replaceAll("__REFERENCE_ID1__", refId1_a);
                j1 = j1.replaceAll("__NAME1__", name1_a);
                j1 = j1.replaceAll("__STDNAME1__", stdName1_a);

                String j2 = template;
                String subId_b = "72c9ee92-8e97-4a19-b208-faf7739ad60d";
                String refId1_b = "b7dcc059-7f47-4815-8444-2157381b8f18";
                String name1_b = "Test2";
                String stdName1_b = "Test1 Std";  // The 1 is on purpose
                j2 = j2.replaceAll("__UUID__", subId_b);
                j2 = j2.replaceAll("__REFERENCE_ID1__", refId1_b);
                j2 = j2.replaceAll("__NAME1__", name1_b);
                j2 = j2.replaceAll("__STDNAME1__", stdName1_b);


                s1 = loadSubstanceFromJsonString(j1);
                s2 = loadSubstanceFromJsonString(j2);

                assertEquals(substanceRepository.count(), 2);

                List<Substance> substances1 = validator.findIndexedSubstancesByStdName(stdName1_b);
                assertEquals(substances1.size(), 2);
                Substance otherSubstance1 = validator.checkStdNameForDuplicateInOtherRecordsViaIndexer(s2, stdName1_b);
                assertEquals(otherSubstance1.getUuid(), s1.getUuid());

                Optional<Substance> substances2 = validator.findOneIndexedSubstanceByStdNameExcludingUuid(stdName1_b, s1.getUuid());
                assertTrue(substances2.isPresent());
                Substance otherSubstance2 = validator.checkStdNameForDuplicateInOtherRecordsViaIndexer(s2, stdName1_b);
                assertEquals(otherSubstance2.getUuid(), s1.getUuid());

                String wontBeFound3 = "Strange thing";
                List<Substance> substances3 = validator.findIndexedSubstancesByStdName(wontBeFound3);
                assertEquals(substances3.size(), 0);
                Substance otherSubstance3 = validator.checkStdNameForDuplicateInOtherRecordsViaIndexer(s2, wontBeFound3);
                assertNull(otherSubstance3);

                String wontBeFound4 = "Strange thing";
                List<Substance> substances4 = validator.findIndexedSubstancesByStdName(wontBeFound4);
                assertEquals(substances4.size(), 0);
                Substance otherSubstance4 = validator.checkStdNameForDuplicateInOtherRecordsViaIndexer(s2, wontBeFound4);
                assertNull(otherSubstance4);


        }

        @Test
        public void testStdNameDuplicateInOtherRecordViaIndexer() {
                StandardNameDuplicateValidator validator = AutowireHelper.getInstance().autowireAndProxy(new StandardNameDuplicateValidator());
                
                validator.setCheckDuplicateInOtherRecord(true);
                validator.setOnDuplicateInOtherRecordShowError(true);

                String template = "{\"uuid\": \"__UUID__\", \"substanceClass\": \"concept\", \"names\": [{\"name\": \"__NAME__\", \"stdName\": \"__STDNAME1__\", \"references\": [\"__REFERENCE_ID1__\"]}], \"references\": [{\"uuid\": \"__REFERENCE_ID1__\", \"citation\": \"Some Citatation __NAME1__\", \"docType\": \"WEBSITE\", \"publicDomain\": true}], \"access\": [\"protected\"]}";

                Substance s1 = null;
                Substance s2 = null;

                String j1 = template;
                String subId_a = "25cc4754-ccf1-4db2-bb6a-367581fa17ea";
                String refId1_a = "a7dcc059-7f47-4815-8444-2157381b8f17";
                String name1_a = "Test1";
                String stdName1_a = "Test1 Std";
                j1 = j1.replaceAll("__UUID__", subId_a);
                j1 = j1.replaceAll("__REFERENCE_ID1__", refId1_a);
                j1 = j1.replaceAll("__NAME1__", name1_a);
                j1 = j1.replaceAll("__STDNAME1__", stdName1_a);

                String j2 = template;
                String subId_b = "72c9ee92-8e97-4a19-b208-faf7739ad60d";
                String refId1_b = "b7dcc059-7f47-4815-8444-2157381b8f18";
                String name1_b = "Test2";
                String stdName1_b = "Test1 Std";  // The 1 is on purpose
                j2 = j2.replaceAll("__UUID__", subId_b);
                j2 = j2.replaceAll("__REFERENCE_ID1__", refId1_b);
                j2 = j2.replaceAll("__NAME1__", name1_b);
                j2 = j2.replaceAll("__STDNAME1__", stdName1_b);

                s1 = loadSubstanceFromJsonString(j1);
                s2 = loadSubstanceFromJsonString(j2);

                assertEquals(substanceRepository.count(), 2);

                ValidationResponse<Substance> response = validator.validate(s2, null);

                boolean found = response.getValidationMessages().stream().
                anyMatch(vm->vm.getMessageType().toString().equals("ERROR") && vm.getMessage().contains(validator.getDUPLICATE_IN_OTHER_RECORD_MESSAGE_TEST_FRAGMENT()));
                Assertions.assertTrue(found);
        }
        

        @Test
        public void testStdNameDuplicateInOtherRecordViaIndexerWhenNameHasWildcard() {
                StandardNameDuplicateValidator validator = AutowireHelper.getInstance().autowireAndProxy(new StandardNameDuplicateValidator());
                
                validator.setCheckDuplicateInOtherRecord(true);
                validator.setOnDuplicateInOtherRecordShowError(true);

                
                UUID uuid1 = UUID.randomUUID();
                UUID uuid2 = UUID.randomUUID();
                
                SubstanceBuilder substanceBuilder1 = new SubstanceBuilder()
                		.addName("TEST* ABC", n->n.stdName="TEST* ABC")
                		.setUUID(uuid1);
                assertCreated(substanceBuilder1.buildJson());
                
                SubstanceBuilder substanceBuilder2 = new SubstanceBuilder()
                		.addName("TEST2* ABC", n->n.stdName="TEST2* ABC")
                		.setUUID(uuid2);
                assertCreated(substanceBuilder2.buildJson());
                
              
                assertEquals(substanceRepository.count(), 2);

                ValidationResponse<Substance> response = validator.validate(substanceBuilder1.build(), null);

                boolean found = response.getValidationMessages().stream().
                anyMatch(vm->vm.getMessageType().toString().equals("ERROR"));
                Assertions.assertFalse(found);
        }


        @Test
        public void testStdNameDuplicateInOtherRecordViaIndexerWhenNameHasWildcardDuplicate() {
                StandardNameDuplicateValidator validator = AutowireHelper.getInstance().autowireAndProxy(new StandardNameDuplicateValidator());
                
                validator.setCheckDuplicateInOtherRecord(true);
                validator.setOnDuplicateInOtherRecordShowError(true);

                
                UUID uuid1 = UUID.randomUUID();
                UUID uuid2 = UUID.randomUUID();
                
                SubstanceBuilder substanceBuilder1 = new SubstanceBuilder()
                		.addName("TEST* ABC", n->n.stdName="TEST* ABC")
                		.setUUID(uuid1);
                assertCreated(substanceBuilder1.buildJson());
                
                SubstanceBuilder substanceBuilder2 = new SubstanceBuilder()
                		.addName("TEST2* ABC", n->n.stdName="TEST* ABC")
                		.setUUID(uuid2);
                assertCreated(substanceBuilder2.buildJson());
                
              
                assertEquals(substanceRepository.count(), 2);

                ValidationResponse<Substance> response = validator.validate(substanceBuilder1.build(), null);

                boolean found = response.getValidationMessages().stream().
                anyMatch(vm->vm.getMessageType().toString().equals("ERROR"));
                Assertions.assertTrue(found);
        }
        
        @Test
        public void testStdNameDuplicateInOtherRecordViaIndexerWhenNameHasWildcardDuplicateQuestionMark() {
                StandardNameDuplicateValidator validator = AutowireHelper.getInstance().autowireAndProxy(new StandardNameDuplicateValidator());
                
                validator.setCheckDuplicateInOtherRecord(true);
                validator.setOnDuplicateInOtherRecordShowError(true);

                
                UUID uuid1 = UUID.randomUUID();
                UUID uuid2 = UUID.randomUUID();
                
                SubstanceBuilder substanceBuilder1 = new SubstanceBuilder()
                		.addName("TEST* ABC", n->n.stdName="TEST? ABC")
                		.setUUID(uuid1);
                assertCreated(substanceBuilder1.buildJson());
                
                SubstanceBuilder substanceBuilder2 = new SubstanceBuilder()
                		.addName("TEST2* ABC", n->n.stdName="TEST? ABC")
                		.setUUID(uuid2);
                assertCreated(substanceBuilder2.buildJson());
                
              
                assertEquals(substanceRepository.count(), 2);

                ValidationResponse<Substance> response = validator.validate(substanceBuilder1.build(), null);

                boolean found = response.getValidationMessages().stream().
                anyMatch(vm->vm.getMessageType().toString().equals("ERROR"));
                Assertions.assertTrue(found);
        }

        public Substance loadSubstanceFromJsonString(String jsonText) {
                Substance substance = null;
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = null;
                try {
                        json = mapper.readTree(jsonText);
                } catch (Exception ex) {
                        ex.printStackTrace();
                }
                try {
                        GsrsEntityService.CreationResult<Substance> result= substanceEntityService.createEntity(json, true);
                        substance = result.getCreatedEntity();
                } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                }
                return substance;
        }

        @Test
        public void testDuplicateInSameRecordShowWarning() {
                boolean showError = false;
                StandardNameDuplicateValidator validator = AutowireHelper.getInstance().autowireAndProxy(new StandardNameDuplicateValidator());
                validator.setCheckDuplicateInSameRecord(true);
//                validator.setCheckDuplicateInOtherRecord(false);
                validator.setOnDuplicateInSameRecordShowError(showError);
                Substance s1 = new Substance();
                Name name1 = new Name();
                name1.name ="TestSame1";
                name1.stdName ="TestSame1 std";
                if (name1.languages == null) {
                        name1.languages = new EmbeddedKeywordList();
                }
                name1.languages.add(new Keyword("en"));
                name1.languages.add(new Keyword("fr"));
                s1.names.add(name1);
                Name name2 = new Name();
                name2.name ="TestSame2";
                name2.stdName ="TestSame1 std";  // 1 on purpose
                if (name1.languages == null) {
                        name1.languages = new EmbeddedKeywordList();
                }
                name2.languages.add(new Keyword("en"));
                name2.languages.add(new Keyword("fr"));
                s1.names.add(name2);
                substanceRepository.saveAndFlush(s1);
                cache.clearCache();
                ValidationResponse<Substance> response = validator.validate(s1, null);
                boolean found = response.getValidationMessages().stream().
                        anyMatch(vm->vm.getMessageType().toString().equals("WARNING") && vm.getMessage().contains(validator.getDUPLICATE_IN_SAME_RECORD_MESSAGE_TEST_FRAGMENT()));

                Assertions.assertTrue(found);
        }

        @Test
        public void testDuplicateInSameRecordShowError() {
                boolean showError = true;
                StandardNameDuplicateValidator validator = AutowireHelper.getInstance().autowireAndProxy(new StandardNameDuplicateValidator());
                validator.setCheckDuplicateInSameRecord(true);
//                validator.setCheckDuplicateInOtherRecord(false);
                validator.setOnDuplicateInSameRecordShowError(showError);
                Substance s1 = new Substance();
                Name name1 = new Name();
                name1.name ="TestSame1";
                name1.stdName ="TestSame1 std";
                if (name1.languages == null) {
                        name1.languages = new EmbeddedKeywordList();
                }
                name1.languages.add(new Keyword("en"));
                name1.languages.add(new Keyword("fr"));
                s1.names.add(name1);
                Name name2 = new Name();
                name2.name ="TestSame2";
                name2.stdName ="TestSame1 std";  // 1 on purpose
                if (name1.languages == null) {
                        name1.languages = new EmbeddedKeywordList();
                }
                name2.languages.add(new Keyword("en"));
                name2.languages.add(new Keyword("fr"));
                s1.names.add(name2);
                substanceRepository.saveAndFlush(s1);
                cache.clearCache();
                ValidationResponse<Substance> response = validator.validate(s1, null);
                boolean found = response.getValidationMessages().stream().
                anyMatch(vm->vm.getMessageType().toString().equals("ERROR") && vm.getMessage().contains(validator.getDUPLICATE_IN_SAME_RECORD_MESSAGE_TEST_FRAGMENT()));

                Assertions.assertTrue(found);
        }

}
