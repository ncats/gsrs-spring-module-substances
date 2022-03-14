package example.substance.indexer;

import gsrs.module.substance.indexers.BracketTermIndexValueMaker;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.models.Keyword;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.tags.TagUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for BracketTermIndexValueMaker. Ensure proper registration management of facets/values.
 */
public class BracketTermIndexValueMakerTest extends AbstractSubstanceJpaEntityTest {
	private final String TAG_FACET_NAME = "GInAS Tag";

	@Autowired
	private TestIndexValueMakerFactory factory;

	@BeforeEach
	public void init(){
		factory.setIndexValueMakers(new BracketTermIndexValueMaker());
	}

	@Test
	public void myTest() {
		Substance t = new Substance();
		t.names = new ArrayList<>();
		t.tags.add(new Keyword("USP"));
		t.tags.add(new Keyword("INN"));
		t.tags.add(new Keyword("GREEN BOOK"));
		t.names.add(new Name("ABC [USP]"));
		t.names.add(new Name("CED [USP]"));
		t.names.add(new Name("PED [INN]"));
		t.names.add(new Name("QAK [INN]"));
		t.names.add(new Name("VAD [VANDF]"));
		t.names.add(new Name("RAGDOLL [NOT][FOOT]"));
		t.names.add(new Name("SPEAK [ZEEL][SPELT]"));
        String facetName = TAG_FACET_NAME;
		Set<String> tagTerms = TagUtilities.extractBracketNameTags(t);
		IndexValueMaker<Substance> ivm = factory.createIndexValueMakerFor(t);
		List<IndexableValue> ivs = new ArrayList<IndexableValue>();
		ivm.createIndexableValues(t, c->{
			ivs.add(c);
		});
		for(Name name: t.getAllNames()) {
			for(String bracketTerm :TagUtilities.getBracketTerms(name.getName())){
				testIndexableValuesHasFacet(t, TAG_FACET_NAME, bracketTerm);
			}
		}
	}

	public void testIndexableValuesMatchCriteria(Substance t, Predicate<Stream<IndexableValue>> matches){
		IndexValueMaker<Substance> ivm = factory.createIndexValueMakerFor(t);

		List<IndexableValue> ivs = new ArrayList<IndexableValue>();

		ivm.createIndexableValues(t, c->{
			ivs.add(c);
		});
		try{
			assertTrue(matches.test(ivs.stream()));
		}catch(Throwable tt){
			throw tt;
		}
	}

	public void testIndexableValuesHasFacet(Substance t, String facetName, Object facetValue ){
		testIndexableValuesMatchCriteria(t, s->{
			boolean match =s.anyMatch(iv->{
				if(iv.facet()){
					if(iv.name().equals(facetName)){
						return facetValue.equals(iv.value());
					}
				}
				return false;
			});
			if(!match){
				throw new AssertionError("Did not find facet:" + facetName + " with value: " + facetValue);
			}
			return match;
		});
	}

}
