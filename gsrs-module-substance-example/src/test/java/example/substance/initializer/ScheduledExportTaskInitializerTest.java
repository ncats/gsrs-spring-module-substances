package example.substance.initializer;

import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.autoconfigure.GsrsExportConfiguration;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.module.substance.tasks.ScheduledExportTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.service.DefaultExportService;
import gsrs.service.ExportService;
import gsrs.springUtils.AutowireHelper;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;

/**
 *
 * @author mitch
 */
@WithMockUser(username = "admin", roles = "Admin")
@Slf4j
public class ScheduledExportTaskInitializerTest extends AbstractSubstanceJpaFullStackEntityTest {

    public ScheduledExportTaskInitializerTest() {
    }

    private final String fileName = "rep18.gsrs";

    /*@TestConfiguration
    public static class TestConfig {

        @Bean
        public ExportService exportService() {
            GsrsExportConfiguration config = new GsrsExportConfiguration();
            return new DefaultExportService(config);
        }
    }*/
    
//    @Autowired
//    ExportService exportService;

    @BeforeEach
    public void clearIndexers() throws IOException {
        SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
        AutowireHelper.getInstance().autowire(hashIndexer);
        //testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);
        /*{
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(ChemicalValidator.class);
            config.setNewObjClass(ChemicalSubstance.class);
            factory.addValidator("substances", config);
        }*/

        File dataFile = new ClassPathResource(fileName).getFile();
        loadGsrsFile(dataFile);
    }

    @Test
    public void testExport() {
        ScheduledExportTaskInitializer exportTask = new ScheduledExportTaskInitializer();
        AutowireHelper.getInstance().autowire(exportTask);

        SchedulerPlugin.TaskListener listener = new SchedulerPlugin.TaskListener();

        exportTask.run(listener);
        String msg = listener.getMessage();
        log.debug("message from listener: " + msg);
        Assertions.assertNotNull(msg);
    }
}
