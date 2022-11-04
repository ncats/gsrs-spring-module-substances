package gsrs.module.substance.controllers;

import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.utils.SubstanceMatchViewGenerator;
import gsrs.legacy.LegacyGsrsSearchService;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubstanceLegacySearchService extends LegacyGsrsSearchService<Substance> {
    @Autowired
    public SubstanceLegacySearchService(SubstanceRepository repository) {
    	//todo: make the "UNII" can be read from config file or other settings
        super(Substance.class, repository, new SubstanceMatchViewGenerator());
    }


}
