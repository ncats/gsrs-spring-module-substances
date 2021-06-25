package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.ginas.models.v1.Mixture;
import ix.ginas.models.v1.MixtureSubstance;
import ix.ginas.models.v1.Modifications;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public interface MixtureSubstanceRepository extends GsrsVersionedRepository<MixtureSubstance, UUID> {


    List<MixtureSubstance> findByMixture_Components_Substance_Refuuid(String refuuid);
}
