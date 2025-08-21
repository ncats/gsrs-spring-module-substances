package gsrs.module.substance.tasks;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import gov.nih.ncats.common.util.TimeUtil;
import gov.nih.ncats.common.util.Unchecked;

import gsrs.autoconfigure.GsrsExportConfiguration;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.service.ExportService;

import ix.core.models.Principal;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.ginas.exporters.*;
import ix.ginas.models.v1.Substance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.provider.AbstractFileSystem;
import org.apache.commons.vfs2.provider.AbstractOriginatingFileProvider;
import org.apache.commons.vfs2.provider.GenericFileName;
import org.apache.commons.vfs2.provider.url.UrlFileNameParser;
import org.apache.commons.vfs2.util.DelegatingFileSystemOptionsBuilder;
import org.apache.commons.vfs2.util.FileObjectUtils;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Safelist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Used to schedule and upload Reports
 *
 * @author Egor Puzanov
 *
 */

@Slf4j
public class ScheduledExportTask extends ScheduledTaskInitializer {

    private String description;
    private List<DestinationConfig> destinations = new ArrayList<DestinationConfig>();
    private String extension;
    private String filenameTemplate = "auto-export-$DATE$";
    private Map<String, String> parameters = new HashMap<>();
    private boolean preserveExports = false;
    private boolean publicOnly = false;
    private String query = null;
    private String username;
    @JsonIgnore
    private final Pattern PERIOD_PAT = Pattern.compile(":\\[(P[0-9YMWD]*)");

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    private ExportService exportService;

    @Autowired
    private GsrsExportConfiguration gsrsExportConfiguration;

    @Autowired
    private SubstanceEntityService substanceEntityService;

    @Autowired
    private SubstanceLegacySearchService searchService;

    @Autowired
    protected PlatformTransactionManager transactionManager;


    @JsonProperty(value="description")
    public void setDescription(String description) {
        this.description = description;
    }

    public String getExtension() {
        return extension;
    }

    @JsonProperty(value="extension")
    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getFilenameTemplate() {
        return filenameTemplate;
    }

    @JsonProperty(value="filenameTemplate")
    public void setFilenameTemplate(String filenameTemplate) {
        this.filenameTemplate = filenameTemplate;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    @JsonProperty(value="parameters")
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public boolean isPreserveExports() {
        return preserveExports;
    }

    @JsonProperty(value="preserveExports")
    public void setPreserveExports(boolean preserveExports) {
        this.preserveExports = preserveExports;
    }

    public boolean isPublicOnly() {
        return publicOnly;
    }

    @JsonProperty(value="publicOnly")
    public void setPublicOnly(boolean publicOnly) {
        this.publicOnly = publicOnly;
    }

    public String getQuery() {
        return query;
    }

    @JsonProperty(value="query")
    public void setQuery(String query) {
        this.query = query;
    }

    public String getUsername() {
        return username;
    }

    @JsonProperty(value="username")
    public void setUsername(String username) {
        this.username = username;
    }

    @JsonProperty(value="destinations")
    public void setDestinations(Map<String, Map<String, String>> m) throws FileSystemException, NoSuchMethodException, URISyntaxException {
        for (Map<String, String> value : m.values()) {
            destinations.add(new DestinationConfig(value));
        }
    }

    protected static class SmtpFileSystemConfigBuilder extends FileSystemConfigBuilder {

        private static final SmtpFileSystemConfigBuilder INSTANCE = new SmtpFileSystemConfigBuilder();

        private static final String TO = "TO";
        private static final String FROM = "FROM";
        private static final String SUBJECT = "SUBJECT";
        private static final String BODY = "BODY";
        private static final String FOOTER = "FOOTER";
        private static final String RECORDTEMPLATE = "RECORDTEMPLATE";
        private static final String CHARSET = "CHARSET";
        private static final String STARTTLS = "STARTTLS";
        private static final String MAXSIZE = "MAXSIZE";

        public static SmtpFileSystemConfigBuilder getInstance() {
            return INSTANCE;
        }

        private SmtpFileSystemConfigBuilder() {
            super("smtp.");
        }

        public void setTo(final FileSystemOptions options, final String to) {
            this.setParam(options, TO, to);
        }
        public void setFrom(final FileSystemOptions options, final String from) {
            this.setParam(options, FROM, from);
        }
        public void setSubject(final FileSystemOptions options, final String subject) {
            this.setParam(options, SUBJECT, subject);
        }
        public void setBody(final FileSystemOptions options, final String body) {
            this.setParam(options, BODY, body);
        }
        public void setFooter(final FileSystemOptions options, final String footer) {
            this.setParam(options, FOOTER, footer);
        }
        public void setRecordTemplate(final FileSystemOptions options, final String template) {
            this.setParam(options, RECORDTEMPLATE, template);
        }
        public void setStartTls(final FileSystemOptions options, final String starttls) {
            this.setParam(options, STARTTLS, starttls);
        }
        public void setCharset(final FileSystemOptions options, final String charset) {
            this.setParam(options, CHARSET, charset);
        }
        public void setMaxSize(final FileSystemOptions options, final String maxSize) {
            this.setParam(options, MAXSIZE, maxSize);
        }

        public String getTo(final FileSystemOptions options) {
            return this.getString(options, TO);
        }
        public String getFrom(final FileSystemOptions options) {
            return this.getString(options, FROM);
        }
        public String getSubject(final FileSystemOptions options) {
            return this.getString(options, SUBJECT);
        }
        public String getBody(final FileSystemOptions options) {
            return this.getString(options, BODY);
        }
        public String getFooter(final FileSystemOptions options) {
            return this.getString(options, FOOTER, "");
        }
        public String getRecordTemplate(final FileSystemOptions options) {
            return this.getString(options, RECORDTEMPLATE);
        }
        public String getCharset(final FileSystemOptions options) {
            return this.getString(options, CHARSET, "utf-8");
        }
        public String getStartTls(final FileSystemOptions options) {
            return this.getString(options, STARTTLS, "true");
        }
        public int getMaxSize(final FileSystemOptions options) {
            String ms = this.getString(options, MAXSIZE, String.valueOf(Integer.MAX_VALUE));
            try {
                switch (ms.substring(ms.length() - 2).toUpperCase()) {
                    case "KB": return Float.valueOf(Float.valueOf(ms.substring(0, ms.length() - 2)) * 1024).intValue();
                    case "MB": return Float.valueOf(Float.valueOf(ms.substring(0, ms.length() - 2)) * 1024 * 1024).intValue();
                    case "GB": return Float.valueOf(Float.valueOf(ms.substring(0, ms.length() - 2)) * 1024 * 1024 * 1024).intValue();
                    default: return Float.valueOf(ms).intValue();
                }
            } catch (final Exception e) {}
            return Integer.MAX_VALUE;
        }

        protected SmtpFileSystemConfigBuilder(final String prefix) {
            super(prefix);
        }

        @Override
        protected Class<? extends FileSystem> getConfigClass() {
            return SmtpFileSystem.class;
        }
    }


    protected static class SmtpFileProvider extends AbstractOriginatingFileProvider {

        private static final SmtpFileProvider INSTANCE = new SmtpFileProvider();

        public SmtpFileProvider() {
            setFileNameParser(new UrlFileNameParser());
        }

        public static SmtpFileProvider getInstance() {
            return INSTANCE;
        }

        static final UserAuthenticationData.Type[] AUTHENTICATOR_TYPES =
            {
                UserAuthenticationData.USERNAME,
                UserAuthenticationData.PASSWORD
            };

        static final Collection<Capability> CAPABILITIES =
            Collections.unmodifiableCollection(
                Arrays.asList(
                    Capability.CREATE,
                    Capability.URI,
                    Capability.WRITE_CONTENT
                    )
            );

        private Session createSession(final GenericFileName rootName,
                final FileSystemOptions fileSystemOptions) throws FileSystemException {
            UserAuthenticationData authData = null;
            SmtpFileSystemConfigBuilder builder = (SmtpFileSystemConfigBuilder) getConfigBuilder();
            try {
                Properties props = new Properties();
                props.put("mail.smtp.starttls.enable", builder.getStartTls(fileSystemOptions));
                props.put("mail.smtp.host", rootName.getHostName());
                props.put("mail.smtp.port", rootName.getPort());
                authData = UserAuthenticatorUtils.authenticate(fileSystemOptions, AUTHENTICATOR_TYPES);
                final String username = authData == null
                    ? rootName.getUserName()
                    : String.valueOf(UserAuthenticatorUtils.getData(
                        authData,
                        UserAuthenticationData.USERNAME,
                        UserAuthenticatorUtils.toChar(rootName.getUserName())));
                final String password = authData == null
                    ? rootName.getPassword()
                    : String.valueOf(UserAuthenticatorUtils.getData(
                        authData,
                        UserAuthenticationData.PASSWORD,
                        UserAuthenticatorUtils.toChar(rootName.getPassword())));
                if (username == null) {
                    return Session.getInstance(props);
                } else {
                    props.put("mail.smtp.auth", "true");
                    return Session.getInstance(props,
                        new Authenticator() {
                            @Override
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(username, password);
                            }
                        });
                }
            } catch (final Exception e) {
                throw new FileSystemException("vfs.provider/copy-file.error", rootName, e);
            } finally {
                UserAuthenticatorUtils.cleanup(authData);
            }
        }

        @Override
        protected FileSystem doCreateFileSystem(final FileName name, final FileSystemOptions fileSystemOptions)
                throws FileSystemException {
            SmtpFileSystemConfigBuilder builder = (SmtpFileSystemConfigBuilder) getConfigBuilder();
            try {
                Session session = createSession((GenericFileName) name, fileSystemOptions);
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(builder.getFrom(fileSystemOptions)));
                message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(builder.getTo(fileSystemOptions)));
                String charset = builder.getCharset(fileSystemOptions);
                message.setSubject(builder.getSubject(fileSystemOptions), charset);
                return new SmtpFileSystem(name,
                    null,
                    fileSystemOptions,
                    message,
                    builder.getBody(fileSystemOptions),
                    builder.getFooter(fileSystemOptions),
                    builder.getRecordTemplate(fileSystemOptions),
                    charset,
                    builder.getMaxSize(fileSystemOptions));
            } catch (final Exception e) {
                e.printStackTrace();
                throw new FileSystemException("vfs.provider/copy-file.error", name, e);
            }
        }

        @Override
        public Collection<Capability> getCapabilities() {
            return CAPABILITIES;
        }

        @Override
        public FileSystemConfigBuilder getConfigBuilder() {
            return SmtpFileSystemConfigBuilder.getInstance();
        }
    }

    protected static class SmtpFileSystem extends AbstractFileSystem {

        private Message message;
        private int maxSize;
        private String body;
        private String footer;
        private String recordTemplate;
        private String charset;

        public SmtpFileSystem (final FileName name,
                final FileObject object,
                final FileSystemOptions fileSystemOptions,
                final Message message,
                final String body,
                final String footer,
                final String recordTemplate,
                final String charset,
                final int maxSize) {
            super(name, object, fileSystemOptions);
            this.message = message;
            this.body = body;
            this.footer = footer;
            this.recordTemplate = recordTemplate;
            this.charset = charset;
            this.maxSize = maxSize;
        }

        @Override
        protected void addCapabilities(final Collection<Capability> caps) {
            caps.addAll(SmtpFileProvider.CAPABILITIES);
        }

        @Override
        protected FileObject createFile(final AbstractFileName name) throws FileSystemException {
            return new SmtpFileObject(name, this);
        }

        @Override
        public Object getAttribute(final String attrName) throws FileSystemException {
            switch (attrName) {
                case "content": try {return message.getContent();} catch (final Exception e) {}; return null;
                case "body": return body;
                case "footer": return footer;
                case "recordTemplate": return recordTemplate;
                case "charset": return charset;
                default: return null;
            }
        }

        @Override
        public void setAttribute(final String attrName, final Object value) throws FileSystemException {
            int messageSize = Integer.MAX_VALUE;
            if ("content".equals(attrName)) {
                String contentType = "text/plain";
                if (Multipart.class.isInstance(value)) {
                    contentType = ((Multipart) value).getContentType();
                }
                try {
                    message.setContent(value, contentType);
                    message.saveChanges();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    message.writeTo(out);
                    messageSize = new String(out.toByteArray(), StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8).length;
                } catch (final Exception e) {
                    e.printStackTrace();
                    throw new FileSystemException("vfs.provider/set-attribute.error", e, attrName, this);
                }
                if (messageSize > maxSize) {
                    throw new FileSystemException("vfs.provider/set-attribute.error",
                        new Exception("No space left. The maximum message size limit is exceeded."),
                        attrName, this);
                }
            }
        }

        @Override
        protected void doCloseCommunicationLink() {
            if (message != null) {
                try {
                    message.getSession().getTransport("smtp").connect();
                    Transport.send(message);
                    //ByteArrayOutputStream out = new ByteArrayOutputStream();
                    //message.writeTo(out);
                    //System.out.println(new String(out.toByteArray(), StandardCharsets.UTF_8));
                } catch (final Exception e) {
                    e.printStackTrace();
                } finally {
                    message = null;
                }
            }
        }
    }

    protected static class SmtpFileObject extends AbstractFileObject<SmtpFileSystem> {

        protected SmtpFileObject(final AbstractFileName fileName,
            final SmtpFileSystem fileSystem) {
            super(fileName, fileSystem);
        }

        @Override
        protected String[] doListChildren() throws Exception {
            throw new FileSystemException("Not implemented.");
        }

        @Override
        protected FileType doGetType() throws Exception {
            if (this.getName().getBaseName().contains(".")) {
                return FileType.FILE;
            } else {
                return FileType.FOLDER;
            }
        }

        @Override
        protected long doGetContentSize() throws Exception {
            throw new FileSystemException("Not implemented.");
        }

        @Override
        public FileObject getParent() throws FileSystemException {
            return this;
        }

        private static String cleanToText(String content, String charset) {
            Document.OutputSettings settings = new Document.OutputSettings();
            settings.prettyPrint(false);
            settings.charset(charset);
            settings.escapeMode(Entities.EscapeMode.base);
            String safeText = Jsoup.clean(content, "", Safelist.none(), settings);
            return(safeText);
        }

        @Override
        public void copyFrom(final FileObject file, final FileSelector selector) throws FileSystemException {
            if (!FileObjectUtils.exists(file)) {
                throw new FileSystemException("vfs.provider/copy-missing-file.error", file);
            }
            final ArrayList<FileObject> files = new ArrayList<>();
            file.findFiles(selector, false, files);
            SmtpFileSystem fileSystem = (SmtpFileSystem) getFileSystem();
            String body = (String) fileSystem.getAttribute("body");
            String recordTemplate = body == null ? null : (String) fileSystem.getAttribute("recordTemplate");
            String footer = (String) fileSystem.getAttribute("footer");
            String charset = (String) fileSystem.getAttribute("charset");
            List<Integer> subst_columns = new ArrayList<Integer>();
            if (recordTemplate != null) {
                Matcher m = Pattern.compile("\\{([0-9]+)\\}").matcher(recordTemplate);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    subst_columns.add(Integer.parseInt(m.group(1)));
                    m.appendReplacement(sb, "%s");
                }
                m.appendTail(sb);
                if (!subst_columns.isEmpty()) {
                    recordTemplate = sb.toString();
                }
            }
            try {
                CSVFormat format = CSVFormat.Builder
                    .create(CSVFormat.RFC4180)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .get();
                MimeMultipart multipart = new MimeMultipart();
                MimeBodyPart messageBodyPart;
                String mimetype;
                for (final FileObject srcFile : files) {
                    try {
                        if (srcFile.getType().hasContent()) {
                            messageBodyPart = new MimeBodyPart();
                            mimetype = Files.probeContentType(srcFile.getPath());
                            messageBodyPart.attachFile(srcFile.getName().getPath(), mimetype + "; charset=" + charset, "base64");
                            messageBodyPart.setFileName(this.getName().getBaseName());
                            multipart.addBodyPart(messageBodyPart);
                            if (recordTemplate != null) {
                                if ("text/csv".equals(mimetype)) {
                                    CSVParser parser = CSVParser.parse(
                                        new InputStreamReader(srcFile.getContent().getInputStream()),
                                        format);
                                    for (CSVRecord csvRecord : parser) {
                                        if (subst_columns.isEmpty()) {
                                            subst_columns = IntStream.range(0, csvRecord.size()).boxed().collect(Collectors.toList());
                                        }
                                        List<Object> values = new ArrayList<Object>();
                                        for (Integer idx : subst_columns) {
                                            values.add(csvRecord.get(idx.intValue()));
                                        }
                                        body = body + "\n" + String.format(recordTemplate, values.stream().toArray(Object[]::new));
                                    }
                                    multipart.removeBodyPart(messageBodyPart);
                                }
                            }
                        }
                    } catch (final IOException e) {
                        throw new FileSystemException("vfs.provider/copy-file.error", e, srcFile, this);
                    }
                }
                body = body + "\n" + footer;
                messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(cleanToText(body, charset), charset);
                messageBodyPart.setHeader("Content-Transfer-Encoding", "base64");
                if (!body.equals(String.valueOf(messageBodyPart.getContent()))) {
                    MimeMultipart alt = new MimeMultipart("alternative", messageBodyPart);
                    messageBodyPart = new MimeBodyPart();
                    messageBodyPart.setText(body, charset, "html");
                    messageBodyPart.setHeader("Content-Transfer-Encoding", "base64");
                    alt.addBodyPart(messageBodyPart);
                    messageBodyPart = new MimeBodyPart();
                    messageBodyPart.setContent(alt);
                }
                multipart.addBodyPart(messageBodyPart, 0);
                fileSystem.setAttribute("content", multipart);
            } catch (final Exception e) {
                throw new FileSystemException("vfs.provider/copy-file.error", e, file, this);
            }
        }
    }

    private class DestinationConfig {
        private final URI uri;
        private final FileSystemOptions options;

        public DestinationConfig(Map<String, String> dst) throws FileSystemException, URISyntaxException {
            FileSystemOptions opts = new FileSystemOptions();
            this.uri = new URI(dst.remove("uri"));
            String scheme = this.uri.getScheme().toLowerCase();
            String domain =  dst.remove("domain");
            String user =  dst.remove("user");
            String password = dst.remove("password");
            if (user != null && user.length() > 0 && password != null && password.length() > 0) {
                UserAuthenticator auth = new StaticUserAuthenticator(domain, user, password);
                DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
            }
            if (dst.size() > 0) {
                StandardFileSystemManager manager = new StandardFileSystemManager();
                manager.addProvider("smtp", SmtpFileProvider.getInstance());
                manager.init();
                DelegatingFileSystemOptionsBuilder delegate = new DelegatingFileSystemOptionsBuilder(manager);
                for (Map.Entry<String, String> entry : dst.entrySet()) {
                    try {
                        delegate.setConfigString(opts, scheme, entry.getKey(), entry.getValue());
                    } catch (Exception e) {
                        log.error("The delegating configuration builder cant set value \"" + entry.getValue() + "\" for the key \"" + entry.getKey() + "\" of the scheme \"" + scheme + "\".");
                    }
                }
                manager.close();
            }
            log.debug(opts.toString());
            this.options = opts;
        }

        public URI getUri() {
            return this.uri;
        }

        public FileSystemOptions getOptions() {
            return this.options;
        }

        public FileObject getFileObject(FileSystemManager manager) throws FileSystemException {
            return manager.resolveFile(uri.toString(), options);
        }
    }

    @Override
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l) {
        log.debug("About to call runAsAdmin with transaction");
        TransactionTemplate transactionRunReport = new TransactionTemplate(transactionManager);
        transactionRunReport.setReadOnly(true);
        transactionRunReport.executeWithoutResult((s)->{
            Optional<ExportDir.ExportFile<ExportMetaData>> ofile = handleExport(l);
            if (ofile.isPresent()) {
                ExportDir.ExportFile<ExportMetaData> file = ofile.get();
                uploadFile(file, l);
                if (!preserveExports) {
                    file.delete();
                }
            }
            l.complete();
            log.debug("completed handleRun");
        });
    }

    @Override
    public String getDescription() {
        return description;
    }

    private Function<String, String> fileNameGenerator() {
        return date -> filenameTemplate.replace("$DATE$", date);
    }

    private Optional<ExportDir.ExportFile<ExportMetaData>> handleExport(SchedulerPlugin.TaskListener l) {
        log.debug("Running export");
        try {
            Principal user = new Principal(username, null);
            ExportMetaData emd = new ExportMetaData("export-all-gsrs", null, user.username, publicOnly, extension)
                .onTotalChanged((c) -> {
                    l.message("Exported " + c + " records");
                });
            Stream<Substance> substanceStream = getStreamSupplier();
            Stream<Substance> effectivelyFinalStream = filterStream(substanceStream, publicOnly, parameters);
            @SuppressWarnings("unchecked")
            ExportProcess<Substance> p = exportService.createExport(emd,() -> effectivelyFinalStream);
            log.trace("p: " + (p==null ? "null" : "not null"));
            log.trace("publicOnly: " + publicOnly);
            l.message("Run export for " + extension + " extension.");
            if (p != null) {
                p.run(r->r.run(), out -> Unchecked.uncheck(() -> getExporterFor(extension, out, publicOnly, parameters)));
            }
            return exportService.getFile(user.username, emd.getFilename());
        } catch (Exception e) {
            log.error("Error in ScheduledExportTask: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private void uploadFile(ExportDir.ExportFile<ExportMetaData> file, SchedulerPlugin.TaskListener l) {
        l.message("Uploading file " + file.getFile().getName());
        StandardFileSystemManager manager = new StandardFileSystemManager();
        LocalDate ld = TimeUtil.getCurrentLocalDate();
        String date = ld.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String fname = fileNameGenerator().apply(date) + "." + extension;

        try {
            manager.addProvider("smtp", SmtpFileProvider.getInstance());
            manager.init();
            String filename = file.getFile().getAbsolutePath();
            FileObject lfo = manager.resolveFile(filename);
            if (lfo.exists()) {
                for (DestinationConfig dst : destinations) {
                    l.message("Uploading file to " + dst.getUri().toString());
                    log.debug("Destination URI: " + dst.getUri().toString());
                    log.trace("Options: " + dst.getOptions().toString());
                    FileObject rfo = dst.getFileObject(manager);
                    if (!rfo.getParent().exists()) {
                        rfo.getParent().createFolder();
                    }
                    if (rfo.isFolder()) {
                        rfo = rfo.resolveFile(fname);
                    }
                    rfo.copyFrom(lfo, Selectors.SELECT_SELF);
                }
            }
        } catch (Exception e) {
            log.error("Exception", e);
            e.printStackTrace();
        } finally {
            manager.close();
        }
    }

    private Exporter<Substance> getExporterFor(String extension, OutputStream pos, boolean publicOnly, Map<String, String> parameters)
            throws IOException {

        log.trace("getExporterFor, extension: " + extension + "; pos: " + pos + "parameters: " + parameters);
        ExporterFactory.Parameters params = createParameters(extension, publicOnly, parameters);
        log.trace("create params");

        log.trace("gsrsExportConfiguration: " + (gsrsExportConfiguration==null ? "null" : "not null"));
        @SuppressWarnings("unchecked")
        ExporterFactory<Substance> factory = gsrsExportConfiguration.getExporterFor(substanceEntityService.getContext(), params);

        log.trace("factory: " + factory);
        if (factory == null) {
            throw new IllegalArgumentException("could not find suitable factory for " + params);
        }
        return factory.createNewExporter(pos, params);
    }

    protected ExporterFactory.Parameters createParameters(String extension, boolean publicOnly, Map<String, String> parameters) {
        for (OutputFormat f : gsrsExportConfiguration.getAllSupportedFormats(substanceEntityService.getContext())) {
            if (extension.equals(f.getExtension())) {
                return new DefaultParameters(f, publicOnly);
            }
        }
        throw new IllegalArgumentException("could not find supported exporter for extension '" + extension + "'");

    }

    @SuppressWarnings("unchecked")
    private Stream<Substance> getStreamSupplier() throws UnsupportedEncodingException {
        if (query != null && !query.isEmpty()) {
            Matcher m = PERIOD_PAT.matcher(query);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                m.appendReplacement(sb, ":[" + String.valueOf(Period.parse(m.group(1)).subtractFrom(ZonedDateTime.now()).getLong(ChronoField.INSTANT_SECONDS)) + "000");
            }
            m.appendTail(sb);
            SearchRequest sr = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(0)
                .query(sb.toString())
                .top(Integer.MAX_VALUE)
                .build();
            return new TransactionTemplate(transactionManager).execute(ts -> {
                try {
                    SearchResult result = searchService.search(sr.getQuery(), sr.getOptions());
                    result.waitForFinish();
                    return result.getMatches().stream();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });
        } else {
            return substanceRepository.streamAll();
        }
    }

    protected Stream<Substance> filterStream(Stream<Substance> stream, boolean publicOnly, Map<String, String> parameters) {
        if (publicOnly) {
            return stream.filter(s -> s.getAccess().isEmpty());
        }
        return stream;
    }
}
