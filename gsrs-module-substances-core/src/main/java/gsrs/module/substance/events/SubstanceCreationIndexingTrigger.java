package gsrs.module.substance.events;

import gsrs.events.ReindexEntityEvent;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Ensures substance indexing happens synchronously after creation.
 * Publishes a ReindexEntityEvent that triggers immediate indexing,
 * preventing the "N-1 refresh" issue where newly created substances
 * don't appear in search results until after a second browser refresh.
 */
@Component
@Slf4j
public class SubstanceCreationIndexingTrigger {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @EventListener
    public void onSubstanceCreated(SubstanceCreatedEvent event) {
        try {
            Substance substance = event.getSource();
            log.trace("Triggering indexing for newly created substance: {}", substance.getUuid());
            
            // Create entity wrapper and publish reindex event to ensure synchronous indexing
            EntityUtils.EntityWrapper<Substance> wrapper = EntityUtils.EntityWrapper.of(substance);
            UUID reindexUuid = UUID.randomUUID();
            
            // Publishing this event will trigger the TextIndexerEntityListener to index the substance
            // The synchronization fixes in TextIndexerEntityListener ensure this happens atomically
            ReindexEntityEvent reindexEvent = new ReindexEntityEvent(reindexUuid, wrapper.getKey(), Optional.of(wrapper));
            applicationEventPublisher.publishEvent(reindexEvent);
            
            log.trace("Published ReindexEntityEvent for substance: {}", substance.getUuid());
        } catch (Exception e) {
            log.warn("Failed to trigger indexing for newly created substance", e);
            // Don't fail the creation if indexing trigger fails
        }
    }
}
