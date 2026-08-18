package ix.ginas.utils.validation.validators;

import gov.nih.ncats.molwitch.Chemical;
import gsrs.module.substance.repository.ReferenceRepository;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.validator.*;
import ix.ginas.models.v1.*;
import ix.ginas.utils.ChemUtils;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.PeptideInterpreter;
import ix.ginas.utils.validation.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Created by katzelda on 5/14/18.
 */
@Slf4j
public class ChemicalValidator extends AbstractValidatorPlugin<Substance> {
	@Autowired
    private StructureProcessor structureProcessor;

	@Autowired
    private ReferenceRepository referenceRepository;

	private boolean allow0AtomStructures = false;

    private boolean allowV3000Molfiles = false;

    private boolean allowAtomLists = false;

    private static final String V3000_MOLFILE_MARKER = "M  V30";
    private static final String V3000_MOLFILE_MARKER2 = "V3000";

    private static final String ATOM_LIST_SIGN = "M  ALS ";

    private enum ChemicalClassification {
        MULTI_ATOM_CHEMICAL,
        ZERO_ATOM_CHEMICAL,
        INVALID_OR_NON_CHEMICAL
    }

    private record ProcessedStructure(
        Structure rootStructure,
        List<Moiety> moieties) {
        }
    
    public ReferenceRepository getReferenceRepository() {
        return referenceRepository;
    }

    public void setReferenceRepository(ReferenceRepository referenceRepository) {
        this.referenceRepository = referenceRepository;
    }

    public StructureProcessor getStructureProcessor() {
        return structureProcessor;
    }

    public void setStructureProcessor(StructureProcessor structureProcessor) {
        this.structureProcessor = structureProcessor;
    }

    private static class DeduplicateCallback implements ValidatorCallback {
        private final ValidatorCallback delegate;
        private final Set<String> warningMessages = new HashSet<>();
        private final Set<String> errorMessages = new HashSet<>();

        public DeduplicateCallback(ValidatorCallback delegate) {
            this.delegate = delegate;
        }

        @Override
        public void addMessage(ValidationMessage message) {
            addMessage(message, null);
        }

        private Set<String> getHashFor(ValidationMessage message){
            switch(message.getMessageType()){

                case ERROR: return errorMessages;
                case WARNING: return warningMessages;
                default: return null;
            }
        }

        @Override
        public void addMessage(ValidationMessage message, Runnable appyAction) {
            Set<String> hash = getHashFor(message);
            if(hash ==null || message.getMessage() ==null || hash.add(message.getMessage())){
                //always let these through
                delegate.addMessage(message, appyAction);
            }
        }

        @Override
        public void setInvalid() {
            delegate.setInvalid();
        }

        @Override
        public void haltProcessing() {
            delegate.haltProcessing();
        }

        @Override
        public void setValid() {
            delegate.setValid();
        }
    }

    @Override
    public void validate(Substance substance, Substance oldSubstance, ValidatorCallback callback) {

        ChemicalSubstance chemical = (ChemicalSubstance) substance;

        if (!validateRequiredStructure(chemical, oldSubstance, callback)) {
            return;
        }

        if (!validateSupportedMolfileFeatures(chemical, callback)) {
            return;
        }

        ProcessedStructure processed =
                processStructure(chemical,callback);

        if (processed == null) {
            return;
        }

        validatePossiblePeptide(chemical, callback);
        DeduplicateCallback deduplicateCallback = new DeduplicateCallback(callback);

        reconcileMoieties(
                chemical,
                oldSubstance,
                processed,
                deduplicateCallback);

        validateChemicalStructure(
                chemical.getStructure(),
                processed.rootStructure(),
                deduplicateCallback);

        ChemUtils.fixChiralFlag(chemical.getStructure(), callback);
        ChemUtils.checkRacemicStereo(chemical.getStructure(), callback);

        validateCharge(chemical, callback);

        ValidationUtils.validateReference(
                chemical,
                chemical.getStructure(),
                callback,
                ValidationUtils.ReferenceAction.FAIL,
                referenceRepository);

    }
        private boolean validateRequiredStructure(
            ChemicalSubstance cs,
            Substance oldSubstance,
            ValidatorCallback callback) {

        if (cs.getStructure() == null) {
            callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
                    "Chemical substance must have a chemical structure"));
            return false;
        }

        if (!allow0AtomStructures
                && classifySubstanceByNumberOfAtoms(cs) == ChemicalClassification.ZERO_ATOM_CHEMICAL &&
                classifySubstanceByNumberOfAtoms(oldSubstance) != ChemicalClassification.ZERO_ATOM_CHEMICAL) {

            callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
                    "Chemical substance must have a chemical structure with one or more atoms"));
            return false;
        }

        if (cs.getStructure().molfile == null) {
            callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
                    "Chemical substance must have a valid chemical structure"));
            return false;
        }

        return true;
    }

    @Override
    public boolean supportsCategory(Substance news, Substance olds, ValidatorCategory category) {
        return ValidatorCategory.CATEGORY_DEFINITION().equals(category)
            || ValidatorCategory.CATEGORY_ALL().equals(category);
    }

	public boolean isAllow0AtomStructures() {
		return allow0AtomStructures;
	}

	public void setAllow0AtomStructures(boolean allow0AtomStructures) {
		this.allow0AtomStructures = allow0AtomStructures;
	}

    public boolean isAllowV3000Molfiles() {
        return allowV3000Molfiles;
    }

    public void setAllowV3000Molfiles(boolean allowV3000Molfiles) {
        this.allowV3000Molfiles = allowV3000Molfiles;
    }

    public boolean isAllowAtomLists() {
        return allowAtomLists;
    }

    public void setAllowAtomLists(boolean allowAtomLists) {
        this.allowAtomLists = allowAtomLists;
    }


    public boolean hasAtomLists(Structure structure) {
        if(structure.molfile == null || structure.molfile.length() ==0 ) return false;

        if(!structure.molfile.contains(ATOM_LIST_SIGN)) return false;

        String[] molfileLines = structure.molfile.split(("\\n"));

        if( molfileLines.length  >= 4) {
            int firstValuePoint = structure.molfile.indexOf(molfileLines[3] + molfileLines[3].length());
            if( structure.molfile.indexOf(molfileLines[3]) > firstValuePoint) {
                return true;
            }
        }
        return true;
    }

    public boolean hasQueryFeatures(Structure structure) {
        Chemical structureAsChemical =structure.toChemical();
        if( structureAsChemical == null)return true;
        return StructureProcessor.hasQueryFeatures(structureAsChemical);
    }

    private ChemicalClassification classifySubstanceByNumberOfAtoms(Substance substance) {
        if( ! (substance instanceof ChemicalSubstance chemicalSubstance)) {
            log.trace("previous substance was other than a Chemical");
            return ChemicalClassification.INVALID_OR_NON_CHEMICAL;
        }
        if(chemicalSubstance.getStructure() == null ) {
            log.trace("no structure found in chemical substance!");
            return ChemicalClassification.INVALID_OR_NON_CHEMICAL;
        }
        Chemical chemical = chemicalSubstance.getStructure().toChemical();
        if( chemical == null ){
            log.info("no valid Chemical found");
            return ChemicalClassification.INVALID_OR_NON_CHEMICAL;
        }
        return chemical.getAtomCount() == 0 ? ChemicalClassification.ZERO_ATOM_CHEMICAL : ChemicalClassification.MULTI_ATOM_CHEMICAL;
    }

    private boolean validateSupportedMolfileFeatures(
        ChemicalSubstance cs,
        ValidatorCallback callback) {
        Structure structure = cs.getStructure();

        if (!allowV3000Molfiles && isV3000(cs)) {
            log.info("V3000 molfile detected");
            callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
                    "GSRS does not currently support V3000 molfiles. " +
                    "Use another program to convert the structure to an earlier format."));
            return false;
        }

        boolean keepGoing = true;
        if (hasQueryFeatures(structure)) {
            callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(
                    "This chemical contains query features that are generally " +
                    "not useful in database structures "));
            keepGoing= false;
        }

        if (!allowAtomLists && hasAtomLists(structure)) {
            callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
                "Atom lists are not allowed for registration"));
            keepGoing= false;
        }
        if(!keepGoing) return false;

        return true;
    }
    
    private boolean isV3000(ChemicalSubstance cs) {
        if( (cs.getStructure().molfile.contains(V3000_MOLFILE_MARKER) && cs.getStructure().molfile.contains(V3000_MOLFILE_MARKER2))
                || (cs.getStructure().smiles.contains(V3000_MOLFILE_MARKER) && cs.getStructure().smiles.contains(V3000_MOLFILE_MARKER2))) {
            log.info("V3000 molfile detected");
            return true;
        }
        return false;
    }

    private ProcessedStructure processStructure(ChemicalSubstance cs, ValidatorCallback callback) {
        String payload = cs.getStructure().molfile;

        List<Structure> computedMoieties = new ArrayList<>();

        Structure computedRoot =structureProcessor.instrument(payload, computedMoieties, true);
        suggestMolfileConversion(cs, computedRoot,callback);

        List<Moiety> moieties = computedMoieties.stream()
            .map(this::toMoiety)
            .toList();

        return new ProcessedStructure(computedRoot, moieties);
    }

    private Moiety toMoiety(Structure structure ){
            Moiety moiety = new Moiety();
            moiety.structure = new GinasChemicalStructure(structure);
            moiety.setCount(structure.count);
            return moiety;
    }

    private void validatePossiblePeptide(ChemicalSubstance chemical,  ValidatorCallback callback) {
        try {
            ix.ginas.utils.validation.PeptideInterpreter.Protein p = PeptideInterpreter
                    .getAminoAcidSequence(chemical.getStructure().molfile);
            if (p != null && !p.getSubunits().isEmpty()
                    && p.getSubunits().get(0).getSequence().length() > 2) {
                GinasProcessingMessage mes = GinasProcessingMessage
                        .WARNING_MESSAGE("Substance may be represented as protein as well. Sequence:[%s]", p.toString());
                callback.addMessage(mes);
            }
        } catch (Exception e) {
            log.warn("Error in validatePossiblePeptide: {}", e.getMessage());
        }
    }

    private void validateChemicalStructure(
            GinasChemicalStructure oldstr, Structure newstr,
            ValidatorCallback callback) {
        List<GinasProcessingMessage> gpm = new ArrayList<>();

        GinasProcessingMessage mes = GinasProcessingMessage
                .INFO_MESSAGE("Recomputing structure hash");
        callback.addMessage(mes);
        Structure struc2 = new GinasChemicalStructure(newstr);
        oldstr.updateStructureFields(struc2);

        if (oldstr.digest == null) {
            oldstr.digest = newstr.digest;
        }
        if (oldstr.smiles == null) {
            oldstr.smiles = newstr.smiles;
        }
        if (oldstr.ezCenters == null) {
            oldstr.ezCenters = newstr.ezCenters;
        }
        if (oldstr.definedStereo == null) {
            oldstr.definedStereo = newstr.definedStereo;
        }
        if (oldstr.stereoCenters == null) {
            oldstr.stereoCenters = newstr.stereoCenters;
        }
        if (oldstr.mwt == null) {
            oldstr.mwt = newstr.mwt;
        }
        if (oldstr.formula == null) {
            oldstr.formula = newstr.formula;
        }
        if (oldstr.charge == null) {
            oldstr.charge = newstr.charge;
        }
        if (oldstr.opticalActivity == null) {
            oldstr.opticalActivity = newstr.opticalActivity;
        }
        if (oldstr.stereoChemistry == null) {
            oldstr.stereoChemistry = newstr.stereoChemistry;
        }

        ChemUtils.checkValance(newstr, callback);

        ChemUtils.fix0Stereo(oldstr, gpm);

        gpm.forEach(m -> {
            callback.addMessage(m);
        });
    }

    private void reconcileMoieties(
        ChemicalSubstance cs,
        Substance oldSubstance,
        ProcessedStructure processed,
        ValidatorCallback callback) {
            List<Moiety> computedMoieties = processed.moieties();
            if (rootStructureMolfileChanged(cs, oldSubstance)) {
                preserveRootMolfileForSingleMoiety(
                    cs.getStructure().molfile,
                    computedMoieties);

                    callback.addMessage(
                    GinasProcessingMessage
                    .INFO_MESSAGE(
                        "Chemical structure changed. " +
                    "Moieties will be regenerated from the submitted structure.")
                        .appliableChange(true),
                    () -> cs.moieties = computedMoieties);

                    return;
            }

            if (cs.moieties == null || cs.moieties.isEmpty()) {
                callback.addMessage(
                    GinasProcessingMessage
                    .INFO_MESSAGE(
                    "No moieties found in submission. They will be generated automatically.")
                    .appliableChange(true),
                () -> cs.moieties = computedMoieties);
                return;
            }
            if (cs.moieties.size() != computedMoieties.size()) {
                callback.addMessage(
                    GinasProcessingMessage
                    .INFO_MESSAGE("Incorrect number of moieties")
                    .appliableChange(true),
                        () -> cs.moieties = computedMoieties);
                return;
            }
            validateExistingMoieties(
                cs.moieties,
                    callback);
    }

    private boolean rootStructureMolfileChanged(ChemicalSubstance updated, Substance oldSubstance) {
        if (!(oldSubstance instanceof ChemicalSubstance oldChemical)) {
            return false;
        }
        String oldMolfile = oldChemical.getStructure() == null ? null : oldChemical.getStructure().molfile;
        String updatedMolfile = updated.getStructure() == null ? null : updated.getStructure().molfile;
        return !Objects.equals(oldMolfile, updatedMolfile);
    }

    private void validateExistingMoieties(
            List<Moiety> moieties,
            ValidatorCallback callback) {

        for (Moiety moiety : moieties) {
            Structure computed =
                    structureProcessor.instrument(
                            moiety.structure.molfile,
                            null,
                            true);

            validateChemicalStructure(
                    moiety.structure,
                    computed,
                    callback);
        }
    }

    private void preserveRootMolfileForSingleMoiety(String rootMolfile, List<Moiety> moietiesForSub) {
        if (rootMolfile == null || moietiesForSub == null || moietiesForSub.size() != 1) {
            return;
        }
        Moiety moiety = moietiesForSub.get(0);
        if (moiety != null && moiety.structure != null) {
            moiety.structure.molfile = rootMolfile;
        }
    }

    private void validateCharge(ChemicalSubstance chemicalSubstance, ValidatorCallback callback) {
        if (chemicalSubstance.getStructure().charge != 0) {
            GinasProcessingMessage mes = GinasProcessingMessage
                    .WARNING_MESSAGE("Structure is not charged balanced, net charge of: %s", chemicalSubstance.getStructure().charge);
            callback.addMessage(mes);
        }

    }

    private void suggestMolfileConversion(
        ChemicalSubstance chemicalSubstance, Structure processed, ValidatorCallback callback) {
        if(!chemicalSubstance.getStructure().molfile.contains("M  END")){
            //not a mol convert it
            //struc is already standardized
            callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(
                            "Structure should always be specified as mol file converting to format to mol automatically").appliableChange(true),
                    () -> {
                        try {
                            chemicalSubstance.setStructure(chemicalSubstance.getStructure().copy());
                            chemicalSubstance.getStructure().molfile = processed.molfile;
                        }catch(Exception e){
                            e.printStackTrace();
                            callback.addMessage(new ExceptionValidationMessage(e));
                        }
                    } );
        }
    }
}
