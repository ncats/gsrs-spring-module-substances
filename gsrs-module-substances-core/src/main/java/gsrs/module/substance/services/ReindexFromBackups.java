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
import lombok.extern.slf4j.Slf4j;

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

/**
 * A {@link ReindexService} that pulls all {@link BackupEntity}
 * objects from the back up table and re-indexes the backed up objects.
 * The indexes are updated using the Reindex Events.
 *
 * @see BeginReindexEvent
 * @see EndReindexEvent
 * @see IncrementReindexEvent
 */
@Slf4j
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
            listener.message("Indexed:" + (++currentCount) + " of " + totalCount);
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
        Set<String> seen = Collections.newSetFromMap(new ConcurrentHashMap<>(count));
        log.debug("found count of " + count);
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

//        try(Stream<BackupEntity> stream = backupRepository.streamAll()){
        try(Stream<BackupEntity> stream = backupRepository.findAll().stream()){

            stream.forEach(be ->{
                try {
                    Optional<Object> opt = be.getOptionalInstantiated();
                    if(opt.isPresent()) {

                        EntityUtils.EntityWrapper wrapper = EntityUtils.EntityWrapper.of(opt.get());

                        wrapper.traverse().execute((p, child) -> {
                            EntityUtils.EntityWrapper<EntityUtils.EntityWrapper> wrapped = EntityUtils.EntityWrapper.of(child);
                            //this should speed up indexing so that we only index
                            //things that are roots.  the actual indexing process of the root should handle any
                            //child objects of that root.
                            if (wrapped.isEntity() && wrapped.isRootIndex()) {
                                try {
                                    EntityUtils.Key key = wrapped.getKey();
                                    String keyString = key.toString();

                                    // TODO add only index if it has a controller?
                                    // TP: actually, for subunits you need to index them even though there is no controller
                                    // however, you could argue there SHOULD be a controller for them
                                    if (seen.add(keyString)) {
                                        //is this a good idea ?
                                        ReindexEntityEvent event = new ReindexEntityEvent(reindexId, key,Optional.of(wrapped));
                                        
//                                        ReindexEntityEvent event = new ReindexEntityEvent(reindexId, key);
                                        
                                        eventPublisher.publishEvent(event);
                                    }
                                } catch (Throwable t) {
                                    log.warn("indexing error handling:" + wrapped, t);
                                }
                            }

                        });
                    }
                    eventPublisher.publishEvent(new IncrementReindexEvent(reindexId));
                } catch (Exception e) {
                    log.warn("indexing error handling:" + be.fetchGlobalId(), e);
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
