package ix.ginas.utils.validation.validators;

import gsrs.module.substance.utils.PubChemUtils;
import ix.core.chem.PubChemResult;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/*
Examine chemicals.
When they are marked as non-public, check whether the structure (InChIKey) is present in PubChem.
In this case, create a warning to the user.

TODO: review with team
1) My understanding of the business rule
2) Implementation of the check
3) Security of the POST
 */
@Slf4j
public class PubChemValidator extends AbstractValidatorPlugin<Substance> {
	
	private final String PubChemValidatorPublicWarning = "PubChemValidatorPublicWarning";
	
    @Override
    public void validate(Substance newSubstance, Substance objold, ValidatorCallback callback) {
        if( newSubstance.substanceClass.equals(Substance.SubstanceClass.chemical)) {
            ChemicalSubstance chemicalSubstance = (ChemicalSubstance) newSubstance;
            if( chemicalSubstance.getStructure()!=null){
                String inChIKey = chemicalSubstance.getStructure().getInChIKey();
                log.trace("inChIKey: {}", inChIKey);
                if(( !chemicalSubstance.isPublic() || !chemicalSubstance.getStructure().getAccess().isEmpty()) && inChIKey!=null && inChIKey.length()>10){
                    log.trace("non-public chemical|non-public structure with inchikey.  Will check PubChem");
                    List<PubChemResult> pubChemResultList =PubChemUtils.lookupInChiKeys(Collections.singletonList(inChIKey));
                    pubChemResultList.forEach(r->log.trace("InChIKey: {} - CID: {}", r.InChIKey, r.CID));
                    if(!pubChemResultList.isEmpty() && pubChemResultList.stream().anyMatch(r->r.getInChIKey().equals(inChIKey))){
                        log.trace("PubChem has a match!");
                        GinasProcessingMessage mesWarn = GinasProcessingMessage
                                .WARNING_MESSAGE(PubChemValidatorPublicWarning,
                                        "Substance and/or structure is marked non-public but the structure is found in PubChem!");
                        callback.addMessage(mesWarn);
                    }
                }
            }
        }
    }
}
