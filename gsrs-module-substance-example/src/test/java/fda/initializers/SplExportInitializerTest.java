package fda.initializers;

import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import fda.gsrs.substance.initializers.SplExportInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

/**
 *
 * @author mitch
 */
@WithMockUser(username = "admin", roles = "Admin")
@Slf4j
public class SplExportInitializerTest extends AbstractSubstanceJpaFullStackEntityTest{
    
    @Test
    public void testExport() {
        SplExportInitializer init = new SplExportInitializer();
        String filePath = System.getProperty("java.io.tmpdir");
        File reportFile = new File(filePath);
//        if( reportFile.exists() ){
//            reportFile.delete();
//        }
        init.setSplExportRootDir(reportFile);
        SchedulerPlugin.TaskListener listener = new SchedulerPlugin.TaskListener();
        init.run(listener);
        System.out.println("Look at " + filePath);
        Assertions.assertTrue(reportFile.list().length >0);
    }

}
