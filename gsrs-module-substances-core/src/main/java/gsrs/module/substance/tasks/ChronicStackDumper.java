package gsrs.module.substance.tasks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.fasterxml.jackson.annotation.JsonProperty;

import gov.nih.ncats.common.util.TimeUtil;
import gsrs.config.FilePathParserUtils;
import gsrs.scheduledTasks.CronExpressionBuilder;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin.TaskListener;
import ix.utils.Util;

/**
 * Prints all currently running stacktraces to a log file
 * on a regular basis.  If not configured, the log will be invoked
 * every 5 minutes.
 *
 * Created by katzelda on 6/13/17.
 */
public class ChronicStackDumper extends ScheduledTaskInitializer{

    private String outputPath = null;
    private DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    private Lock lock = new ReentrantLock();

    @JsonProperty("outputPath")
    public void setOutputPath(String path) {
        outputPath=path;
    }
    
    @JsonProperty("dateFormat")
    public void setFormat(String format) {
        if(format !=null){
            formatter = DateTimeFormatter.ofPattern(format);
        }
    }

    public File getOutputFile() {
        return FilePathParserUtils.getFileParserBuilder()
                           .suppliedFilePath(outputPath)
                           .defaultFilePath("logs/all-running-stacktraces.log")
                           .dateFormatter(formatter)
                           .build()
                           .getFile();
    }
    

	@Override
	public void run(TaskListener l) {
		    lock.lock();
            try {
                try (PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(getOutputFile(), true)))) {
                    out.println(" ==========================================");
                    out.println(formatter.format(TimeUtil.getCurrentLocalDateTime()));
                    out.println(" ==========================================");

                    Util.printAllExecutingStackTraces(out);
                    out.println("==========================================");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }finally{
                lock.unlock();
            }
	}

	@Override
	public String getDescription() {
		return "Log all Executing Stack Traces to " + getOutputFile().getPath();
	}
}
