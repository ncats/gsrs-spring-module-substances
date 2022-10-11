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

@Entity
@Table(name = "ix_ginas_physicalmod", indexes = {@Index(name = "physicalmod_owner_index", columnList = "owner_uuid")})
@JSONEntity(title = "Physical Modification", isFinal = true)
@SingleParent
public class PhysicalModification extends GinasCommonSubData {

    @ParentReference
	@ManyToOne(
	        cascade = CascadeType.PERSIST
	        )
	private Modifications owner;
    @JSONEntity(title = "Physical Modification Role", isRequired = true)
    public String physicalModificationRole;
    
    @JSONEntity(title = "Physical Parameters", isRequired = true, minItems = 1)
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    public List<PhysicalParameter> parameters =
        new ArrayList<PhysicalParameter>();
    
    @JSONEntity(title = "Modification Group")
    public String modificationGroup = "1";
    public PhysicalModification () {}



    @Override
   	@JsonIgnore
   	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
   		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();
   		if(this.parameters!=null){
   			for(PhysicalParameter p: parameters){
   				temp.addAll(p.getAllChildrenAndSelfCapableOfHavingReferences());
   			}
   		}
   		return temp;
   	}
}
