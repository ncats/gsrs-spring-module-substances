package gsrs.module.substance.tasks;

import gsrs.module.substance.services.ReindexService;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.UUID;

public class ReindexTaskInitializer extends ScheduledTaskInitializer {

    @Autowired
    private ReindexService reindexService;

    @Override
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l){
        UUID uuid = UUID.randomUUID();
        try {
            reindexService.execute(uuid, l);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String getDescription() {
        return "Reindex all core entities from backup tables";
    }


}