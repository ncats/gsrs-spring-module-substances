package ix.ginas.utils.validation.validators;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.ncats.molwitch.Chemical;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.repository.ReferenceRepository;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.validator.ExceptionValidationMessage;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidatorCallback;
import ix.core.validator.ValidatorCategory;
import ix.ginas.models.v1.*;
import ix.ginas.utils.ChemUtils;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.ChemicalDuplicateFinder;
import ix.ginas.utils.validation.PeptideInterpreter;
import ix.ginas.utils.validation.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Supplier;

/**
 * Created by katzelda on 5/14/18.
 */
@Slf4j
public class ChemicalValidator extends AbstractValidatorPlugin<Substance> {
	@Autowired
    private StructureProcessor structureProcessor;

	@Autowired
    private ReferenceRepository referenceRepository;

	@Autowired
    private ChemicalDuplicateFinder chemicalDuplicateFinder;

	@Autowired
    private EntityLinks entityLinks;

    @Autowired
    private SubstanceLegacySearchService legacySearchService;

    @Autowired
    private PlatformTransactionManager transactionManager;
	private boolean allow0AtomStructures = false;

    private boolean allowV3000Molfiles = false;

    private final String V3000_MOLFILE_MARKER = "M  V30";
    private final String V3000_MOLFILE_MARKER2 = "V3000";

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
    
    @Override
    public boolean supportsCategory(Substance news, Substance olds, ValidatorCategory c) {
        if(ValidatorCategory.CATEGORY_DEFINITION().equals(c) || ValidatorCategory.CATEGORY_ALL().equals(c)) {
            return true;
        }else {
            return false;
        }
    }

    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
        log.trace("starting in validate");
        ChemicalSubstance cs = (ChemicalSubstance)s;

        if (cs.getStructure() == null) {
            callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
                    "Chemical substance must have a chemical structure"));
            return;
        }

        if (!allow0AtomStructures
                && cs.getStructure().toChemical().getAtomCount() == 0
                && !substanceIs0AtomChemical(objold)) {
            callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
                    "Chemical substance must have a chemical structure with one or more atoms"));
            return;
        }

        if( !allowV3000Molfiles) {
            //for some reason, when this is run from a unit test with CDK as the molwitch implementation, the original
            // V3000 molfile appears in the SMILES field
            if( (cs.getStructure().molfile.contains(V3000_MOLFILE_MARKER) && cs.getStructure().molfile.contains(V3000_MOLFILE_MARKER2))
                || (cs.getStructure().smiles.contains(V3000_MOLFILE_MARKER) && cs.getStructure().smiles.contains(V3000_MOLFILE_MARKER2))) {
                log.info("V3000 molfile detected");
                callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
                        "GSRS does not currently support V3000 molfiles. Use another program to convert the structure to an earlier format."));
                return;
            }
        }

        String payload = cs.getStructure().molfile;
        if (payload != null) {

            try {
                ix.ginas.utils.validation.PeptideInterpreter.Protein p = PeptideInterpreter
                        .getAminoAcidSequence(cs.getStructure().molfile);
                if (p != null && !p.getSubunits().isEmpty()
                        && p.getSubunits().get(0).getSequence().length() > 2) {
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .WARNING_MESSAGE("Substance may be represented as protein as well. Sequence:["
                                    + p.toString() + "]");
                    callback.addMessage(mes);
                }
            } catch (Exception e) {

            }


            List<Moiety> moietiesForSub = new ArrayList<Moiety>();


            List<Structure> moieties = new ArrayList<Structure>();
						//computed, idealized structure info.
            Structure struc = structureProcessor.instrument(payload, moieties, true); 

            if(!payload.contains("M  END")){
                //not a mol convert it
                //struc is already standardized
                callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(
                        "structure should always be specified as mol file converting to format to mol automatically").appliableChange(true),
                        () -> {
                    try {
                        cs.setStructure(cs.getStructure().copy());
                        cs.getStructure().molfile = struc.molfile;
                    }catch(Exception e){
                        e.printStackTrace();
                        callback.addMessage(new ExceptionValidationMessage(e));
                    }

                } );

            }

            //GSRS-914 verify valid atoms
            verifyValidAtoms(()->struc.toChemical(), callback);
            for (Structure m : moieties) {
                Moiety m2 = new Moiety();
                m2.structure = new GinasChemicalStructure(m);
                m2.setCount(m.count);
                moietiesForSub.add(m2);
            }

            //GSRS-1648 deduplicate messages in moieties
            DeduplicateCallback deduplicateCallback = new DeduplicateCallback(callback);
            if (cs.moieties != null
            		&& !cs.moieties.isEmpty()
                    && cs.moieties.size() != moietiesForSub.size()) {

                GinasProcessingMessage mes = GinasProcessingMessage
                        .INFO_MESSAGE("Incorrect number of moieties")
                        .appliableChange(true);
                callback.addMessage(mes, ()-> cs.moieties = moietiesForSub);


            }else if (cs.moieties == null
            		|| cs.moieties.isEmpty()) {

                GinasProcessingMessage mes = GinasProcessingMessage
                        .INFO_MESSAGE("No moieties found in submission. They will be generated automatically.")
                        .appliableChange(true);
                callback.addMessage(mes, ()-> cs.moieties = moietiesForSub);
            } else {
                for (Moiety m : cs.moieties) {
                    Structure struc2 = structureProcessor.instrument(
                            m.structure.molfile, null, true); // don't
                    // standardize

                    validateChemicalStructure(m.structure, struc2, deduplicateCallback);
                }
            }
            validateChemicalStructure(cs.getStructure(), struc, deduplicateCallback);

            ChemUtils.fixChiralFlag(cs.getStructure(), callback);
            
            // check on Racemic stereochemistry October 2020 MAM
            ChemUtils.checkRacemicStereo(cs.getStructure(), callback);

//            ChemUtils.checkChargeBalance(cs.structure, gpm);
            if (cs.getStructure().charge != 0) {
                GinasProcessingMessage mes = GinasProcessingMessage
                        .WARNING_MESSAGE("Structure is not charged balanced, net charge of: " + cs.getStructure().charge);
                callback.addMessage(mes);
            }

            ValidationUtils.validateReference(s,cs.getStructure(), callback, ValidationUtils.ReferenceAction.FAIL, referenceRepository);

            //validateStructureDuplicates(cs, callback);
        } else {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE("Chemical substance must have a valid chemical structure"));

        }

    }

    private void verifyValidAtoms(Supplier<Chemical> chemical, ValidatorCallback callback) {
//        for(ChemicalAtom a : chemical.getAtomArray()){
//            if("Ac".equals(a.getSymbol())){
//                System.out.println(a.getSymbol() + "  atno = " + a.getAtomNo() +"  query = " + a.isQueryAtom() + "  " + a.isRgroupAtom());
//
//            }
//            if(!a.isQueryAtom() & !a.isRgroupAtom()){
//                int atomNo = a.getAtomNo();
//                if(atomNo < 1 && atomNo > 110){
//                    callback.addMessage(GinasProcessingMessage
//                            .ERROR_MESSAGE("Chemical substance must have a valid atoms found atom symol '" +a.getSymbol()   + "' atomic number " + atomNo));
//                }
//            }
//        }
        //TODO noop for now pushed to 2.3.7 until we find out more for GSRS-914
    }

    private void validateStructureDuplicates(
            ChemicalSubstance cs, ValidatorCallback callback) {
        if(chemicalDuplicateFinder==null)return;
        try {

            List<SubstanceReference> sr = chemicalDuplicateFinder.findPossibleDuplicatesFor(cs.asSubstanceReference());

            if (sr != null && !sr.isEmpty()) {
                
                //the duplicate check object should handle filtering out ourselves so don't need to check anymore

                GinasProcessingMessage mes;
                if(sr.size() > 1){
                    mes = GinasProcessingMessage.WARNING_MESSAGE("Structure has " + sr.size() + " possible duplicates:");
                }else{
                    mes = GinasProcessingMessage.WARNING_MESSAGE("Structure has 1 possible duplicate: " + sr.get(0).refuuid);
                }
                for (SubstanceReference s : sr) {
                    mes.addLink(ValidationUtils.createSubstanceLink(s));
                    
                }
                callback.addMessage(mes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void validateChemicalStructure(
            GinasChemicalStructure oldstr, Structure newstr,
            ValidatorCallback callback) {
        List<GinasProcessingMessage> gpm = new ArrayList<GinasProcessingMessage>();
//
//        String oldhash = null;
//        String newhash = null;
        // oldhash = oldstr.getExactHash();
        // newhash = newstr.getExactHash();
        // // Should always use the calculated pieces
        // // TODO: Come back to this and allow for SOME things to be overloaded
        // if (true || !newhash.equals(oldhash)) {
        
        GinasProcessingMessage mes = GinasProcessingMessage
                .INFO_MESSAGE("Recomputing structure hash");
//                .appliableChange(true);
        Structure struc2 = new GinasChemicalStructure(newstr);
        oldstr.updateStructureFields(struc2);
        
        // }
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
            // System.out.println(m);
        });
    }

    private static class DeduplicateCallback implements ValidatorCallback {
        private ValidatorCallback delegate;
        private Set<String> warningMessages = new HashSet<>();
        private Set<String> errorMessages = new HashSet<>();

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

		private boolean substanceIs0AtomChemical(Substance s) {
			if( s==null) {
				return false;
			}
			ChemicalSubstance chem = (ChemicalSubstance) s;
			return chem.toChemical().getAtomCount() == 0;
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

    private List<ValidationMessage> handleDuplcateCheck(JsonNode updatedEntityJson) throws Exception {
        if( !updatedEntityJson.hasNonNull("structure") || !updatedEntityJson.get("structure").hasNonNull("molfile")) {
            return Collections.singletonList(GinasProcessingMessage.ERROR_MESSAGE("Please provide a structure"));
        }
        String molfile=updatedEntityJson.get("structure").get("molfile").asText();
        log.trace("handleDuplicateCheck found molfile {}", molfile);
        Structure structure = structureProcessor.instrument(molfile);
        if( structure.toChemical().getAtomCount()==0) {
            return Collections.singletonList(GinasProcessingMessage.ERROR_MESSAGE("Please provide a structure"));
        }

        int defaultTop=10;
        int skipZero =0;
        String structureSearchType="flex";

        String sins=structure.getStereoInsensitiveHash();
        log.trace("StereoInsensitiveHash: {}", sins);
        String hash= "( root_structure_properties_STEREO_INSENSITIVE_HASH:" + sins + " OR " + "root_moieties_properties_STEREO_INSENSITIVE_HASH:" + sins + " )";
        log.trace("query: {}", hash);
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .query(hash)
                .kind(Substance.class);
        builder.top(defaultTop);
        builder.skip(skipZero);
        SearchRequest searchRequest = builder.build();

        SearchResult result = null;
        try {
            result = legacySearchService.search(searchRequest.getQuery(), searchRequest.getOptions() );
        } catch (Exception e) {
            log.error("Error running search for duplicates", e);
            return new ArrayList<>();
        }
        SearchResult fresult=result;

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setReadOnly(true);
        List results = transactionTemplate.execute(stauts -> {
            //the top and skip settings  look wrong, because we're not skipping
            //anything, but it's actually right,
            //because the original request did the skipping.
            //This mechanism should probably be worked out
            //better, as it's not consistent.

            //Note that the SearchResult uses a LazyList,
            //but this is copying to a real list, this will
            //trigger direct fetches from the lazylist.
            //With proper caching there should be no further
            //triggered fetching after this.

            String viewType="complete";
            if("key".equals(viewType)){
                List<ix.core.util.EntityUtils.Key> klist=new ArrayList<>(Math.min(fresult.getCount(),1000));
                fresult.copyKeysTo(klist, 0, defaultTop, true);
                return klist;
            }else{
                List tlist = new ArrayList<>(defaultTop);
                fresult.copyTo(tlist, 0, defaultTop, true);
                return tlist;
            }
        });

        List<ValidationMessage> messages = new ArrayList<>();
        results.forEach(r -> {
            Substance duplicate = (Substance) r;
            GinasProcessingMessage message = GinasProcessingMessage.WARNING_MESSAGE(
                    String.format("Record %s appears to be a duplicate", duplicate.getName()));
            message.addLink(ValidationUtils.createSubstanceLink(duplicate.asSubstanceReference()));
            messages.add(message);
        });
        if (messages.isEmpty()) {
            messages.add(GinasProcessingMessage.SUCCESS_MESSAGE("Structure is unique"));
        }
        return messages;
    }
}
