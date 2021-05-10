package gsrs.module.substance.services;

import gsrs.scheduledTasks.SchedulerPlugin;

import java.io.IOException;

public interface ReindexService {

    void execute(SchedulerPlugin.TaskListener l) throws IOException;
}
