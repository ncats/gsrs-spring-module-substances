package gsrs.module.substance.tasks;

import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.services.BackupService;
import gsrs.springUtils.StaticContextAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public class RebackupTaskInitializer extends ScheduledTaskInitializer {
    @Autowired
    private BackupService backupService;

    private String description;

    private Class<? extends JpaRepository> repositoryClass;

    public void setDescription(String description) {
        this.description = description;
    }

    public Class<? extends JpaRepository> getRepositoryClass() {
        return repositoryClass;
    }

    public void setRepositoryClass(Class<? extends JpaRepository> repositoryClass) {
        this.repositoryClass = repositoryClass;
    }

    @Override
    public String getDescription() {
        return description;
    }


    @Override
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l){
            backupService.reBackupAllEntitiesOfType(StaticContextAccessor.getBean(repositoryClass),
                    PageRequest.of(0, 200), taskProgress -> {
                l.message(taskProgress.getCurrentCount() + "  of " + taskProgress.getTotalCount());
            });
    }
}
