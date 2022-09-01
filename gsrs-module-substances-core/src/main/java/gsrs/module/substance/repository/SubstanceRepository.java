package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.models.Keyword;
import ix.ginas.models.v1.*;
import ix.utils.UUIDUtil;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
@Transactional
public interface SubstanceRepository extends GsrsVersionedRepository<Substance, UUID> {

    default boolean exists(SubstanceReference substanceReference){
        if(substanceReference ==null){
            return false;
        }
        return (substanceReference.approvalID !=null && existsByApprovalID(substanceReference.approvalID))
                || ( substanceReference.refuuid !=null && UUIDUtil.isUUID(substanceReference.refuuid) && existsById(UUID.fromString(substanceReference.refuuid)));
    }
    default Substance findBySubstanceReference(SubstanceReference substanceReference){
        if(substanceReference ==null){
            return null;
        }
        if(substanceReference.approvalID !=null) {
            Substance s = findByApprovalID(substanceReference.approvalID);
            if (s != null) {
                return s;
            }
        }
        //Older Substance data did not have a refuuid as all references were based on approval id
        //so we need to check for null here
        if(substanceReference.refuuid !=null && UUIDUtil.isUUID(substanceReference.refuuid)) {
            return findById(UUID.fromString(substanceReference.refuuid)).orElse(null);
        }

        return null;
    }
    @Query("select s from Substance s where s.approvalID= ?1")
    Substance findByApprovalID(String approvalID);
    @Query("select s from Substance s where s.approvalID= ?1")
    SubstanceSummary findSummaryByApprovalID(String approvalID);

    Optional<SubstanceSummary> findSummaryByUuid(UUID uuid);

    List<SubstanceSummary> findByNames_NameIgnoreCase(String name);
    List<SubstanceSummary> findByCodes_CodeIgnoreCase(String code);
    List<SubstanceSummary> findByCodes_CodeAndCodes_CodeSystem(String code, String codeSystem);

    Substance findByModifications_Uuid(UUID uuid);
    

    @Query("select s from Substance s JOIN s.relationships r where r.uuid = ?1")
    Substance findByRelationships_Uuid(UUID uuid);



//    List<SubstanceSummary> findSubstanceSummaryByMoieties_Structure_Properties_Term(String term);

    default List<Substance> findSubstanceSummaryByMoieties_Structure_Properties_Term(String term){
        ChemicalSubstance example = new ChemicalSubstance();
        Moiety moiety = new Moiety();
        moiety.structure = new GinasChemicalStructure();
        moiety.structure.properties.add(new Keyword(null, term));
        example.moieties.add(moiety);

        return findAll(Example.of(example));
    }

    /**
     * Find all substances that start with the uuid prefix.  This
     * should not be used as it's a very inefficient search and is only included
     * in this repository to help with backwards compatibility in old GSRS systems
     * which often did lookups by only the first 8 characters of a uuid until
     * it became clear that there were too many collisions.
     *
     * @param partialUUID
     * @return
     */
    //hibernate query will not convert uuid into a string so we have to concatenate it with empty string for this to work.
    @Query("select s from Substance s where CONCAT(s.uuid, '') like ?1%")
    List<Substance> findByUuidStartingWith(String partialUUID);
    @Query("select case when count(s)> 0 then true else false end from Substance s where s.approvalID= ?1")
    boolean existsByApprovalID(String approvalID);

    @Transactional(readOnly=true)
    @Query("select s from Substance s JOIN s.relationships r where r.relatedSubstance.refuuid=?1 and r.type='"+ Substance.ALTERNATE_SUBSTANCE_REL +"'")
    List<Substance> findSubstancesWithAlternativeDefinition(String alternativeUuid);

    
    default List<Substance> findSubstancesWithAlternativeDefinition(Substance alternative){
        return findSubstancesWithAlternativeDefinition(alternative.getOrGenerateUUID().toString());
    }
    
    default Optional<SubstanceSummary> findSummaryBySubstanceReference(SubstanceReference substanceReference){
        if(substanceReference ==null){
            return Optional.empty();
        }
        if(substanceReference.approvalID !=null) {
            SubstanceSummary s = findSummaryByApprovalID(substanceReference.approvalID);
            if (s != null) {
                return Optional.of(s);
            }
        }
        return findSummaryByUuid(UUID.fromString(substanceReference.refuuid));
    }
    interface SubstanceSummary{
        UUID getUuid();
        Substance.SubstanceClass getSubstanceClass();
        Substance.SubstanceDefinitionType getDefinitionType();
        Substance.SubstanceDefinitionLevel getDefinitionLevel();
        String getStatus();
        String getApprovalID();

        default boolean isValidated(){
            //copied from Substance class
            return Substance.STATUS_APPROVED.equalsIgnoreCase(getStatus());
        }
        default SubstanceReference toSubstanceReference(){
            SubstanceReference ref = new SubstanceReference();
            ref.approvalID = getApprovalID();
            ref.refuuid = getUuid()==null?null: getUuid().toString();
            
            ref.substanceClass = Substance.SubstanceClass.reference.toString();
            
            //This is reasonable to have done, but it's not the way references work
            //now. The substance class is set to be a reference if it's a reference.
//            ref.substanceClass = getSubstanceClass().toString();

            //TODO: REALLY REALLY need refPname here
            //How best to do this?
            //For now use explicit query
            NameRepository nr= StaticContextAccessor.getBean(NameRepository.class);
            ref.refPname = nr.findDisplayNameByOwnerID(getUuid()).map(nn->nn.getName())
                             .orElse("NO NAME");
            
            //This is reasonable to have done, but it's not the way references work
            //now. The substance class is set to be a reference if it's a reference.
            //            ref.substanceClass = getSubstanceClass().toString();

            
            return ref;
        }
    }
    
    @Query("select s from Substance s")
    public Stream<Substance> streamAll();

    @Query("select s.id from Substance s")
    List<UUID> getAllIds();

    @Query("select s.id from Substance s where dtype='CHE'")
    List<UUID> getAllChemicalIds();
}
