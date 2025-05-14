package ix.ginas.models.v1;


import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.core.IgnoredModel;
import ix.core.models.Indexable;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSimplifiedSubData;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("serial")
@Entity
@Table(name="ix_ginas_disulfide")
//@IgnoredModel
public class DisulfideLink extends GinasCommonSimplifiedSubData {
	
	@JsonIgnore
	@OneToOne(cascade= CascadeType.ALL)
	@Indexable(indexed=false)
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

    // @JsonView(BeanViews.Internal.class)
	public String getSitesShorthand(){
		if(siteContainer!=null){
    		return siteContainer.getShorthand();
    	}
    	return "";
	}
	
	@JsonIgnore
	public String getLinksShorthand(){
		return getSitesShorthand().replace(";", "->");
	}
	
    public DisulfideLink () {}
    
    @Override
    @JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
		return new ArrayList<>();
	}
    

    @Override
    public void forceUpdate() {
        super.forceUpdate();
        siteContainer.forceUpdate();
    }
}
