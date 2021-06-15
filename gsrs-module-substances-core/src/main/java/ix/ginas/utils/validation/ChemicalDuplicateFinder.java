package ix.ginas.utils.validation;

import gsrs.module.substance.repository.ChemicalSubstanceRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.models.Keyword;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ChemicalDuplicateFinder implements DuplicateFinder<SubstanceReference> {

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    private ChemicalSubstanceRepository chemicalSubstanceRepository;

    @Autowired
    private EntityManager entityManager;
    /**
     * Currently uses the structure.properties.term keys for the duplicate matching
     */
    @Override
    public List<SubstanceReference> findPossibleDuplicatesFor(SubstanceReference subRef) {
        int max=10;

        Map<UUID, SubstanceReference> dupMap = new LinkedHashMap<>();
        Substance sub = subRef.wrappedSubstance;
        if(sub ==null){
            sub = substanceRepository.findBySubstanceReference(subRef);
        }
        if(sub instanceof ChemicalSubstance){
            ChemicalSubstance cs = (ChemicalSubstance)sub;
         // System.out.println("Dupe chack");
            GinasChemicalStructure structure = cs.getStructure();

            String hash = structure.getStereoInsensitiveHash();
//            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
//            CriteriaQuery<Keyword> query = cb.createQuery(Keyword.class);
//            Root<Keyword> root = query.from(Keyword.class);
//
//            query.select(root).where(cb.equal(root.get("term"), hash));
//            List<Keyword> keywords = entityManager.createQuery(query).getResultList();
//            System.out.println("keywords = " + keywords);
//            dupMap = entityManager.createQuery(query).getResultStream()
//                                            .limit(max)
//                    .collect(Collectors.toMap(Substance::getUuid, Substance::asSubstanceReference, (x, y) -> y, LinkedHashMap::new));
            dupMap = substanceRepository.findSubstanceSummaryByStructure_Properties_Term(hash)
                        .stream()
                        .limit(max)

                        .collect(Collectors.toMap(Substance::getUuid, Substance::asSubstanceReference, (x, y) -> y, LinkedHashMap::new));
//            dupeList = new LinkedHashSet<>(SubstanceFactory.finder.get()
//                                              .where()
//                                              .eq("structure.properties.term", hash)
//                                              .setMaxRows(max)
//                                              .findList());
            
            //
            if(dupMap.size()<max){
                String hash2 = cs.getStructure().getStereoInsensitiveHash();
                dupMap.putAll(substanceRepository.findSubstanceSummaryByMoieties_Structure_Properties_Term(hash2)
                        .stream()

                        .limit(max- dupMap.size())
                        .collect(Collectors.toMap(Substance::getUuid, Substance::asSubstanceReference, (x, y) -> y, LinkedHashMap::new)));
//
//                dupMap.addAll(SubstanceFactory.finder.get()
//                                            .where()
//                                            .eq("moieties.structure.properties.term", hash2)
//                                            .setMaxRows(max-dupeList.size())
//                                            .findList());
                
            }
        }
        
        return dupMap.values().stream()

                                .collect(Collectors.toList());
    }
    
    public static ChemicalDuplicateFinder instance(){
        return new ChemicalDuplicateFinder();
    }

}
