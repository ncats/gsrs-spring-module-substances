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
@DiscriminatorValue("DIV")
@Slf4j
public class StructurallyDiverseSubstance extends Substance implements GinasSubstanceDefinitionAccess{
    @OneToOne(cascade= CascadeType.ALL)
    public StructurallyDiverse structurallyDiverse;

    public StructurallyDiverseSubstance () {
    	super(SubstanceClass.structurallyDiverse);
        
    }
    


    @JsonIgnore
    public GinasAccessReferenceControlled getDefinitionElement(){
        return structurallyDiverse;
    }

    @Override
	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences(){
		List<GinasAccessReferenceControlled> temp = super.getAllChildrenCapableOfHavingReferences();
		if(this.structurallyDiverse!=null){
			temp.addAll(this.structurallyDiverse.getAllChildrenAndSelfCapableOfHavingReferences());
		}
		return temp;
	}
    @Override
    @JsonIgnore
    public List<Tuple<GinasAccessControlled,SubstanceReference>> getDependsOnSubstanceReferencesAndParents(){
        List<Tuple<GinasAccessControlled,SubstanceReference>> srefs=new ArrayList<>();
        srefs.addAll(super.getDependsOnSubstanceReferencesAndParents());
        if(structurallyDiverse.parentSubstance !=null ) {
            srefs.add(Tuple.of(structurallyDiverse,structurallyDiverse.parentSubstance));
        }
        if(structurallyDiverse.hybridSpeciesMaternalOrganism != null) {
        	srefs.add(Tuple.of(structurallyDiverse,structurallyDiverse.hybridSpeciesMaternalOrganism));
        }
        if(structurallyDiverse.hybridSpeciesPaternalOrganism !=null) {
        	srefs.add(Tuple.of(structurallyDiverse,structurallyDiverse.hybridSpeciesPaternalOrganism));
        }
        return srefs;
    }
    
    @Override
    @JsonIgnore
    public List<SubstanceReference> getDependsOnSubstanceReferences(){
        List<SubstanceReference> sref = new ArrayList<SubstanceReference>();
        sref.addAll(super.getDependsOnSubstanceReferences());
        if( structurallyDiverse.parentSubstance !=null ) {
            sref.add(structurallyDiverse.parentSubstance);
        }
        if(structurallyDiverse.hybridSpeciesMaternalOrganism != null) {
            sref.add(structurallyDiverse.hybridSpeciesMaternalOrganism);
        }
        if( structurallyDiverse.hybridSpeciesPaternalOrganism !=null) {
            sref.add(structurallyDiverse.hybridSpeciesPaternalOrganism);
        }
        return sref;
    }
}
