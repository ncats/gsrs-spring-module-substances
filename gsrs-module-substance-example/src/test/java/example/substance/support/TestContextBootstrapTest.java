package example.substance.support;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class TestContextBootstrapTest {

    @Test
    void runOnceExecutesOnlyOncePerContextAndKey() throws Exception {
        ApplicationContext context = mock(ApplicationContext.class);
        AtomicInteger counter = new AtomicInteger();

        TestContextBootstrap.runOnce(context, "dataset:rep18", counter::incrementAndGet);
        TestContextBootstrap.runOnce(context, "dataset:rep18", counter::incrementAndGet);

        assertEquals(1, counter.get());
    }

    @Test
    void runOnceExecutesForDifferentKeys() throws Exception {
        ApplicationContext context = mock(ApplicationContext.class);
        AtomicInteger counter = new AtomicInteger();

        TestContextBootstrap.runOnce(context, "dataset:rep18", counter::incrementAndGet);
        TestContextBootstrap.runOnce(context, "dataset:rep90", counter::incrementAndGet);

        assertEquals(2, counter.get());
    }

    @Test
    void runOnceDoesNotMarkKeyWhenBootstrapFails() {
        ApplicationContext context = mock(ApplicationContext.class);
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(IOException.class, () -> TestContextBootstrap.runOnce(context, "broken", () -> {
            attempts.incrementAndGet();
            throw new IOException("boom");
        }));
        assertThrows(IOException.class, () -> TestContextBootstrap.runOnce(context, "broken", () -> {
            attempts.incrementAndGet();
            throw new IOException("boom");
        }));

        assertEquals(2, attempts.get());
    }
}

