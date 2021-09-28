package fda.gsrs.substance.initializers;

import gsrs.module.substance.tasks.ScheduledExportTaskInitializer;
import gsrs.scheduledTasks.CronExpressionBuilder;
import java.io.File;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by katzelda on 6/20/17.
 */
@Slf4j
public class SplExportInitializer extends ScheduledExportTaskInitializer{
    public SplExportInitializer() {
        this(CronExpressionBuilder.builder()
                .everyDay()
                .atHourAndMinute(3, 04)
                .build());
    }


    private File splExportRootDir;

    public File getSplExportRootDir() {
        return splExportRootDir;
    }

    public void setSplExportRootDir(File splExportRootDir) {
        this.splExportRootDir = splExportRootDir;
    }

    /*protected void additionalInitializeWith(Map<String, ?> m) {

        String path = (String)m.get("output.path");
        if(path !=null){
            splExportRootDir = new File(path);
        }else{
            splExportRootDir = null;
        }
    }*/

    public SplExportInitializer(String defaultCron) {
        super();
        this.setCron(defaultCron);
    }


    protected String getCollectionID() {
        return "export-spl";
    }

    protected String getExtension() {
        return "spl.xml";
    }


    @Override
    public String getDescription() {
        return "Create a file with all available records in SPL";
    }
}
