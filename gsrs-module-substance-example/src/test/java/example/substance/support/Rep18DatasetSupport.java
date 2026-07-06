package example.substance.support;

import java.io.IOException;

import org.springframework.context.ApplicationContext;

public final class Rep18DatasetSupport {

    private Rep18DatasetSupport() {
    }

    public static void loadOnce(ApplicationContext applicationContext,
                                String fileName,
                                ThrowingRunnable loader) throws IOException {
        TestContextBootstrap.runOnce(applicationContext, "dataset:" + fileName, loader::run);
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws IOException;
    }
}
