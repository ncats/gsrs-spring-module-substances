package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gsrs.model.Sizeable;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;
import ix.ginas.models.utils.JSONEntity;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="ix_ginas_modifications")
@JSONEntity(name = "modifications", title = "Modifications", isFinal = true)
@Slf4j
public class Modifications extends GinasCommonSubData implements Sizeable {
    @JSONEntity(title = "Agent Modifications")
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    //@JoinTable(name="ix_ginas_mod_agent")
    public List<AgentModification> agentModifications =
        new ArrayList<AgentModification>();
    
    @JSONEntity(title = "Physical Modifications")
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    //@JoinTable(name="ix_ginas_mod_physical")
    public List<PhysicalModification> physicalModifications =
        new ArrayList<PhysicalModification>();
    
    @JSONEntity(title = "Structural Modifications")
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    //@JoinTable(name="ix_ginas_mod_structural")
    public List<StructuralModification> structuralModifications =
        new ArrayList<StructuralModification>();

    public Modifications () {}
    
    @JsonIgnore
    public int getLength(){
        return this.agentModifications.size() + this.physicalModifications.size() + this.structuralModifications.size();
    }
	@JsonIgnore
	@Override
	public int getSize() {
		return getLength();
	}

	@JsonIgnore
    public List<GinasCommonSubData> allModifications(){
    	List<GinasCommonSubData> mods = new ArrayList<GinasCommonSubData>();
    	mods.addAll(this.agentModifications);
    	mods.addAll(this.physicalModifications);
    	mods.addAll(this.structuralModifications);
    	return mods;
    }

    @Override
   	@JsonIgnore
   	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
   		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();
   		for(GinasCommonSubData mod:allModifications()){
   			temp.addAll(mod.getAllChildrenAndSelfCapableOfHavingReferences());
   		}
   		return temp;
   	}

   	//TODO katzelda Mar 2021: moved DefinitionalElements to factory

//		@JsonIgnore
//		public DefinitionalElements getDefinitionalElements() {
//
//			List<DefinitionalElement> definitionalElements = new ArrayList<>();
//			if( agentModifications != null){
//					for(int i =0; i < agentModifications.size(); i++)	{
//					AgentModification a = agentModifications.get(i);
//					//DefinitionalElement agentModElement = DefinitionalElement.of("modifications.agentModificationProcess", a.agentModificationProcess, 2);
//					//definitionalElements.add(agentModElement);
//					if( a.agentSubstance != null ){
//						DefinitionalElement agentModElement = DefinitionalElement.of("modifications.agentSubstance", a.agentSubstance.refuuid, 2);
//						definitionalElements.add(agentModElement);
//						log.debug("processing agent modification with agent substance " + a.agentSubstance.refuuid);
//					}
//					if( a.amount != null) {
//						DefinitionalElement amountElement = DefinitionalElement.of("modifications.amount", a.amount.toString(), 2);
//						definitionalElements.add(amountElement);
//						log.debug("processing agent modification with amount " + a.amount);
//					}
//				}
//			}
//			if( physicalModifications != null) {
//				for (int i = 0; i < physicalModifications.size(); i++) {
//					PhysicalModification p = physicalModifications.get(i);
//					if( p.modificationGroup !=null) {
//						log.debug("processing physical modification " + p.modificationGroup);
//						DefinitionalElement physicalModElement = DefinitionalElement.of("modifications.physicalModificationGroup", p.modificationGroup, 2);
//						definitionalElements.add(physicalModElement);
//					}
//					if( p.physicalModificationRole !=null) {
//						log.debug("processing p.physicalModificationRole " + p.physicalModificationRole);
//						DefinitionalElement physicalModElementProcess = DefinitionalElement.of("modifications.physicalModificationRole", p.physicalModificationRole, 2);
//						definitionalElements.add(physicalModElementProcess);
//					}
//				}
//			}
//
//			if( structuralModifications != null) {
//				for (int i = 0; i < structuralModifications.size(); i++) {
//					StructuralModification sm = structuralModifications.get(i);
//					if( sm.modificationGroup != null ) {
//						log.debug("processing structural modification with group " + sm.modificationGroup);
//						DefinitionalElement structModElement = DefinitionalElement.of("modifications.structuralModifications.group", sm.modificationGroup, 2);
//						definitionalElements.add(structModElement);
//					}
//					if( sm.residueModified != null){
//						log.debug("processing sm.residueModified " + sm.residueModified);
//						DefinitionalElement structModResidueElement = DefinitionalElement.of("modifications.structuralModifications.residueModified", sm.residueModified, 2);
//						definitionalElements.add(structModResidueElement);
//					}
//				}
//			}
//			return new DefinitionalElements(definitionalElements);
//		}
}
