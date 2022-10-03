package gsrs.module.substance.scrubbers.basic;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;

import gov.nih.ncats.common.stream.StreamUtil;
import ix.core.EntityFetcher;
import ix.core.models.Group;
import ix.core.models.Keyword;
import ix.core.util.EntityUtils;
import ix.ginas.exporters.RecordScrubber;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.GinasAccessControlled;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;
import ix.ginas.models.GinasSubstanceDefinitionAccess;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Note;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.Substance.SubstanceClass;
import ix.ginas.models.v1.Substance.SubstanceDefinitionLevel;
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
    private Set<String> groupsToRemove;
    private Set<String> codeSystemsToRemove;
    private Set<String> codeSystemsToKeep;
    private BasicSubstanceScrubberParameters scrubberSettings;

    public BasicSubstanceScrubber(BasicSubstanceScrubberParameters scrubberSettings){
        this.scrubberSettings = scrubberSettings;
        groupsToInclude= Optional.ofNullable(scrubberSettings.getAccessGroupsToInclude()).orElse(Collections.emptyList()).stream()
                .map(t->t.trim())
                .filter(t->t.length()>0)
                .collect(Collectors.toSet());
        
        
        //TODO: not entirely sure when we'd use this
        groupsToRemove= Optional.ofNullable(scrubberSettings.getAccessGroupsToRemove()).orElse(Collections.emptyList()).stream()
                .map(t->t.trim())
                .filter(t->t.length()>0)
                .collect(Collectors.toSet());
        codeSystemsToRemove = Optional.ofNullable(scrubberSettings.getCodeSystemsToRemove()).orElse(Collections.emptyList()).stream()
                .map(t->t.trim())
                .filter(t->t.length()>0)
                .collect(Collectors.toSet());
        codeSystemsToKeep=Optional.ofNullable(scrubberSettings.getCodeSystemsToKeep()).orElse(Collections.emptyList()).stream()
                .map(t->t.trim())
                .filter(t->t.length()>0)
                .collect(Collectors.toSet());
    }
    
    private void forEachObjectWithAccess(Substance s, Consumer<GinasAccessControlled> consumer) {
    	StreamUtil.with(s.getAllChildrenCapableOfHavingReferences().stream().map(n->(GinasAccessControlled)n))
    			  .and(s.references.stream().map(n->(GinasAccessControlled)n))
    			  .and(s)
    			  .stream()
    			  .distinct()
    			  .forEach(ss->{
    				  consumer.accept(ss);
    			  });
    }
    
    private void forEachObjectWithReferences(Substance s, Consumer<GinasAccessReferenceControlled> consumer) {
    	StreamUtil.with(s.getAllChildrenCapableOfHavingReferences().stream())
    			  .stream()
    			  .forEach(ss->{
    				  consumer.accept(ss);
    			  });
    }
    
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
    		Set<Keyword> refs= o.getReferencesAsUUIDs()
    						    .stream()
    						    .filter(uu->references.contains(uu))
    						    .map(uu->new Keyword(GinasCommonSubData.REFERENCE,uu.toString()))
    						    .collect(Collectors.toSet());
    		o.setReferences(refs);
    		
    	});
    	
    }


    private Substance scrubAccess(Substance starting) throws IOException {
    	Set<Group> toDelete = new HashSet<>();
    	toDelete.add(new Group(TO_DELETE));
    	Set<Group> toKeep = new HashSet<>();
    	toKeep.add(new Group(TO_FORCE_KEEP));
    	
    	boolean[] isDefinitionScrubbed = new boolean[] {false};
    	GinasAccessReferenceControlled mainDefinition = null;
    	
    	if(starting instanceof GinasSubstanceDefinitionAccess) {
    		GinasSubstanceDefinitionAccess def = (GinasSubstanceDefinitionAccess)starting;
    		mainDefinition=def.getDefinitionElement();
    	}
    	GinasAccessReferenceControlled finalMainDefinition = mainDefinition;    	
    	
    	if(scrubberSettings.isRemoveAllLocked()) {
    		forEachObjectWithAccess(starting, (bm)->{
    			
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
    	}
    	
    	//remove all definitions if this setting is true
    	if(isDefinitionScrubbed[0] && scrubberSettings.isRemoveScrubbedDefinitionalElementsEntirely()) {
    		starting.properties.stream()
					    		.filter(prop->prop.isDefining())
					    		.forEach(p->{
					    			p.setAccess(toDelete);
					    		});
    		
    		starting.modifications.setAccess(toDelete);
    		if(starting instanceof ChemicalSubstance) {
    			ChemicalSubstance chem = (ChemicalSubstance)starting;
    			chem.moieties.forEach(m->{
    				m.setAccess(toDelete);
    			});
    		}    		
    	}
    	if(isDefinitionScrubbed[0] && scrubberSettings.isSetScrubbedDefinitionalElementsIncomplete()) {
    		starting.definitionLevel=SubstanceDefinitionLevel.INCOMPLETE;
    	}
    	
       	if(isDefinitionScrubbed[0] && scrubberSettings.isConvertScrubbedDefinitionsToConcepts()) {
    		starting.substanceClass=SubstanceClass.concept;
    	}
       	
       	if(isDefinitionScrubbed[0] && scrubberSettings.getAddNoteToScrubbedDefinitions()!=null) {
       		Note nn=starting.addNote(scrubberSettings.getAddNoteToScrubbedDefinitions());
       		nn.setAccess(toKeep);
       	}
        
        return starting;
    }

    private Substance scrubApprovalId(Substance substance) {
        String approvalId = substance.getApprovalID();
        System.out.printf("in scrubApprovalId, approvalId: %s\n", approvalId);
        if(approvalId!=null && approvalId.length()>0 && this.scrubberSettings.getApprovalIdCodeSystem()!= null
                && this.scrubberSettings.getApprovalIdCodeSystem().length()>0
                && this.scrubberSettings.isCopyApprovalIdToCode()) {
            boolean foundCode =false;
            Optional<Code> code=substance.codes.stream().filter(c->c.codeSystem.equals(this.scrubberSettings.getApprovalIdCodeSystem())).findFirst();
            if( code.isPresent()) {
                System.out.println("code already present");
                code.get().setCode(approvalId);
            }else{
                System.out.println("will create code");
                Code approvalIdCode= new Code();
                approvalIdCode.codeSystem=scrubberSettings.getApprovalIdCodeSystem();
                approvalIdCode.code=approvalId;
                approvalIdCode.type="PRIMARY";
                substance.addCode(approvalIdCode);
            }
        }
        return substance;
    }

    public String restrictedJSONSimple(String s) {
        System.out.println("starting restrictedJSONSimple 4");
        DocumentContext dc = JsonPath.parse(s);

        if( scrubberSettings.isRemoveNotes()){
            log.trace("deleting notes");
            dc.delete("$['notes'][?]", (ctx)->{
            	Map code=ctx.item(Map.class);
            	Object o=code.get("access");
            	if(o!=null && o.toString().contains(TO_FORCE_KEEP)) {
            		return false;
            	}
            	return true;
            });
        }

        if( scrubberSettings.isRemoveChangeReason()){
            dc.delete("$.changeReason");
        }

        if(scrubberSettings.isRemoveDates()) {
            dc.delete("$..lastEdited");
            dc.delete("$..created");
        }
        if(scrubberSettings.isAuditInformationCleanup()) {
        	if(scrubberSettings.isDeidentifyAuditUser()) {
        		dc.delete("$..lastEditedBy");
        		dc.delete("$..createdBy");
        		dc.delete("$..approvedBy");
        	}else if(scrubberSettings.getNewAuditorValue()!=null) {
        		String newAuditor = scrubberSettings.getNewAuditorValue();
        		
        		dc.set("$..lastEditedBy", newAuditor);
        		dc.set("$..createdBy", newAuditor);
        		dc.set("$..approvedBy", newAuditor);
        	}
        }
        
        if(scrubberSettings.isRemoveCodesBySystem() && (!codeSystemsToRemove.isEmpty() || !codeSystemsToKeep.isEmpty())){
            Predicate codeSystemPredicate = context -> {
                System.out.println("hello from codeSystemPredicate");
                Map code=context.item(Map.class);
                String codeSystem= (String)code.get("codeSystem");
                System.out.printf("got codeSystem %s\n", codeSystem);
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
        //TODO Keep list for codes?
        
        dc.delete("$..[?(@.access[0]===\"" + TO_DELETE + "\")]");
        
        
        
        return dc.jsonString();
    }


    public String testAll(String json){
        DocumentContext dc = JsonPath.parse(json);
        //"$..[?]",
        String output;
        try{
            output= dc.read("$..[?]['access']", (s)-> {

                System.out.printf("item class: %s; root: %s\n", s.item().getClass().getName(), s.root().getClass().getName());
                if( s.item().getClass().equals(JSONArray.class)){
                    JSONArray array = s.item(JSONArray.class);
                    if( array == null) {
                        System.out.println("item null");
                    } else {
                        System.out.printf("array size: %d",array.size());
                        array.forEach(a-> System.out.printf("element type: %s\n", a.getClass().getName()));
                    }

                }
                System.out.println(s.item());
                return true;
            });
        }
        catch (Exception ex){
            System.err.println("Error: " + ex.getMessage());
            output="error!";
        }
        return output;
    }
    
    
    @SneakyThrows
    @Override
    public Optional<Substance> scrub(Substance substance) {
        log.trace("starting in scrub");
       
        log.trace("cast to substance with UUID {}", (substance.uuid==null) ? "null" : substance.uuid.toString());

        
        String substanceJson;
        try {
        	//just a force thing to fetch if needed
            substanceJson = substance.toFullJsonNode().toString();
//            msub=substance;
            System.out.println("before");
            System.out.println(substanceJson);
        } catch (Exception ex){
            log.error("Error retrieving substance; using alternative method");
            EntityUtils.Key skey = EntityUtils.Key.of(Substance.class, substance.uuid);
            Optional<Substance> substanceRefetch = EntityFetcher.of(skey).getIfPossible().map(o->(Substance)o);
//            msub=substanceRefetch.orElse(null);
            substanceJson = substanceRefetch.get().toFullJsonNode().toString();
        }
       
        
        
        
        log.trace("got json");
        try {
        	 //TODO: confirm if this forces as a concept. It should not,
            // but we need to check.
            Substance snew = SubstanceBuilder.from(substanceJson).build();            
            scrubAccess(snew);
            substanceJson = snew.toFullJsonNode().toString();
            String cleanJson= restrictedJSONSimple(substanceJson);
            snew = SubstanceBuilder.from(cleanJson).build();
            removeStaleReferences(snew);
            scrubApprovalId(snew);
            return Optional.ofNullable(snew);
        }
        catch (Exception ex) {
            log.warn("error processing record; Will return empty", ex);
        }
        return Optional.empty();
    }
    
    public static void main(String[] args) {
    	System.out.println("ASDASD");
    }
}
