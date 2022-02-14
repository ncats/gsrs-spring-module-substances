package gsrs.module.substance.controllers;

import gsrs.controller.GsrsControllerConfiguration;
import gsrs.module.substance.services.ProcessingJobEntityService;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.payload.PayloadController;
import gsrs.security.hasAdminRole;
import gsrs.service.PayloadService;
import ix.core.models.Payload;
import ix.core.models.ProcessingJob;
import ix.core.processing.GinasRecordProcessorPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RestController
public class SubstanceLegacyBulkLoadController {

    @Autowired
    private PayloadService payloadService;

    @Autowired
    private SubstanceBulkLoadService substanceBulkLoadService;

    @Autowired
    private GsrsControllerConfiguration controllerConfiguration;

    @Autowired
    private ProcessingJobEntityService processingJobService;



    @hasAdminRole
    @GetMapping("/api/v1/admin/{id}")
    public Object getLoadStatus(@PathVariable("id") String id, @RequestParam Map<String, String> queryParameters){
        Optional<ProcessingJob> jobs = processingJobService.flexLookup(id);
        if(!jobs.isPresent()){
            return controllerConfiguration.handleNotFound(queryParameters);
        }
        return jobs.get();
    }

    ///admin/load
    @Transactional
    @hasAdminRole
    @PostMapping("/api/v1/admin/load")
    public Object handleFileUpload(@RequestParam("file-name") MultipartFile file,
                                                   @RequestParam("file-type") String type,
                                                   @RequestParam Map<String, String> queryParameters) throws IOException {

        System.out.println("in handle file upload!!!");
/*
if (!GinasLoad.config.get().ALLOW_LOAD) {
			return badRequest("Invalid request!");
		}

		DynamicForm requestData = Form.form().bindFromRequest();
		String type = requestData.get("file-type");
		String preserveAudit = requestData.get("preserve-audit");
		Logger.info("type =" + type);
		try {
		//...
		Payload payload = payloadPlugin.get().parseMultiPart("file-name",
					request(), PayloadPersistType.TEMP);
			switch (type) {
				case "JSON":
					Logger.info("JOS =" + type);
 */

        //legacy GSRS 2.x only supported JSON we turned of sd support in this method at some point
        //between 2.0 and 2.7 instead waiting for the new importer in 3.x to be written in a more robust way.
        if(!"JSON".equals(type)){
            return controllerConfiguration.handleBadRequest("invalid file type:" + type, queryParameters);
        }
        Payload payload = payloadService.createPayload(file.getOriginalFilename(), PayloadController.predictMimeTypeFromFile(file),
                file.getBytes(), PayloadService.PayloadPersistType.TEMP);

        //GSRS 2 preserve audit if the parameter is present, don't care what it's set to!!
         GinasRecordProcessorPlugin.PayloadProcessor processor=  substanceBulkLoadService.submit(
                SubstanceBulkLoadService.SubstanceBulkLoadParameters.builder()
                        .payload(payload)
                        .preserveOldEditInfo(queryParameters.containsKey("preserve-audit"))

                .build());

         return processingJobService.get(processor.jobId).get();

    }
}
