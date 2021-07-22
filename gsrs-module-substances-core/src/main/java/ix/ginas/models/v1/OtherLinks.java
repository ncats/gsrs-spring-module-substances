package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.core.SingleParent;
import ix.core.models.Indexable;
import ix.core.models.ParentReference;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name="ix_ginas_otherlinks")
@SingleParent
public class OtherLinks extends GinasCommonSubData {
	@ManyToOne(cascade = CascadeType.PERSIST)
	@ParentReference
	private Protein owner;
	
    @Indexable(facet=true,name="Linkage Type")
    public String linkageType;
    
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

    public OtherLinks () {}

    @Override
   	@JsonIgnore
   	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
   		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();
   		if(this.siteContainer!=null){
   				temp.addAll(siteContainer.getAllChildrenAndSelfCapableOfHavingReferences());
   		}
   		return temp;
   	}
}
