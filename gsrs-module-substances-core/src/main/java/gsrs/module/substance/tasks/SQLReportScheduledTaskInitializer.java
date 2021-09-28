package gsrs.module.substance.tasks;

import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.springUtils.StaticContextAccessor;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import gov.nih.ncats.common.util.TimeUtil;
import gsrs.scheduledTasks.SchedulerPlugin.TaskListener;

import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.Data;
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
public class SQLReportScheduledTaskInitializer
        extends ScheduledTaskInitializer {

    private String name = "sqlReport";
    private String sql;
    private String outputPath;
    private DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
    private DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("HHmmss");
    private String dataSourceQualifier;

    private Lock lock = new ReentrantLock();

    /**
     * Returns the File used to output the report
     *
     * @return
     */
    public File getWriteFile() {
        if(outputPath ==null) {
            outputPath= "reports/" + name + "-%DATE%.txt";
        }
        String date = formatter.format(TimeUtil.getCurrentLocalDateTime());
        String time = formatterTime.format(TimeUtil.getCurrentLocalDateTime());

        String fpath = outputPath.replace("%DATE%", date)
                .replace("%TIME%", time);

        return new File(fpath);
    }

    private PrintStream makePrintStream(File writeFile) throws IOException {
        return new PrintStream(
                new BufferedOutputStream(new FileOutputStream(writeFile)),
                false, "UTF-8");
    }

    public void run(TaskListener l) {
        try {
            lock.lock();
            l.message("Initializing SQL");

            File writeFile = getWriteFile();
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
        return "SQL Report:" + name + ". Output to:" + getWriteFile().getPath();
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
