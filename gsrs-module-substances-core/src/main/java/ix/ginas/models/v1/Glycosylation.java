package ix.ginas.models.v1;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import ix.core.models.Indexable;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;
import ix.ginas.models.utils.JSONEntity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("serial")
@Entity
@Table(name="ix_ginas_glycosylation")
@JSONEntity(name = "glycosylation", title = "Glycosylation", isFinal = true)
public class Glycosylation extends GinasCommonSubData {

	@OneToOne(mappedBy="glycosylation")
    private Protein protein;
	
	
//	@Transient
    
	@JsonIgnore
    @Indexable(indexed=false)
	@OneToOne(cascade= CascadeType.ALL)
    @JoinColumn(name="c_glycosylation_sites_uuid")
    SiteContainer _CGlycosylationSiteContainer;
	
    
    @JsonIgnore
    @Indexable(indexed=false)
    @OneToOne(cascade= CascadeType.ALL)
    @JoinColumn(name="n_glycosylation_sites_uuid")
    SiteContainer _NGlycosylationSiteContainer;
    
    
    
    @JsonIgnore
    @Indexable(indexed=false)
    @OneToOne(cascade= CascadeType.ALL)
    @JoinColumn(name="o_glycosylation_sites_uuid")
    SiteContainer _OGlycosylationSiteContainer;
    
    

    @JsonProperty("CGlycosylationSites")
    public List<Site> getCGlycosylationSites(){
    	if(_CGlycosylationSiteContainer!=null){
    		return _CGlycosylationSiteContainer.getSites();
    	}
    	return new ArrayList<Site>();
    }
    @JsonProperty("CGlycosylationSites")
    public void setCGlycosylationSites(List<Site> sites){
    	if(_CGlycosylationSiteContainer==null){
    		_CGlycosylationSiteContainer=new SiteContainer(this.getClass().getName());
    	}
    	_CGlycosylationSiteContainer.setSites(sites);
    }
    

    @JsonProperty("NGlycosylationSites")
    public List<Site> getNGlycosylationSites(){
    	if(_NGlycosylationSiteContainer!=null){
    		return _NGlycosylationSiteContainer.getSites();
    	}
    	return new ArrayList<Site>();
    }
    
    @JsonProperty("NGlycosylationSites")
    public void setNGlycosylationSites(List<Site> sites){
    	if(_NGlycosylationSiteContainer==null){
    		_NGlycosylationSiteContainer=new SiteContainer(this.getClass().getName());
    		
    	}
    	_NGlycosylationSiteContainer.setSites(sites);
    }
    

    @JsonProperty("OGlycosylationSites")
    public List<Site> getOGlycosylationSites(){
    	if(_OGlycosylationSiteContainer!=null){
    		return _OGlycosylationSiteContainer.getSites();
    	}
    	return new ArrayList<Site>();
    }
    
    @JsonProperty("OGlycosylationSites")
    public void setOGlycosylationSites(List<Site> sites){
    	if(_OGlycosylationSiteContainer==null){
    		_OGlycosylationSiteContainer=new SiteContainer(this.getClass().getName());
    	}
    	_OGlycosylationSiteContainer.setSites(sites);
    }
    
    @Indexable(facet=true,name="Glycosylation Type")
    public String glycosylationType;
    
    

    public Glycosylation () {}
    
    @JsonIgnore
    public List<Site> getAllSites(){
    	List<Site> allSites=new ArrayList<Site>();
    	
    	allSites.addAll(this.getOGlycosylationSites());
    	allSites.addAll(this.getNGlycosylationSites());
    	allSites.addAll(this.getCGlycosylationSites());
    	
        return allSites;
    }
    
    
    @JsonIgnore
    @Indexable(facet=true,name="Glycosylation Site Count")
    public int getSiteCount(){
    	int count = 0;
        try {
                count+=this.getCGlycosylationSites().size();
                count+=this.getOGlycosylationSites().size();
                count+=this.getNGlycosylationSites().size();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return count;
    }

	@Override
	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();
		if(_CGlycosylationSiteContainer!=null){
			temp.addAll(_CGlycosylationSiteContainer.getAllChildrenAndSelfCapableOfHavingReferences());
		}
		if(_NGlycosylationSiteContainer!=null){
			temp.addAll(_NGlycosylationSiteContainer.getAllChildrenAndSelfCapableOfHavingReferences());
		}
		if(_OGlycosylationSiteContainer!=null){
			temp.addAll(_OGlycosylationSiteContainer.getAllChildrenAndSelfCapableOfHavingReferences());
		}
		return temp;
	}

    @Override
    public void forceUpdate() {
        super.forceUpdate();
        
        if(_CGlycosylationSiteContainer!=null) {
            _CGlycosylationSiteContainer.forceUpdate();
        }
        if(_NGlycosylationSiteContainer!=null) {
            _NGlycosylationSiteContainer.forceUpdate();
        }
        if(_OGlycosylationSiteContainer!=null) {
            _OGlycosylationSiteContainer.forceUpdate();
        }
    }
}
