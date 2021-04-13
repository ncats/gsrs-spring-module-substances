package gsrs.module.substance.controllers;

import gsrs.controller.EtagLegacySearchEntityController;
import gsrs.controller.GsrsRestApiController;
import gsrs.controller.IdHelpers;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.module.substance.services.ReferenceEntityService;
import gsrs.repository.EditRepository;
import gsrs.service.GsrsEntityService;
import ix.ginas.models.v1.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.ExposesResourceFor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@ExposesResourceFor(Reference.class)
@GsrsRestApiController(context = "references",  idHelper = IdHelpers.UUID)
public class ReferenceController extends EtagLegacySearchEntityController<ReferenceController, Reference, UUID> {

    @Autowired
    private EditRepository editRepository;
    @Autowired
    private ReferenceLegacySearchService referenceLegacySearchService;
    @Autowired
    private ReferenceEntityService referenceEntityService;

    @Override
    protected Stream<Reference> filterStream(Stream<Reference> stream, boolean publicOnly, Map<String, String> parameters) {
        if(publicOnly){
            return stream.filter(r-> r.getAccess() ==null);
        }
        return stream;
    }

    @Override
    protected LegacyGsrsSearchService<Reference> getlegacyGsrsSearchService() {
        return referenceLegacySearchService;
    }

    @Override
    public GsrsEntityService<Reference, UUID> getEntityService() {
        return referenceEntityService;
    }

    @Override
    protected Optional<EditRepository> editRepository() {
        return Optional.of(editRepository);
    }
}
