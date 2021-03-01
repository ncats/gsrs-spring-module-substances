package gsrs.module.substance.definitional;

import gsrs.module.substance.services.DefinitionalElementImplementation;
import ix.ginas.models.v1.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;
@Slf4j
public class NucleicAcidDefinitionalElementImpl implements DefinitionalElementImplementation {
    @Override
    public boolean supports(Substance s) {
        return s instanceof NucleicAcidSubstance;
    }

    @Override
    public void computeDefinitionalElements(Substance substance, Consumer<DefinitionalElement> consumer) {
        NucleicAcidSubstance nucleicAcidSubstance = (NucleicAcidSubstance) substance;
        if(nucleicAcidSubstance.nucleicAcid ==null || nucleicAcidSubstance.nucleicAcid.subunits ==null){
            return;
        }

        for (int i = 0; i < nucleicAcidSubstance.nucleicAcid.subunits.size(); i++) {
            Subunit s = nucleicAcidSubstance.nucleicAcid.subunits.get(i);
            log.debug("processing subunit with sequence " + s.sequence);
            consumer.accept(DefinitionalElement.of("nucleicAcid.subunits.sequence", s.sequence, 1));

        }


        List<Linkage> linkages = nucleicAcidSubstance.nucleicAcid.getLinkages();
        if(linkages !=null) {
                for (Linkage l : linkages){
                    log.debug("processing linkage " + l.getLinkage());
                    DefinitionalElement linkageHash = DefinitionalElement.of("nucleicAcid.linkages.linkage", l.getLinkage(), 2);
                    consumer.accept(linkageHash);

                    String shorthand = l.getSitesShorthand();
                    //check if siteContainer is null
                    if(shorthand !=null) {
                        log.debug("processing l.siteContainer.sitesShortHand " + shorthand);
                        DefinitionalElement siteElement = DefinitionalElement.of("nucleicAcid.linkages.site", shorthand, 2);
                        consumer.accept(siteElement);
                    }
                }

            }
        List<Sugar> sugars = nucleicAcidSubstance.nucleicAcid.getSugars();
        if(sugars !=null) {
                for (Sugar s : sugars){
                    log.debug("processing sugar " + s.getSugar());
                    DefinitionalElement sugarElement = DefinitionalElement.of("nucleicAcid.sugars.sugar", s.getSugar(), 2);
                    consumer.accept(sugarElement);
                    //check if siteContainer is null
                    String sugarShorthand = s.getSitesShorthand();
                    if(sugarShorthand!=null) {
                        log.debug("processing s.siteContainer.sitesShortHand " + sugarShorthand);
                        DefinitionalElement siteElement = DefinitionalElement.of("nucleicAcid.sugars.site", sugarShorthand, 2);
                        consumer.accept(siteElement);
                    }
                }
            }


        if( nucleicAcidSubstance.modifications != null ){
            for(DefinitionalElement e : nucleicAcidSubstance.modifications.getDefinitionalElements().getElements()){
                consumer.accept(e);
            }
        }
    }
}
