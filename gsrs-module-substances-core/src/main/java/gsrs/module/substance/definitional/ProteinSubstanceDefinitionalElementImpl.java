package gsrs.module.substance.definitional;

import gsrs.module.substance.services.DefinitionalElementFactory;
import gsrs.module.substance.services.DefinitionalElementImplementation;
import ix.ginas.models.v1.*;
import lombok.extern.slf4j.Slf4j;
import org.jcvi.jillion.core.residue.aa.AminoAcid;
import org.jcvi.jillion.core.residue.aa.ProteinSequence;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class ProteinSubstanceDefinitionalElementImpl implements DefinitionalElementImplementation {

    @Autowired
    private DefinitionalElementFactory definitionalElementFactory;

    public DefinitionalElementFactory getDefinitionalElementFactory() {
        return definitionalElementFactory;
    }

    public void setDefinitionalElementFactory(DefinitionalElementFactory definitionalElementFactory) {
        this.definitionalElementFactory = definitionalElementFactory;
    }

    @Override
    public boolean supports(Object s) {
        return s instanceof ProteinSubstance;
    }

    private List<Subunit> asCanonicalListOrder(List<Subunit> originalOrder, Map<Integer, Integer> canonicalOrder){
        List<Subunit> list = new ArrayList<>(originalOrder.size());
        for(int i=0; i< originalOrder.size(); i++){
            list.add(null);
        }
        for(Subunit s : originalOrder){
            Integer oldIndex = s.subunitIndex;
            if(oldIndex !=null){
                //order map is 1-based
              list.set(canonicalOrder.get(s.subunitIndex) -1, s);
            }
        }
        return list;
    }

    private List<Site> canonicalizeSites(List<Site> oldSites, Map<Integer, Integer> canonicalOrder){
        return oldSites.stream()
                .map(s-> new Site(canonicalOrder.get(s.subunitIndex), s.residueIndex))
                .collect(Collectors.toList());
    }

    @Override
    public void computeDefinitionalElements(Object substance, Consumer<DefinitionalElement> consumer) {

        ProteinSubstance proteinSubstance = (ProteinSubstance)substance;
        //this implementation is different than the version in GSRS 2.x in that
        //the old version made a copy of the ProteinSubstance to reorder the subunits and all corresponding  Sites
        //but the way GSRS makes a copy of a Substance is very computationally expensive and at least in the 2.x
        //required a running GSRS instance which made this impossible to run stand alone.

        //We don't actually need to make a copy and modify the order of the sites if we just keep better record keeping in
        //this method we only need to make new Site objects locally here and it should work...

        Map<Integer, Integer> canonicalOrder = canonicalOrderOfSubunits(proteinSubstance);
        log.trace("starting in ProteinSubstance.addDefinitionalElements");
        for(Subunit s : asCanonicalListOrder( proteinSubstance.protein.subunits, canonicalOrder)){
            if(s !=null && s.sequence !=null){
                ProteinSequence seq = ProteinSequence.of(AminoAcid.cleanSequence(s.sequence));

                consumer.accept(DefinitionalElement.of("subunitIndex.", s.subunitIndex==null? null: Integer.toString(canonicalOrder.get(s.subunitIndex)), 1));
                consumer.accept(DefinitionalElement.of("subunitSeq.", seq.toString(), 1));
                consumer.accept(DefinitionalElement.of("subunitSeqLength.", Long.toString(seq.getLength()), 1));

            }
        }

        Glycosylation glycosylation = proteinSubstance.protein.glycosylation;
        if(glycosylation !=null){
            handleGlycosylationSites(canonicalizeSites(glycosylation.getNGlycosylationSites(), canonicalOrder), "N", consumer);
            handleGlycosylationSites(canonicalizeSites(glycosylation.getOGlycosylationSites(), canonicalOrder), "O", consumer);
            handleGlycosylationSites(canonicalizeSites(glycosylation.getCGlycosylationSites(), canonicalOrder), "C", consumer);
            if(glycosylation.glycosylationType !=null){
                consumer.accept(DefinitionalElement.of("protein.glycosylation.type", glycosylation.glycosylationType, 2));
            }
        }
        List<DisulfideLink> disulfideLinks = proteinSubstance.protein.getDisulfideLinks();
        if(disulfideLinks !=null){
            for(DisulfideLink disulfideLink : disulfideLinks){
                if(disulfideLink !=null) {
                    consumer.accept(DefinitionalElement.of("protein.disulfide", SiteContainer.generateShorthand(canonicalizeSites(disulfideLink.getSites(), canonicalOrder)), 2));
                }
            }
        }

        List<OtherLinks> otherLinks = proteinSubstance.protein.otherLinks;
        if(otherLinks !=null){
            for(OtherLinks otherLink : otherLinks){
                if(otherLink ==null){
                    continue;
                }
                List<Site> sites = otherLink.getSites();
                if(sites !=null) {
                    String shortHand = SiteContainer.generateShorthand(canonicalizeSites(sites, canonicalOrder));
                    consumer.accept(DefinitionalElement.of("protein."+shortHand, shortHand, 2));
                    String type = otherLink.linkageType;
                    if(type !=null){
                        consumer.accept(DefinitionalElement.of("protein."+shortHand +".linkageType", type, 2));
                    }
                }
            }
        }

        if (proteinSubstance.modifications != null) {
            definitionalElementFactory.addDefinitionalElementsFor(proteinSubstance.modifications, consumer);

        }
    }

    private void handleGlycosylationSites(List<Site> sites, String letter, Consumer<DefinitionalElement> consumer){
        if(sites ==null || sites.isEmpty()){
            return;
        }
        consumer.accept(DefinitionalElement.of("protein.glycosylation."+letter, SiteContainer.generateShorthand(sites), 2));

    }

    /**
     * order of the subunits from old index (1-based) to new index (1-based).
     * @param substance
     * @return
     */
    private Map<Integer, Integer> canonicalOrderOfSubunits(ProteinSubstance substance){
        List<Subunit> orderedSubunits = new ArrayList<>(substance.protein.subunits);//sort the subunits by canonical sort order
        //the sort order is based on sequence length
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
        return subunitIndexMap;
    }
}
