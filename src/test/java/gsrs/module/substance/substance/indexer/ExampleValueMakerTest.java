package gsrs.module.substance.substance.indexer;


import gsrs.module.substance.substance.AbstractSubstanceJpaEntityTest;
import gsrs.startertests.TestIndexValueMakerFactory;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;

import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for ExampleValueMaker. The purpose of this test is to ensure
 * that the ExampleValueMaker is properly registered, and that it is
 * called and computes the facets / values as expected.
 * @author peryeata
 *
 */
public class ExampleValueMakerTest extends AbstractSubstanceJpaEntityTest {


	@Autowired
	private TestIndexValueMakerFactory factory;

	@BeforeEach
	public void init(){
		factory.setIndexValueMakers(new ExampleValueMaker());
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


	@Test
	public void addRelfexiveActiveMoietyShouldResultInActiveMoietyTag(){
		
		Substance s=new SubstanceBuilder()
			.asChemical()
			.setStructure("CCC1CCCC1")
			.addName("Test Guy")
			.addReflexiveActiveMoietyRelationship()
			.build();
		this.testIndexableValuesHasFacet(s, ExampleValueMaker.MOIETY_TYPE_FACET, Relationship.ACTIVE_MOIETY_RELATIONSHIP_TYPE);
	}
	
	
	@Test
	public void addExternalActiveMoietyShouldResultInChildSubstanceTag(){
		
		Substance amSub=new SubstanceBuilder()
				.asChemical()
				.setStructure("COCCO")
				.addName("Am record")
				.addReflexiveActiveMoietyRelationship()
				.build();
		
		Substance childSub=new SubstanceBuilder()
				.asChemical()
				.setStructure("COCC[O-].[Na+]")
				.addName("Non-am record")
				.andThenMutate(sb->{
					Relationship r = new Relationship();
		    		r.type= Relationship.ACTIVE_MOIETY_RELATIONSHIP_TYPE;
		    		r.relatedSubstance=amSub.asSubstanceReference();
		    		sb.relationships.add(r);
		    		
				})
				.generateNewUUID()
				.build();
		
		this.testIndexableValuesHasFacet(childSub, ExampleValueMaker.MOIETY_TYPE_FACET, ExampleValueMaker.CHILD_SUBSTANCE_RELATIONSHIP);
	}

}
