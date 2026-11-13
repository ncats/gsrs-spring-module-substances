package gsrs.module.substance.tasks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.sql.DataSource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import gsrs.config.FilePathParserUtils;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.scheduledTasks.SchedulerPlugin.TaskListener;
import gsrs.springUtils.StaticContextAccessor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * Used to schedule output of certain reports, using defined SQL queries in the
 * config file
 *
 * @author tyler
 *
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper=false)
public class SQLReportScheduledTaskInitializer
        extends ScheduledTaskInitializer {

    private String name = "sqlReport";
    private String sql;
    private String outputPath;
    @JsonIgnore
    private DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
    @JsonIgnore
    private DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("HHmmss");
    private String dataSourceQualifier;

    private Lock lock = new ReentrantLock();

    
    @JsonProperty("formatter")
    public void setFormat(String format) {
        if(format !=null){
            formatter = DateTimeFormatter.ofPattern(format);
        }
    }
    

    @JsonProperty("formatterTime")
    public void setFormatTime(String format) {
        if(format !=null){
        	formatterTime = DateTimeFormatter.ofPattern(format);
        }
    }
    
    /**
     * Returns the File used to output the report
     *
     * @return
     */
    public File getOutputFile() {
        return FilePathParserUtils.getFileParserBuilder()
                           .suppliedFilePath(outputPath)
                           .defaultFilePath("reports/" + name + "-%DATE%.txt")
                           .dateFormatter(formatter)
                           .build()
                           .getFile();
    }

    private PrintStream makePrintStream(File writeFile) throws IOException {
        return new PrintStream(
                new BufferedOutputStream(new FileOutputStream(writeFile)),
                false, "UTF-8");
    }

    public void run(SchedulerPlugin.JobStats stats, TaskListener l) {
        try {
            lock.lock();
            l.message("Initializing SQL");

            File writeFile = getOutputFile();
            File abfile = writeFile.getAbsoluteFile();
            File pfile = abfile.getParentFile();
            
            pfile.mkdirs();

            try (PrintStream out = makePrintStream(writeFile)) {

                l.message("Establishing connection");

                try (Connection c = getConnection()) {

                    Statement s = c.createStatement(
                            ResultSet.TYPE_SCROLL_INSENSITIVE,
                            ResultSet.CONCUR_READ_ONLY);

                    l.message("Executing Statement");
                    ResultSet rs1 = s.executeQuery(sql);

                    // Count rows
                    l.message("Counting Rows");
                    rs1.last();
                    int total = rs1.getRow();
                    rs1.beforeFirst();

                    l.message("Preparing export rows");

                    int ccount = rs1.getMetaData().getColumnCount();
                    l.message("Getting column names");
                    for (int i = 1; i <= ccount; i++) {
                        out.print(rs1.getMetaData().getColumnName(i));
                        if (i < ccount) {
                            out.print("\t");
                        }
                    }
                    out.println();

                    double denom = 1 / (total * 100.0);

                    // Output each row
                    while (rs1.next()) {
                        for (int i = 1; i <= ccount; i++) {
                            out.print(rs1.getString(i));
                            if (i < ccount) {
                                out.print("\t");
                            }
                        }
                        out.println();
                        int r = rs1.getRow();
                        l.progress(r * denom);
                        if (r % 10 == 0) {
                            l.message("Exporting " + r
                                    + " of " + total);
                        }
                    }

                    rs1.close();
                } finally {
                    l.message("Closed Connection");
                }

            } catch (Exception e) {
                log.error("Error writing SQL export", e);
            }
        } finally {
            lock.unlock();
        }

    }

    @Override
    public String getDescription() {
        return "SQL Report:" + name + ". Output to:" + getOutputFile().getPath();
    }

    public Connection getConnection() throws SQLException {

        DataSource dataSource;

        if (this.dataSourceQualifier != null && this.dataSourceQualifier.length() > 0) {
            dataSource = StaticContextAccessor.getBeanQualified(DataSource.class, dataSourceQualifier);
        }
        else {
            dataSource = StaticContextAccessor.getBean(DataSource.class);
        }

        if (dataSource == null) {
            log.error("data source is null!");
            return null;
        }
        return dataSource.getConnection();
    }
}
