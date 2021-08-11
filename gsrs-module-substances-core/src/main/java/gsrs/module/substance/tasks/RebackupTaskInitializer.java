package gsrs.module.substance.tasks;

import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.services.BackupService;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;

public class RebackupTaskInitializer extends ScheduledTaskInitializer {
    @Autowired
    private BackupService backupService;

    @Autowired
    private SubstanceRepository substanceRepository;

    @Override
    public String getDescription() {
        return "Re-backup all Substance entities";
    }


    @Override
    public void run(SchedulerPlugin.TaskListener l){
            backupService.reBackupAllEntitiesOfType(substanceRepository, taskProgress -> {
                l.message(taskProgress.getCurrentCount() + "  of " + taskProgress.getTotalCount());
            });
    }
}
