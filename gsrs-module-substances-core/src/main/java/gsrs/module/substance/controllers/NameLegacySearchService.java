package gsrs.module.substance.controllers;

import gsrs.legacy.DefaultReindexService;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.module.substance.repository.NameRepository;
import ix.ginas.models.v1.Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NameLegacySearchService extends LegacyGsrsSearchService<Name> {
        @Autowired
        public NameLegacySearchService(NameRepository repository) {
            super(Name.class, repository, new DefaultReindexService<>(repository, Name.class));
        }

}
