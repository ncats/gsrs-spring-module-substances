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
@Table(name="ix_ginas_linkage")
@SingleParent
public class Linkage extends GinasCommonSubData {
	@ParentReference
	@ManyToOne(cascade = CascadeType.PERSIST)
	private NucleicAcid owner;

	String linkage;
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
	@Indexable
	public String getLinkage() {
		return linkage;
	}

	public void setLinkage(String linkage) {
		this.linkage = linkage;
	}
	
	public String getSitesShorthand(){
		if(siteContainer!=null){
    		return siteContainer.getShorthand();
    	}
    	return "";
	}

	@Override
	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
	    List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();
	    if(siteContainer!=null){
	        temp.addAll(siteContainer.getAllChildrenAndSelfCapableOfHavingReferences());
	    }
	    return temp;
	}
}
