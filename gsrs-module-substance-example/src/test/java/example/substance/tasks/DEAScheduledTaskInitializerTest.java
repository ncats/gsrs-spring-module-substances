package example.substance.tasks;

import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.module.substance.tasks.DEAScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Slf4j
@WithMockUser(username = "admin", roles = "Admin")
@Disabled
public class DEAScheduledTaskInitializerTest extends AbstractSubstanceJpaFullStackEntityTest {

    private String fileName = "testdumps/rep19.tsv";

    @Autowired
    private TestGsrsValidatorFactory factory;

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;

    @BeforeEach
    public void clearIndexers() throws IOException {
        SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
        AutowireHelper.getInstance().autowire(hashIndexer);
        testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);
        {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(ChemicalValidator.class);
            config.setNewObjClass(ChemicalSubstance.class);
            factory.addValidator("substances", config);
        }

        File dataFile = new ClassPathResource(fileName).getFile();
        loadGsrsFile(dataFile);
    }

    @Test
    public void DeaScheduledTaskTest1() throws IOException {
        File reportFile = File.createTempFile("task_test.report", "txt");
        String reportFilePath =reportFile.getAbsolutePath();
        log.debug("using reportFilePath {}", reportFilePath);
        DEAScheduledTaskInitializer task = new DEAScheduledTaskInitializer();
        task.setOutputFilePath(reportFilePath);
        String deaDataFilePath = "DEA_LIST.txt";
        File deaDataFile = new ClassPathResource(deaDataFilePath).getFile();

        task.setDeaNumberFileName(deaDataFile.getAbsolutePath());
        SchedulerPlugin.TaskListener listener = new SchedulerPlugin.TaskListener();
        AutowireHelper.getInstance().autowire(task);
        task.run(null, listener);

        List<String> lines = Files.readAllLines((new File(reportFilePath)).toPath());
        lines.forEach(l-> System.out.println(l));
        Assertions.assertTrue(lines.size()>0);

    }
}
