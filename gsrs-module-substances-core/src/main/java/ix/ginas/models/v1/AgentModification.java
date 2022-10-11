package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.core.SingleParent;
import ix.core.models.ParentReference;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;
import ix.ginas.models.utils.JSONEntity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@JSONEntity(title = "Agent Modification", isFinal = true)
@Entity
@Table(name="ix_ginas_agentmod", indexes = {@Index(name = "agentmod_owner_index", columnList = "owner_uuid")})
@SingleParent
public class AgentModification extends GinasCommonSubData {

    @ParentReference
	@ManyToOne(
	        cascade = CascadeType.PERSIST
	        )
	private Modifications owner;
	
    @JSONEntity(title = "Process")
    public String agentModificationProcess;
    
    @JSONEntity(title = "Role")
    public String agentModificationRole;
    
    @JSONEntity(title = "Type", isRequired = true)
    public String agentModificationType;
    
    @JSONEntity(title = "Agent Material", isRequired = true)
    @OneToOne(cascade= CascadeType.ALL)
    public SubstanceReference agentSubstance;
    
    @OneToOne(cascade= CascadeType.ALL)
    public Amount amount;
    @JSONEntity(title = "Modification Group")
    public String modificationGroup = "1";
    public AgentModification () {}

    @Override
    @JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();
		if(amount!=null){
			temp.addAll(amount.getAllChildrenAndSelfCapableOfHavingReferences());
		}
		if(agentSubstance!=null){
			temp.addAll(agentSubstance.getAllChildrenAndSelfCapableOfHavingReferences());
		}
		return temp;
	}
}
