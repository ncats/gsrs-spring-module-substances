package gsrs.module.substance.controllers;

import gsrs.legacy.DefaultReindexService;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.module.substance.repository.ReferenceRepository;
import ix.ginas.models.v1.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReferenceLegacySearchService extends LegacyGsrsSearchService<Reference> {
        @Autowired
        public ReferenceLegacySearchService(ReferenceRepository repository) {
            super(Reference.class, repository,
                    new DefaultReindexService<>(repository, Reference.class));
        }

}
