package ix.ginas.models.v1;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import ix.core.util.ModelUtils;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;

@SuppressWarnings("serial")
@Entity
@Table(name="ix_ginas_site_lob")
public class SiteContainer extends GinasCommonSubData{
	@Lob
	@JsonIgnore
	String sitesShortHand;
	@Lob
	@JsonIgnore
	@Column(name="sites_json")
	String sitesJSON;	
	
	long siteCount;
		
	String siteType;

	public SiteContainer() {};

	public SiteContainer(String type){
		this.siteType=type;
	}
	
	public List<Site> getSites(){
		ObjectMapper om = new ObjectMapper();
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		List<Site> sites=new ArrayList<Site>();
		try {
			sites = om.readValue(sitesJSON, new TypeReference<List<Site>>(){});
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return sites;
	}
	
	public String getShorthand(){
		return sitesShortHand;
	}
	public void setShorthand(String shorthand){
		setSites(parseShorthandRanges(shorthand));
	}
	public void setSites(List<Site> sites){
                String beforeShorthand = sitesShortHand;
		if(sites!=null){
			sitesShortHand=generateShorthand(sites);
			
			ObjectMapper om = new ObjectMapper();
			
			List<Site> nlist = sites;
			
			//TODO: this used to be done as a normalizing step
			// but it caused problems with POJODiff
//			List<Site> nlist=parseShorthandRanges(sitesShortHand);
			
			
			sitesJSON=om.valueToTree(nlist).toString();
			siteCount=nlist.size();
		}

                //if something changed, set it to dirty
                //TODO: this kind of dirty detection isn't always
                // ideal, since setters are used by JACKSON,
                // hibernate, POJODiff, and sometimes explicitly.
                // Not every time something is set to dirty is there
                // an intention to update. 
                if((""+sitesShortHand).equals(beforeShorthand)){
		        this.forceUpdate();
                }
	}

	public static List<Site> parseShorthandRanges(String srsdisulf){
		return ModelUtils.parseShorthandRanges(srsdisulf);
	}
	
	public static String generateShorthand(List<Site> sites) {
		return ModelUtils.shorthandNotationFor(sites);
	}
	
	 @Override
	   	@JsonIgnore
	   	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
	   		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();

	   		return temp;
	   	}

}
