package gsrs.module.substance.tasks;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.fasterxml.jackson.annotation.JsonProperty;

import gov.nih.ncats.common.util.TimeUtil;
import ix.core.util.KeepLastList;
import ix.utils.Util;
import gsrs.config.FilePathParserUtils;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin.TaskListener;

public class DataRecorder extends ScheduledTaskInitializer {

    private static final int DEFAULT_NUM_RECORDS= 10; // with default cron of very 30 sec this makes last 5 mins snapshots
   
//    private String logFileString = new File("logs/dataRecorder.log");
    
    private String outputPath;

    private DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    private Lock lock = new ReentrantLock();

    private boolean enabled;

    private KeepLastList<String> data= new KeepLastList<String>(DEFAULT_NUM_RECORDS);
    
    
    @JsonProperty("dateFormat")
    public void setFormat(String format) {
        if(format !=null){
            formatter = DateTimeFormatter.ofPattern(format);
        }
    }
    

    @JsonProperty("outputPath")
    public void setOutputPath(String path) {
        this.outputPath=path;
    }
    
    @JsonProperty("keep_record_count")
    public void setKeepRecordCount(Integer krc) {
        if(krc !=null && krc>0){
            data= new KeepLastList<String>(krc);
        }
    }
    
    
    
    public File getOutputFile() {
        return FilePathParserUtils.getFileParserBuilder()
                           .suppliedFilePath(outputPath)
                           .defaultFilePath("logs/dataRecorder.log")
                           .dateFormatter(formatter)
                           .build()
                           .getFile();
    }
    
    

    @Override
    public void run(TaskListener l) {
        lock.lock();

        try {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 PrintStream ps = new PrintStream(baos, true, "utf-8");
            ){
                ps.println(" ==========================================");
                ps.println(formatter.format(TimeUtil.getCurrentLocalDateTime()));
                ps.println(" ==========================================");
                Util.printAllExecutingStackTraces(ps);
                ps.println("==========================================");

                data.add(new String(baos.toByteArray(), StandardCharsets.UTF_8));
            }catch(IOException e){

            }
            File logFile =getOutputFile();
            
            File tmpFile = new File(logFile.getParentFile(), logFile.getName()+".tmp");

            try (PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpFile)))) {


                for(String s: data){
                    out.println(s);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try{
                Files.copy(tmpFile.toPath(), logFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }catch(IOException e){
                e.printStackTrace();
            }
        }finally{
            lock.unlock();
        }
    }

    @Override
    public String getDescription() {
        return "Record Last X stack traces like an airplane's Flight Data Recorder to :" + getOutputFile().getPath();
    }
}
