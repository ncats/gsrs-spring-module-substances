package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.ginas.models.v1.NucleicAcidSubstance;
import ix.ginas.models.v1.Substance;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface NucleicAcidSubstanceRepository extends GsrsVersionedRepository<NucleicAcidSubstance, UUID> {


    /*
    SubstanceFactory.nucfinder.get()
				.where()
				.eq("nucleicAcid.subunits.uuid", suid)
				.findList()
				.stream()
				.findFirst();
     */

    List<NucleicAcidSubstance> findNucleicAcidSubstanceByNucleicAcid_Subunits_Uuid(UUID uuid);
}
