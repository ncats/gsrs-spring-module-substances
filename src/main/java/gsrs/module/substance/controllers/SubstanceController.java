package gsrs.module.substance.controllers;


import gsrs.controller.EtagLegacySearchEntityController;
import gsrs.controller.GsrsRestApiController;
import gsrs.controller.IdHelpers;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.repository.EditRepository;
import ix.ginas.exporters.DefaultParameters;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.ExposesResourceFor;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * GSRS Rest API controller for the {@link Substance} entity.
 */
@ExposesResourceFor(Substance.class)
@GsrsRestApiController(context = SubstanceEntityService.CONTEXT,  idHelper = IdHelpers.UUID)
public class SubstanceController extends EtagLegacySearchEntityController<SubstanceController, Substance, Long> {


    @Autowired
    private EditRepository editRepository;
    @Autowired
    private SubstanceLegacySearchService legacySearchService;

    @Override
    protected LegacyGsrsSearchService<Substance> getlegacyGsrsSearchService() {
        return legacySearchService;
    }

    @Override
    protected Optional<EditRepository> editRepository() {
        return Optional.of(editRepository);
    }

    @Override
    protected Stream<Substance> filterStream(Stream<Substance> stream,boolean publicOnly, Map<String, String> parameters) {
        if(publicOnly){
            return stream.filter(s-> s.getAccess().isEmpty());
        }
        return stream;
    }
}
