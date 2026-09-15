package ix.ginas.models.v1;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tools.jackson.core.type.TypeReference;

import ix.core.util.ModelUtils;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

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

	public SiteContainer() {}

	@Transient
	private transient final JsonMapper mapper = JsonMapper.builderWithJackson2Defaults()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build();

	public SiteContainer(String type){
		this.siteType=type;
	}
	
	public List<Site> getSites(){
		List<Site> sites=new ArrayList<>();
		try {
			sites = mapper.readValue(sitesJSON, new TypeReference<>() {
			});
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

			List<Site> nlist = sites;
			
			//TODO: this used to be done as a normalizing step
			// but it caused problems with POJODiff

			sitesJSON=mapper.valueToTree(nlist).toString();
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
	   		return new ArrayList<GinasAccessReferenceControlled>();
	   	}

}
