package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.json.GsrsUrlLinkSerializer;
import gsrs.model.GsrsUrlLink;
import ix.core.models.BeanViews;
import ix.core.models.Indexable;
import ix.core.util.ModelUtils;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("serial")
@Entity
@Table(name = "ix_ginas_protein")
@Slf4j
public class Protein extends GinasCommonSubData {
	@Indexable(facet = true, name = "Protein Type")
	public String proteinType;

	
	public String proteinSubType;

	@Indexable(facet = true, name = "Sequence Origin")
	public String sequenceOrigin;

	@Indexable(facet = true, name = "Sequence Type")
	public String sequenceType;

	@Lob
	@JsonIgnore
	@Indexable(indexed = false)
	@Column(name="disulf_json")
	public String disulfJSON = null;

    @Transient
    List<DisulfideLink> tmpDisulfides = null;

	@OneToOne(mappedBy = "protein")
	private ProteinSubstance proteinSubstance;



	@JsonView(BeanViews.Full.class)
	public List<DisulfideLink> getDisulfideLinks() {
		if (tmpDisulfides != null)
			return tmpDisulfides;
		List<DisulfideLink> disulfs = new ArrayList<DisulfideLink>();
		if (this.disulfJSON != null) {
			try {
				ObjectMapper om = new ObjectMapper();
				List l = om.readValue(disulfJSON, List.class);
				for (Object o : l) {
					try {
					    disulfs.add(om.treeToValue(om.valueToTree(o), DisulfideLink.class));
					} catch (Exception e) {
						System.err.println(e.getMessage());
						log.trace("Error parsing disulfides", e);
					}
				}
			} catch (Exception e) {
				log.trace("Error parsing disulfides", e);
			}

		}
		tmpDisulfides = disulfs;
		return tmpDisulfides;
	}

	@JsonView(BeanViews.Compact.class)
	@JsonProperty("_disulfideLinks")
	public DisulfideLinksSummary getJsonDisulfideLinks() {

		List<DisulfideLink> links = getDisulfideLinks();
		if(links.isEmpty()){
			return null;
		}
		return new DisulfideLinksSummary(getProteinSubstance(), links);
	}
	@Data
	private static class DisulfideLinksSummary{
		private int count;
		private String shorthand;
		private GsrsUrlLink href;

		public DisulfideLinksSummary(ProteinSubstance proteinSubstance, List<DisulfideLink> links){
			count= links.size();
			shorthand = ModelUtils.shorthandNotationForLinks(links);

			href= GsrsUrlLink.builder()
					.entityClass(proteinSubstance.getClass())
					.id(proteinSubstance.getOrGenerateUUID().toString())
					.fieldPath("protein/disulfideLinks")
					.build();
		}

		@JsonUnwrapped
		@JsonSerialize(using =GsrsUrlLinkSerializer.class)
		public GsrsUrlLink getHref(){
			return href;
		}

	}

	@JsonView(BeanViews.Compact.class)
	@JsonProperty("_glycosylation")
	public GlycosylationSummary getJsonGlycosylation() {
		if( this.glycosylation ==null){
			return null;
		}
		return new GlycosylationSummary(this);
	}
	@Data
	private static class GlycosylationSummary{
		private String type;
		private Long nsites;
		private Long osites;
		private Long csites;

		private ProteinSubstance proteinSubstance;

		public GlycosylationSummary(Protein p){
			Glycosylation glyc = p.glycosylation;
			if(glyc !=null){
				type = glyc.glycosylationType;
				if(glyc._NGlycosylationSiteContainer !=null) {
					nsites= glyc._NGlycosylationSiteContainer.siteCount;
				}
				if(glyc._OGlycosylationSiteContainer !=null) {
					osites= glyc._OGlycosylationSiteContainer.siteCount;
				}
				if(glyc._CGlycosylationSiteContainer !=null) {
					csites= glyc._CGlycosylationSiteContainer.siteCount;
				}
				proteinSubstance = p.getProteinSubstance();
			}

		}
		@JsonIgnore
		public ProteinSubstance getProteinSubstance() {
			return proteinSubstance;
		}

		@JsonUnwrapped
		@JsonSerialize(using =GsrsUrlLinkSerializer.class)
		public GsrsUrlLink getHref(){
			return  GsrsUrlLink.builder()
					.entityClass(proteinSubstance.getClass())
					.id(proteinSubstance.getOrGenerateUUID().toString())
					.fieldPath("protein/glycosylation")
					.build();
		}

	}

	public void setDisulfideLinks(List<DisulfideLink> links) {
		ObjectMapper om = new ObjectMapper();
		disulfJSON = om.valueToTree(links).toString();
		tmpDisulfides = null;
	}

	@JsonView(BeanViews.Full.class)
	@OneToOne(cascade = CascadeType.ALL)
	public Glycosylation glycosylation;

	@ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name="ix_ginas_protein_subunit", inverseJoinColumns = {
            @JoinColumn(name="ix_ginas_subunit_uuid")
    })
	@OrderBy("subunitIndex asc")
	public List<Subunit> subunits = new ArrayList<Subunit>();
	
	
	/**
	 * This is something that should have existed in earlier versions but did not.
	 * Basically the subtypes _shoud_ have been a list or set to begin with, 
	 * but were accidentally stored as a single string.
	 * @return
	 */
	@JsonIgnore
	@Indexable(facet = true, name = "Protein Subtype")
	public List<String> getProteinSubtypes(){
		return Optional.ofNullable(this.proteinSubType)
			    .flatMap(new Function<String,Optional<Stream<String>>>(){
					@Override
					public Optional<Stream<String>> apply(String arg0) {
						return Optional.of(Arrays.stream(arg0.split("\\|")));
					}
			    })
			    .orElse(Stream.empty())
			    .collect(Collectors.toList());
	}
	
	/**
	 * This is a setter for some future version where protein subtypes are allowed
	 * to be lists rather than single entities.
	 * @param subtypes
	 */
	@JsonIgnore
	public void setProteinSubtypes(List<String> subtypes){
		if(subtypes !=null){
			this.proteinSubType = subtypes.stream().collect(Collectors.joining("|"));
		}
	}

	@OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
	public List<OtherLinks> otherLinks = new ArrayList<OtherLinks>();

	public Protein() {
	}

	/**
	 * mark our child subunits as ours.
	 * Mostly used so we know what kind of type
	 * this subunit is by walking up the tree
	 * to inspect its parent (us).
	 */
	//@PostLoad
	@PreUpdate
	@PrePersist
	public void adoptChildSubunits(){
		List<Subunit> subunits=this.subunits;
		for(Subunit s: subunits){
			s.setParent(this);
		}
	}

	//**************************
	//START MODs
	//**************************
	//This section may need to be removed or re-imagined
	//It's here to support an old style for modifications that's
	//not produced by the forms anymore but may exist elsewhere
	//We may need to port some of the logic to NAs as well, which
	//appear inconsistent.
	//**************************
	
	@JsonIgnore
	@Transient
	private List<Runnable> onParentSet = new ArrayList<>();
	public void setModifications(Modifications mod) {
		if (mod == null) {
			return;
		}
		Runnable r=new Runnable(){

			@Override
			public void run() {
				if(mod!=getProteinSubstance().modifications){
					getProteinSubstance().setModifications(mod);
				}
			}
			
		};
		if(this.proteinSubstance==null){
			onParentSet.add(r);
		}else{
			r.run();
		}
	}
	//**************************
    //END MODs
    //**************************


	@Indexable
	public List<Subunit> getSubunits() {
	    Collections.sort(subunits, new Comparator<Subunit>() {
            @Override
            public int compare(Subunit o1, Subunit o2) {
                if(o1.subunitIndex ==null){
//                  System.out.println("null subunit index");
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
	
	

	/**
	 * Get the residue string at the specified site. Returns null if it does not exist.
	 * @param site
	 * @return
	 */
	public String getResidueAt(Site site) {
		Integer i = site.subunitIndex;
		Integer j = site.residueIndex;

		try {
			for (Subunit su : this.subunits) {
				if (su.subunitIndex.equals(i)) {
					if (j - 1 >= su.sequence.length() || j - 1 < 0)
						return null;
					char res = su.sequence.charAt(j - 1);
					return res + "";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@JsonIgnore
	public void setProteinSubstance(ProteinSubstance proteinSubstance) {
		this.proteinSubstance = proteinSubstance;
		for(Runnable r:onParentSet){
			r.run();
		}
		onParentSet.clear();
	}
	
	@JsonIgnore
	public ProteinSubstance getProteinSubstance() {
		return this.proteinSubstance;
	}

	@Override
	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
	    List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();

	    if(this.glycosylation!=null){
	        temp.addAll(glycosylation.getAllChildrenAndSelfCapableOfHavingReferences());
	    }
	    if(this.subunits!=null){
	        for(Subunit s : this.subunits){
	            temp.addAll(s.getAllChildrenAndSelfCapableOfHavingReferences());
	        }
	    }
	    if(this.otherLinks!=null){
	        for(OtherLinks ol : this.otherLinks){
	            temp.addAll(ol.getAllChildrenAndSelfCapableOfHavingReferences());
	        }
	    }
	    return temp;
	}
}
