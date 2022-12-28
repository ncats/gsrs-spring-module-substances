package gsrs.module.substance.scrubbers.basic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;

import gov.nih.ncats.common.stream.StreamUtil;
import ix.core.EntityFetcher;
import ix.core.models.Group;
import ix.core.models.Structure;
import ix.core.util.EntityUtils;
import ix.ginas.exporters.RecordScrubber;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.GinasAccessControlled;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonData;
import ix.ginas.models.GinasSubstanceDefinitionAccess;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Mixture;
import ix.ginas.models.v1.Note;
import ix.ginas.models.v1.Parameter;
import ix.ginas.models.v1.PolymerClassification;
import ix.ginas.models.v1.Property;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.StructurallyDiverse;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.Substance.SubstanceClass;
import ix.ginas.models.v1.Substance.SubstanceDefinitionLevel;
import ix.ginas.models.v1.SubstanceReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;

/*
Note: as of 22 September, most of this class is commented out and a quick and dirty implementation is in place.
This will change in the ensuing weeks
 */
@Slf4j
public class BasicSubstanceScrubber implements RecordScrubber<Substance> {
    private static final String TO_DELETE = "TO_DELETE";
    private static final String TO_FORCE_KEEP = "TO_FORCE_KEEP";
	private Set<String> groupsToInclude;
	private Set<String> statusesToInclude;
    private Set<String> codeSystemsToRemove;
    private Set<String> codeSystemsToKeep;
    private BasicSubstanceScrubberParameters scrubberSettings;

    private MISSING_SUBSTANCE_REFERENCE_ACTION definitionalDependencyAction = MISSING_SUBSTANCE_REFERENCE_ACTION.KEEP_REFERENCE;
    private MISSING_SUBSTANCE_REFERENCE_ACTION relationalDependencyAction   = MISSING_SUBSTANCE_REFERENCE_ACTION.KEEP_REFERENCE;
    
    //TODO
    private MISSING_SUBSTANCE_REFERENCE_ACTION hierarchicalDependencyAction = MISSING_SUBSTANCE_REFERENCE_ACTION.KEEP_REFERENCE;
    
    private static Set<Group> toDelete = new HashSet<>();
    private static Set<Group> toKeep = new HashSet<>();

    enum RECORD_PUB_STATUS {
        FULLY_PUBLIC,
        DEFINITION_PROTECTED,
        FULLY_PROTECTED
    }

    enum MISSING_SUBSTANCE_REFERENCE_ACTION {
        KEEP_REFERENCE(DEFINITION_LEVEL_ACTION.FULL_DEFINITION),
        REMOVE_PARENT_SUBSTANCE_ENTIRELY(DEFINITION_LEVEL_ACTION.FULL_DEFINITION),
        DEIDENTIFY_SUBSTANCE_REFERENCE(DEFINITION_LEVEL_ACTION.PARTIAL_DEFINITION),   					  //potentially affects definition
        REMOVE_ONLY_SUBSTANCE_REFERENCE(DEFINITION_LEVEL_ACTION.PARTIAL_DEFINITION),  					  //potentially affects definition
        REMOVE_SUBSTANCE_REFERENCE_AND_PARENT_IF_NECESSARY(DEFINITION_LEVEL_ACTION.PARTIAL_DEFINITION),   //potentially affects definition
        REMOVE_DEFINITION_ENTIRELY(DEFINITION_LEVEL_ACTION.EMPTY_DEFINITION);                             //potentially affects definition

        DEFINITION_LEVEL_ACTION definitionalAction;

        MISSING_SUBSTANCE_REFERENCE_ACTION(DEFINITION_LEVEL_ACTION action){
            this.definitionalAction = action;
        }

        public void act(Substance substance, SubstanceReference thing, GinasAccessControlled parent){
            switch(this){
                case KEEP_REFERENCE:
                    break;
                case REMOVE_PARENT_SUBSTANCE_ENTIRELY:
                    markForDelete(substance);
                    break;
                case DEIDENTIFY_SUBSTANCE_REFERENCE:
                    deidentifySubstanceReference(thing);
                    break;
                case REMOVE_ONLY_SUBSTANCE_REFERENCE:
                    markForDelete(thing);
                    break;
                case REMOVE_SUBSTANCE_REFERENCE_AND_PARENT_IF_NECESSARY:
                    markSubstanceReferenceForDelete(parent, thing);
                    break;
                case REMOVE_DEFINITION_ENTIRELY:
                    markSubstanceReferenceForDelete(parent, thing);
                    //this needs more actions to happen elsewhere to handle the deleted definition
                    break;
            }
        }
    }
    enum DEFINITION_LEVEL_ACTION {
        FULL_DEFINITION(2),
        PARTIAL_DEFINITION(1),
        EMPTY_DEFINITION(0);
        int priority;
        DEFINITION_LEVEL_ACTION(int priority){
            this.priority=priority;
        }
        public DEFINITION_LEVEL_ACTION combine(DEFINITION_LEVEL_ACTION action2){
            if(this.priority<action2.priority){
                return this;
            }else{
                return action2;
            }
        }
    }

    public interface SubstanceReferenceResolver {
        Substance resolve(SubstanceReference sref) throws Exception;
    }

    public class DefaultSubstanceReferenceResolver implements SubstanceReferenceResolver {
        public Substance resolve(SubstanceReference sref) throws Exception {
            return (Substance) EntityFetcher.of(sref.getKeyForReferencedSubstance()).call();
        }
    }

    public BasicSubstanceScrubber(BasicSubstanceScrubberParameters scrubberSettings){
        this.scrubberSettings = scrubberSettings;

        groupsToInclude= Optional.ofNullable(scrubberSettings.getRemoveAllLockedAccessGroupsToInclude()).orElse(Collections.emptyList()).stream()
                .map(String::trim)
                .filter(t->t.length()>0)
                .collect(Collectors.toSet());

        statusesToInclude= Optional.ofNullable(scrubberSettings.getStatusesToInclude()).orElse(Collections.emptyList()).stream()
                .map(String::trim)
                .filter(t->t.length()>0)
                .collect(Collectors.toSet());
        
        codeSystemsToRemove = Optional.ofNullable(scrubberSettings.getRemoveCodesBySystemCodeSystemsToRemove()).orElse(Collections.emptyList()).stream()
                .map(String::trim)
                .filter(t->t.length()>0)
                .collect(Collectors.toSet());
        codeSystemsToKeep=Optional.ofNullable(scrubberSettings.getRemoveCodesBySystemCodeSystemsToKeep()).orElse(Collections.emptyList()).stream()
                .map(String::trim)
                .filter(t->t.length()>0)
                .collect(Collectors.toSet());
        
        
    	toDelete.add(new Group(TO_DELETE));
    	toKeep.add(new Group(TO_FORCE_KEEP));
        try {
            definitionalDependencyAction = MISSING_SUBSTANCE_REFERENCE_ACTION.valueOf(scrubberSettings.getSubstanceReferenceCleanupActionForDefinitionalDependentScrubbedSubstanceReferences());
        } catch (Exception ex){
            definitionalDependencyAction = MISSING_SUBSTANCE_REFERENCE_ACTION.KEEP_REFERENCE;
        }
        try {
            relationalDependencyAction = MISSING_SUBSTANCE_REFERENCE_ACTION.valueOf(scrubberSettings.getSubstanceReferenceCleanupActionForRelationalScrubbedSubstanceReferences());
        } catch (Exception ex){
            relationalDependencyAction = MISSING_SUBSTANCE_REFERENCE_ACTION.REMOVE_SUBSTANCE_REFERENCE_AND_PARENT_IF_NECESSARY;
        }
    }

    public void setResolver( SubstanceReferenceResolver newResolver) {
        this.resolver= newResolver;
    }
    private void forEachObjectWithAccess(Substance s, Consumer<GinasAccessControlled> consumer) {
    	StreamUtil.with(s.getAllChildrenCapableOfHavingReferences().stream().map(n->(GinasAccessControlled)n))
    			  .and(s.references.stream().map(n->n))
    			  .and(s)
    			  .stream()
    			  .distinct()
    			  .forEach(consumer::accept);
    }

    private void forEachObjectWithReferences(Substance s, Consumer<GinasAccessReferenceControlled> consumer) {
    	StreamUtil.with(s.getAllChildrenCapableOfHavingReferences().stream())
    			  .stream()
    			  .forEach(consumer::accept);
    }

    private SubstanceReferenceResolver resolver = (SubstanceReferenceResolver) new DefaultSubstanceReferenceResolver();

    //TODO remove all private access things
    //As transformer
    //1. Remove all things which have $..access!=[]
    //2. Remove modifications and properties if [definingpart].access is not []
    //3. Remove all references which are not PD=true
    //4. Remove all notes
    //5. Look for "IND[ -][0-9][0-9]*" in references, remove them

    //NOT FINISHED!!!!

    private void removeStaleReferences(Substance s) {
    	Set<UUID> references = s.references.stream().map(r->r.uuid).collect(Collectors.toSet());

    	forEachObjectWithReferences(s, (o)->{
    		Set<UUID> refs= o.getReferencesAsUUIDs()
    						    .stream()
    						    .filter(uu->references.contains(uu))
    						    .collect(Collectors.toSet());
    		o.setReferenceUuids(refs);

    	});
    }

    private void cleanUpReferences(Substance substance){
        log.trace("starting CleanUpReferences");
        if(!scrubberSettings.getRemoveReferencesByCriteria()){
            return;
        }
        List<Pattern> patternsToCheck = new ArrayList<>();
        if(scrubberSettings.getRemoveReferencesByCriteriaExcludeReferenceByPattern() && scrubberSettings.getRemoveReferencesByCriteriaCitationPatternsToRemove()!=null) {
            String[] patternArray=scrubberSettings.getRemoveReferencesByCriteriaCitationPatternsToRemove().split("\n");
            log.trace("split array into {}", patternArray.length);
            patternsToCheck.addAll( Arrays.stream(patternArray)
                    .map(s ->Pattern.compile(s))
                    .collect(Collectors.toList()));
            log.trace("pattern total {} ",  patternsToCheck.size());
        }

        substance.getAllChildrenCapableOfHavingReferences().forEach(c->{
            List<Reference> referencesToRemove= new ArrayList<>();
            log.trace("looking at refs for {}", c.toString());
            c.getReferencesAsUUIDs().forEach(r->{
                Reference reference = substance.getReferenceByUUID(r.toString());
                if(scrubberSettings.getRemoveReferencesByCriteriaReferenceTypesToRemove()!=null
                        && !scrubberSettings.getRemoveReferencesByCriteriaReferenceTypesToRemove().isEmpty()) {
                    if (scrubberSettings.getRemoveReferencesByCriteriaReferenceTypesToRemove().contains(reference.docType)) {
                        referencesToRemove.add(reference);
                        log.trace("Adding reference to deletion list: {}", r);
                    }
                }
                if(!patternsToCheck.isEmpty()){
                    patternsToCheck.forEach(p->{
                       if( p.matcher(reference.citation).matches()) {
                            log.trace("adding reference with citation {} because it matches {}", reference.citation,
                                    p.pattern());
                            referencesToRemove.add(reference);
                        }
                    });
                }
            });
            if(!referencesToRemove.isEmpty()) {
                substance.references.removeAll(referencesToRemove);
                log.trace("removed references");
            }
        });
    }

    private Substance scrubAccess(Substance starting) throws IOException {
    	

    	boolean[] isDefinitionScrubbed = new boolean[] {false};
    	GinasAccessReferenceControlled mainDefinition = null;

    	if(starting instanceof GinasSubstanceDefinitionAccess) {
    		GinasSubstanceDefinitionAccess def = (GinasSubstanceDefinitionAccess)starting;
    		mainDefinition=def.getDefinitionElement();
    	}
    	GinasAccessReferenceControlled finalMainDefinition = mainDefinition;

    	if(scrubberSettings.getRemoveAllLocked()) {
    		forEachObjectWithAccess(starting, (bm)->{
    			//log.trace("examining object {}", bm.getClass().getName());
    			GinasAccessControlled b=bm;
    			Set<String> accessSet = b.getAccess().stream().map(g->g.name).collect(Collectors.toSet());
    			//If it's not empty, remove everything UNLESS
    			if(!accessSet.isEmpty()) {

    				accessSet.retainAll(groupsToInclude);
    				// There's something in the inclusion list
    				if(!accessSet.isEmpty()) {
    					//Set the access to the newer restriction list
    					b.setAccess(accessSet.stream().map(ss->new Group(ss)).collect(Collectors.toSet()));
    				}else {
    					//this means the whole thing is restricted
    					b.setAccess(toDelete);
    					if(finalMainDefinition == bm) {
    						//definition is private based on this algorithm
    						isDefinitionScrubbed[0] = true;
    	    			}
    				}
    			}
    		});
            if(scrubberSettings.getRemoveAllLockedRemoveElementsIfNoExportablePublicRef()
                    && scrubberSettings.getRemoveElementsIfNoExportablePublicRefElementsToRemove().size()>0){
                starting.getAllChildrenCapableOfHavingReferences().forEach(c->{
                    if( isElementOnList(c, scrubberSettings.getRemoveElementsIfNoExportablePublicRefElementsToRemove())){
                        if( c.getReferencesAsUUIDs().stream().noneMatch(r-> {
                            Reference ref = starting.getReferenceByUUID(r.toString());
                            return (ref!=null && ref.publicDomain);
                        })){
                            log.trace("going to delete element {} ({}) because it has no public ref", c.getClass().getName(), c.toString());
                            c.setAccess(toDelete);
                        }
                    }
                    if(scrubberSettings.getRemoveElementsIfNoExportablePublicRefElementsToRemove().contains("Definition")){
                        if(starting instanceof GinasSubstanceDefinitionAccess) {
                            GinasSubstanceDefinitionAccess definitionAccess = (GinasSubstanceDefinitionAccess)starting;
                            GinasAccessReferenceControlled defining= definitionAccess.getDefinitionElement();
                            if(defining.getReferencesAsUUIDs().stream().noneMatch(r->{
                                Reference ref = starting.getReferenceByUUID(r.toString());
                                return (ref!=null && ref.publicDomain);
                            })){
                                isDefinitionScrubbed[0]=true;
                                //do we set it to concept here?
                                //todo: find out if more is required
                                log.info("definition to be scrubbed");
                            }
                        }
                    }
                });
            }
    	}

    	if(isDefinitionScrubbed[0]){
    		return scrubDefinition(starting, DEFINITION_LEVEL_ACTION.EMPTY_DEFINITION).orElse(null);
    	}
        return starting;
    }

    private Optional<Substance> scrubDefinition(Substance starting, DEFINITION_LEVEL_ACTION action){

    	if(action==DEFINITION_LEVEL_ACTION.FULL_DEFINITION){
    		return Optional.of(starting);
    	}
    	
    	boolean removeScrubbedDefinitionalElementsEntirely=false;
    	boolean setScrubbedDefinitionalElementsIncomplete=false;
    	boolean convertScrubbedDefinitionsToConcepts=false;
    	String addNoteToScrubbedDefinitions=null;
    	
    	if(action==DEFINITION_LEVEL_ACTION.EMPTY_DEFINITION || (action==DEFINITION_LEVEL_ACTION.PARTIAL_DEFINITION && scrubberSettings.getScrubbedDefinitionHandlingTreatPartialDefinitionsAsMissingDefinitions())){
    		removeScrubbedDefinitionalElementsEntirely=scrubberSettings.getScrubbedDefinitionHandlingRemoveScrubbedDefinitionalElementsEntirely();
    		setScrubbedDefinitionalElementsIncomplete=scrubberSettings.getScrubbedDefinitionHandlingSetScrubbedDefinitionalElementsIncomplete();
    		convertScrubbedDefinitionsToConcepts=scrubberSettings.getScrubbedDefinitionHandlingConvertScrubbedDefinitionsToConcepts();
    		addNoteToScrubbedDefinitions=scrubberSettings.getScrubbedDefinitionHandlingAddNoteToScrubbedDefinitions();
    	}else if (action==DEFINITION_LEVEL_ACTION.PARTIAL_DEFINITION){
    		removeScrubbedDefinitionalElementsEntirely=scrubberSettings.getScrubbedDefinitionHandlingRemoveScrubbedPartialDefinitionalElementsEntirely();
    		setScrubbedDefinitionalElementsIncomplete=scrubberSettings.getScrubbedDefinitionHandlingSetScrubbedPartialDefinitionalElementsIncomplete();
    		convertScrubbedDefinitionsToConcepts=scrubberSettings.getScrubbedDefinitionHandlingConvertScrubbedPartialDefinitionsToConcepts();
    		addNoteToScrubbedDefinitions=scrubberSettings.getScrubbedDefinitionHandlingAddNoteToScrubbedPartialDefinitions();
    	}
    	
    	
    	//remove all potential elements of definitions if this setting is true
    	if(removeScrubbedDefinitionalElementsEntirely) {
    		starting.properties.stream()
					    		.filter(Property::isDefining)
					    		.forEach(p->{
					    			markForDelete(p);
					    		});

            if(starting.hasModifications()) {
                markForDelete(starting.modifications);
            }
    		if(starting instanceof ChemicalSubstance) {
    			ChemicalSubstance chem = (ChemicalSubstance)starting;
    			chem.moieties.forEach(m->{
    				markForDelete(m);
    			});
    		}    		
    	}
    	if(setScrubbedDefinitionalElementsIncomplete) {
    		starting.definitionLevel=SubstanceDefinitionLevel.INCOMPLETE;
    	}
    	
       	if(convertScrubbedDefinitionsToConcepts) {
    		starting.substanceClass=SubstanceClass.concept;
    	}
       	
       	if(addNoteToScrubbedDefinitions!=null) {
       		Note nn=starting.addNote(addNoteToScrubbedDefinitions);
       		nn.setAccess(toKeep);
       	}
       	return Optional.ofNullable(starting);
    }
    
    private Substance scrubApprovalId(Substance substance) {
        String approvalId = substance.getApprovalID();
        log.trace("in scrubApprovalId, approvalId: {}", approvalId);
        if(approvalId!=null && approvalId.length()>0 && this.scrubberSettings.getApprovalIdCleanupApprovalIdCodeSystem()!= null
                && this.scrubberSettings.getApprovalIdCleanupApprovalIdCodeSystem().length()>0
                && this.scrubberSettings.getApprovalIdCleanupCopyApprovalIdToCode()) {
            Optional<Code> code=substance.codes.stream().filter(c->c.codeSystem.equals(this.scrubberSettings.getApprovalIdCleanupApprovalIdCodeSystem())).findFirst();
            if( code.isPresent()) {
                log.trace("code already present");
                code.get().setCode(approvalId);
            }else{
                log.trace("will create code");
                Code approvalIdCode= new Code();
                approvalIdCode.codeSystem=scrubberSettings.getApprovalIdCleanupApprovalIdCodeSystem();
                approvalIdCode.code=approvalId;
                approvalIdCode.type="PRIMARY";
                substance.addCode(approvalIdCode);
            }
        }

        if(scrubberSettings.getApprovalIdCleanupRemoveApprovalId()){
            substance.approvalID=null;
        }
        return substance;
    }

    private Substance scrubUUID(Substance substance) {
        UUID topLevelUuid = substance.getOrGenerateUUID();
        log.trace("in scrubUUID, approvalId: {}", topLevelUuid);

        if(topLevelUuid!=null && this.scrubberSettings.getUUIDCleanupUUIDCodeSystem()!= null
                && this.scrubberSettings.getUUIDCleanupUUIDCodeSystem().length()>0
                && this.scrubberSettings.getUUIdCleanupCopyUUIDIdToCode()) {
            Optional<Code> code=substance.codes.stream().filter(c->c.codeSystem.equals(this.scrubberSettings.getUUIDCleanupUUIDCodeSystem())).findFirst();
            if( code.isPresent()) {
                log.trace("code already present");
                code.get().setCode(topLevelUuid.toString());
            }else{
                log.trace("will create code");
                Code uuidCode= new Code();
                uuidCode.codeSystem=scrubberSettings.getUUIDCleanupUUIDCodeSystem();
                uuidCode.code=topLevelUuid.toString();
                uuidCode.type="PRIMARY";
                substance.addCode(uuidCode);
            }
        }

        if(scrubberSettings.getApprovalIdCleanupRemoveApprovalId()){
            substance.approvalID=null;
        }
        return substance;
    }

    public String restrictedJSONSimple(String s) {
        log.trace("starting restrictedJSONSimple 4");
        DocumentContext dc = JsonPath.parse(s);

        if( scrubberSettings.getRemoveNotes()){
            log.trace("deleting notes");
            dc.delete("$['notes'][?]", (ctx)->{
            	Map code=ctx.item(Map.class);
            	Object o=code.get("access");
                return o == null || !o.toString().contains(TO_FORCE_KEEP);
            });
        }

        if( scrubberSettings.getRemoveChangeReason()){
            dc.delete("$.changeReason");
        }

        if(scrubberSettings.getRemoveDates()) {
            dc.delete("$..lastEdited");
            dc.delete("$..created");
        }
        if(scrubberSettings.getAuditInformationCleanup()) {
        	if(scrubberSettings.getAuditInformationCleanupDeidentifyAuditUser()) {
        		dc.delete("$..lastEditedBy");
        		dc.delete("$..createdBy");
        		dc.delete("$..approvedBy");
        	}else if(scrubberSettings.getAuditInformationCleanupNewAuditorValue()!=null) {
        		String newAuditor = scrubberSettings.getAuditInformationCleanupNewAuditorValue();

        		dc.set("$..lastEditedBy", newAuditor);
        		dc.set("$..createdBy", newAuditor);
        		dc.set("$..approvedBy", newAuditor);
        	}
        }

        if(scrubberSettings.getRemoveCodesBySystem() && (!codeSystemsToRemove.isEmpty() || !codeSystemsToKeep.isEmpty())){
            Predicate codeSystemPredicate = context -> {
                Map code=context.item(Map.class);
                String codeSystem= (String)code.get("codeSystem");
                if( codeSystemsToKeep.size()>0) {
                    if (codeSystemsToKeep.contains(codeSystem)) {
                        log.trace("not going to delete code with system {}", codeSystem);
                        return false;
                    } else {
                        return true;
                    }
                }
                if(codeSystemsToRemove.contains(codeSystem)) {
                    log.trace("predicate found a match to {}", String.join(",", codeSystem));
                    return true;
                }
                return false;
            };
            log.trace("Before code delete");
            dc.delete("$['codes'][?]",codeSystemPredicate);
        }


        dc.delete("$..[?(@.access[0]===\"" + TO_DELETE + "\")]");

        return dc.jsonString();
    }


    public String testAll(String json){
        DocumentContext dc = JsonPath.parse(json);
        //"$..[?]",
        String output;
        try{
            output= dc.read("$..[?]['access']", (s)-> {

                log.trace("item class: {}; root: {}", s.item().getClass().getName(), s.root().getClass().getName());
                if( s.item().getClass().equals(JSONArray.class)){
                    JSONArray array = s.item(JSONArray.class);
                    if( array == null) {
                        log.trace("item null");
                    } else {
                        log.trace("array size: {}",array.size());
                        array.forEach(a-> log.trace("element type: {}", a.getClass().getName()));
                    }

                }
                //log.trace(s.item());
                return true;
            });
        }
        catch (Exception ex){
            System.err.println("Error: " + ex.getMessage());
            output="error!";
        }
        return output;
    }

    public boolean isElementOnList(Object object, List<String> approximateNames) {
        String[] nameParts=object.getClass().getName().split("\\.");
        String switchName=nameParts[nameParts.length-1].toUpperCase(Locale.ROOT);
        switch (switchName){
            case "NAME" :
                return approximateNames.contains("Names");
            case "CODE" :
                return approximateNames.contains("Codes");
            case "NOTE":
                return approximateNames.contains("Notes");
            case "RELATIONSHIP" :
                return approximateNames.contains("Relationships");
            case "PROPERTY" :
                return approximateNames.contains("Properties");
            case "MODIFICATION" :
                return approximateNames.contains("Modifications");
        }
        return false;
    }

    private Substance reassignUuids(Substance substance) {
        log.trace("starting reassignUuids");
        Map<UUID,UUID> oldToNewUUIDMap = new HashMap<>();
        substance.uuid=oldToNewUUIDMap.computeIfAbsent(substance.uuid, (k)->UUID.randomUUID());
        substance.references.forEach(r->{
        	r.uuid=oldToNewUUIDMap.computeIfAbsent(r.uuid, (k)->UUID.randomUUID());
        });
        substance.getAllChildrenCapableOfHavingReferences().stream()
                .forEach(c-> {

                	//change reference uuids and references to those uuids
                	Set<UUID> newRefUuids = c.getReferencesAsUUIDs().stream().map(u->oldToNewUUIDMap.getOrDefault(u, null)).filter(u->u!=null).collect(Collectors.toSet());
                	c.setReferenceUuids(newRefUuids);

                    if (c instanceof GinasCommonData) {
                        ((GinasCommonData) c).uuid = oldToNewUUIDMap.computeIfAbsent(((GinasCommonData) c).uuid, (k)->UUID.randomUUID());
                    }else if(c instanceof Structure) {
                    	((Structure) c).id = oldToNewUUIDMap.computeIfAbsent(((Structure) c).id , (k)->UUID.randomUUID());
                    }
                });
        return substance;
    }

    //todo: within the following method, look at computed status and exclude based on the status... if setting includes looking at
    // status

    public boolean isRecordExcluded(Substance starting){
    	//This is theoretically the implementation of that TODO
    	if(scrubberSettings.getRemoveBasedOnStatus()) {
    		String status=getStatusForSubstance(starting);
    		//filter out
    		if(!this.statusesToInclude.contains(status)) {
    			return true;
    		}

        }
        if(scrubberSettings.getRemoveAllLocked()) {
            Set<String> accessSet = starting.getAccess().stream().map(g->g.name).collect(Collectors.toSet());
            //If it's not empty, remove everything UNLESS
            if(!accessSet.isEmpty()) {
                accessSet.retainAll(groupsToInclude);
                // There's something in the inclusion list
                if(!accessSet.isEmpty()) {
                    return false; //record NOT excluded (okay to include)
                }else {
                    //this means the whole thing is restricted
                    return true; //record IS to be excluded
                }
            }
        }
        return false; //if scrubber not set to scrub out, always return false
    }

    public boolean isRecordDefinitionExcluded(Substance substance) {
        //look at refs to make sure there's nothing that looks a protected document... but that's done
        // elsewhere and too complicated to repeat

        /*Set<String> accessSet = substance.getPrimaryDefinitionReference().getAccess().stream()
                .map(a->a.name)
                .collect(Collectors.toSet());*/
        Set<String> accessSet = new HashSet<>();
        if(substance instanceof GinasSubstanceDefinitionAccess) {
            GinasAccessReferenceControlled accessHolder= (( GinasSubstanceDefinitionAccess)substance).getDefinitionElement();
            accessHolder.getAccess().stream()
                    .map(g->g.name)
                    .forEach(a-> accessSet.add(a));
        }
        /*
        May need to comment this out?
        every record (except concepts) has a def
        can protect alt def independent of primary def...
        not necessarily a relationship between the 2
        e.g. vaccine
            1) sequence of mRNA
            2) ref to virus and part
        in case where primary def is protected but alt def is public, we could make alt def primary
        use case: user desires all chemical definitions of things that have an alt def.

        skip this for now.
        if(substance.getPrimaryDefinitionRelationships().isPresent()){
            substance.getPrimaryDefinitionRelationships().get().getAccess().stream()
                    .map(g->g.name)
                    .forEach(a-> accessSet.add(a));
        }*/
        //todo: make this a helper method
        if(!accessSet.isEmpty()) {
            accessSet.retainAll(groupsToInclude);
            // There's something in the inclusion list
            if(!accessSet.isEmpty()) {
                return false; //record NOT excluded (okay to include)
            }else {
                //this means the whole thing is restricted
                return true; //record IS to be excluded
            }
        }
        return false; //if scrubber not set to scrub out, always return false
    }

    public RECORD_PUB_STATUS getPublishable(SubstanceReference sref) throws Exception {
        Substance rsub =this.resolver.resolve(sref);

        if(rsub==null){
            return RECORD_PUB_STATUS.FULLY_PROTECTED;
        }
        if(isRecordExcluded(rsub)){
            return RECORD_PUB_STATUS.FULLY_PROTECTED;
        }else if(isRecordDefinitionExcluded(rsub)){
            return RECORD_PUB_STATUS.DEFINITION_PROTECTED;
        }
        return RECORD_PUB_STATUS.FULLY_PUBLIC;
    }


    /*
    take an input substance and mutate it in a way that handles substance refs that may be publishable.
     */
    public Optional<Substance> scrubBasedOnDefDefs(Substance substance) {
        DEFINITION_LEVEL_ACTION[] dAction = new DEFINITION_LEVEL_ACTION[]{DEFINITION_LEVEL_ACTION.FULL_DEFINITION};
        
        substance.getDependsOnSubstanceReferencesAndParents().forEach(tt->{
        	SubstanceReference thing = tt.v();
        	GinasAccessControlled parent = tt.k();
        	
            RECORD_PUB_STATUS status;
            try {
                status=getPublishable(thing);
                switch(status){
                    case FULLY_PUBLIC: return;

                    case DEFINITION_PROTECTED:
                        //do  C2 stuff here
                    	//for now considered the same as fully public
                    	return;
                    	
                    case FULLY_PROTECTED:
                        //do  C3 stuff here
                    	dAction[0]=dAction[0].combine(definitionalDependencyAction.definitionalAction);
                    	definitionalDependencyAction.act(substance, thing, parent);
                    	return;

                }
            }catch(Exception ex) {
                log.trace("Error computing publishable of substance", ex);
            }
        });

        //now look at the top-level status
        if(scrubberSettings.getRemoveBasedOnStatus() && scrubberSettings.getStatusesToInclude()!= null
                && !scrubberSettings.getStatusesToInclude().contains(substance.status)) {
            markForDelete(substance);
        }
        if(isMarkedForDelete(substance))return Optional.empty();
		scrubDefinition(substance, dAction[0]);
		
		DEFINITION_LEVEL_ACTION[] dAction2 = new DEFINITION_LEVEL_ACTION[]{DEFINITION_LEVEL_ACTION.FULL_DEFINITION};
        
		substance.getNonDefiningSubstanceReferencesAndParents().forEach(tt->{
        	SubstanceReference thing = tt.v();
        	GinasAccessControlled parent = tt.k();
        	
            RECORD_PUB_STATUS status;
            try {
                status=getPublishable(thing);
                switch(status){
                    case FULLY_PUBLIC: 
                    	return;
                    case DEFINITION_PROTECTED:
                    	return;
                    case FULLY_PROTECTED:
                    	dAction2[0]=dAction2[0].combine(relationalDependencyAction.definitionalAction);
                    	relationalDependencyAction.act(substance, thing, parent);
                    	return;
                }
            }catch(Exception ex) {
                log.trace("Error computing publishable of substance", ex);
            }
        });
		
		if(isMarkedForDelete(substance))return Optional.empty();
		scrubDefinition(substance, dAction2[0]);
		
        return Optional.of(substance);
    }


    private static String getStatusForSubstance(Substance substance){
        if(substance.substanceClass.equals(SubstanceClass.concept)){
            if(substance.isSubstanceVariant()){
                if(substance.getParentSubstanceReference().approvalID!=null){
                    //note: possible to fetch full record and check its status, but the above is a faster way to get
                    //  the same info
                    return "approved subconcept";
                }
                return "pending subconcept";
            }
            return "concept";
        }
        return substance.status;
    }

    private static void markForDelete(GinasAccessControlled b){
        b.setAccess(toDelete);
    }

    private static SubstanceReference deidentifySubstanceReference(SubstanceReference sref){
        //SubstanceReference sref = new SubstanceReference();
        sref.refPname = "UNSPECIFIED_SUBSTANCE";
        sref.substanceClass = "mention";
        sref.approvalID=null;
        //sref.refuuid=sref.refuuid;
        return sref;
    }

    public static void markSubstanceReferenceForDelete(GinasAccessControlled parent, SubstanceReference sref){
        boolean deleteParent=true;

        //these are cases where it's okay to JUST delete the substance reference
        if(parent instanceof StructurallyDiverse
                || parent instanceof Parameter
                || parent instanceof Property
                || parent instanceof Mixture
                || parent instanceof PolymerClassification
        ){
            deleteParent=false;
        }else if(parent instanceof Relationship){
            Relationship rel=(Relationship)parent;
            if(rel.mediatorSubstance == sref){
                deleteParent=false;
            }
        }
        if(deleteParent){
            markForDelete(parent);
        }else{
            markForDelete(sref);
        }


    }

    private static boolean isMarkedForDelete(GinasAccessControlled s){
        Set<String> accessSet = s.getAccess().stream().map(g->g.name).collect(Collectors.toSet());
        return accessSet.contains(TO_DELETE);
    }

    @SneakyThrows
    @Override
    public Optional<Substance> scrub(Substance substance) {
        log.trace("starting in scrub");

        String substanceJson;
        try {
        	//just a force thing to fetch if needed
            substanceJson = substance.toInternalJsonNode().toString();
        } catch (Exception ex){
            log.error("Error retrieving substance; using alternative method");
            EntityUtils.Key skey = EntityUtils.Key.of(Substance.class, substance.uuid);
            Optional<Substance> substanceRefetch = EntityFetcher.of(skey).getIfPossible().map(o->(Substance)o);
            substanceJson = substanceRefetch.get().toInternalJsonNode().toString();
        }

        try {
        	 //TODO: confirm if this forces as a concept. It should not,
            // but we need to check.
            Substance snew = SubstanceBuilder.from(substanceJson).build();
            //hack 27 October 2022 MAM
            snew.setAccess(substance.getAccess());
            scrubAccess(snew);
            Optional<Substance> scrubbedDefs=scrubBasedOnDefDefs(snew);
            if( !scrubbedDefs.isPresent()) {
                return scrubbedDefs;
            }
            snew=scrubbedDefs.get();
            substanceJson = snew.toInternalJsonNode().toString();

            String cleanJson= restrictedJSONSimple(substanceJson);
            snew = SubstanceBuilder.from(cleanJson).build();
            if(scrubberSettings.getSubstanceReferenceCleanup()) {
                cleanUpReferences(snew);
            }
            removeStaleReferences(snew);
            if( scrubberSettings.getApprovalIdCleanup()) {
                scrubApprovalId(snew);
            }
            if(scrubberSettings.UUIDCleanup){
                scrubUUID(snew);
            }
            //keep this separate because earlier versions did not make RegenerateUUIDs depend on UUIDCleanup
            if(scrubberSettings.getRegenerateUUIDs()){
                snew = reassignUuids(snew);
            }
            if(scrubberSettings.getChangeAllStatuses() ) {
                //even null works
                snew.status=scrubberSettings.getChangeAllStatusesNewStatusValue();
            }
            log.trace("successful completion of scrub");
            return Optional.of(snew);
        }
        catch (Exception ex) {
            log.warn("error processing record; Will return empty", ex);
        }
        return Optional.empty();
    }
    
    public static void main(String[] args) {
    	log.trace("main method");
    }
}
