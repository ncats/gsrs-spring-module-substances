package ix.ginas.utils.validation.validators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import gov.nih.ncats.common.Tuple;
import gov.nih.ncats.molwitch.Chemical;
import ix.core.chem.Chem;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.ControlledVocabulary;
import ix.ginas.models.v1.FragmentVocabularyTerm;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

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

	private final static int R_GROUP_ADJUSTMENT = 87;

	@Autowired
	private StructureProcessor structureProcessor;

	private class FragmentChanges{
		
		private List<FragmentVocabularyTerm> addedTerms = new ArrayList<FragmentVocabularyTerm>();
		private List<FragmentVocabularyTerm> updatedTerms = new ArrayList<FragmentVocabularyTerm>();;
		private List<FragmentVocabularyTerm> deletedTerms = new ArrayList<FragmentVocabularyTerm>();;
	}
		
	@Override
    public void validate(ControlledVocabulary newCV, ControlledVocabulary oldCV, ValidatorCallback callback) {
		
		List<FragmentVocabularyTerm> invalidUpdateTerms =newCV.getTerms().stream()
				.filter(term-> term instanceof FragmentVocabularyTerm)
				.map(term->(FragmentVocabularyTerm)term)
				.filter(term->!Optional.ofNullable(term.getFragmentStructure()).isPresent()
						|| !Optional.ofNullable(term.getDisplay()).isPresent()
						|| !Optional.ofNullable(term.getValue()).isPresent())
				.collect(Collectors.toList());
				
		if(invalidUpdateTerms.size()>0) {
			callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE("Display, fragmentStructure or value cannot be null."));
			return;
		}
		
		FragmentChanges changes = getAddedUpdatedDeletedTerms(newCV, oldCV);
		
		Map<String, List<String>> hashLookup = newCV.getTerms().stream()
		.filter(term-> term instanceof FragmentVocabularyTerm)
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
	
		
	public Optional<String> getHash(FragmentVocabularyTerm term) {
		try {
			Optional<String> inchiKeyOne= getInChIKeyFromComplexSmiles(term.getFragmentStructure());
			if(inchiKeyOne.isPresent()) {
				return inchiKeyOne;
			}
			String inputStructure = term.getFragmentStructure().split(" ")[0];
			Chemical chem = Chemical.parse(inputStructure);
			//see if we get a good result without changing the structure
			Optional<String> initialHash= getInitialHash(chem);
			log.trace("in getHash, initialHash: {}", initialHash);
			if(initialHash.isPresent()) {
				return initialHash;
			}
			chem = Chem.RemoveQueryFeaturesForPseudoInChI(chem);
			return Optional.of(chem.toInchi().getKey());
		} catch (Exception e) {
			log.error("Error processing fragment structure {}", term.getFragmentStructure());
			e.printStackTrace();
			return Optional.empty();
		}
	}

	private static Optional<String> getInitialHash(Chemical chem) {
		try {
			return Optional.of(chem.toInchi().getKey());
		}
		catch(IOException ignore){
			return Optional.empty();
		}
	}

	//input 'complex' because it contains both a simple SMILES string and a set of R-Group designations.
	// for example, [*]OC[C@@]12CO[C@@H]([C@H]([*])O1)[C@@H]2O[*] |$_R91;;;;;;;;_R90;;;;_R92$|
	private Optional<String> getInChIKeyFromComplexSmiles(String complexInput) {
        Chemical initiallyParsedChemical;
        try {
            initiallyParsedChemical = Chemical.parse(complexInput);
	       	initiallyParsedChemical.atoms()
				.filter(at->at.getRGroupIndex().isPresent() && at.getRGroupIndex().getAsInt() >0)
				.forEach(at->{
					///Subtracting this number from an RGroup index will give us a mass number that InChI can use to
					// differentiate atoms.  When the mass number is too high, InChI ignores it.
					at.setMassNumber( Math.max(0, at.getRGroupIndex().getAsInt()- R_GROUP_ADJUSTMENT));
					log.warn("r group: {}", at.getRGroupIndex().getAsInt());
					at.setAlias(Chem.WILDCARD_SUBSTITUTION_ATOM);
					at.setAtomicNumber(Chem.WILDCARD_SUBSTITUTION_ATOM_NUMBER);
					at.setRGroup(0);
				});
			String molfile;
			try {
				molfile =getMolfileFromSmiles(initiallyParsedChemical.toSmiles());
			} catch(Exception errorConvertingMolfile) {
				log.warn("Error in getMolfileFromSmiles", errorConvertingMolfile);
				molfile = initiallyParsedChemical.toMol();
			}
			log.trace("getInChIKeyFromComplexSmiles going to parse molfile: {}", molfile);
			Chemical transformedChemical= Chemical.parse(molfile);
			return Optional.of(transformedChemical.toInchi().getKey());
		} catch (Exception e) {
			log.info("in getInChIKeyFromComplexSmiles, error parsing input {}", complexInput);
			return Optional.empty();
		}
    }
	private void chemicalValidation(FragmentVocabularyTerm term, Map<String,List<String>> lookup, ValidatorCallback callback) {
		
		String fragmentStructure = term.getFragmentStructure().trim();
		Chemical chem;
		try {
			chem = Chemical.parse(fragmentStructure);
		}catch(IOException ex) {
			try {
				chem = Chemical.parse(fragmentStructure.split(" ")[0]);
				if (!Optional.ofNullable(chem).isPresent()) {
					callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
							"Unrecognized chemical structure format: %s", term.getFragmentStructure()));
					return;
				}
			}catch(IOException IOEx) {
				callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
						"Unrecognized chemical structure format: %s", term.getFragmentStructure()));
				return;
			}
		}
		
		String smiles;
		try {
			Chemical cleanChemical = Chem.RemoveQueryFeaturesForPseudoInChI(chem);
			smiles = cleanChemical.toSmiles();
			// todo: may need to add warning with applicable change
			if(!Optional.ofNullable(term.getSimplifiedStructure()).isPresent())
				term.setSimplifiedStructure(smiles);
		} catch (Exception e) {
			callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
					"Unrecognized chemical structure format: %s", term.getFragmentStructure()));
			return;
		}
		
		Optional<String> hash = getHash(term);
		if(!hash.isPresent()) {
			callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
                    "Unparseable chemical structure format getting hash: %s", term.getFragmentStructure()));
		} else if(lookup.get(hash.get()).size()>1) {
			callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
                    "This fragment structure appears to have duplicates: %s with hash: %s", term.getFragmentStructure(), hash.get()));
			log.warn("Duplicate: {}", hash.get());
		}
	}	
	
	private FragmentChanges getAddedUpdatedDeletedTerms(ControlledVocabulary newCV, ControlledVocabulary oldCV) {
		
		FragmentChanges fragmentChanges = new FragmentChanges();

		List<FragmentVocabularyTerm> termsAfterUpdate =newCV.getTerms().stream()
				.filter(term->term instanceof FragmentVocabularyTerm)
				.map(term->(FragmentVocabularyTerm)term)
				.collect(Collectors.toList());
		
		Optional <ControlledVocabulary> oldCVOptional = Optional.ofNullable(oldCV);
		if(!oldCVOptional.isPresent()) {
			fragmentChanges.addedTerms.addAll(termsAfterUpdate);
			return fragmentChanges;
		}
		
		List<FragmentVocabularyTerm> termsBeforeUpdate = oldCV.getTerms().stream()
				.filter(term-> term instanceof FragmentVocabularyTerm)
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
				  .filter(oTerm -> (oTerm.getId()!=null && oTerm.getId().equals(id)))
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

	/*
	Create a molfile from a SMILES in a way that preserves stereochemistry, allowing it to transfer from
	the atom-based representation of SMILES to the bond-based representation of molfiles.
	 */
	private String getMolfileFromSmiles(String inputSmiles) throws Exception {
		boolean isQuery = true;
		Structure struc = structureProcessor.taskFor(inputSmiles)
				.standardize(false)
				.query(isQuery)
				.build()
				.instrument()
				.getStructure();

		return struc.molfile;
	}
}	