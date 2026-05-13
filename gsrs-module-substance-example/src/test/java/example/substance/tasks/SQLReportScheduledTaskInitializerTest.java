package example.substance.tasks;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.module.substance.tasks.SQLReportScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.GsrsFullStackTest;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/**
 *
 * @author mitch
 */
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
@GsrsFullStackTest(dirtyMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(AutowireHelper.class)
@Slf4j
public class SQLReportScheduledTaskInitializerTest extends AbstractSubstanceJpaFullStackEntityTest{

    private static final String TEST_DATA_FILE = "rep18.gsrs";

    public SQLReportScheduledTaskInitializerTest() {
        super(false);
    }

    @BeforeEach
    public void setup() throws IOException {
        if (substanceRepository.count() == 0) {
            SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
            AutowireHelper.getInstance().autowire(hashIndexer);

            File dataFile = new ClassPathResource(TEST_DATA_FILE).getFile();
            loadGsrsFile(dataFile);
        }
    }

    @Test
    public void testReport1(@TempDir Path tmpDir){
        String reportFilePath = "test1.report.txt";
        File report = new File(tmpDir.toFile(), reportFilePath);
        if( report.exists()) {
            report.delete();
        }
        log.debug("writing report to file:  " + reportFilePath);
        SQLReportScheduledTaskInitializer sqlReport = new SQLReportScheduledTaskInitializer();
        sqlReport.setSql("select uuid, dtype, current_version, created from ix_ginas_substance ");
        sqlReport.setOutputPath(report.toString());
        SchedulerPlugin.TaskListener listener = new SchedulerPlugin.TaskListener();
        sqlReport.run(null, listener);
        Assertions.assertTrue(report.exists(), "Report file must be created");
    }

    @Test
    public void testReport2(@TempDir Path tmpDir){
        String reportFilePath = "test2.report.txt";
        File report = new File(tmpDir.toFile(), reportFilePath);
        if( report.exists()) {
            report.delete();
        }
        SQLReportScheduledTaskInitializer sqlReport = new SQLReportScheduledTaskInitializer();
        sqlReport.setSql("select s.uuid, dtype, s.current_version, s.created, n.name from ix_ginas_substance s, ix_ginas_name n where s.uuid =n.owner_uuid and n.display_name = true");
        sqlReport.setOutputPath(report.toString());
        SchedulerPlugin.TaskListener listener = new SchedulerPlugin.TaskListener();
        sqlReport.run(null, listener);
        Assertions.assertTrue(report.exists(), "Report file must be created");
    }

    @Test
    public void testSqlConnection() throws SQLException {
        SQLReportScheduledTaskInitializer taskInit = new SQLReportScheduledTaskInitializer();
        String sql = "select dtype from ix_ginas_substance where uuid ='1cf410f9-3eeb-41ed-ab69-eeb5076901e5'";
        try (Connection con = taskInit.getConnection();
             Statement statement = con.createStatement();
             ResultSet results = statement.executeQuery(sql)) {
            Assertions.assertTrue(results.next(), "Must be able to move to first record");
            String compoundClass = results.getString(1);
            String expectedClass = "NA";
            Assertions.assertEquals(expectedClass, compoundClass);
        }
    }
}
