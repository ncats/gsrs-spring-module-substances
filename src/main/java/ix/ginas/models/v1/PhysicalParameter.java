package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.core.SingleParent;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;
import ix.ginas.models.utils.JSONEntity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="ix_ginas_physicalpar")
@JSONEntity(title = "Physical Parameter", isFinal = true)
@SingleParent
public class PhysicalParameter extends GinasCommonSubData {
	@ManyToOne(cascade = CascadeType.PERSIST)
	private PhysicalModification owner;
	
    @JSONEntity(title = "Parameter Name", isRequired = true)
    public String parameterName;
    
    @OneToOne(cascade= CascadeType.ALL)
    public Amount amount;

    public PhysicalParameter () {}
    
    public String toString(){
        return parameterName + "," + amount.toString();
    }


    @Override
   	@JsonIgnore
   	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
   		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();
   		if(this.amount!=null){
   				temp.addAll(amount.getAllChildrenAndSelfCapableOfHavingReferences());
   		}
   		return temp;
   	}
}
