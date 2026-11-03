package gsrs.module.substance.services;

import ix.core.plugins.MultiThreadedPersistRecordWorkerFactory;
import ix.core.plugins.SingleThreadedPersistRecordWorkerFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubstanceBulkLoadConcurrencyConfigurationTest {

    @Test
    void defaultsShouldFavorCorrectSequentialLoading() {
        SubstanceBulkLoadServiceConfiguration configuration =
                new SubstanceBulkLoadServiceConfiguration();

        assertEquals(1, configuration.getLoadingThreads());
        assertEquals(100, configuration.getMaxQueueSize());
        assertEquals(SingleThreadedPersistRecordWorkerFactory.class.getName(),
                configuration.getPersistRecordWorkerFactoryImpl());
    }

    @Test
    void concurrencyShouldRemainAnExplicitOptIn() {
        SubstanceBulkLoadServiceConfiguration configuration =
                new SubstanceBulkLoadServiceConfiguration();

        configuration.setLoadingThreads(4);
        configuration.setMaxQueueSize(100);
        configuration.setPersistRecordWorkerFactoryImpl(
                MultiThreadedPersistRecordWorkerFactory.class.getName());

        assertEquals(4, configuration.getLoadingThreads());
        assertEquals(100, configuration.getMaxQueueSize());
        assertEquals(MultiThreadedPersistRecordWorkerFactory.class.getName(),
                configuration.getPersistRecordWorkerFactoryImpl());
    }
}
