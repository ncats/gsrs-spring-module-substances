package example.substance.tasks;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.tasks.SQLReportScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.startertests.GsrsFullStackTest;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
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
@Tag("fullstack")
@Slf4j
public class SQLReportScheduledTaskInitializerTest extends AbstractSubstanceJpaFullStackEntityTest{

    public SQLReportScheduledTaskInitializerTest() {
        super(false);
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
        String sql = "select 1";
        try (Connection con = taskInit.getConnection();
             Statement statement = con.createStatement();
             ResultSet results = statement.executeQuery(sql)) {
            Assertions.assertTrue(results.next(), "Must be able to move to first record");
            Assertions.assertEquals(1, results.getInt(1), "Expected a successful SQL scalar query");
        }
    }
}
