package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="ix_ginas_ssg1")
public class SpecifiedSubstanceGroup1 extends GinasCommonSubData {
	@ManyToMany(cascade= CascadeType.ALL)
    @JoinTable(name="ix_ginas_substance_ss_comp", inverseJoinColumns = {
            @JoinColumn(name="ix_ginas_component_uuid")
    })
	public List<SpecifiedSubstanceComponent> constituents;
	
	public int size(){
		return constituents.size();
	}

	@Override
	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();

		if (this.constituents != null) {
			for (SpecifiedSubstanceComponent s : this.constituents) {
				temp.addAll(s.getAllChildrenAndSelfCapableOfHavingReferences());
			}
		}

		return temp;
	}

}