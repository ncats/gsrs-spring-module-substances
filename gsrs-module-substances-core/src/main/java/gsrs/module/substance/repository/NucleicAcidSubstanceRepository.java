package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.ginas.models.v1.NucleicAcidSubstance;
import ix.ginas.models.v1.Substance;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
@Repository
@Transactional
public interface NucleicAcidSubstanceRepository extends GsrsVersionedRepository<NucleicAcidSubstance, UUID> {

    List<NucleicAcidSubstance> findNucleicAcidSubstanceByNucleicAcid_Subunits_Uuid(UUID uuid);
}
