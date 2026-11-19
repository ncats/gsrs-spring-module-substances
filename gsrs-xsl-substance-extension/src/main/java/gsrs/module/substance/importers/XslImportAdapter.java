package gsrs.module.substance.importers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gsrs.imports.ImportAdapter;
import gsrs.json.JsonEntityUtil;

import ix.ginas.models.v1.Substance;
import ix.ginas.utils.JsonSubstanceFactory;

import lombok.extern.slf4j.Slf4j;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Source;

/**
 * Import adapter that uses an XSL 3.0 template to transform custom XML or JSON
 * input into GSRS-formatted JSON, then converts the resulting JSON records into
 * {@link Substance} entities for import.
 *
 * <p>The adapter reads the incoming {@link InputStream} as text, passes it to
 * a configurable XSL stylesheet, and expects the stylesheet to emit one or more
 * GSRS JSON substance records. Each record is then parsed through
 * {@link JsonSubstanceFactory#makeSubstance(JsonNode)} and returned as a
 * {@link Stream} of {@link Substance} objects.</p>
 *
 * <p>The XSL template receives the raw input text via the {@code raw-input}
 * parameter. For JSON-to-JSON transformations the stylesheet can parse the value
 * with {@code json-to-xml($raw-input)}; for XML-to-JSON transformations the
 * value can be parsed with {@code parse-xml($raw-input)}. The existing
 * GSRS-to-FHIR stylesheets in this project demonstrate the JSON parsing
 * pattern.</p>
 *
 * @author Egor Puzanov
 */
@Slf4j
public class XslImportAdapter implements ImportAdapter<Substance> {

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String templateFile;
    private String inputFormat = "JSON";
    private String header = "";
    private String footer = "";
    private String delimiter = "";

    public void setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
    }

    public String getTemplateFile() {
        return templateFile;
    }

    public void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }

    public String getInputFormat() {
        return inputFormat;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getHeader() {
        return header;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public String getFooter() {
        return footer;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getDelimiter() {
        return delimiter;
    }

    @Override
    public Stream<Substance> parse(InputStream is, ObjectNode settings, JsonNode schema) {
        Stream.Builder<Substance> substanceStream = Stream.builder();
        String rawInput = readInputStreamAsString(is);
        if (rawInput == null || rawInput.isEmpty()) {
            log.warn("Input stream is empty; no substances to import.");
            return substanceStream.build();
        }

        String cleanedInput = rawInput;
        if (header != null && !header.isEmpty() && cleanedInput.startsWith(header)) {
            cleanedInput = cleanedInput.substring(header.length());
        }
        if (footer != null && !footer.isEmpty() && cleanedInput.endsWith(footer)) {
            cleanedInput = cleanedInput.substring(0, cleanedInput.length() - footer.length());
        }

        String[] records;
        if (delimiter != null && !delimiter.isEmpty()) {
            records = cleanedInput.split(Pattern.quote(delimiter), -1);
        } else {
            records = new String[]{cleanedInput};
        }

        for (String record : records) {
            if (record == null) {
                continue;
            }

            String cleanedRecord = record.trim();
            if (cleanedRecord.isEmpty()) {
                continue;
            }

            try {
                String transformedJson = transform(cleanedRecord);
                if (transformedJson == null || transformedJson.isEmpty()) {
                    log.warn("XSL transformation produced no output for a record; skipping.");
                    continue;
                }

                log.trace("Transformed record to JSON:\n{}", transformedJson);

                JsonNode currentRecord = OBJECT_MAPPER.readTree(transformedJson);
                TransactionTemplate txManageConversion = new TransactionTemplate(platformTransactionManager);
                txManageConversion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                Substance converted = txManageConversion.execute(t -> convertJsonNode(currentRecord));

                if (converted != null) {
                    log.trace("converted JSON to substance with ID {}. It has {} names",
                            converted.getUuid(), converted.names.size());
                    substanceStream.add(converted);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse input record using XSL template: " + templateFile, e);
            }
        }

        return substanceStream.build();
    }

    private Substance convertJsonNode(JsonNode node) {
        Substance substance = JsonSubstanceFactory.makeSubstance(node);
        return JsonEntityUtil.fixOwners(substance, true);
    }

    private String readInputStreamAsString(InputStream is) {
        StringBuilder builder = new StringBuilder();
        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to read import input stream", e);
        }
        return builder.toString();
    }

    private String transform(String rawInput) throws SaxonApiException, IOException {
        Processor processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();

        InputStream templateStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(templateFile);
        if (templateStream == null) {
            File file = new File(templateFile);
            if (file.exists()) {
                templateStream = new FileInputStream(file);
            } else {
                throw new IOException("XSL template not found on classpath or filesystem: " + templateFile);
            }
        }

        XsltExecutable executable;
        try {
            executable = compiler.compile(new StreamSource(templateStream));
        } finally {
            templateStream.close();
        }

        XsltTransformer transformer = executable.load();

        DocumentBuilder documentBuilder = processor.newDocumentBuilder();
        XdmNode initialNode = documentBuilder.build(
            new StreamSource(
                new StringReader(
                    "XML".equalsIgnoreCase(inputFormat) ? rawInput : "<root/>")));
        transformer.setInitialContextNode(initialNode);

        transformer.setParameter(new QName("raw-input"), XdmAtomicValue.makeAtomicValue(rawInput));
        transformer.setParameter(new QName("input-format"), XdmAtomicValue.makeAtomicValue(inputFormat));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Serializer serializer = processor.newSerializer(outputStream);
        serializer.setOutputProperty(Serializer.Property.METHOD, "text");
        transformer.setDestination(serializer);
        transformer.transform();

        return outputStream.toString(StandardCharsets.UTF_8.name());
    }
}
