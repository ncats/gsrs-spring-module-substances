package example.substance.datasearch;

import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.legacy.structureIndexer.StructureIndexerService;
import gsrs.module.substance.controllers.ReIndexController;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.definitional.DefinitionalElements;
import gsrs.module.substance.services.DefinitionalElementFactory;
import ix.core.search.SearchOptions;
import ix.core.search.SearchRequest;
import ix.core.search.SearchRequest.Builder;
import ix.core.search.SearchResult;
import ix.ginas.modelBuilders.SubstanceBuilder;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import java.util.Arrays;
import org.junit.Before;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 * @author mitch
 */
//Changed base clas from AbstractSubstanceJpaFullStackEntityTest to AbstractSubstanceJpaEntityTest
// 16 July based on recommdendtion from Danny K.
@WithMockUser(username = "admin", roles = "Admin")
public class DataSearchTests extends AbstractSubstanceJpaFullStackEntityTest
{

    @Autowired
    private StructureIndexerService indexer;

    @Autowired
    private SubstanceLegacySearchService searchService;

    @Autowired
    private DefinitionalElementFactory definitionalElementFactory;

    private static boolean setup = false;

    @BeforeEach
    public void loadSearchData() throws IOException {
        //if (!setup) {
            File dataFile = new ClassPathResource("testdumps/rep90.ginas").getFile();
            loadGsrsFile(dataFile);
            System.out.println("loaded rep90 data file");
            setup = true;
        /*} else {
            System.out.println("skipped loading file");
        }*/
    }

    @Test
    public void testSearchByName() {
        String name1 = "TRANSFERRIN ALDIFITOX R EPIMER";
        TransactionTemplate transactionSearch = new TransactionTemplate(transactionManager);
        List<String> nameValues = transactionSearch.execute(ts -> {

            SearchRequest request = new SearchRequest.Builder()
                    .kind(Substance.class)
                    .fdim(0)
                    .query("root_names_name:\"" + name1 + "\"")
                    .top(Integer.MAX_VALUE)
                    .build();
            try {
                SearchResult sr = searchService.search(request.getQuery(), request.getOptions());
                sr.waitForFinish();

                List futureList = sr.getMatches();
                Stream<String> stream = futureList
                        .stream()
                        .flatMap(sub -> {
                            Substance ps = (Substance) sub;
                            return ps.names.stream()
                                    .map(n -> n.name);
                        });
                return stream.collect(Collectors.toList());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

        }
        );

        assertEquals(Arrays.asList(name1), nameValues);
    }

    @Test
    public void testSearchByCode() {
        String code1 = "37477";
        String requiredCodeSystem="ITIS";
        TransactionTemplate transactionSearch = new TransactionTemplate(transactionManager);
        List<String> codeValues = transactionSearch.execute(ts -> {

            SearchRequest request = new SearchRequest.Builder()
                    .kind(Substance.class)
                    .fdim(0)
                    .query("root_codes_ITIS:\"" + code1 + "\"")
                    .top(Integer.MAX_VALUE)
                    .build();
            try {
                SearchResult sr = searchService.search(request.getQuery(), request.getOptions());
                sr.waitForFinish();

                List futureList = sr.getMatches();
                Stream<String> stream = futureList
                        .stream()
                        .flatMap(sub -> {
                            Substance ps = (Substance) sub;
                            return ps.codes.stream()
                                    .filter(c->c.codeSystem.equals(requiredCodeSystem))
                                    .map(n -> n.code);
                        });
                return stream.collect(Collectors.toList());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

        }
        );

        assertEquals(Arrays.asList(code1), codeValues);
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

    @Test
    public void testDuplicates() {
        Substance chemical = getSampleChemicalFromFile();
        List<Substance> matches = findFullDefinitionalDuplicateCandidates(chemical);
        assertTrue(matches.size() > 0, "must find some duplicates");
    }

    public List<Substance> findFullDefinitionalDuplicateCandidates(Substance substance) {
        List<Substance> candidates = new ArrayList<>();
        try {
            Builder searchBuilder = new SearchRequest.Builder();
            DefinitionalElements newDefinitionalElements = definitionalElementFactory.computeDefinitionalElementsFor(substance);
            //List<String> hashes= substance.getDefinitionalElements().getDefinitionalHashLayers();
            int layer = newDefinitionalElements.getDefinitionalHashLayers().size() - 1; // hashes.size()-1;
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, "handling layer: " + (layer + 1));
            String searchItem = "root_definitional_hash_layer_" + (layer + 1) + ":"
                    + newDefinitionalElements.getDefinitionalHashLayers().get(layer);
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, "layer query: " + searchItem);
            //searchBuilder = searchBuilder.query(searchItem);

            TransactionTemplate transactionSearch = new TransactionTemplate(transactionManager);
            List<String> nameValues = (List<String>) transactionSearch.execute(new TransactionCallback()
            {
                @Override
                public Object doInTransaction(TransactionStatus ts) {
                    List<String> nameValues = new ArrayList<>();
                    SearchRequest request = new SearchRequest.Builder()
                            .kind(Substance.class)
                            .fdim(0)
                            .query(searchItem)
                            .top(Integer.MAX_VALUE)
                            .build();
                    System.out.println("built query: " + request.getQuery());
                    try {
                        SearchOptions options = new SearchOptions();
                        SearchResult sr = searchService.search(request.getQuery(), options);
                        Future<List> fut = sr.getMatchesFuture();
                        Stream<String> names = fut.get(30_000, TimeUnit.MILLISECONDS)
                                .stream()
                                .peek(i -> System.out.println("item type: " + i.getClass().getName()))
                                .map(s -> (Substance) s)
                                .flatMap(sub -> {
                                    Substance ps = (Substance) sub;
                                    candidates.add(ps);
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
            nameValues.forEach(n -> System.out.println(n));
            //        });
            //
            //			SearchResult sres = searchBuilder
            //							.kind(Substance.class)
            //							.fdim(0)
            //							.build()
            //                    .
            //							.execute();
            //			sres.waitForFinish();
            /*List<Substance> submatches = (List<Substance>) sres.getMatches();
			Logger.getLogger(this.getClass().getName()).log(Level.FINE, "total submatches: " + submatches.size());

			for (int i = 0; i < submatches.size(); i++)	{
				Substance s = submatches.get(i);
				if (!s.getUuid().equals(substance.getUuid()))	{
					candidates.add(s);
				}
			}*/
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error running query", ex);
        }
        return candidates;
    }

    private Substance getSampleChemicalFromFile() {
        try {
            File chemicalFile = new ClassPathResource("testJSON/editChemical.json").getFile();
            ChemicalSubstanceBuilder builder = SubstanceBuilder.from(chemicalFile);
            System.out.println("first name of read-in chem: " + builder.build().names.get(0).name);
            return builder.build();
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
