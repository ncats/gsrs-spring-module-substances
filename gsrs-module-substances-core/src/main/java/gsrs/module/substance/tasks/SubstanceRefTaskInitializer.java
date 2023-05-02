package gsrs.module.substance.tasks;

import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
public class SubstanceRefTaskInitializer extends ScheduledTaskInitializer {

    private String reportFilePath;

    private String refUuidCodeSystem;

    private String refApprovalIdCodeSystem;

    @Autowired
    SubstanceRepository substanceRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private SubstanceLegacySearchService searchService;

    @Override
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l) {

    }

    @Override
    public String getDescription() {
        return "Fix references from one substance to another when possible";
    }


    /*
    Search order:
    1) UUID. When the UUID matches, the reference will already show up as existing so no need to do searches
    2) UUID as xcode
    3) Approval ID
    4) Approval ID as code
    5) name as primary name
     */
    public boolean fixSubstanceReferences(Substance startingSubstance, Consumer<String> missingMessages, Consumer<String> multipleMatchMessages) {
        AtomicBoolean substanceNeedsToSave = new AtomicBoolean(false);
        startingSubstance.relationships.forEach(r -> {
            if (!substanceRepository.exists(r.relatedSubstance)) {
                boolean currentReferenceResolved = false;
                List<Substance> uuidMatches = ValidationUtils.findSubstancesByCode(refUuidCodeSystem, r.relatedSubstance.refuuid,
                        transactionManager, searchService);

                if (uuidMatches.size() > 1) {
                    String message = String.format("More than one record found with UUID code %s and code system %s. Using first matching record", r.relatedSubstance.refuuid,
                            refUuidCodeSystem);
                    log.warn(message);
                    multipleMatchMessages.accept(message);
                    r.relatedSubstance.refuuid = uuidMatches.get(0).uuid.toString();
                    currentReferenceResolved = true;
                    substanceNeedsToSave.set(true);
                } else if (uuidMatches.size() == 1) {
                    r.relatedSubstance.refuuid = uuidMatches.get(0).uuid.toString();
                    currentReferenceResolved = true;
                    substanceNeedsToSave.set(true);
                }
                if (!currentReferenceResolved && r.relatedSubstance.approvalID != null && r.relatedSubstance.approvalID.length() > 0) {
                    Substance approvalIdMatch = substanceRepository.findByApprovalID(r.relatedSubstance.approvalID);
                    if (approvalIdMatch != null) {
                        r.relatedSubstance.refuuid = approvalIdMatch.uuid.toString();
                        currentReferenceResolved = true;
                        substanceNeedsToSave.set(true);
                    } else{
                        List<Substance> approvalIdMatches= ValidationUtils.findSubstancesByCode(this.refApprovalIdCodeSystem, r.relatedSubstance.approvalID,
                                transactionManager, searchService);
                        if(approvalIdMatches!=null && !approvalIdMatches.isEmpty()){
                           if(approvalIdMatches.size()>1){
                               String message = String.format("More than one record found with Approval ID code %s and code system %s. Using first matching record",
                                       r.relatedSubstance.refuuid, refApprovalIdCodeSystem);
                               log.warn(message);
                               multipleMatchMessages.accept(message);
                          }
                          r.relatedSubstance.refuuid = approvalIdMatches.get(0).uuid.toString();
                          currentReferenceResolved = true;
                          substanceNeedsToSave.set(true);
                        }
                    }
                }
            }
        });
        return substanceNeedsToSave.get();
    }
}
