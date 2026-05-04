package example.substance.support;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

public final class TestTransactionManagers {

    private TestTransactionManagers() {
    }

    public static PlatformTransactionManager mockTransactionManager() {
        PlatformTransactionManager transactionManager = Mockito.mock(PlatformTransactionManager.class);
        Mockito.lenient().when(transactionManager.getTransaction(ArgumentMatchers.any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
        return transactionManager;
    }
}
