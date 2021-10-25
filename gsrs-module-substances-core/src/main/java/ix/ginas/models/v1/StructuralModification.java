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
@Table(name="ix_ginas_structuralmod")
@JSONEntity(title = "Structural Modification", isFinal = true)
@SingleParent
public class StructuralModification extends GinasCommonSubData {
	@ParentReference
	@ManyToOne(
	        cascade = CascadeType.PERSIST
	        )
	private Modifications owner;
	
    @JSONEntity(title = "Modification Type", isRequired = true)
    public String structuralModificationType;
    
    @JSONEntity(title = "Modification Location Type")
    public String locationType;
    
    @JSONEntity(title = "Residue Modified")
    public String residueModified;
    
    @JsonIgnore
	@OneToOne(cascade= CascadeType.ALL)
    SiteContainer siteContainer;
    
    
    public List<Site> getSites(){
    	if(siteContainer!=null){
    		return siteContainer.getSites();
    	}
    	return new ArrayList<Site>();
    }
    
    public void setSites(List<Site> sites){
    	if(siteContainer==null){
    		siteContainer=new SiteContainer(this.getClass().getName());
    	}
    	siteContainer.setSites(sites);
    }
    
    @JSONEntity(title = "Extent", values = "JSONConstants.ENUM_EXTENT", isRequired = true)
    public String extent;

    @OneToOne(cascade= CascadeType.ALL)
    public Amount extentAmount;
    @OneToOne(cascade= CascadeType.ALL)
    public SubstanceReference molecularFragment;


    @JSONEntity(title = "Modified Fragment Role")
    public String moleculareFragmentRole;
    
    @JSONEntity(title = "Modification Group")
    public String modificationGroup = "1";
    
    
    public StructuralModification () {}
	@JsonIgnore
	public String getMoleculareFragmentRole(){
		return moleculareFragmentRole;
	}

	public String getMolecularFragmentRole(){
		return moleculareFragmentRole;
	}
	/**
	 * This is purposefully spelled wrong for backwards compatibility
	 * purposed when an older version of GSRS had a typo in the name
	 * so we can parse old JSON.
	 * @param molecularFragmentRole
	 */
	public void setMolecularFragmentRole(String molecularFragmentRole){
    	this.moleculareFragmentRole = molecularFragmentRole;
	}
    @Override
   	@JsonIgnore
   	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
   		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();

   		if(this.siteContainer!=null){
   			temp.addAll(siteContainer.getAllChildrenAndSelfCapableOfHavingReferences());
   		}
   		if(this.extentAmount!=null){
			temp.addAll(extentAmount.getAllChildrenAndSelfCapableOfHavingReferences());
		}
   		if(this.molecularFragment!=null){
			temp.addAll(molecularFragment.getAllChildrenAndSelfCapableOfHavingReferences());
		}

   		return temp;
   	}
}
