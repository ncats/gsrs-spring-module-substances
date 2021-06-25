package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.ginas.models.v1.SpecifiedSubstanceGroup1Substance;
import ix.ginas.models.v1.StructurallyDiverseSubstance;
import ix.ginas.models.v1.Substance;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public interface StructurallyDiverseRepository extends GsrsVersionedRepository<StructurallyDiverseSubstance, UUID> {
    /*
    extends GsrsVersionedRepository<MixtureSubstance, UUID> {

    List<MixtureSubstance> findByMixture_Components_Substance_Refuuid(String refuuid);
     */
    //specifiedSubstance.constituents.substance.refuuid
    List<Substance> findByStructurallyDiverse_ParentSubstance_Refuuid(String refuuid);
}
