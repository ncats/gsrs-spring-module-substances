package example.substance.datasearch;

import example.substance.AbstractSubstanceJpaEntityTest;
import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.legacy.structureIndexer.StructureIndexerService;
import gsrs.module.substance.controllers.ReIndexController;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.services.PrincipalServiceImpl;
import ix.core.search.SearchOptions;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.ginas.models.v1.Substance;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

/**
 *
 * @author mitch
 */

//Changed base clas from AbstractSubstanceJpaFullStackEntityTest to AbstractSubstanceJpaEntityTest
// 16 July based on recommdendtion from Danny K.
@WithMockUser(username = "admin", roles = "Admin")
public class DataSearchTests extends AbstractSubstanceJpaEntityTest
{

//    @Autowired
//    private SubstanceEntityService substanceEntityService;
    @Autowired
    private PrincipalServiceImpl principalService;

    @Autowired
    private StructureIndexerService indexer;

    @Autowired
    private SubstanceLegacySearchService searchService;

    private boolean setup = false;

    @BeforeEach
    public void clearIndexers() throws IOException {
        System.out.println("clearIndexers");
        if (!setup) {
            try {
                indexer.removeAll();
            } catch (Exception ex) {
                System.err.println("error during indexer.removeAll");
                ex.printStackTrace();
            }
            try {
                principalService.clearCache();
            } catch (Exception ex) {
                System.err.println("error during principalService.clearCache");
                ex.printStackTrace();
            }

            File dataFile = new ClassPathResource("testdumps/rep90.ginas").getFile();
            loadGsrsFile(dataFile);
            setup = true;
            //reindexDirect();
            System.out.println("loaded rep90 data file");
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void testSearchByName() throws Exception {
        String name1 = "TRANSFERRIN ALDIFITOX R EPIMER";
        String q = "root_name s_name:" + name1;
        //List<String> nameValues = new ArrayList<>();
        TransactionTemplate transactionSearch = new TransactionTemplate(transactionManager);
        List<String> nameValues = (List<String>) transactionSearch.execute(new TransactionCallback()
        {
            @Override
            public Object doInTransaction(TransactionStatus ts) {
                List<String> nameValues = new ArrayList<>();
                SearchRequest request = new SearchRequest.Builder()
                        .kind(Substance.class)
                        .fdim(0)
                        .query(q)
                        .top(Integer.MAX_VALUE)
                        .build();
                System.out.println("built query: " + request.getQuery());
                try {
                    SearchOptions options = new SearchOptions();
                    SearchResult sr = searchService.search(request.getQuery(), options);
                    Future<List> fut = sr.getMatchesFuture();
                    Stream<String> names = fut.get(30_000, TimeUnit.MILLISECONDS)
                            .stream()
                            .map(s -> (Substance) s)
                            .flatMap(sub -> {
                                Substance ps = (Substance) sub;
                                return ps.names.stream()
                                        .map(n -> n.name);
                            });
                    nameValues = names.collect(Collectors.toList());
                } catch (Exception ex) {
                    System.err.println("error during search");
                    ex.printStackTrace();
                }
                return nameValues;
            }
        });

        System.out.println(
                "total names: " + nameValues.size());
        nameValues.forEach(n
                -> System.out.println(n));

        assertTrue(nameValues.size() > 0);
    }

    private void reindexDirect() {
        ReIndexController controller = new ReIndexController();
        Map<String, String> parms = new HashMap<>();
        parms.put("view", "full");
        try {
            controller.reindex(parms);
        } catch (IOException ex) {
            Logger.getLogger(DataSearchTests.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
