package example.imports;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import example.GsrsModuleSubstanceApplication;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.dataexchange.SubstanceStagingAreaEntityService;
import gsrs.module.substance.importers.GSRSJSONImportAdapter;
import gsrs.module.substance.importers.GSRSJSONImportAdapterFactory;
import gsrs.springUtils.AutowireHelper;
import gsrs.stagingarea.model.ImportRecordParameters;
import gsrs.stagingarea.service.DefaultStagingAreaService;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
public class GSRSJSONImportAdapterFactoryTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Test
    public void parseTest() throws IOException {
        GSRSJSONImportAdapterFactory factory = new GSRSJSONImportAdapterFactory();
        GSRSJSONImportAdapter adapter = (GSRSJSONImportAdapter) factory.createAdapter(JsonNodeFactory.instance.objectNode());
        Resource dataFile = new ClassPathResource("testdumps/rep90.ginas");
        FileInputStream fis = new FileInputStream(dataFile.getFile());
        Stream<Substance> subs = adapter.parse(fis, null, null);
        Assertions.assertEquals(90, subs.count());
    }

    @Test
    public void saveSubstanceTest() throws IOException {
        GSRSJSONImportAdapterFactory factory = new GSRSJSONImportAdapterFactory();
        GSRSJSONImportAdapter adapter = (GSRSJSONImportAdapter) factory.createAdapter(JsonNodeFactory.instance.objectNode());
        Resource dataFile = new ClassPathResource("testdumps/rep19.tsv");
        List<String> lines = Files.readAllLines(dataFile.getFile().toPath());
        List<String> resultingSubstanceIds = new ArrayList<>();
        lines.forEach(l -> {
                    ImportRecordParameters.ImportRecordParametersBuilder builder =
                            ImportRecordParameters.builder()
                                    .jsonData(l.trim())
                                    .entityClassName("ix.ginas.models.v1.Substance")
                                    .formatType("application/json")
                                    .source(dataFile.getFilename())
                                    .adapterName("this");
                    ImportRecordParameters parameters = builder.build();

                    DefaultStagingAreaService<Substance> service = new DefaultStagingAreaService<>();
                    AutowireHelper.getInstance().autowire(service);
                    SubstanceStagingAreaEntityService substanceStagingAreaEntityService = new SubstanceStagingAreaEntityService();
                    AutowireHelper.getInstance().autowire(substanceStagingAreaEntityService);
                    service.registerEntityService(substanceStagingAreaEntityService);
                    String result = service.createRecord(parameters);
                    log.trace("result: {}", result);
                    Assertions.assertNotNull(result);
                    resultingSubstanceIds.add(result);
                }
        );

        Assertions.assertEquals(19, resultingSubstanceIds.size());
    }
}

