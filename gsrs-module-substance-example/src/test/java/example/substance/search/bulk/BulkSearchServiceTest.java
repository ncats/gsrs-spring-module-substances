package example.substance.search.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;

import example.GsrsModuleSubstanceApplication;
import gsrs.cache.GsrsCache;
import gsrs.repository.GsrsRepository;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.search.SearchOptions;
import ix.core.search.SearchResultContext;
import ix.core.search.bulk.BulkSearchService;
import ix.core.search.bulk.BulkSearchService.SanitizedBulkSearchRequest;
import ix.core.search.bulk.MatchViewGenerator;
import ix.core.search.bulk.SearchResultSummaryRecord;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;


@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
public class BulkSearchServiceTest extends AbstractSubstanceJpaFullStackEntityTest{
	
	@Autowired
	private GsrsRepository gsrsRepository;
	
	@Autowired
	private GsrsCache cache;
	
	@Autowired
	private MatchViewGenerator generator;
	
	@Autowired
	private BulkSearchService bulkSearchService;
	
	private TextIndexer indexer;	
	
	private SearchOptions options;
	
		
	private void setupSearchOptions() {
		options = new SearchOptions();
		options.setTop(10);
		options.setSkip(0);
	}
	
	private void loadData() throws IOException {
		loadGsrsFile(new ClassPathResource("rep18.gsrs"));		
	}
	
	@Before
	public void setup() throws IOException {
		loadData();
		setupSearchOptions();		
		
	}
	
	
	@After
	public void cleanup() {
		
	}
		
	
	@Test
	public void testBulkSearchWithIdentifierOnlyQueries() throws IOException {
		
		TextIndexerFactory textIndexFactory = new TextIndexerFactory();
        AutowireHelper.getInstance().autowire(textIndexFactory);
		
		List<String> queries = Arrays.asList("SODIUM","CHLORIDE","THIOFLAVIN","test");
		SanitizedBulkSearchRequest request = createRequest(queries);
		indexer = textIndexFactory.getDefaultInstance();		
		SearchResultContext searchContent = bulkSearchService.search(gsrsRepository, request, options,indexer, generator);
		//Maybe check keys
		assertEquals(searchContent.getResultsAsList().size(),queries.size());
		
		List<SearchResultSummaryRecord> summary = (List<SearchResultSummaryRecord>)cache.getRaw("BulkSearchSummary/"+request.computeKey());
		List<String> queriesInResult = summary.stream().map(r->r.getSearchTerm()).collect(Collectors.toList());
		assertEquals(queries.size(),queriesInResult.size());
		queriesInResult.removeAll(queries);
		assertEquals(queries.size(),0);
		
		for(SearchResultSummaryRecord record: summary) {
			String query = record.getSearchTerm();
			List<String> ids = record.getRecords().stream().map(m->m.getId()).collect(Collectors.toList());	
			if(query.equals("test")) {
				assertTrue(ids.contains(""));
			}			
		}		
	}
	
	
//	@Test
//	public void testBulkSearchWithComplexQueries() throws IOException {
//		List<String> queries = Arrays.asList("queryString1","test","queryString3");
//		SanitizedBulkSearchRequest request = createRequest(queries);
//		SearchResultContext searchContent = bulkSearchService.search(gsrsRepository, request, options,indexer, generator);
//		
//	}
//	
//
//	@Test
//	public void testBulkSearchWithEmptySearchResult() throws IOException {
//		List<String> queries = Arrays.asList("queryString1","test","queryString3");
//		SanitizedBulkSearchRequest request = createRequest(queries);
//		SearchResultContext searchContent = bulkSearchService.search(gsrsRepository, request, options,indexer, generator);
//		
//	}
	
	private SanitizedBulkSearchRequest createRequest(List<String> queries) {
		SanitizedBulkSearchRequest request = new SanitizedBulkSearchRequest();
		request.setQueries(queries);
		return request;		
	}	
}
