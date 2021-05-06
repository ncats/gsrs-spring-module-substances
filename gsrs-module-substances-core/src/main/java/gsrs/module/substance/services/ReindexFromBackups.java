package gsrs.module.substance.services;

import gsrs.events.MaintenanceModeEvent;
import gsrs.indexer.IndexCreateEntityEvent;
import gsrs.module.substance.tasks.ProcessExecutionService;
import gsrs.repository.BackupRepository;
import gsrs.scheduledTasks.SchedulerPlugin;
import ix.core.models.BackupEntity;
import ix.core.util.EntityUtils;
import ix.core.utils.executor.ProcessListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ReindexFromBackups implements ReindexService{

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private BackupRepository backupRepository;


    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void execute(SchedulerPlugin.TaskListener l) throws IOException {
        l.message("Initializing reindexing");
        ProcessListener listen = ProcessListener.onCountChange((sofar, total)->{
            if(total!=null){
                l.message("Indexed:" + sofar + " of " + total);
            }else{
                l.message("Indexed:" + sofar);
            }
        });
//this is all handled now by Spring events
        int count = (int) backupRepository.count();

        Set<String> seen = Collections.newSetFromMap(new ConcurrentHashMap<>(count));
        new ProcessExecutionService(1, 10).buildProcess(BackupEntity.class)
                .before(()->{
                    eventPublisher.publishEvent(new MaintenanceModeEvent(MaintenanceModeEvent.Mode.BEGIN));
                })
                .after(()->{
                    eventPublisher.publishEvent(new MaintenanceModeEvent(MaintenanceModeEvent.Mode.END));
                })
                .listener(listen)
                .streamSupplier(ProcessExecutionService.EntityStreamSupplier.of(()->backupRepository.findAll().stream()))
                .consumer( be->{
                    try {
                        EntityUtils.EntityWrapper wrapper = EntityUtils.EntityWrapper.of(be.getInstantiated());
                        wrapper.traverse().execute((p, child)->{
                            EntityUtils.EntityWrapper<EntityUtils.EntityWrapper> wrapped = EntityUtils.EntityWrapper.of(child);
                            if(wrapped.isEntity()) {
                                try {
                                    String key = wrapped.getKey().toString();

                                    //TODO add only index if it has a controller
                                    if (seen.add(key)) {
                                        //is this a good idea ?
                                        eventPublisher.publishEvent(new IndexCreateEntityEvent(wrapped));
                                    }
                                }catch(Throwable t){
                                    System.err.println("error handling "+wrapped);
                                    t.printStackTrace();
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .build()
                .execute();
    }
}
