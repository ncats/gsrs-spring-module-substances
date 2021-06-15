package gsrs.module.substance.services;

import gsrs.events.*;
import gsrs.indexer.IndexCreateEntityEvent;
import gsrs.module.substance.tasks.ProcessExecutionService;
import gsrs.repository.BackupRepository;
import gsrs.scheduledTasks.SchedulerPlugin;
import ix.core.models.BackupEntity;
import ix.core.util.EntityUtils;
import ix.core.utils.executor.ProcessListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class ReindexFromBackups implements ReindexService{

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private BackupRepository backupRepository;

    @Async
    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void execute(Object id, SchedulerPlugin.TaskListener l) throws IOException {
        l.message("Initializing reindexing");

//this is all handled now by Spring events
        int count = (int) backupRepository.count();
        Map<Class, Boolean> isRootIndexCache = new ConcurrentHashMap<>();
        Set<String> seen = Collections.newSetFromMap(new ConcurrentHashMap<>(count));
        System.out.println("found count of " + count);
        //single thread for now...
        UUID reindexId = (UUID) id;
        eventPublisher.publishEvent(new BeginReindexEvent(reindexId, count));
        try(Stream<BackupEntity> stream = backupRepository.findAll().stream()){

            stream.forEach(be ->{
                try {
                    EntityUtils.EntityWrapper wrapper = EntityUtils.EntityWrapper.of(be.getInstantiated());

                    wrapper.traverse().execute((p, child)->{
                        EntityUtils.EntityWrapper<EntityUtils.EntityWrapper> wrapped = EntityUtils.EntityWrapper.of(child);
                        if(wrapped.isEntity()) {

                            //this should speed up indexing so that we only index
                            //things that are roots.  the actual indexing process of the root should handle any
                            //child objects of that root.
                            if(isRootIndexCache.computeIfAbsent(child.getEntityClass(), c->wrapped.getEntityInfo().isRootIndex())) {
                                try {
                                    EntityUtils.Key key = wrapped.getKey();
                                    String keyString = key.toString();

                                    //TODO add only index if it has a controller
                                    if (seen.add(keyString)) {
                                        //is this a good idea ?
                                        ReindexEntityEvent event = new ReindexEntityEvent(reindexId, key);
                                        eventPublisher.publishEvent(event);
                                    }
                                } catch (Throwable t) {
                                    System.err.println("error handling " + wrapped);
                                    t.printStackTrace();
                                }
                            }
                        }

                    });
                    eventPublisher.publishEvent(new IncrementReindexEvent(reindexId));
//                    l.message("Indexed:" + counter.incrementAndGet() + messageTail);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                    });
        }
        //other index listeners now figure out when indexing end is so don't need to that publish anymore (here)


        /*
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
                                //this should speed up indexing so that we only index
                                //things that are roots.  the actual indexing process of the root should handle any
                                //child objects of that root.
                                if(isRootIndexCache.computeIfAbsent(child.getEntityClass(), c->wrapped.getEntityInfo().isRootIndex())) {
                                    try {
                                        String key = wrapped.getKey().toString();

                                        //TODO add only index if it has a controller
                                        if (seen.add(key)) {
                                            //is this a good idea ?
                                            IndexCreateEntityEvent event = new IndexCreateEntityEvent(wrapped);
                                            eventPublisher.publishEvent(event);
                                        }
                                    } catch (Throwable t) {
                                        System.err.println("error handling " + wrapped);
                                        t.printStackTrace();
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .build()
                .execute();

         */
    }
}
