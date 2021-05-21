package gsrs.module.substance.tasks;

import gsrs.events.MaintenanceModeEvent;
import gsrs.indexer.IndexCreateEntityEvent;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//           ReindexTaskInitializer
public class ReindexTaskInitializer extends ScheduledTaskInitializer {

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private BackupRepository backupRepository;

	@Override
	public void run(SchedulerPlugin.TaskListener l){
		
            try {
                l.message("Initializing reindexing");
                ProcessListener listen = ProcessListener.onCountChange((sofar, total)->{
                    if(total!=null){
                        l.message("Indexed:" + sofar + " of " + total);
                    }else{
                        l.message("Indexed:" + sofar);
                    }
                });


//                .and(GinasApp.getReindexListener())
//                .and(Play.application().plugin(TextIndexerPlugin.class).getIndexer())
//                .and(EntityPersistAdapter.getInstance().getProcessListener());
                
//                new ProcessExecutionService(5, 10).buildProcess(Object.class)
//                        .consumer(CommonConsumers.REINDEX_FAST)
//                        .streamSupplier(CommonStreamSuppliers.allBackups())
//                        .before(ProcessExecutionService::nukeEverything)
//                        .listener(listen)
//                        .build()
//                        .execute();

                //this is all handled now by Spring events
                Set<String> seen = Collections.newSetFromMap(new ConcurrentHashMap<>(100_000));
                new ProcessExecutionService(5, 10).buildProcess(BackupEntity.class)
                        .before(()->{
                            eventPublisher.publishEvent(new MaintenanceModeEvent(MaintenanceModeEvent.Mode.BEGIN));
                        })
                        .after(()->{
                            eventPublisher.publishEvent(new MaintenanceModeEvent(MaintenanceModeEvent.Mode.END));
                        })
                        .listener(listen)
                        .streamSupplier(ProcessExecutionService.EntityStreamSupplier.of(backupRepository::streamAll))
                        .consumer( be->{
                            try {
                                EntityUtils.EntityWrapper wrapper = EntityUtils.EntityWrapper.of(be.getInstantiated());
                                wrapper.traverse().execute((p, child)->{
                                    EntityUtils.EntityWrapper<EntityUtils.EntityWrapper> wrapped = EntityUtils.EntityWrapper.of(child);
                                    String key = wrapped.getKey().toString();
                                    //TODO add only index if it has a controller
                                    if(!seen.add(key)){
                                        //is this a good idea ?
                                        eventPublisher.publishEvent(new IndexCreateEntityEvent(wrapped));
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        })
                        .build()
                        .execute();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
	}

	@Override
	public String getDescription() {		
		return "Reindex all core entities from backup tables";
	}


}
