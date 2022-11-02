package example.substance.search.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;

import example.GsrsModuleSubstanceApplication;
import gsrs.cache.GsrsCache;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.services.DefinitionalElementFactory;
import gsrs.module.substance.utils.SubstanceMatchViewGenerator;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.chem.StructureProcessor;
import ix.core.search.SearchOptions;
import ix.core.search.SearchResultContext;
import ix.core.search.SearchResultContext.Status;
import ix.core.search.bulk.BulkSearchService;
import ix.core.search.bulk.BulkSearchService.BulkQuerySummary;
import ix.core.search.bulk.BulkSearchService.SanitizedBulkSearchRequest;
import ix.core.search.bulk.MatchView;
import ix.core.search.bulk.SearchResultSummaryRecord;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.ChemicalValidator;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
public class BulkSearchTests extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;
    
	@Autowired
	private TextIndexerFactory textIndexerFactory;	
	
	@Autowired
	private BulkSearchService bulkSearchService;	
	
	@Autowired
	private SubstanceMatchViewGenerator generator;

    @Autowired
    private TestGsrsValidatorFactory factory;
    
	@Autowired
	private SubstanceRepository gsrsRepository;
	
	@Autowired
	private GsrsCache cache;

    private String fileName = "rep18.gsrs";
    
    private TextIndexer indexer;	
	
	private SearchOptions options;
    
    private void setupSearchOptions(Boolean searchOnIdentifier) {
		options = new SearchOptions();
		options.setTop(10);
		options.setSkip(0);
		options.setKind(Substance.class);
		options.setBulkSearchOnIdentifiers(false);
		options.setDefaultField(TextIndexer.FULL_IDENTIFIER_FIELD);
	}

    @BeforeEach
    public void clearIndexers() throws IOException {
        SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
        AutowireHelper.getInstance().autowire(hashIndexer);
        testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);
        {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(ChemicalValidator.class);
            config.setNewObjClass(ChemicalSubstance.class);
            factory.addValidator("substances", config);
        }

        File dataFile = new ClassPathResource(fileName).getFile();
        loadGsrsFile(dataFile);
        
    }

    @Test
    public void testBulkSearchWithNonIdenfifiers() throws IOException, InterruptedException, ExecutionException {
    	
    	setupSearchOptions(false);    	
    	List<String> queries = Arrays.asList("SODIUM*","CHLORIDE","THIOFLAVIN","test",
    			"root_names_name:\"^Sodium Gluconate$\"", "root_codes_code:\"^7447-40-7$\"");
		SanitizedBulkSearchRequest request = createRequest(queries);				
		indexer = textIndexerFactory.getDefaultInstance();		
			
		SearchResultContext searchContent = bulkSearchService.search(gsrsRepository, request, options, indexer, generator);
		
		searchContent.getDeterminedFuture().get();			
		assertEquals(searchContent.getResultsAsList().size(),9);
			
		
		BulkQuerySummary summary = (BulkQuerySummary)cache.getRaw("BulkSearchSummary/"+request.computeKey(false));
			
		
		List<String> queriesInResult = summary.getQueries().stream().map(r->r.getSearchTerm()).collect(Collectors.toList());		
		assertEquals(queries.size(),queriesInResult.size());
		assertTrue(queriesInResult.contains("SODIUM*"));
		assertTrue(queriesInResult.contains("CHLORIDE"));
		assertTrue(queriesInResult.contains("THIOFLAVIN"));
		assertTrue(queriesInResult.contains("test"));
		assertTrue(queriesInResult.contains("root_names_name:\"^Sodium Gluconate$\""));
		assertTrue(queriesInResult.contains("root_codes_code:\"^7447-40-7$\""));
		
		for(SearchResultSummaryRecord record: summary.getQueries()) {
			String query = record.getSearchTerm();			
			List<MatchView> items = record.getRecords();			
			if(query.equals("test")) {
				assertTrue(items.size()==0);
			}else {
				assertTrue(items.size()>0);
			}			
		}		
    }
    
    
	@Test
	public void testBulkSearchWithIdenfifiers() throws IOException, InterruptedException, ExecutionException {
		
		setupSearchOptions(true);
		List<String> queries = Arrays.asList("SODIUM ACETATE","POTASSIUM CHLORIDE","THIOFLAVIN S2");
		SanitizedBulkSearchRequest request = createRequest(queries);
		indexer = textIndexerFactory.getDefaultInstance();	
		SearchResultContext searchContent = bulkSearchService.search(gsrsRepository, request, options,indexer, generator);
		searchContent.getDeterminedFuture().get();
		
		assertEquals(searchContent.getResultsAsList().size(),3);
		
		BulkQuerySummary summary = (BulkQuerySummary)cache.getRaw("BulkSearchSummary/"+request.computeKey(false));
		List<String> queriesInResult = summary.getQueries().stream().map(r->r.getSearchTerm()).collect(Collectors.toList());
		
		assertEquals(queries.size(),queriesInResult.size());
		assertTrue(queriesInResult.contains("SODIUM ACETATE"));
		assertTrue(queriesInResult.contains("POTASSIUM CHLORIDE"));
		assertTrue(queriesInResult.contains("THIOFLAVIN S2"));
			
	}
    
    private SanitizedBulkSearchRequest createRequest(List<String> queries) {
		SanitizedBulkSearchRequest request = new SanitizedBulkSearchRequest();
		request.setQueries(queries);
		return request;		
	}	
    
}
