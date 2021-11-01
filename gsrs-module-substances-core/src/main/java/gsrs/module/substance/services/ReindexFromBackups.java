package gsrs.module.substance.services;

import gsrs.events.BeginReindexEvent;
import gsrs.events.EndReindexEvent;
import gsrs.events.IncrementReindexEvent;
import gsrs.events.ReindexEntityEvent;
import gsrs.repository.BackupRepository;
import gsrs.scheduledTasks.SchedulerPlugin;
import ix.core.models.BackupEntity;
import ix.core.util.EntityUtils;
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
        // TP 11/01/2021 TODO: this is used to lock how many events there should be,
        // but it's not clear this is actually the correct way for this to work
        // since the count of backup objects can change from this point
        // to the iteration. In addition, some objects can fail reindexing
        // for a variety of reasons. It's not clear to me why the count
        // mechanism should be used to track reindexing completeness
        // when the process isn't always for a known number of entities.
        // Worth reevaluating. For now, just making sure that even indexing failures
        // count as events.
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
        l.message("Initializing reindexing: acquiring list");
//        try(Stream<BackupEntity> stream = backupRepository.streamAll()){
        try(Stream<BackupEntity> stream = backupRepository.findAll().stream()){
            l.message("Initializing reindexing: beginning process");

            stream
            .map(be->{
                try {
                    Optional<Object> opt = be.getOptionalInstantiated();
                    return opt;
                }catch(Exception e) {
                    log.warn("indexing error handling:" + be.fetchGlobalId(), e);
                    eventPublisher.publishEvent(new IncrementReindexEvent(reindexId));
                    return Optional.empty();
                }
            })
            .filter(op->op.isPresent())
//            .parallel()
            .map(oo->EntityUtils.EntityWrapper.of(oo.get()))
            .forEach(wrapper ->{
                try {
                    wrapper.traverse().execute((p, child) -> {
                        EntityUtils.EntityWrapper<EntityUtils.EntityWrapper> wrapped = EntityUtils.EntityWrapper.of(child);
                        //this should speed up indexing so that we only index
                        //things that are roots.  the actual indexing process of the root should handle any
                        //child objects of that root.
                        boolean isEntity = wrapped.isEntity();
                        boolean isRootIndexed = wrapped.isRootIndex();
                        
                        if (isEntity && isRootIndexed) {
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
                    
                }catch(Throwable ee) {
                    log.warn("indexing error handling:" + wrapper, ee);
                }finally {
                    eventPublisher.publishEvent(new IncrementReindexEvent(reindexId));
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
