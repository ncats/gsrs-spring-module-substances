package gsrs.module.substance.exporters;

import gsrs.springUtils.StaticContextAccessor;

import ix.ginas.exporters.Exporter;
import ix.ginas.models.v1.Substance;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


/**
 * Substance Exporter that used SQL to get data from GSRS.
 * Created by Egor Puzanov
 */
@Slf4j
public class SQLExporter implements Exporter<Substance> {

    private OutputStream out;
    private String extension;
    private StringConverter stringConverter;
    private List<EntryConfig> files;
    private File tmpdir;
    private Lock lock = new ReentrantLock();

    public interface StringConverter {
        String toFormat(String format, String str);
    }

    public static abstract class AbstractStringConverter implements StringConverter {
        protected static String replaceFromLists(String str, String[] searchList, String[] replaceList) {
            if(str == null){
                return null;
            }
            StringBuilder sb = new StringBuilder(str);
            for (int i = 0; i < searchList.length; i++)
            {
                String key = searchList[i];
                if ("".equals(key)) {
                    continue;
                }
                String value = replaceList[i];
                int start = sb.indexOf(key, 0);
                while (start > -1) {
                    int end = start + key.length();
                    int nextSearchStart = start + value.length();
                    sb.replace(start, end, value);
                    start = sb.indexOf(key, nextSearchStart);
                }
            }
            return sb.toString();
        }

        @Override
        public String toFormat(String format, String str){
            return str;
        }
    }

    public static class DefaultStringConverter extends AbstractStringConverter {
        public String toFormat(String format, String str) {
            return str;
        }
    }

    public static class EntryConfig {
        private final String name;
        private final String sql;
        private final Charset encoding;
        private final CSVFormat format;

        public EntryConfig (Map<String, Object> m) {
            this.name = (String) m.get("name");
            this.sql = (String) m.get("sql");
            this.encoding = Charset.forName(String.valueOf(m.getOrDefault("encoding", "UTF-8")));
            CSVFormat format =  CSVFormat.valueOf(String.valueOf(m.getOrDefault("format", "Default")));
            String delimiter = String.valueOf(m.getOrDefault("delimiter", format.getDelimiterString()));
            this.format = format.builder()
                .setDelimiter(delimiter)
                .setHeader(String.valueOf(m.getOrDefault("header", "")).isEmpty() ? null : String.valueOf(m.get("header")).trim().split(delimiter))
                .setQuote(m.get("quoteChar") != null ? Character.valueOf(String.valueOf(m.get("quoteChar")).charAt(0)) : format.getQuoteCharacter())
                .setQuoteMode(m.get("quoteMode") != null ? QuoteMode.valueOf(String.valueOf(m.get("quoteMode"))) : format.getQuoteMode())
                .setEscape(m.get("escapeChar") != null ? Character.valueOf(String.valueOf(m.get("escapeChar")).charAt(0)) : format.getEscapeCharacter())
                .setNullString((String) m.getOrDefault("nullString", format.getNullString()))
                .setRecordSeparator((String) m.getOrDefault("recordSeparator", format.getRecordSeparator()))
                .get();
        }

        public String getName() {
            return this.name;
        }

        public String getSql() {
            return this.sql;
        }

        public Charset getEncoding() {
            return this.encoding;
        }

        public CSVFormat getFormat() {
            return this.format;
        }
    }

    private SQLExporter(Builder builder){
        this.out = builder.out;
        this.extension = builder.extension;
        this.stringConverter = builder.stringConverter;
        this.files = builder.files;
        this.tmpdir = builder.tmpdir;
    }

    @Override
    public void exportForEachAndClose(Iterator<Substance> it) throws IOException{
        this.close();
    }

    @Override
    public void export(Substance s) throws IOException {
    }

    @Override
    public void close() throws IOException {
        try (Connection c = getConnection()) {
            log.debug("Establishing SQL connection");
            lock.lock();
            for (EntryConfig entry : files) {
                //log.debug("EntryConfig: " + entry.getName() + " " + entry.getSql());
                try (PreparedStatement s = c.prepareStatement(entry.getSql())){
                    try (ResultSet rs = s.executeQuery()) {
                        makeCsvFile(entry, rs);
                    }
                }
            }
            compressIfNeeded();
        } catch (Exception e) {
            log.error("Error writing SQL export", e);
        } finally {
            lock.unlock();
            log.debug("Closed SQL Connection");
        }
        tmpdir.delete();
        this.out.close();
    }

    private Connection getConnection() throws SQLException {
        DataSource dataSource = StaticContextAccessor.getBean(DataSource.class);
        if (dataSource == null) {
            log.error("data source is null!");
            return null;
        }
        return dataSource.getConnection();
    }

    private void makeCsvFile(EntryConfig entry, ResultSet rs) throws IOException {
        ResultSetMetaData metadata = null;
        String value = "";
        String part_value = "";
        int ccount = 0;
        List<String> cast_fields = new ArrayList<String>();
        File file = new File(tmpdir, entry.getName());
        file.getParentFile().mkdirs();

        try (
            FileWriter fileWriter = new FileWriter(file);
            CSVPrinter csvPrinter = new CSVPrinter(fileWriter, entry.getFormat());
        ) {
            metadata = rs.getMetaData();
            ccount = metadata.getColumnCount();
            // Output header
            for (int i = 1; i <= ccount; i++) {
                value = metadata.getColumnName(i).toUpperCase();
                if (value.startsWith("CAST_TO_PART")) {
                    cast_fields.add("PART");
                    continue;
                } else if (value.startsWith("CAST_TO_")) {
                    cast_fields.add(value.substring(8, 12));
                    value = value.substring(13);
                } else {
                    cast_fields.add("NONE");
                }
            }

            // Output each row
            while (rs.next()) {
                part_value = "";
                // log.debug("Processing row: " + rs.getString(1));
                for (int i = 0; i < ccount; i++) {
                    value = rs.getString(i + 1);
                    if (cast_fields.get(i) == "PART") {
                        if (value == null) {
                            part_value = "";
                        } else {
                            part_value = value;
                        }
                        continue;
                    }
                    if (!"".equals(part_value)) {
                        if (value == null) {
                            value = "";
                        }
                        value = value + part_value;
                        part_value = "";
                    }
                    csvPrinter.print(stringConverter.toFormat(cast_fields.get(i), value));
                }
                csvPrinter.println();
            }
        } catch (Exception e) {
            log.error("Exception:", e);
        }
    }

    private void compressIfNeeded() throws ArchiveException, CompressorException, IOException {
        byte[] buffer = new byte[1024];
        int len;
        String archiverName = null;
        String compressorName = null;
        File exportFile = null;
        String[] tmpFiles = tmpdir.list();

        if (tmpFiles.length < 1) {
            return;
        }

        exportFile = new File(tmpdir, tmpFiles[0]);
        boolean isSingleRegularFile = tmpFiles.length == 1 && Files.isRegularFile(exportFile.toPath(), LinkOption.NOFOLLOW_LINKS);

        switch ((extension.contains(".") ? extension.substring(extension.lastIndexOf(".") + 1) : extension).toLowerCase()) {
            case "zip":
                archiverName = ArchiveStreamFactory.ZIP;
                break;
            case "jar":
                archiverName = ArchiveStreamFactory.JAR;
                break;
            case "7z":
                archiverName = ArchiveStreamFactory.SEVEN_Z;
                break;
            case "ar":
                archiverName = ArchiveStreamFactory.AR;
                break;
            case "arj":
                archiverName = ArchiveStreamFactory.ARJ;
                break;
            case "tar":
                archiverName = ArchiveStreamFactory.TAR;
                break;
            case "cpio":
                archiverName = ArchiveStreamFactory.CPIO;
                break;
            case "gz": case "gzip":
                compressorName = CompressorStreamFactory.GZIP;
                break;
            case "bz2": case "bzip2":
                compressorName = CompressorStreamFactory.BZIP2;
                break;
            case "br":
                compressorName = CompressorStreamFactory.BROTLI;
                break;
            case "xz":
                compressorName = CompressorStreamFactory.XZ;
                break;
            case "z":
                compressorName = CompressorStreamFactory.Z;
                break;
            case "cpgz":
                archiverName = ArchiveStreamFactory.CPIO;
                compressorName = CompressorStreamFactory.GZIP;
                break;
            case "tgz":
                archiverName = ArchiveStreamFactory.TAR;
                compressorName = CompressorStreamFactory.GZIP;
                break;
            case "cpbz2":
                archiverName = ArchiveStreamFactory.CPIO;
                compressorName = CompressorStreamFactory.BZIP2;
                break;
            case "tbz2":
                archiverName = ArchiveStreamFactory.TAR;
                compressorName = CompressorStreamFactory.BZIP2;
                break;
            case "xlsx":
                archiverName = "xlsx";
                break;
            default:
                if (!isSingleRegularFile) {
                    archiverName = ArchiveStreamFactory.ZIP;
                }
        }

        if (compressorName != null && archiverName == null && !isSingleRegularFile) {
            if (extension.toLowerCase().contains(".cpio.")) {
                archiverName = ArchiveStreamFactory.CPIO;
            } else {
                archiverName = ArchiveStreamFactory.TAR;
            }
        }

        if ("xlsx".equals(archiverName)) {
            Workbook workbook = new XSSFWorkbook();
            for (EntryConfig entry : files) {
                File file = new File(tmpdir, entry.getName());
                if (!file.isFile()) {
                    continue;
                }
                Sheet sheet = workbook.createSheet(entry.getName());
                CSVParser parser = CSVParser.parse(
                    new FileReader(file),
                    entry.getFormat());
                for (CSVRecord csvRecord : parser) {
                    Row row = sheet.createRow((int) csvRecord.getRecordNumber() - 1);
                    for (int i = 0; i < csvRecord.size(); i++) {
                        Cell cell = row.createCell(i);
                        cell.setCellValue(csvRecord.get(i));
                    }
                }
                file.delete();
            }
            try {
                workbook.write(this.out);
            } catch (Exception e) {
                log.error("Exception:", e);
            } finally {
                workbook.close();
            }

            exportFile = null;
            archiverName = null;
        }

        if (archiverName != null) {
            exportFile = new File(tmpdir, "export.arcTmp");
            try (
                OutputStream fos = new FileOutputStream(exportFile);
                ArchiveOutputStream<ArchiveEntry> aos = new ArchiveStreamFactory().createArchiveOutputStream(archiverName, fos);
            ) {
                for (EntryConfig entry : files) {
                    File file = new File(tmpdir, entry.getName());
                    if (!file.isFile()) {
                        continue;
                    }
                    aos.putArchiveEntry(
                        aos.createArchiveEntry(
                            file,
                            entry.getName()));
                    try (InputStream is = new FileInputStream(file)) {
                        while (( len = is.read(buffer)) > 0) {
                            aos.write(buffer, 0, len);
                        }
                    }
                    aos.closeArchiveEntry();
                    file.delete();
                }
            }
        }

        if (compressorName != null) {
            try (
                InputStream is = new FileInputStream(exportFile);
                CompressorOutputStream cos = new CompressorStreamFactory().createCompressorOutputStream(compressorName, this.out);
            ) {
                while (( len = is.read(buffer)) > 0) {
                    cos.write(buffer, 0, len);
                }
            }
            if (exportFile != null) {
                exportFile.delete();
                exportFile = null;
            }
        }

        if (exportFile != null) {
            try (InputStream is = new FileInputStream(exportFile)) {
                while (( len = is.read(buffer)) > 0) {
                    this.out.write(buffer, 0, len);
                }
            }
            exportFile.delete();
        }
    }



    /**
     * Builder class that makes a SQLExporter.
     *
     */
    public static class Builder{
        private final OutputStream out;
        private final String extension;
        private final StringConverter stringConverter;
        private final File tmpdir;
        private final List<EntryConfig> files = new ArrayList<>();

        public Builder(OutputStream out, String extension, StringConverter stringConverter){
            Objects.requireNonNull(out);
            Objects.requireNonNull(extension);
            this.out = out;
            this.extension = extension;
            this.stringConverter = stringConverter != null ? stringConverter : new DefaultStringConverter();
            File tmpdir;
            try {
                tmpdir = Files.createTempDirectory("export-").toFile();
            } catch (Exception ex) {
                tmpdir = null;
            }
            this.tmpdir = tmpdir;
        }

        public Builder(OutputStream out, String extension, String stringConverterClassName){
            Objects.requireNonNull(out);
            Objects.requireNonNull(extension);
            this.out = out;
            this.extension = extension;
            StringConverter stringConverter;
            try {
                stringConverter = (StringConverter) Class.forName(stringConverterClassName).getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                stringConverter = new DefaultStringConverter();
            }
            this.stringConverter = stringConverter;
            File tmpdir;
            try {
                tmpdir = Files.createTempDirectory("export-").toFile();
            } catch (Exception ex) {
                tmpdir = null;
            }
            this.tmpdir = tmpdir;
        }

        public Builder addFile(Map<String, Object> m){
            Objects.requireNonNull(m);
            addFile(new EntryConfig(m));
            return this;
        }

        public Builder addFile(EntryConfig file){
            Objects.requireNonNull(file);
            files.add(file);
            return this;
        }

        public SQLExporter build(){
            return new SQLExporter(this);
        }
    }
}