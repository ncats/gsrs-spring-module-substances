package example.substance.support;

import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class TestContextBootstrap {

    private static final Map<ApplicationContext, Set<String>> COMPLETED_KEYS_BY_CONTEXT =
            Collections.synchronizedMap(new WeakHashMap<>());

    private TestContextBootstrap() {
    }

    public static void runOnce(ApplicationContext applicationContext,
                               String bootstrapKey,
                               ThrowingRunnable bootstrap) throws IOException {
        synchronized (COMPLETED_KEYS_BY_CONTEXT) {
            Set<String> completedKeys = COMPLETED_KEYS_BY_CONTEXT.computeIfAbsent(applicationContext,
                    key -> new HashSet<>());
            if (completedKeys.contains(bootstrapKey)) {
                return;
            }
            bootstrap.run();
            completedKeys.add(bootstrapKey);
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws IOException;
    }
}
