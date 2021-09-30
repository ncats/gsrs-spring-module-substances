package gsrs.module.substance.tasks;

/**
 * Created by katzelda on 6/20/17.
 */
public class SplExportInitializer extends ScheduledExportTaskInitializer{
    @Override
    public String getCollectionID() {
        return "export-spl";
    }

    @Override
    public String getExtension() {
        return "spl.xml";
    }

}
