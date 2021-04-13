package gsrs.module.substance.controllers;

import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.legacy.LegacyGsrsSearchService;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubstanceLegacySearchService extends LegacyGsrsSearchService<Substance> {
    @Autowired
    public SubstanceLegacySearchService(SubstanceRepository repository) {
        super(Substance.class, repository);
    }


}
