package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import gsrs.module.substance.definitional.DefinitionalElement;
import ix.core.util.EntityUtils;
import ix.core.util.LogUtil;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasSubstanceDefinitionAccess;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("serial")
@Entity
@Inheritance
@DiscriminatorValue("PRO")
@Slf4j
public class ProteinSubstance extends Substance implements GinasSubstanceDefinitionAccess {
	@OneToOne(cascade= CascadeType.ALL)
    public Protein protein;

    
    public ProteinSubstance () {
        super (SubstanceClass.protein);
    }
    @Override
    public boolean hasModifications(){
    	if(this.modifications!=null){
    		if(this.modifications.agentModifications.size()>0 || this.modifications.physicalModifications.size()>0 || this.modifications.structuralModifications.size()>0){
    			return true;
    		}
    	}
		return false;
    }
    
    @Override
    public Modifications getModifications(){
    	return this.modifications;
    }
    
    
    @Transient
    private boolean _dirtyModifications=false;
    
    
    
    public void setModifications(Modifications m){
    	if(this.protein==null){
    		this.protein = new Protein();
    		_dirtyModifications=true;
    	}
    	this.modifications=m;
    	this.protein.setModifications(m);
    }
    
    public void setProtein(Protein p){
    	this.protein=p;
    	if(this.protein !=null) {
			this.protein.setProteinSubstance(this);
			//TODO do we still unset the dirtyMod flag if protein is null?
			if(_dirtyModifications){

				this.protein.setModifications(this.modifications);
				_dirtyModifications=false;
			}
		}
    }

	@JsonIgnore
	public GinasAccessReferenceControlled getDefinitionElement(){
		return protein;
	}
    
	@Override
	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences(){
		List<GinasAccessReferenceControlled> temp = super.getAllChildrenCapableOfHavingReferences();
		if(this.protein!=null){
			temp.addAll(this.protein.getAllChildrenAndSelfCapableOfHavingReferences());
		}
		return temp;
	}

	public ProteinSubstance copy() throws JsonProcessingException {
		log.trace("in ProteinSubstance.copy method");
			return EntityUtils.EntityWrapper.of(this).getClone();
	}

	public ProteinSubstance canonicalize() {
		log.trace("starting in ProteinSubstance.canonicalize");
		//make a copy of the subunits
		List<Subunit> orderedSubunits = new ArrayList<>(this.protein.subunits);//sort the subunits by canonical sort order
		Collections.sort(orderedSubunits, SubunitComparator.INSTANCE);//look through each subunit
		Map<Integer, Integer> subunitIndexMap = new HashMap<>();
		for(int i=0;i<orderedSubunits.size();i++){
			//the OLD index (as used by sites, etc) is whatever subunitIndex it had
			int oindex = orderedSubunits.get(i).subunitIndex; //already 1-index on the actual property   
			//the NEW index (as it would be used by sites after canonicalization) is whatever its current
			//index is in the sorted array (+1)
			int nindex = i + 1; // 0-index on the incremental count, so add 1   //a map from old index to new index is added to the map for later use
			subunitIndexMap.put(oindex, nindex);
		}
		boolean performTranslations = false;
		log.trace("subunit map after sorting");
		for (Integer k : subunitIndexMap.keySet()) {
			String msg = String.format("mapped site %d to %d", k, subunitIndexMap.get(k));
			if (!k.equals(subunitIndexMap.get(k))) {
				performTranslations = true;
			}
			log.debug(msg);
		}
		log.trace("performTranslations: " + performTranslations);
		Glycosylation glycosylation = this.protein.glycosylation;
		if (glycosylation != null && performTranslations) {
			List<Site> translatedNGlycosylationSites = translateSites(glycosylation.getNGlycosylationSites(), subunitIndexMap);
			glycosylation.setNGlycosylationSites(translatedNGlycosylationSites);
			List<Site> translatedOGlycosylationSites = translateSites(glycosylation.getOGlycosylationSites(), subunitIndexMap);
			glycosylation.setOGlycosylationSites(translatedOGlycosylationSites);
			List<Site> translatedCGlycosylationSites = translateSites(glycosylation.getCGlycosylationSites(), subunitIndexMap);
			glycosylation.setCGlycosylationSites(translatedCGlycosylationSites);
		}
		
		List<DisulfideLink> disulfideLinks = this.protein.getDisulfideLinks();
		if (disulfideLinks != null && performTranslations) {
			log.trace("going to translate sites within disulfide links");
			for (DisulfideLink disulfideLink : disulfideLinks) {
				if (disulfideLink != null) {
					List<Site> originalDisulfideLinkSites = disulfideLink.getSites();
					log.trace("going to translate sites for link " + disulfideLink.getSitesShorthand());
					List<Site> translatedDisulfideLinkSites = translateSites(originalDisulfideLinkSites, subunitIndexMap);
					//temporarily add debug info
					LogUtil.trace(new Supplier<String>() {

						@Override
						public String get() {

							StringBuilder builder = new StringBuilder();
							for (int i = 0; i < disulfideLink.getSites().size(); i++) {
								builder.append(String.format("old site: %s; new site: %s%n", originalDisulfideLinkSites.get(i).toString(),
										translatedDisulfideLinkSites.get(i).toString()));

							}
							return builder.toString();
						}
					});
					disulfideLink.setSites(translatedDisulfideLinkSites);
				}
				this.protein.setDisulfideLinks(disulfideLinks);
			}
			
			List<OtherLinks> otherLinks = this.protein.otherLinks;
			if (otherLinks != null && performTranslations) {
				for (OtherLinks otherLink : otherLinks) {
					if (otherLink == null) {
						continue;
					}
					List<Site> sites = otherLink.getSites();
					List<Site> translatedOtherLinkSites = sites;
					log.trace("going to translate sites for other links");
					translatedOtherLinkSites = translateSites(sites, subunitIndexMap);
					otherLink.setSites(translatedOtherLinkSites);
				}
				this.protein.otherLinks = otherLinks;
			}
		}
		if(performTranslations&& this.hasModifications() && this.modifications.structuralModifications.size() >0 ) {
			log.trace("going to translate sites for struct mods");
			for(StructuralModification structMod : this.modifications.structuralModifications) {
				for(Site site: structMod.siteContainer.getSites()) {
					int originalSubunitIndex = site.subunitIndex;
					site.subunitIndex = subunitIndexMap.get(site.subunitIndex);
					LogUtil.trace(new Supplier<String>() {

						@Override
						public String get() {
							return String.format("translated site subunit for struct mod from %d to %d",
									originalSubunitIndex,
									site.subunitIndex);
						}

					});

				}
			}
		}
		return this;
	}

	private List<Site> translateSites(List<Site> startingSites, Map<Integer, Integer> subunitChanges) {
		log.trace("starting in translateSites. total sites " + startingSites.size());
		List<Site> translatedSites = new ArrayList<>();
		for (Site site : startingSites) {
			int startingSiteIndex = site.subunitIndex;
			log.trace("Looking for mapped subunit index " + startingSiteIndex);
			if (!subunitChanges.containsKey(startingSiteIndex)) {
				log.error("site index not found for site " + site.toString());
			}
			int newSubunitIndex = subunitChanges.get(startingSiteIndex);
			int newSubunit = newSubunitIndex;
			LogUtil.trace(new Supplier<String>() {

							  @Override
							  public String get() {
								  return String.format("index changed from %d to %d", site.subunitIndex, newSubunit);
							  }
						  });
			translatedSites.add(new Site(newSubunit, site.residueIndex));
		}
		return translatedSites;
	}
}
