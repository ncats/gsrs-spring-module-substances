package ix.ginas.utils;

import ix.ginas.models.v1.Linkage;
import ix.ginas.models.v1.NucleicAcidSubstance;
import ix.ginas.models.v1.Subunit;
import ix.ginas.models.v1.Sugar;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class NucleicAcidUtils {
	public static int getBaseCount(NucleicAcidSubstance nas){
		int l=0;
		for(Subunit su:nas.nucleicAcid.getSubunits()){
			String sequence = su.sequence;
			l+= sequence==null? 0 :sequence.length();
		}
		return l;
	}
	
	public static int getPossibleSugarSiteCount(NucleicAcidSubstance nas){
		return getBaseCount(nas);
	}
	public static int getActualSugarSiteCount(NucleicAcidSubstance nas){
		log.warn("starting getActualSugarSiteCount");
		int ssites=0;
		for(Sugar s:nas.nucleicAcid.getSugars()){
			ssites+=s.getSites().size();
			log.warn("site total for {} = {}", s.getSugar(), s.getSites().size());
		}
		return ssites;
	}
	public static int getPossibleLinkageSiteCount(NucleicAcidSubstance nas){
		return getBaseCount(nas) - nas.nucleicAcid.getSubunits().size();
	}
	public static int getActualLinkageSiteCount(NucleicAcidSubstance nas){
		int lsites=0;
		if(nas.nucleicAcid !=null) {
			List<Linkage> linkages = nas.nucleicAcid.getLinkages();
			if(linkages !=null) {
				for (Linkage l : linkages) {
					lsites += l.getSites().size();
				}
			}
		}
		return lsites;
	}
	
	public static int getNumberOfUnspecifiedSugarSites(NucleicAcidSubstance nas){
		int psugars=getPossibleSugarSiteCount(nas);
		int asugars=getActualSugarSiteCount(nas);
		
		return psugars-asugars;
		
	}
	public static int getNumberOfUnspecifiedLinkageSites(NucleicAcidSubstance nas){
		int plinkage=getPossibleLinkageSiteCount(nas);
		int alinkage=getActualLinkageSiteCount(nas);
		
		return plinkage-alinkage;
		
	}
	
	
}
