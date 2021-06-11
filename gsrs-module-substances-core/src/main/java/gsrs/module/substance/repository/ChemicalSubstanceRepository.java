package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.MixtureSubstance;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public interface ChemicalSubstanceRepository extends GsrsVersionedRepository<ChemicalSubstance, UUID> {

    ChemicalSubstance findByStructure_Id(UUID structureId);
}
