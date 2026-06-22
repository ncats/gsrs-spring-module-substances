package example.imports;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import gsrs.module.substance.importers.GSRSJSONImportAdapter;
import gsrs.module.substance.importers.GSRSJSONImportAdapterFactory;
import gsrs.springUtils.AutowireHelper;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.mockito.Mockito;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.stream.Stream;

class GSRSJSONImportAdapterFactoryParsingTest {

    @BeforeAll
    static void configureAutowireHelper() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.refresh();
        AutowireHelper helper = new AutowireHelper();
        helper.setApplicationContext(applicationContext);
    }

    @Test
    void parseTest() throws IOException {
        GSRSJSONImportAdapterFactory factory = new GSRSJSONImportAdapterFactory();
        GSRSJSONImportAdapter adapter = (GSRSJSONImportAdapter) factory.createAdapter(JsonNodeFactory.instance.objectNode());
        PlatformTransactionManager transactionManager = Mockito.mock(PlatformTransactionManager.class);
        Mockito.when(transactionManager.getTransaction(Mockito.any())).thenReturn(new SimpleTransactionStatus());
        ReflectionTestUtils.setField(adapter, "platformTransactionManager", transactionManager);

        Resource dataFile = new ClassPathResource("testdumps/rep90.ginas");

        try (FileInputStream fis = new FileInputStream(dataFile.getFile());
             Stream<Substance> substances = adapter.parse(fis, null, null)) {
            Assertions.assertEquals(90, substances.count());
        }
    }
}

