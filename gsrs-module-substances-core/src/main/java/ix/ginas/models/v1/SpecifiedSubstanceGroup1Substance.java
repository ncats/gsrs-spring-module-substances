package ix.ginas.models.v1;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

import gov.nih.ncats.common.Tuple;
import ix.ginas.models.GinasAccessControlled;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasSubstanceDefinitionAccess;
import lombok.extern.slf4j.Slf4j;

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
    public List<Tuple<GinasAccessControlled,SubstanceReference>> getDependsOnSubstanceReferencesAndParents(){
        List<Tuple<GinasAccessControlled,SubstanceReference>> srefs=new ArrayList<>();
        srefs.addAll(super.getDependsOnSubstanceReferencesAndParents());
		if(specifiedSubstance!= null && specifiedSubstance.constituents!=null) {
			for (Component c : specifiedSubstance.constituents) {
				srefs.add(Tuple.of(c, c.substance));
			}
		}
        return srefs;
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
