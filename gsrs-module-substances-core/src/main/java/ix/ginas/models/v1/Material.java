package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.core.SingleParent;
import ix.core.models.ParentReference;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="ix_ginas_material", indexes = {@Index(name = "material_owner_index", columnList = "owner_uuid")})
@SingleParent
public class Material extends GinasCommonSubData {
	@ParentReference
	@ManyToOne(cascade = CascadeType.PERSIST)
	private Polymer owner;
	
    @OneToOne(cascade= CascadeType.ALL)
    public Amount amount;
    @OneToOne(cascade= CascadeType.ALL)
    public SubstanceReference monomerSubstance;
    public String type;
    public Boolean defining;

    public Material () {}

    @Override
	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();
		if(amount!=null){
			temp.addAll(amount.getAllChildrenAndSelfCapableOfHavingReferences());
		}
		if(monomerSubstance!=null){
			temp.addAll(monomerSubstance.getAllChildrenAndSelfCapableOfHavingReferences());
		}
		return temp;
	}
}
