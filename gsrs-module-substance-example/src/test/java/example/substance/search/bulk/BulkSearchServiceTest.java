package example.substance.search.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import example.GsrsModuleSubstanceApplication;
import gsrs.cache.GsrsCache;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.repository.GsrsRepository;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.search.SearchOptions;
import ix.core.search.SearchResultContext;
import ix.core.search.bulk.BulkSearchService;
import ix.core.search.bulk.BulkSearchService.SanitizedBulkSearchRequest;
import ix.core.search.bulk.MatchViewGenerator;
import ix.core.search.bulk.SearchResultSummaryRecord;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.utils.validation.validators.ChemicalValidator;

@ActiveProfiles("test")
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
public class BulkSearchServiceTest extends AbstractSubstanceJpaFullStackEntityTest{
//	
//	@Autowired
//    private TestIndexValueMakerFactory testIndexValueMakerFactory;
//   
//    @Autowired
//    private TestGsrsValidatorFactory factory;
//	
//	@Autowired
//	private GsrsRepository gsrsRepository;
//	
//	@Autowired
//	private GsrsCache cache;
//	
//	@Autowired
//	private MatchViewGenerator generator;
//	
//	@Autowired
//	private BulkSearchService bulkSearchService;	
//	
//	@Autowired
//	private TextIndexerFactory textIndexerFactory;	
//	
//	private TextIndexer indexer;	
//	
//	private SearchOptions options;
//		
//		
//	private void setupSearchOptions() {
//		options = new SearchOptions();
//		options.setTop(10);
//		options.setSkip(0);
//	}
//	
//	@BeforeEach
//    public void setup() throws IOException {		
//		
////        SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
////        AutowireHelper.getInstance().autowire(hashIndexer);
////        testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);
////        {
////            ValidatorConfig config = new DefaultValidatorConfig();
////            config.setValidatorClass(ChemicalValidator.class);
////            config.setNewObjClass(ChemicalSubstance.class);
////            factory.addValidator("substances", config);
////        }
//
//        File dataFile = new ClassPathResource("rep18.gsrs").getFile();
//        loadGsrsFile(dataFile);
//        
//        setupSearchOptions();
//        
//        
//    }
//
//	
//	@AfterEach
//	public void cleanup() {
//		
//	}
//		
//	
//	@Test
//	public void testBulkSearchWithIdentifierOnlyQueries() throws IOException {
//		
//		
//		List<String> queries = Arrays.asList("SODIUM","CHLORIDE","THIOFLAVIN","test");
//		SanitizedBulkSearchRequest request = createRequest(queries);
//		
//		bulkSearchService = new BulkSearchService();			
//		
//		bulkSearchService.setTransactionManager(transactionManager);	
//		
//		textIndexerFactory = new TextIndexerFactory();
//		if(textIndexerFactory==null) {			
//			System.out.println("textIndexFactory is null");
//		}		
//		
//		indexer = textIndexerFactory.getDefaultInstance();
//		
//		if(indexer==null) {			
//			System.out.println("indexer is null");
//		}	
//			
//		SearchResultContext searchContent = bulkSearchService.search(gsrsRepository, request, options, indexer, generator);
//			
//		assertEquals(searchContent.getResultsAsList().size(),queries.size());
//		
//		List<SearchResultSummaryRecord> summary = (List<SearchResultSummaryRecord>)cache.getRaw("BulkSearchSummary/"+request.computeKey());
//		List<String> queriesInResult = summary.stream().map(r->r.getSearchTerm()).collect(Collectors.toList());
//		assertEquals(queries.size(),queriesInResult.size());
//		queriesInResult.removeAll(queries);
//		assertEquals(queries.size(),0);
//		
//		for(SearchResultSummaryRecord record: summary) {
//			String query = record.getSearchTerm();
//			List<String> ids = record.getRecords().stream().map(m->m.getId()).collect(Collectors.toList());	
//			if(query.equals("test")) {
//				assertTrue(ids.contains(""));
//			}			
//		}		
//	}
//	
//	
////	@Test
////	public void testBulkSearchWithComplexQueries() throws IOException {
////		List<String> queries = Arrays.asList("queryString1","test","queryString3");
////		SanitizedBulkSearchRequest request = createRequest(queries);
////		SearchResultContext searchContent = bulkSearchService.search(gsrsRepository, request, options,indexer, generator);
////		
////	}
////	
////
////	@Test
////	public void testBulkSearchWithEmptySearchResult() throws IOException {
////		List<String> queries = Arrays.asList("queryString1","test","queryString3");
////		SanitizedBulkSearchRequest request = createRequest(queries);
////		SearchResultContext searchContent = bulkSearchService.search(gsrsRepository, request, options,indexer, generator);
////		
////	}
//	
//	private SanitizedBulkSearchRequest createRequest(List<String> queries) {
//		SanitizedBulkSearchRequest request = new SanitizedBulkSearchRequest();
//		request.setQueries(queries);
//		return request;		
//	}	
}
