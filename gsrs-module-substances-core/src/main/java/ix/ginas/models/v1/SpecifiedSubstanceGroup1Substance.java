package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasSubstanceDefinitionAccess;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Inheritance
@DiscriminatorValue("SSI")
@Slf4j
public class SpecifiedSubstanceGroup1Substance extends Substance implements GinasSubstanceDefinitionAccess{
	@OneToOne(cascade= CascadeType.ALL)
    public SpecifiedSubstanceGroup1 specifiedSubstance;

    public SpecifiedSubstanceGroup1Substance() {
    	 super (SubstanceClass.specifiedSubstanceG1);
    }

    @JsonIgnore
    public GinasAccessReferenceControlled getDefinitionElement(){
        return specifiedSubstance;
    }

    @Override
   	@JsonIgnore
   	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences(){
   		List<GinasAccessReferenceControlled> temp = super.getAllChildrenCapableOfHavingReferences();
   		if(this.specifiedSubstance!=null){
   			temp.addAll(this.specifiedSubstance.getAllChildrenAndSelfCapableOfHavingReferences());
   		}
   		return temp;
   	}

	@Override
	@JsonIgnore
	public List<SubstanceReference> getDependsOnSubstanceReferences()
	{
		List<SubstanceReference> sref = new ArrayList<SubstanceReference>();
		sref.addAll(super.getDependsOnSubstanceReferences());
		for (Component c : specifiedSubstance.constituents)
		{
			sref.add(c.substance);
		}
		return sref;
	}
}
