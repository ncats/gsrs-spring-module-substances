package example.substance.support;

import gsrs.springUtils.AutowireHelper;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Keeps AutowireHelper aligned with the active Spring test context.
 */
public final class AutowireHelperResetTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public int getOrder() {
        return 1750;
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        AutowireHelper helper = new AutowireHelper();
        helper.setApplicationContext(testContext.getApplicationContext());
    }
}
