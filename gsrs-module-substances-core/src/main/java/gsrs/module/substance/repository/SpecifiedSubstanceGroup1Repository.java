package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.ginas.models.v1.SpecifiedSubstanceGroup1Substance;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public interface SpecifiedSubstanceGroup1Repository extends GsrsVersionedRepository<SpecifiedSubstanceGroup1Substance, UUID> {
    /*
    extends GsrsVersionedRepository<MixtureSubstance, UUID> {

    List<MixtureSubstance> findByMixture_Components_Substance_Refuuid(String refuuid);
     */
    //structurallyDiverse.parentSubstance.refuuid

    List<SpecifiedSubstanceGroup1Substance> findBySpecifiedSubstance_Constituents_Substance_Refuuid(String refuuid);
}
