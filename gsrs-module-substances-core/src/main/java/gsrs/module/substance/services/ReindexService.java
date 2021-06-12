package gsrs.module.substance.services;

import gsrs.scheduledTasks.SchedulerPlugin;

import java.io.IOException;

public interface ReindexService {

    void execute(Object id, SchedulerPlugin.TaskListener l) throws IOException;
}
