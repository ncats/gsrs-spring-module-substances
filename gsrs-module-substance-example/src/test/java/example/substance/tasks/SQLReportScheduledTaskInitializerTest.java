package example.substance.tasks;

import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.module.substance.tasks.SQLReportScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.springUtils.AutowireHelper;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;


/**
 *
 * @author mitch
 */
@WithMockUser(username = "admin", roles = "Admin")
@Slf4j
public class SQLReportScheduledTaskInitializerTest extends AbstractSubstanceJpaFullStackEntityTest{
    
    private final String fileName = "rep18.gsrs";

    @BeforeEach
    public void setup() throws IOException {
        SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
        AutowireHelper.getInstance().autowire(hashIndexer);

        File dataFile = new ClassPathResource(fileName).getFile();
        loadGsrsFile(dataFile);
    }

    @Test
    public void testReport1(){
        String reportFilePath = "d:\\temp\\test1.report.txt";
        File report = new File(reportFilePath);
        if( report.exists()) {
            report.delete();
        }
        SQLReportScheduledTaskInitializer sqlReport = new SQLReportScheduledTaskInitializer();
        sqlReport.setSql("select uuid, dtype, current_version, created from ix_ginas_substance ");
        sqlReport.setOutputPath(reportFilePath);
        SchedulerPlugin.TaskListener listener = new SchedulerPlugin.TaskListener();
        sqlReport.run(listener);
        Assertions.assertTrue(report.exists(), "Report file must be created");
    }
    
    @Test
    public void testReport2(){
        String reportFilePath = "d:\\temp\\test2.report.txt";
        File report = new File(reportFilePath);
        if( report.exists()) {
            report.delete();
        }
        SQLReportScheduledTaskInitializer sqlReport = new SQLReportScheduledTaskInitializer();
        sqlReport.setSql("select s.uuid, dtype, s.current_version, s.created, n.name from ix_ginas_substance s, ix_ginas_name n where s.uuid =n.owner_uuid and n.display_name = true");
        sqlReport.setOutputPath(reportFilePath);
        SchedulerPlugin.TaskListener listener = new SchedulerPlugin.TaskListener();
        sqlReport.run(listener);
        Assertions.assertTrue(report.exists(), "Report file must be created");
    }
    
    @Test
    public void testSqlConnection() throws SQLException {
        SQLReportScheduledTaskInitializer taskInit = new SQLReportScheduledTaskInitializer();
        Connection con = taskInit.getConnection();
        Statement statement = con.createStatement();
        String sql = "select dtype from ix_ginas_substance where uuid ='1cf410f9-3eeb-41ed-ab69-eeb5076901e5'";
        ResultSet results=  statement.executeQuery(sql);
        Assertions.assertTrue(results.next(), "Must be able to move to first record");
        String compoundClass= results.getString(1);
        String expectedClass = "NA";
        Assertions.assertEquals(expectedClass, compoundClass);
    }
}
