package gsrs.module.substance.controllers;

import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.module.substance.repository.CodeRepository;
import gsrs.module.substance.repository.ReferenceRepository;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CodeLegacySearchService extends LegacyGsrsSearchService<Code> {
        @Autowired
        public CodeLegacySearchService(CodeRepository repository) {
            super(Code.class, repository);
        }

}
