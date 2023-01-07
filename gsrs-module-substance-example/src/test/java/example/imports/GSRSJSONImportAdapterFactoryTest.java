package example.imports;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.importers.GSRSJSONImportAdapter;
import gsrs.module.substance.importers.utils.GSRSJSONImportAdapterFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.stream.Stream;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
public class GSRSJSONImportAdapterFactoryTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Test
    public void parseTest() throws IOException {
        GSRSJSONImportAdapterFactory factory = new GSRSJSONImportAdapterFactory();
        GSRSJSONImportAdapter  adapter= (GSRSJSONImportAdapter) factory.createAdapter(JsonNodeFactory.instance.objectNode());
        Resource dataFile = new ClassPathResource("testdumps/rep90.ginas");
        FileInputStream fis = new FileInputStream(dataFile.getFile());
        Stream<Substance> subs = adapter.parse(fis, null);
        Assertions.assertEquals(90, subs.count());
    }
}

