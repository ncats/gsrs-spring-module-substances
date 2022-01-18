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

@SuppressWarnings("serial")
@Entity
@Table(name="ix_ginas_sugar")
@SingleParent
public class Sugar extends GinasCommonSubData {
	@ManyToOne(cascade = CascadeType.PERSIST)
	@ParentReference
	private NucleicAcid owner;

	String sugar;

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
    
    
    public void setSitesShorthand(String sites){
    	if(siteContainer==null){
    		siteContainer=new SiteContainer(this.getClass().getName());
    	}
    	siteContainer.setShorthand(sites);
    }

    public String getSitesShorthand(){
        if(siteContainer!=null){
            return siteContainer.getShorthand();
        }
        return "";
    }

    @Indexable
	public String getSugar() {
		return sugar;
	}

	public void setSugar(String sugar) {
		this.sugar = sugar;
	}



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
