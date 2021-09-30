package gsrs.module.substance.tasks;


import gsrs.module.substance.services.ReindexFromBackups;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.util.TaskListener;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.UUID;

public class ReindexTaskInitializer extends ScheduledTaskInitializer {

    @Autowired
    private ReindexFromBackups reindexService;

	@Override
	public void run(TaskListener l){
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
