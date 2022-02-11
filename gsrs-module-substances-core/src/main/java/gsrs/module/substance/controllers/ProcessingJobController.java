package gsrs.module.substance.controllers;

import gsrs.controller.AbstractGsrsEntityController;
import gsrs.controller.GsrsRestApiController;
import gsrs.module.substance.services.ProcessingJobEntityService;
import gsrs.service.GsrsEntityService;
import ix.core.models.ProcessingJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.ExposesResourceFor;

@GsrsRestApiController(context= ProcessingJobEntityService.CONTEXT)
@ExposesResourceFor(ProcessingJob.class)
public class ProcessingJobController extends AbstractGsrsEntityController<ProcessingJobController, ProcessingJob, Long> {

    @Autowired
    private ProcessingJobEntityService service;

    @Override
    protected GsrsEntityService<ProcessingJob, Long> getEntityService() {
        return service;
    }
}
