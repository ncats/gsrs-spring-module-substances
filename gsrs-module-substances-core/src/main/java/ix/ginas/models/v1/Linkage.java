package ix.ginas.models.v1;


import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.core.SingleParent;
import ix.core.models.Indexable;
import ix.core.models.ParentReference;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;

import jakarta.persistence.*;
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
	
    //@JsonView(BeanViews.Internal.class)
	public String getSitesShorthand(){
		if(siteContainer!=null){
    		return siteContainer.getShorthand();
    	}
    	return "";
	}

/*
	public void setFromMap(Map m) {
		super.setFromMap(m);
		linkage = (java.lang.String) (m.get("linkage"));
		sites = toDataHolderList(
				(List<Map>) m.get("sites"),
				new DataHolderFactory<gov.nih.ncats.informatics.ginas.shared.model.v1.NASite>() {
					@Override
					public gov.nih.ncats.informatics.ginas.shared.model.v1.NASite make() {
						return new gov.nih.ncats.informatics.ginas.shared.model.v1.NASite();
					}
				});
	}

	@Override
	public Map addAttributes(Map m) {
		super.addAttributes(m);

		m.put("linkage", linkage);
		m.put("sites", toMapList(sites));
		return m;
	}*/

		@Override
		@JsonIgnore
		public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
			List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();
			if(siteContainer!=null){
				temp.addAll(siteContainer.getAllChildrenAndSelfCapableOfHavingReferences());
			}
			return temp;
		}
		

	    @Override
	    public void forceUpdate() {
	        super.forceUpdate();
	        
	        if(siteContainer!=null) {
	            siteContainer.forceUpdate();
	        }
	    }
}
