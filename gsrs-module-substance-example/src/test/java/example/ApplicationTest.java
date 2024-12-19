/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package example;

import static org.junit.Assert.assertSame;

import java.util.concurrent.ExecutionException;

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
//import io.hypersistence.optimizer.hibernate.event.configuration.connection.Connection9071557016Event;
//import io.hypersistence.optimizer.hibernate.event.configuration.query.Query7786258879Event;
//import io.hypersistence.optimizer.hibernate.event.configuration.query.Query2468924130Event;
import io.hypersistence.optimizer.hibernate.event.configuration.schema.SchemaGenerationEvent;
//import io.hypersistence.optimizer.hibernate.event.mapping.association.Association3890524098Event;
//import io.hypersistence.optimizer.hibernate.event.mapping.association.Association934543432Event;
import io.hypersistence.optimizer.hibernate.event.mapping.association.OneToOneWithoutMapsIdEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.association.fetching.EagerFetchingEvent;
import io.hypersistence.optimizer.hibernate.event.session.SessionEvent;

@SpringBootTest
public class ApplicationTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private HypersistenceOptimizer hypersistenceOptimizer;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void test() throws ExecutionException, InterruptedException {
 	
        assertEventTriggered(2, EagerFetchingEvent.class);
        assertEventTriggered(1, OneToOneWithoutMapsIdEvent.class);
        assertEventTriggered(1, SchemaGenerationEvent.class);
        assertEventTriggered(1, SessionEvent.class);
    }

    protected void assertEventTriggered(int expectedCount, Class<? extends Event> eventClass) {
        int count = 0;

        for (Event event : hypersistenceOptimizer.getEvents()) {
            if (event.getClass().equals(eventClass)) {
                count++;
            }
        }

        assertSame(expectedCount, count);
    }
}