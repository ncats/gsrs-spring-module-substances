package gsrs.dataexchange.processingactions;

import gsrs.dataexchange.model.ProcessingAction;
import gsrs.module.substance.scrubbers.basic.BasicSubstanceScrubber;
import gsrs.module.substance.scrubbers.basic.BasicSubstanceScrubberParameters;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class ScrubProcessingAction implements ProcessingAction<Substance> {
    @Override
    public Substance process(Substance stagingAreaRecord, Substance additionalRecord, Map<String, Object> parameters, Consumer<String> logger) throws Exception {
        log.trace("Starting in process");
        BasicSubstanceScrubberParameters scrubberParameters = new BasicSubstanceScrubberParameters();
        if(hasTrueValue(parameters, "RegenerateUUIDs")) {
            scrubberParameters.setRegenerateUUIDs(true);
            logger.accept("going to regerate UUIDs");
        }
        if(hasTrueValue(parameters, "RemoveApprovalId")){
            scrubberParameters.setApprovalIdCleanup(true);
            scrubberParameters.setApprovalIdCleanupRemoveApprovalId(true);
            if(hasStringValue(parameters, "ApprovalIdCodeSystem")) {
                scrubberParameters.setApprovalIdCleanupCopyApprovalIdToCode(true);;
                scrubberParameters.setApprovalIdCleanupApprovalIdCodeSystem((String)parameters.get("ApprovalIdCodeSystem"));
                logger.accept("going to create code for approval id");
            }
        }
        if( hasStringValue(parameters, "ReplacementAuditUser")) {
            scrubberParameters.setAuditInformationCleanup(true);
            scrubberParameters.setAuditInformationCleanupNewAuditorValue((String)parameters.get("ReplacementAuditUser"));
            logger.accept("replacing audit user");
        }
        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberParameters);
        Optional<Substance> scrubbed =scrubber.scrub(stagingAreaRecord);
        return scrubbed.orElse(null);
    }

    @Override
    public String getActionName() {
        return "Scrub";
    }

    @Override
    public List<String> getOptions() {
        return Arrays.asList("RegenerateUUIDs", "RemoveApprovalId", "CreateCodeForApprovalId", "ApprovalIdCodeSystem", "ReplacementAuditUser");
    }
}
