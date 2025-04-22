package gsrs.module.substance.tasks;

import com.fasterxml.jackson.annotation.JsonProperty;

import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gsrs.EntityProcessorFactory;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.AdminService;
import gsrs.springUtils.StaticContextAccessor;
import ix.ginas.models.GinasCommonData;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Used to update any kind of Entity objects
 *
 * @author Egor Puzanov
 *
 */
@Slf4j
public class UpdateEntityTaskInitializer extends ScheduledTaskInitializer {

    private Class<? extends GinasCommonData> entityClass;
    private String description;
    private String query;
    private List<Field> resetFields;
    private List<Field> publicFields;

    @Autowired
    private EntityProcessorFactory entityProcessorFactory;

    @Autowired
    private AdminService adminService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @SuppressWarnings("unchecked")
    public UpdateEntityTaskInitializer(
        @JsonProperty("entityClass") String className,
        @JsonProperty("description") String description,
        @JsonProperty("query") String query,
        @JsonProperty("resetFields") Map<Integer, String> resetFields) {
        try {
            this.entityClass = (Class<? extends GinasCommonData>) Class.forName(className);
        } catch (Exception e) {
            this.entityClass = null;
        }
        if (this.entityClass != null) {
            String simpleClassName = this.entityClass.getSimpleName();
            this.publicFields = Arrays.asList(this.entityClass.getFields());
            if (description != null && !description.isEmpty()) {
                this.description = description;
            } else {
                this.description = "Update all " + simpleClassName + " entities in the database";
            }
            if (query != null && !query.isEmpty()) {
                this.query = query;
            } else {
                this.query = "select " + ("Structure".equals(simpleClassName) ? "" : "uu") + "id from " + simpleClassName;
            }
            if (resetFields != null && !resetFields.isEmpty()) {
                this.resetFields = resetFields
                    .values()
                    .stream()
                    .map(f->{
                        Field field = null;
                        try {
                            field = this.entityClass.getDeclaredField(f);
                            field.setAccessible(true);
                        } catch (Exception e) {}
                        return field;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            } else {
                this.resetFields = new ArrayList<>();
            }
        }
    }

    @Override
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l) {

        String entityType = entityClass.getSimpleName();
        l.message("Initializing " + entityType + " Updater");
        log.trace("Initializing " + entityType + " Updater");

        l.message("Initializing " + entityType + " Updater: acquiring list");

        ExecutorService executor = BlockingSubmitExecutor.newFixedThreadPool(5, 10);
        l.message("Initializing " + entityType + " Updater: acquiring user account");
        Authentication adminAuth = adminService.getAnyAdmin();
        l.message("Initializing " + entityType + " Updater: starting process");
        log.trace("starting process");

        try {
            adminService.runAs(adminAuth, (Runnable) () -> {
                TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                log.trace("got outer tx " + tx);
                tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                try {
                    processEntities(l);
                } catch (Exception ex) {
                    l.message(entityType + " processing error: " + ex.getMessage());
                }
            });
        } catch (Exception ee) {
            log.error(entityType + " processing error: ", ee);
            l.message("ERROR:" + ee.getMessage());
            throw new RuntimeException(ee);
        }

        l.message("Shutting down executor service");
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            //should never happen
            log.error("Interrupted exception!");
        }
        l.message("Task finished");
        l.complete();
        //listen.doneProcess();
    }

    @Override
    public String getDescription() {
        return description;
    }

    private int makeHash(Object entity) {
        return (resetFields.isEmpty() ? publicFields : resetFields)
            .stream()
            .map(f->{
                try {
                    return String.valueOf(f.get(entity));
                } catch (Exception e) {
                    return "";
                }
            })
            .collect(Collectors.joining()).hashCode();
    }

    @SuppressWarnings("unchecked")
    private void processEntities(SchedulerPlugin.TaskListener l){
        String entityType = entityClass.getSimpleName();
        log.trace("starting in {} entities", entityType);

        @SuppressWarnings({ "rawtypes" })
        JpaRepository<Object, UUID> repository = new SimpleJpaRepository(entityClass, StaticContextAccessor.getEntityManagerFor(entityClass));
        List<UUID> uuids = StaticContextAccessor
            .getEntityManagerFor(entityClass)
            .createQuery(query, UUID.class)
            .getResultList();
        int total = uuids.size();
        log.trace("total {} entities: {}", entityType, total);
        int soFar = 0;
        for (UUID uuid: uuids) {
            soFar++;
            log.trace("processing {} entity with ID {}", entityType, uuid.toString());
            try {
                Optional<Object> entityOpt = repository.findById(uuid);
                if (!entityOpt.isPresent()) {
                    log.info("No {} entity found with ID {}", entityType, uuid.toString());
                    continue;
                }
                Object entity = entityOpt.get();
                int hash = makeHash(entity);
                for (Field field : resetFields) {
                    field.set(entity, null);
                }
                entityProcessorFactory.getCombinedEntityProcessorFor(entity).preUpdate(entity);
                if(makeHash(entity) != hash) {
                    try {
                        log.trace("resaving {} entity {}", entityType, entity.toString());
                        TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                        tx.setReadOnly(false);
                        tx.executeWithoutResult(e -> {
                            log.trace("before saveAndFlush");
                            Object e2 = StaticContextAccessor.getEntityManagerFor(entityClass).merge(entity);
                            GinasCommonData.class.cast(e2).forceUpdate();
                            repository.saveAndFlush(entityClass.cast(e2));
                        });
                        log.trace("saved {} entity {}", entityType, entity.toString());
                    } catch (Exception ex) {
                        log.error("Error during save: {}", ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                log.error("Error during processing: {}", ex.getMessage());
            }
            l.message(String.format("Processed %d of %d %s entities", soFar, total, entityType));
        }
    }
}
