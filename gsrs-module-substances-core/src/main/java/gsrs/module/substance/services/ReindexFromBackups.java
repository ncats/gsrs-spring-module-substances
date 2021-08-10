package gsrs.module.substance.services;

import gsrs.events.*;
import gsrs.indexer.IndexCreateEntityEvent;
import gsrs.module.substance.tasks.ProcessExecutionService;
import gsrs.repository.BackupRepository;
import gsrs.scheduledTasks.SchedulerPlugin;
import ix.core.models.BackupEntity;
import ix.core.util.EntityUtils;
import ix.core.utils.executor.ProcessListener;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class ReindexFromBackups implements ReindexService{

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private BackupRepository backupRepository;

    private Map<UUID, CountDownLatch> latchMap = new ConcurrentHashMap<>();
    private Map<UUID, TaskProgress> listenerMap = new ConcurrentHashMap<>();

    @Data
    @Builder
    private static class TaskProgress{
        private SchedulerPlugin.TaskListener listener;
        private UUID id;
        private long totalCount;
        private long currentCount;

        public synchronized void increment(){
            listener.message("Indexed:" + (++currentCount) + "of " + totalCount);
        }
    }
    @Async
    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void executeAsync(Object id, SchedulerPlugin.TaskListener l) throws IOException {
        execute(id, l);
    }
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
        CountDownLatch latch = new CountDownLatch(1);
        latchMap.put(reindexId, latch);
        listenerMap.put(reindexId, TaskProgress.builder()
                                    .id(reindexId)
                                    .totalCount(count)
                                    .listener(l)
                                    .build());

        eventPublisher.publishEvent(new BeginReindexEvent(reindexId, count));

        try(Stream<BackupEntity> stream = backupRepository.findAll().stream()){

            stream.forEach(be ->{
                try {
                    Optional<Object> opt = be.getOptionalInstantiated();
                    if(opt.isPresent()) {

                        EntityUtils.EntityWrapper wrapper = EntityUtils.EntityWrapper.of(opt.get());

                        wrapper.traverse().execute((p, child) -> {
                            EntityUtils.EntityWrapper<EntityUtils.EntityWrapper> wrapped = EntityUtils.EntityWrapper.of(child);
                            if (wrapped.isEntity()) {

                                //this should speed up indexing so that we only index
                                //things that are roots.  the actual indexing process of the root should handle any
                                //child objects of that root.
                                if (isRootIndexCache.computeIfAbsent(child.getEntityClass(), c -> wrapped.getEntityInfo().isRootIndex())) {
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
                    }
                    eventPublisher.publishEvent(new IncrementReindexEvent(reindexId));
//                    l.message("Indexed:" + counter.incrementAndGet() + messageTail);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                    });
        }
        //other index listeners now figure out when indexing end is so don't need to that publish anymore (here)
        //but we will block until we get that end event
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @EventListener(EndReindexEvent.class)
    public void endReindex(EndReindexEvent event){
        CountDownLatch latch = latchMap.remove(event.getId());
        if(latch !=null){
            latch.countDown();
        }
    }

    @EventListener(IncrementReindexEvent.class)
    public void endReindex(IncrementReindexEvent event){
        TaskProgress progress = listenerMap.get(event.getId());
        if(progress !=null){
            progress.increment();
        }
    }
}
