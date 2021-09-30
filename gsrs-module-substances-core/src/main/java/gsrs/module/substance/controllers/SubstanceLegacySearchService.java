package gsrs.module.substance.controllers;

import gsrs.legacy.DefaultReindexService;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.legacy.LegacyGsrsSearchService;
import ix.ginas.models.v1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubstanceLegacySearchService extends LegacyGsrsSearchService<Substance> {
    @Autowired
    public SubstanceLegacySearchService(SubstanceRepository repository) {
        super(Substance.class, repository, new DefaultReindexService<>(repository,
                Substance.class,
                ChemicalSubstance.class, MixtureSubstance.class, NucleicAcidSubstance.class, ProteinSubstance.class,
                PolymerSubstance.class,SpecifiedSubstanceGroup1Substance.class, StructurallyDiverseSubstance.class));
    }


}
