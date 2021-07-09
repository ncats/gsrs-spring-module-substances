package ix.ginas.utils.validation;

import gsrs.module.substance.repository.ChemicalSubstanceRepository;
import gsrs.module.substance.repository.KeywordRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.repository.ValueRepository;
import ix.core.models.Keyword;
import ix.core.models.Value;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
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
    private KeywordRepository keywordRepository;

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
            List<Keyword> keywords = keywordRepository.findByTerm(hash);
            if(!keywords.isEmpty()) {
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<ChemicalSubstance> query = cb.createQuery(ChemicalSubstance.class);
                Root<ChemicalSubstance> root = query.from(ChemicalSubstance.class);


                CriteriaBuilder.In<Value> in = cb.in(root.join("structure").get("properties"));
                keywords.forEach(in::value);

//            predicates[1] = cb.equal(root.join("structure").get("properties").get("term"), hash);

//            List<Keyword> keywords = entityManager.createQuery(query).getResultList();
//            System.out.println("keywords = " + keywords);
//            dupMap = entityManager.createQuery(query).getResultStream()
//                                            .limit(max)
//                    .collect(Collectors.toMap(Substance::getUuid, Substance::asSubstanceReference, (x, y) -> y, LinkedHashMap::new));
//            List<Keyword> byTerm = keywordRepository.findByTerm(hash);
//            for(Keyword k: byTerm) {
//                List<ChemicalSubstance> chemicalSubstance = chemicalSubstanceRepository.findByStructure_Properties_Id(k.id);
//                System.out.println(chemicalSubstance);
//            }
                dupMap = entityManager.createQuery(query.select(root).where(in)).getResultList()
//                    chemicalSubstanceRepository.findByStructure_Properties_Term(hash)
//                    substanceRepository.findSubstanceSummaryByStructure_Properties_Term(hash)
                        .stream()
                        .limit(max)

                        .collect(Collectors.toMap(Substance::getUuid, Substance::asSubstanceReference, (x, y) -> y, LinkedHashMap::new));
//            dupeList = new LinkedHashSet<>(SubstanceFactory.finder.get()
//                                              .where()
//                                              .eq("structure.properties.term", hash)
//                                              .setMaxRows(max)
//                                              .findList());

                //
                if (dupMap.size() < max) {
                    CriteriaBuilder cb2 = entityManager.getCriteriaBuilder();
                    CriteriaQuery<ChemicalSubstance> query2 = cb2.createQuery(ChemicalSubstance.class);
                    Root<ChemicalSubstance> root2 = query2.from(ChemicalSubstance.class);


                    CriteriaBuilder.In<Value> in2 = cb2.in(root2.join("moieties").get("structure").get("properties"));
                    keywords.forEach(in2::value);
                    dupMap.putAll(entityManager.createQuery(query2.select(root2).where(in2)).getResultList()
                            .stream()

                            .limit(max - dupMap.size())
                            .collect(Collectors.toMap(Substance::getUuid, Substance::asSubstanceReference, (x, y) -> y, LinkedHashMap::new)));
//
//                dupMap.addAll(SubstanceFactory.finder.get()
//                                            .where()
//                                            .eq("moieties.structure.properties.term", hash2)
//                                            .setMaxRows(max-dupeList.size())
//                                            .findList());

                }
            }
        }
        
        return dupMap.values().stream()

                                .collect(Collectors.toList());
    }
    
    public static ChemicalDuplicateFinder instance(){
        return new ChemicalDuplicateFinder();
    }

}
