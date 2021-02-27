package ix.ginas.utils.validation;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ChemicalDuplicateFinder implements DuplicateFinder<SubstanceReference> {

    @Autowired
    private SubstanceRepository substanceRepository;
    /**
     * Currently uses the structure.properties.term keys for the duplicate matching
     */
    @Override
    public List<SubstanceReference> findPossibleDuplicatesFor(SubstanceReference subRef) {
        int max=10;

        Map<UUID, SubstanceRepository.SubstanceSummary> dupMap = new LinkedHashMap<>();
        Substance sub = subRef.wrappedSubstance;
        if(sub ==null){
            sub = substanceRepository.findBySubstanceReference(subRef);
        }
        if(sub instanceof ChemicalSubstance){
            ChemicalSubstance cs = (ChemicalSubstance)sub;
         // System.out.println("Dupe chack");
            String hash = cs.structure.getStereoInsensitiveHash();
            dupMap = substanceRepository.findSubstanceSummaryByStructure_Properties_Term(hash)
                        .stream()
                        .limit(max)
                        .collect(Collectors.toMap(SubstanceRepository.SubstanceSummary::getUUID, Function.identity(), (x, y) -> y, LinkedHashMap::new));
//            dupeList = new LinkedHashSet<>(SubstanceFactory.finder.get()
//                                              .where()
//                                              .eq("structure.properties.term", hash)
//                                              .setMaxRows(max)
//                                              .findList());
            
            //
            if(dupMap.size()<max){
                String hash2 = cs.structure.getStereoInsensitiveHash();
                dupMap.putAll(substanceRepository.findSubstanceSummaryByMoieties_Structure_Properties_Term(hash2)
                        .stream()
                        .limit(max- dupMap.size())
                        .collect(Collectors.toMap(SubstanceRepository.SubstanceSummary::getUUID, Function.identity(), (x, y) -> y, LinkedHashMap::new)));
//
//                dupMap.addAll(SubstanceFactory.finder.get()
//                                            .where()
//                                            .eq("moieties.structure.properties.term", hash2)
//                                            .setMaxRows(max-dupeList.size())
//                                            .findList());
                
            }
        }
        
        return dupMap.values().stream()
                                .map(SubstanceRepository.SubstanceSummary::toSubstanceReference)
                                .collect(Collectors.toList());
    }
    
    public static ChemicalDuplicateFinder instance(){
        return new ChemicalDuplicateFinder();
    }

}
