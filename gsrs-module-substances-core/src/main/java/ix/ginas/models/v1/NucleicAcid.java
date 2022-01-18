package ix.ginas.models.v1;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import ix.core.models.BeanViews;
import ix.core.models.Indexable;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;
import ix.ginas.models.utils.JSONConstants;
import ix.ginas.models.utils.JSONEntity;

import javax.persistence.*;
import java.util.*;

@JSONEntity(title = "Nucleic Acid", isFinal = true)
@SuppressWarnings("serial")
@Entity
@Table(name="ix_ginas_nucleicacid")
public class NucleicAcid extends GinasCommonSubData {
	
	@JSONEntity(title = "Linkages")
	@OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
	@JsonView(BeanViews.Full.class)
	List<Linkage> linkages;
	

    @JSONEntity(title = "Sugars", isRequired = true)
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    @JsonView(BeanViews.Full.class)
    List<Sugar> sugars = new ArrayList<>();
	
	
	@JSONEntity(title = "Nucleic Acid Type", format = JSONConstants.CV_NUCLEIC_ACID_TYPE)
	String nucleicAcidType;
	
	@JSONEntity(title = "Nucleic Acid Subtypes", isUniqueItems = true, format = "table", itemsTitle = "Subtype", itemsFormat = JSONConstants.CV_NUCLEIC_ACID_SUBTYPE)
	@JsonIgnore
	private String nucleicAcidSubType;
	
	
    @Indexable(facet=true,name="Sequence Origin")
    String sequenceOrigin;
    
    @Indexable(facet=true,name="Sequence Type")
    String sequenceType;
	
	@JSONEntity(name = "subunits", title = "Subunits")
	@ManyToMany(cascade= CascadeType.ALL)
	@OrderBy("subunitIndex asc")
    @JoinTable(name="ix_ginas_nucleicacid_subunits", inverseJoinColumns = {
            @JoinColumn(name="ix_ginas_subunit_uuid")
    })
	public List<Subunit> subunits = new ArrayList<>();
	

	@Indexable
	public List<Linkage> getLinkages() {
		return linkages;
	}

	public void setLinkages(List<Linkage> linkages) {
		this.linkages = linkages;
	}

	@Indexable
	public String getNucleicAcidType() {
		return nucleicAcidType;
	}

	public void setNucleicAcidType(String nucleicAcidType) {
		this.nucleicAcidType = nucleicAcidType;
	}


	@JsonProperty("nucleicAcidSubType")
	@Indexable(facet=true,name="Nucleic Acid Subtype")
	public List<String> getNucleicAcidSubType() {
		if( this.nucleicAcidSubType != null && this.nucleicAcidSubType.length() > 0){
			String[] type = this.nucleicAcidSubType.split(";");
			return new ArrayList<String>(Arrays.asList(type));
		}else {
			return new ArrayList<String>();
		}
	}

	@JsonProperty("nucleicAcidSubType")
	public void setNucleicAcidSubType(List<String> nucleicAcidSubType) {
		StringBuilder sb = new StringBuilder();
		if(nucleicAcidSubType!=null){
			for(String s:nucleicAcidSubType){
				if(sb.length()>0){
					sb.append(";");
				}
				sb.append(s);
				
			}
		}
		this.nucleicAcidSubType = sb.toString();
	}

	@Indexable
	public String getSequenceOrigin() {
		return sequenceOrigin;
	}

	public void setSequenceOrigin(String sequenceOrigin) {
		this.sequenceOrigin = sequenceOrigin;
	}

	@Indexable
	public String getSequenceType() {
		return sequenceType;
	}

	public void setSequenceType(String sequenceType) {
		this.sequenceType = sequenceType;
	}
	
	@Indexable
	public List<Subunit> getSubunits() {
		Collections.sort(subunits, new Comparator<Subunit>() {
			@Override
			public int compare(Subunit o1, Subunit o2) {
				if(o1.subunitIndex ==null){
					if(o2.subunitIndex ==null){
						return Integer.compare(o2.getLength(), o1.getLength());
					}else{
						return 1;
					}
				}
				return o1.subunitIndex - o2.subunitIndex;
			}
		});
		adoptChildSubunits();
		return this.subunits;
	}

	public void setSubunits(List<Subunit> subunits) {
		this.subunits = subunits;
		adoptChildSubunits();
	}


	@Indexable
	public List<Sugar> getSugars() {
		return sugars;
	}

	public void setSugars(List<Sugar> sugars) {
		this.sugars = sugars;
	}
    
	/**
	 * mark our child subunits as ours.
	 * Mostly used so we know what kind of type
	 * this subunit is by walking up the tree
	 * to inspect its parent (us).
	 */
	@PreUpdate
	@PrePersist
	public void adoptChildSubunits(){
		List<Subunit> subunits=this.subunits;
		for(Subunit s: subunits){
			s.setParent(this);
		}
	}

	@Override
	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();
		if(this.linkages!=null){
			for(Linkage l : this.linkages){
				temp.addAll(l.getAllChildrenAndSelfCapableOfHavingReferences());
			}
		}
		if(this.sugars!=null){
			for(Sugar s : this.sugars){
				temp.addAll(s.getAllChildrenAndSelfCapableOfHavingReferences());
			}
		}
		if(this.subunits!=null){
			for(Subunit s : this.subunits){
				temp.addAll(s.getAllChildrenAndSelfCapableOfHavingReferences());
			}
		}

		return temp;
	}
}