package gsrs.module.substance.controllers;

import gsrs.controller.EtagLegacySearchEntityController;
import gsrs.controller.GsrsRestApiController;
import gsrs.controller.IdHelpers;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.module.substance.services.CodeEntityService;
import gsrs.repository.EditRepository;
import gsrs.service.GsrsEntityService;
import ix.core.search.bulk.ResultListRecordGenerator;
import ix.ginas.models.v1.Code;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.ExposesResourceFor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@ExposesResourceFor(Code.class)
@GsrsRestApiController(context = "codes",  idHelper = IdHelpers.UUID)
public class CodeController extends EtagLegacySearchEntityController<CodeController, Code, UUID> {

    @Autowired
    private EditRepository editRepository;
    @Autowired
    private CodeLegacySearchService codeLegacySearchService;

    @Autowired
    private CodeEntityService codeEntityService;

    @Override
    protected Stream<Code> filterStream(Stream<Code> stream, boolean publicOnly, Map<String, String> parameters) {
        if(publicOnly){
            return stream.filter(r-> r.getAccess() ==null);
        }
        return stream;
    }

    @Override
    protected LegacyGsrsSearchService<Code> getlegacyGsrsSearchService() {
        return codeLegacySearchService;
    }

    @Override
    public GsrsEntityService<Code, UUID> getEntityService() {
        return codeEntityService;
    }

    @Override
    protected Optional<EditRepository> editRepository() {
        return Optional.of(editRepository);
    }	
}
