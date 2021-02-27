package gsrs.module.substance.controllers;

import gsrs.indexer.EntitySearchService;
import ix.ginas.models.v1.ControlledVocabulary;
import ix.ginas.models.v1.Substance;
import org.springframework.stereotype.Service;

@Service
public class SubstanceSearchService extends EntitySearchService<Substance> {
    public SubstanceSearchService() {
//        super(ControlledVocabulary.class);
    }
}
