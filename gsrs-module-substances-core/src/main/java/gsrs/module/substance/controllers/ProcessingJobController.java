package gsrs.module.substance.controllers;

import gsrs.controller.AbstractGsrsEntityController;
import gsrs.controller.GsrsRestApiController;
import gsrs.module.substance.services.ProcessingJobEntityService;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.service.GsrsEntityService;
import ix.core.models.ProcessingJob;
import ix.core.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.ExposesResourceFor;

import java.util.Optional;

@GsrsRestApiController(context= ProcessingJobEntityService.CONTEXT)
@ExposesResourceFor(ProcessingJob.class)
public class ProcessingJobController extends AbstractGsrsEntityController<ProcessingJobController, ProcessingJob, Long> {

    @Autowired
    private ProcessingJobEntityService service;

    @Autowired
    private SubstanceBulkLoadService bulkLoadService;

    @Override
    protected Optional<Object> handleSpecialFields(EntityUtils.EntityWrapper<ProcessingJob> entity, String field) {
        if("@cancel".equals(field)){
            String jobKey = entity.getValue().loaderLabel();
            //the bulk load service#cancel should handle the admin prev check so this should be safe...
            boolean cancelled = bulkLoadService.cancel(jobKey);
            if(!cancelled){
                return Optional.empty();
            }
            return Optional.of(bulkLoadService.getStatisticsForJob(jobKey));

        }
        return null;
    }

    @Override
    protected GsrsEntityService<ProcessingJob, Long> getEntityService() {
        return service;
    }
}
