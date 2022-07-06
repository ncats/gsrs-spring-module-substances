package ix.ginas.utils.validation.validators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import gov.nih.ncats.common.Tuple;
import gov.nih.ncats.molwitch.Chemical;
import ix.core.chem.Chem;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.ControlledVocabulary;
import ix.ginas.models.v1.FragmentVocabularyTerm;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;

//Ensure that specified structures are valid  
//
//Ensure that specified structures are not duplicates
//
//Create the unspecified “simplified” smiles form if needed
//
//todo in the future: If deleting or updating a CV value element, ensure it’s not used

//todo in the future: check if it has the right number of connection(star atom)
// put: api/v1/vocabularies update
// post: api/vi/vocabularies/@validate 

@Slf4j
public class CVFragmentStructureValidator extends AbstractValidatorPlugin<ControlledVocabulary> {
	
	public CVFragmentStructureValidator(){
		log.error("In CVFragmentStructureValidator");		
	}	
	
	private class FragmentChanges{
		
		private List<FragmentVocabularyTerm> addedTerms = new ArrayList<FragmentVocabularyTerm>();
		private List<FragmentVocabularyTerm> updatedTerms = new ArrayList<FragmentVocabularyTerm>();;
		private List<FragmentVocabularyTerm> deletedTerms = new ArrayList<FragmentVocabularyTerm>();;
		
	}	
		
	@Override
    public void validate(ControlledVocabulary newCV, ControlledVocabulary oldCV, ValidatorCallback callback) {
		
		List<FragmentVocabularyTerm> invalidUpdateTerms =newCV.getTerms().stream()				
				.map(term->(FragmentVocabularyTerm)term)
				.filter(term->!Optional.ofNullable(term.getFragmentStructure()).isPresent() 
						|| !Optional.ofNullable(term.getDisplay()).isPresent()
						|| !Optional.ofNullable(term.getValue()).isPresent())
				.collect(Collectors.toList());
				
		if(invalidUpdateTerms.size()>0) {
			callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE("Display, fragmentStructure or value cannot be null."));			
		}
		
		FragmentChanges changes = getAddedUpdatedDeletedTerms(newCV, oldCV);
		
		Map<String, List<String>> hashLookup = newCV.getTerms().stream()
		.map(key->(FragmentVocabularyTerm)key)		
		.map(f->Tuple.of(getHash(f), f.getValue()))
		.filter(t->t.k().isPresent())
		.map(Tuple.kmap(k->k.get()))
		.collect(Tuple.toGroupedMap());
		
		
		List<FragmentVocabularyTerm> addedOrUpdatedTerms = new ArrayList<FragmentVocabularyTerm>(); 
		addedOrUpdatedTerms.addAll(changes.addedTerms);
		addedOrUpdatedTerms.addAll(changes.updatedTerms);		
		addedOrUpdatedTerms.forEach(term -> chemicalValidation(term, hashLookup, callback));		
	}
	
		
	private Optional<String> getHash(FragmentVocabularyTerm term) {
		try {
			String inputStructure = term.getFragmentStructure().split(" ")[0];			
			Chemical chem = Chemical.createFromSmarts(inputStructure);
			chem = Chem.RemoveQueryAtomsForPseudoInChI(chem);
			return Optional.of(chem.toInchi().getKey());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Optional.empty();
		}
		
	}	
		
	private void chemicalValidation(FragmentVocabularyTerm term, Map<String,List<String>> lookup, ValidatorCallback callback) {
		
		try {
			String fragmentStructure = term.getFragmentStructure().trim();
			Chemical chem;
//			try {
//				chem = Chemical.parse(fragmentStructure);				
//			}catch(IOException ex) {				
				chem = Chemical.createFromSmarts(fragmentStructure.split(" ")[0]);
				if (!Optional.ofNullable(chem).isPresent()) {
					callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
	                    "Illegal chemical structure format: " + term.getFragmentStructure()));
					return;
				}
//			}
			
			String smiles = chem.toSmiles();
			// todo: may need to add warning with applicable change
			if(!Optional.ofNullable(term.getSimplifiedStructure()).isPresent())
				term.setSimplifiedStructure(smiles);
			
			
			Optional<String> hash = getHash(term);
			if(!hash.isPresent()) {
				callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
	                    "Illegal chemical structure format: " + term.getFragmentStructure()));
			} else if(lookup.get(hash.get()).size()>1) {
				callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
	                    "This fragment structure appears to have duplicates: " + term.getFragmentStructure()));				
			}			
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}					
	}	
	
	private FragmentChanges getAddedUpdatedDeletedTerms(ControlledVocabulary newCV, ControlledVocabulary oldCV) {	
		
		FragmentChanges fragmentChanges = new FragmentChanges();
					
		List<FragmentVocabularyTerm> termsAfterUpdate =newCV.getTerms().stream()							
				.map(term->(FragmentVocabularyTerm)term)			
				.collect(Collectors.toList());
		
		Optional <ControlledVocabulary> oldCVOptional = Optional.ofNullable(oldCV);
		if(!oldCVOptional.isPresent()) {
			fragmentChanges.addedTerms.addAll(termsAfterUpdate);
			return fragmentChanges;
		}
		
		List<FragmentVocabularyTerm> termsBeforeUpdate = oldCV.getTerms().stream()				
				.map(term->(FragmentVocabularyTerm)term)				
				.collect(Collectors.toList());
							
		termsAfterUpdate.stream().forEach(term -> {
				Long id = term.getId();
				FragmentVocabularyTerm originalTerm = termsBeforeUpdate.stream()
					  .filter(oTerm -> oTerm.getId().equals(id))
					  .findAny()
					  .orElse(null);
				if(!Optional.ofNullable(originalTerm).isPresent()) {
					fragmentChanges.addedTerms.add(term);						
				}else if(!sameVocabularyTerm(term, originalTerm)) {
					fragmentChanges.updatedTerms.add(term);					
				}});
		
		fragmentChanges.deletedTerms = termsBeforeUpdate.stream().filter(term -> {
			Long id = term.getId();
			FragmentVocabularyTerm newTerm = termsAfterUpdate.stream()
				  .filter(oTerm -> oTerm.getId().equals(id))
				  .findAny()
				  .orElse(null);
			if(!Optional.ofNullable(newTerm).isPresent())
				return true;
			else
				return false;
			}).collect(Collectors.toList());	
		
		return fragmentChanges;
	}
	
	private boolean sameVocabularyTerm(FragmentVocabularyTerm term1, FragmentVocabularyTerm term2) {
		
		if( term1.getValue().equalsIgnoreCase(term2.getValue()) 
			&& term1.getDisplay().equalsIgnoreCase(term2.getDisplay())
			&& term1.getFragmentStructure().equalsIgnoreCase(term2.getFragmentStructure()))
			return true;
		
		return false;		
	}
}	