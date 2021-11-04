package gsrs.module.substance.services;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import gsrs.events.BeginReindexEvent;
import gsrs.events.EndReindexEvent;
import gsrs.events.IncrementReindexEvent;
import gsrs.events.ReindexEntityEvent;
import gsrs.repository.BackupRepository;
import gsrs.scheduledTasks.SchedulerPlugin;
import ix.core.models.BackupEntity;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

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

    private static final boolean FORCE_SINGLE_THREADED_EVENT_HANDLING=false;
    
    @Autowired
    PlatformTransactionManager transactionManager;


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
        
        UUID reindexId = (UUID) id;

        CountDownLatch endLatch = new CountDownLatch(1);
        latchMap.put(reindexId, endLatch);
        listenerMap.put(reindexId, TaskProgress.builder()
                .id(reindexId)
                .totalCount(count)
                .listener(l)
                .build());

        
        l.message("Initializing reindexing: acquiring list");
        
        
        
        LinkedBlockingDeque<Object> qevents = new LinkedBlockingDeque<>(1_000);

        Consumer<Object> eventConsumer;
        
        if(FORCE_SINGLE_THREADED_EVENT_HANDLING) {
            eventConsumer= (ev)->{
                qevents.add(ev);
            };
        }else {
            eventConsumer= (ev)->{
                eventPublisher.publishEvent(ev);
            };
        }
        
        //This is hard-coded to 4 for now. It may be able to use the 
        //common forkjoin pool, but there are reasons to suspect that
        //can overwhelm the application.
        ForkJoinPool customThreadPool = new ForkJoinPool(4);
        try {
            customThreadPool.submit(
                    () -> {
                        TransactionTemplate tx = new TransactionTemplate(transactionManager);
                        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                        tx.setReadOnly(true);
                        tx.executeWithoutResult(stat->{
                            List<BackupEntity> blist=backupRepository.findAll();
                            eventConsumer.accept(new BeginReindexEvent(reindexId, blist.size()));
                            try(Stream<BackupEntity> stream = blist.stream()){
                                l.message("Initializing reindexing: beginning process");

                                Stream<EntityWrapper> ewStream=stream
                                        .map(be->{
                                            try {
                                                Optional<Object> opt = be.getOptionalInstantiated();
                                                return opt;
                                            }catch(Exception e) {
                                                log.warn("indexing error handling:" + be.fetchGlobalId(), e);
                                                eventConsumer.accept(new IncrementReindexEvent(reindexId));
                                                return Optional.empty();
                                            }
                                        })
                                        .filter(op->op.isPresent())
                                        .map(oo->EntityUtils.EntityWrapper.of(oo.get()));

                                ewStream
                                .parallel()
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
                                                        eventConsumer.accept(event);
                                                    }
                                                } catch (Throwable t) {
                                                    log.warn("indexing error handling:" + wrapped, t);
                                                }
                                            }

                                        });

                                    }catch(Throwable ee) {
                                        log.warn("indexing error handling:" + wrapper, ee);
                                    }finally {
                                        eventConsumer.accept(new IncrementReindexEvent(reindexId));
                                    }
                                });

                            }
                        });
                        return true;
                    });
            
            if(FORCE_SINGLE_THREADED_EVENT_HANDLING) {
                while(true) {
                    Object ot=qevents.take();
                    eventPublisher.publishEvent(ot);
                    if(endLatch.getCount()==0)break;
                }
            }
        }catch(Exception e) {
            log.warn("indexing error", e);
            endLatch.countDown();
        }


        //other index listeners now figure out when indexing end is so don't need to that publish anymore (here)
        //but we will block until we get that end event
        try {
            endLatch.await();
        } catch (InterruptedException e) {
            log.warn("Reindexing inturrupted", e);
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
