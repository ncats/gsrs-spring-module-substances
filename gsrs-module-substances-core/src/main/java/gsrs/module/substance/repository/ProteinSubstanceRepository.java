package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.ginas.models.v1.NucleicAcidSubstance;
import ix.ginas.models.v1.ProteinSubstance;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
@Transactional
@Repository
public interface ProteinSubstanceRepository extends GsrsVersionedRepository<ProteinSubstance, UUID> {

    List<ProteinSubstance> findProteinSubstanceByProtein_Subunits_Uuid(UUID uuid);
}
