package example;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import io.hypersistence.optimizer.HypersistenceOptimizer;
import io.hypersistence.optimizer.core.event.Event;
import io.hypersistence.optimizer.hibernate.event.configuration.batching.JdbcBatchSizeEvent;
import io.hypersistence.optimizer.hibernate.event.configuration.connection.SkipAutoCommitCheckEvent;
import io.hypersistence.optimizer.hibernate.event.configuration.fetching.JdbcFetchSizeEvent;
import io.hypersistence.optimizer.hibernate.event.configuration.query.DefaultQueryPlanCacheMaxSizeEvent;
import io.hypersistence.optimizer.hibernate.event.configuration.query.QueryInClauseParameterPaddingEvent;
import io.hypersistence.optimizer.hibernate.event.configuration.query.QueryPaginationCollectionFetchingEvent;
import io.hypersistence.optimizer.hibernate.event.configuration.schema.SchemaGenerationEvent;
import io.hypersistence.optimizer.hibernate.event.connection.AutoCommittingConnectionEvent;
import io.hypersistence.optimizer.hibernate.event.connection.StatementlessConnectionEvent;
import io.hypersistence.optimizer.hibernate.event.connection.TransactionReconnectionEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.association.BidirectionalSynchronizationEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.association.ElementCollectionEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.association.ManyToManyCascadeRemoveEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.association.ManyToManyListEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.association.NullCollectionEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.association.OneToOneParentSideEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.association.OneToOneWithoutMapsIdEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.association.UnidirectionalOneToManyEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.association.fetching.EagerFetchingEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.basic.LargeColumnEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.basic.LongVersionColumnSizeEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.basic.TimestampVersionEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.inheritance.StringDiscriminatorTypeEvent;
import io.hypersistence.optimizer.hibernate.event.query.MassQueryCacheInvalidationEvent;
import io.hypersistence.optimizer.hibernate.event.query.PaginationWithoutOrderByEvent;
import io.hypersistence.optimizer.hibernate.event.query.PassDistinctThroughEvent;
import io.hypersistence.optimizer.hibernate.event.query.QueryResultIteratorCountEvent;
import io.hypersistence.optimizer.hibernate.event.query.QueryResultListSizeEvent;
import io.hypersistence.optimizer.hibernate.event.query.QueryTimeoutEvent;
import io.hypersistence.optimizer.hibernate.event.session.EntityAlreadyManagedEvent;
import io.hypersistence.optimizer.hibernate.event.session.NPlusOneQueryEntityFetchingEvent;
import io.hypersistence.optimizer.hibernate.event.session.OptimisticLockModeEvent;
import io.hypersistence.optimizer.hibernate.event.session.RedundantSessionFlushEvent;
import io.hypersistence.optimizer.hibernate.event.session.SecondaryQueryEntityFetchingEvent;
import io.hypersistence.optimizer.hibernate.event.session.SessionEvent;
import io.hypersistence.optimizer.hibernate.event.session.SessionFlushTimeoutEvent;
import io.hypersistence.optimizer.hibernate.event.session.TableRowAlreadyManagedEvent;
import io.hypersistence.optimizer.hibernate.event.session.TransactionlessSessionEvent;

@SpringBootTest
public class HypersistenceApplicationTest {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    @Autowired
    private HypersistenceOptimizer hypersistenceOptimizer;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @PersistenceContext
    private EntityManager entityManager;
    
    @Test
    public void test() throws ExecutionException, InterruptedException {
    	
    	printOutAllEvents();
    	
        assertEventTriggered(196, EagerFetchingEvent.class);
        assertEventTriggered(58, OneToOneWithoutMapsIdEvent.class);
        assertEventTriggered(57,LongVersionColumnSizeEvent.class);        
        assertEventTriggered(47, BidirectionalSynchronizationEvent.class);
        assertEventTriggered(44, LargeColumnEvent.class);
        assertEventTriggered(23,ManyToManyListEvent.class);
        assertEventTriggered(23,ManyToManyCascadeRemoveEvent.class);
        assertEventTriggered(9,StringDiscriminatorTypeEvent.class);
        assertEventTriggered(3,OneToOneParentSideEvent.class);
        assertEventTriggered(3,AutoCommittingConnectionEvent.class);
        assertEventTriggered(2,UnidirectionalOneToManyEvent.class);
        assertEventTriggered(2,NullCollectionEvent.class);
        
        
        assertEventTriggered(1,QueryPaginationCollectionFetchingEvent.class);
        assertEventTriggered(1,QueryInClauseParameterPaddingEvent.class);
        assertEventTriggered(1,JdbcFetchSizeEvent.class);       
        assertEventTriggered(1, DefaultQueryPlanCacheMaxSizeEvent.class);
        assertEventTriggered(1, TimestampVersionEvent.class);
        assertEventTriggered(1, JdbcBatchSizeEvent.class);
        assertEventTriggered(1, SkipAutoCommitCheckEvent.class);
        assertEventTriggered(1, SchemaGenerationEvent.class);
        assertEventTriggered(1,RedundantSessionFlushEvent.class);
        assertEventTriggered(1,SecondaryQueryEntityFetchingEvent.class);
        
        assertEventTriggered(0,QueryTimeoutEvent.class);
        assertEventTriggered(0, ElementCollectionEvent.class); 
        assertEventTriggered(0,SessionFlushTimeoutEvent.class);
        assertEventTriggered(0,TableRowAlreadyManagedEvent.class);
        assertEventTriggered(0,TransactionlessSessionEvent.class);
        assertEventTriggered(0, EntityAlreadyManagedEvent.class);        
        assertEventTriggered(0,NPlusOneQueryEntityFetchingEvent.class);
        assertEventTriggered(0,OptimisticLockModeEvent.class);
        assertEventTriggered(0,StatementlessConnectionEvent.class);
        assertEventTriggered(0,TransactionReconnectionEvent.class);
        assertEventTriggered(0,MassQueryCacheInvalidationEvent.class);
        assertEventTriggered(0,PaginationWithoutOrderByEvent.class);
        assertEventTriggered(0,PassDistinctThroughEvent.class);
        assertEventTriggered(0,QueryResultIteratorCountEvent.class);
        assertEventTriggered(0,QueryResultListSizeEvent.class);
        assertEventTriggered(0, SessionEvent.class);
    }
    
    private void printOutAllEvents() {
    	Map<String, Integer> classes = new HashMap<String, Integer>();
    	for (Event event : hypersistenceOptimizer.getEvents()) {
    		classes.computeIfAbsent(event.getClass().getName(), s->0);
    		classes.put(event.getClass().getName(), classes.get(event.getClass().getName())+1);        		
    	}    	
    	
    	Map<String, Integer> sorted = classes.entrySet().stream()
    			.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
    			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    	
    	sorted.keySet().forEach(cls->{System.out.println(cls + " "+ classes.get(cls));});
    }
    protected void assertEventTriggered(int expectedCount, Class<? extends Event> eventClass) {
        int count = 0;
                
        for (Event event : hypersistenceOptimizer.getEvents()) {
            if (event.getClass().equals(eventClass)) {
                count++;
            }
        }        
        System.out.println(expectedCount + " " + count);
        assertEquals(expectedCount, count);
    }  
   
}