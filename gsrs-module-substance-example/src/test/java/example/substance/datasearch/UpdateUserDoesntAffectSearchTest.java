package example.substance.datasearch;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.repository.PrincipalRepository;
import gsrs.services.PrincipalService;
import gsrs.startertests.GsrsFullStackTest;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WithMockUser(username = "admin", roles = "Admin")
@GsrsFullStackTest(dirtyMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UpdateUserDoesntAffectSearchTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private SubstanceLegacySearchService searchService;

    @Autowired
    private PrincipalService principalService;

    @Autowired
    PrincipalRepository principalRepository;

    private UUID uuid = UUID.fromString("e92bc4ad-250a-4eef-8cd7-0b0b1e3b6cf0");

    private Random rand = new Random();
    @BeforeEach
    public void loadData() throws IOException {
        principalService.clearCache();
        loadGsrsFile(new ClassPathResource("rep18.gsrs"));
    }

    @Test
    @Order(2)
    public void searchAfterEditingUserShouldStillWork(){
        performSearch();
        updateAdminEmail();
        performSearch();
    }
    @Test
    public void updateRecord(){
        performSearch();
        ChemicalSubstance substance = (ChemicalSubstance) substanceEntityService.get(uuid).get();
        substance.changeReason= "test change";

        assertUpdated(substance.toFullJsonNode());


        performSearch();
    }
    @Test
    @Order(1)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void updateRecordThenEmail(){
        performSearch();
        ChemicalSubstance substance = (ChemicalSubstance) substanceEntityService.get(uuid).get();
        substance.changeReason= "test change";

        assertUpdated(substance.toFullJsonNode());


        updateAdminEmail();
        performSearch();
    }

    private void updateAdminEmail() {
        TransactionTemplate template = newTransactionTemplate();
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.executeWithoutResult(status->{
            principalRepository.findDistinctByUsernameIgnoreCase("admin").email="newEmail"+rand.nextInt()+"@example.com";
        });
    }

    private void performSearch() {
        String name1 = "THIOFLAVIN S2";
        SearchRequest sr = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(0)
                .query("root_names_name:\"" + name1 + "\"")
                .top(Integer.MAX_VALUE)
                .build();


        TransactionTemplate transactionSearch = new TransactionTemplate(transactionManager);
        transactionSearch.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        List<Substance> substances = transactionSearch.execute(ts -> {

            try {
                SearchResult sresult = searchService.search(sr.getQuery(), sr.getOptions());

                List<Substance> list = sresult.getMatches();
                return list;
                /* removing this processing which seems no longer necessary (26-September-2025)
                return list.stream()
                        //force fetching
                        .peek(ss -> EntityUtils.EntityWrapper.of(ss).toInternalJson())
                        .collect(Collectors.toList());*/
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        assertEquals(uuid, substances.stream().findFirst().get().uuid);
    }
}
