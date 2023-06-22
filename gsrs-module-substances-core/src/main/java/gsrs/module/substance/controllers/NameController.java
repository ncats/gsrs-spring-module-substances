package gsrs.module.substance.controllers;

import gsrs.controller.EtagLegacySearchEntityController;
import gsrs.controller.GsrsRestApiController;
import gsrs.controller.IdHelpers;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.module.substance.services.NameEntityService;
import gsrs.repository.EditRepository;
import gsrs.service.GsrsEntityService;
import ix.core.search.bulk.ResultListRecordGenerator;
import ix.ginas.models.v1.Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.ExposesResourceFor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@ExposesResourceFor(Name.class)
@GsrsRestApiController(context = "names",  idHelper = IdHelpers.UUID)
public class NameController extends EtagLegacySearchEntityController<NameController, Name, UUID> {

    @Autowired
    private EditRepository editRepository;
    @Autowired
    private NameLegacySearchService nameLegacySearchService;

    @Autowired
    private NameEntityService nameEntityService;

    @Override
    protected Stream<Name> filterStream(Stream<Name> stream, boolean publicOnly, Map<String, String> parameters) {
        if(publicOnly){
            return stream.filter(r-> r.getAccess() ==null);
        }
        return stream;
    }

    @Override
    protected LegacyGsrsSearchService<Name> getlegacyGsrsSearchService() {
        return nameLegacySearchService;
    }

    @Override
    public GsrsEntityService<Name, UUID> getEntityService() {
        return nameEntityService;
    }

    @Override
    protected Optional<EditRepository> editRepository() {
        return Optional.of(editRepository);
    }

}
