package gsrs.module.substance.tasks;

import gsrs.events.MaintenanceModeEvent;
import gsrs.indexer.IndexCreateEntityEvent;
import gsrs.module.substance.services.ReindexService;
import gsrs.repository.BackupRepository;
import gsrs.scheduledTasks.ScheduledTaskInitializer;

import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.GsrsSecurityUtils;
import gsrs.springUtils.GsrsSpringUtils;
import ix.core.models.BackupEntity;
import ix.core.models.Role;
import ix.core.models.UserProfile;

import ix.core.util.EntityUtils;
import ix.core.utils.executor.ProcessListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReindexTaskInitializer extends ScheduledTaskInitializer {

    @Autowired
    private ReindexService reindexService;

	@Override
	public void run(SchedulerPlugin.TaskListener l){
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
