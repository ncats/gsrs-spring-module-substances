package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.core.models.Keyword;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.MixtureSubstance;
import ix.ginas.models.v1.Substance;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public interface ChemicalSubstanceRepository extends GsrsVersionedRepository<ChemicalSubstance, UUID> {

    ChemicalSubstance findByStructure_Id(UUID structureId);
    List<ChemicalSubstance> findByStructure_PropertiesIn(List<Keyword> keywords);
    List<ChemicalSubstance> findByMoieties_Structure_PropertiesIn(List<Keyword> keywords);

    @Query("select s.uuid from Substance s")
    List<UUID> getAllIds();
}
