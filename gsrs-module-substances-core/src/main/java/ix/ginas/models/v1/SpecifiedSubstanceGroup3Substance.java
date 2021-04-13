package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasSubstanceDefinitionAccess;

import javax.persistence.*;
import java.util.List;

@Entity
@Inheritance
@DiscriminatorValue("SSIII")
public class SpecifiedSubstanceGroup3Substance extends Substance implements GinasSubstanceDefinitionAccess{
	@OneToOne(cascade= CascadeType.ALL)
	@JoinColumn(name="specified_substance_g3_uuid")
    public SpecifiedSubstanceGroup3Details specifiedSubstanceG3;

    public SpecifiedSubstanceGroup3Substance() {
    	 super (SubstanceClass.specifiedSubstanceG3);
    }

    @JsonIgnore
    public GinasAccessReferenceControlled getDefinitionElement(){
        return specifiedSubstanceG3;
    }

    @Override
   	@JsonIgnore
   	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences(){
   		List<GinasAccessReferenceControlled> temp = super.getAllChildrenCapableOfHavingReferences();
   		if(this.specifiedSubstanceG3!=null){
   			temp.addAll(this.specifiedSubstanceG3.getAllChildrenAndSelfCapableOfHavingReferences());
   		}
   		return temp;
   	}
}
