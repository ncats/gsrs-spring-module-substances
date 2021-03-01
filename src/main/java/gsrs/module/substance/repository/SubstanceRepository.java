package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubstanceRepository extends GsrsVersionedRepository<Substance, UUID> {

    default boolean exists(SubstanceReference substanceReference){
        if(substanceReference ==null){
            return false;
        }
        return existsByApprovalIDIgnoreCase(substanceReference.approvalID) || existsById(UUID.fromString(substanceReference.refuuid));
    }
    default Substance findBySubstanceReference(SubstanceReference substanceReference){
        if(substanceReference ==null){
            return null;
        }
        Substance s = findByApprovalIDIgnoreCase(substanceReference.approvalID);
        if(s !=null){
            return s;
        }
        return findById(UUID.fromString(substanceReference.refuuid)).orElse(null);
    }
    Substance findByApprovalIDIgnoreCase(String approvalID);
    SubstanceSummary findSummaryByApprovalIDIgnoreCase(String approvalID);

    Optional<SubstanceSummary> findSummaryById(UUID uuid);

    List<SubstanceSummary> findByNames_NameIgnoreCase(String name);
    List<SubstanceSummary> findByCodes_CodeIgnoreCase(String code);
    List<SubstanceSummary> findByCodes_CodeAndCodes_CodeSystem(String code, String codeSystem);
    List<SubstanceSummary> findSubstanceSummaryByStructure_Properties_Term(String term);
    List<SubstanceSummary> findSubstanceSummaryByMoieties_Structure_Properties_Term(String term);
//moieties.structure.properties.term
    List<Substance> findByUuidStartingWith(String partialUUID);
    boolean existsByApprovalIDIgnoreCase(String approvalID);

    default Optional<SubstanceSummary> findSummaryBySubstanceReference(SubstanceReference substanceReference){
        if(substanceReference ==null){
            return Optional.empty();
        }
        SubstanceSummary s = findSummaryByApprovalIDIgnoreCase(substanceReference.approvalID);
        if(s !=null){
            return Optional.of(s);
        }
        return findSummaryById(UUID.fromString(substanceReference.refuuid));
    }
    interface SubstanceSummary{
        UUID getUUID();
        Substance.SubstanceClass getSubstanceClass();
        Substance.SubstanceDefinitionType getSubstanceDefinitionType();
        Substance.SubstanceDefinitionLevel getSubstanceDefinitionLevel();
        String getStatus();
        String getApprovalId();

        default SubstanceReference toSubstanceReference(){
            SubstanceReference ref = new SubstanceReference();
            ref.approvalID = getApprovalId();
            ref.refuuid = getUUID()==null?null: getUUID().toString();
            ref.substanceClass = getSubstanceClass().toString();

            return ref;
        }
    }
}
