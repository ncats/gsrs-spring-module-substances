package gsrs.module.substance.importers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gsrs.imports.ImportAdapter;
import gsrs.json.JsonEntityUtil;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.utils.XslParameterUtils;

import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.transform.stream.StreamSource;

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
 * <p>The XSL template receives the raw input text via the {@code input}
 * parameter. For JSON-to-JSON transformations the stylesheet can parse the value
 * with {@code json-to-xml($input)}; for XML-to-JSON transformations the
 * value can be parsed with {@code parse-xml($input)}. The existing
 * GSRS-to-FHIR stylesheets in this project demonstrate the JSON parsing
 * pattern.</p>
 *
 * @author Egor Puzanov
 */
@Slf4j
public class XslImportAdapter implements ImportAdapter<Substance> {

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private SubstanceRepository substanceRepository;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Matches a colon bounded by non-whitespace on both sides: the separator of
     * a {@code <codeSystem>:<code>} linking identifier.
     */
    private static final Pattern LINKING_ID_SEPARATOR = Pattern.compile("\\S:\\S");

    private String templateFile;
    private String inputFormat = "JSON";
    private String header = "";
    private String footer = "";
    private String delimiter = "";
    private Map<String, String> xslParameters = Collections.emptyMap();
    private Map<String, String> xslNamespaces = Collections.emptyMap();
    private Map<String, String> codeSystemMappings = Collections.emptyMap();

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

    /**
     * Sets the additional parameters passed to the XSL template on every record.
     *
     * <p>The parameters are passed to the template in addition to the standard
     * {@code input} and {@code input-format} parameters. Values supplied in the
     * parse settings under the {@code xslParameters} key override the ones
     * configured here.</p>
     *
     * @param xslParameters map of template parameter names to their values
     */
    public void setXslParameters(Map<String, String> xslParameters) {
        this.xslParameters = xslParameters == null ? Collections.emptyMap() : xslParameters;
    }

    public Map<String, String> getXslParameters() {
        return xslParameters;
    }

    /**
     * Sets the namespace prefix mappings used to resolve prefixed parameter names.
     *
     * @param xslNamespaces mapping of namespace prefixes to namespace URIs
     */
    public void setXslNamespaces(Map<String, String> xslNamespaces) {
        this.xslNamespaces = xslNamespaces == null ? Collections.emptyMap() : xslNamespaces;
    }

    public Map<String, String> getXslNamespaces() {
        return xslNamespaces;
    }

    /**
     * Sets the code system name remapping applied to the imported records.
     *
     * <p>Each entry maps a code system name found in the input to the name it
     * must have after import. For example {@code {"UNII": "FDA UNII"}} rewrites
     * every code whose code system is {@code UNII} to {@code FDA UNII}, both on
     * the substance codes and on the code system read from a
     * {@code <codeSystem>:<code>} linking identifier.</p>
     *
     * @param codeSystemMappings mapping of input code system names to their replacements
     */
    public void setCodeSystemMappings(Map<String, String> codeSystemMappings) {
        this.codeSystemMappings = codeSystemMappings == null ? Collections.emptyMap() : codeSystemMappings;
    }

    public Map<String, String> getCodeSystemMappings() {
        return codeSystemMappings;
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

        Map<String, String> parameters = XslParameterUtils.merge(
            xslParameters,
            XslParameterUtils.fromSettings(settings));
        Map<String, String> namespaces = XslParameterUtils.merge(
            xslNamespaces,
            XslParameterUtils.namespacesFromSettings(settings));

        for (String record : records) {
            if (record == null) {
                continue;
            }

            String cleanedRecord = record.trim();
            if (cleanedRecord.isEmpty()) {
                continue;
            }

            try {
                String transformedJson = transform(cleanedRecord, parameters, namespaces);
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
        remapCodeSystems(node);
        resolveSubstanceReferences(node);
        Substance substance = JsonSubstanceFactory.makeSubstance(node);
        return JsonEntityUtil.fixOwners(substance, true);
    }

    /**
     * Walks the parsed GSRS JSON and rewrites every {@code codeSystem} value
     * that has a configured replacement.
     *
     * @param node the parsed record, may be {@code null}
     */
    private void remapCodeSystems(JsonNode node) {
        if (node == null || codeSystemMappings.isEmpty()) {
            return;
        }
        if (node.isObject()) {
            JsonNode codeSystem = node.get("codeSystem");
            if (codeSystem != null && codeSystem.isTextual()) {
                String replacement = codeSystemMappings.get(codeSystem.asText());
                if (replacement != null) {
                    ((ObjectNode) node).put("codeSystem", replacement);
                }
            }
        }
        node.forEach(this::remapCodeSystems);
    }

    /**
     * Applies the configured code system remapping to the given name.
     *
     * @param codeSystem the code system name read from the input
     * @return the remapped name, or the input when no mapping matches
     */
    private String remapCodeSystem(String codeSystem) {
        String replacement = codeSystemMappings.get(codeSystem);
        return replacement == null ? codeSystem : replacement;
    }

    /**
     * Walks the parsed GSRS JSON and resolves every substance reference that
     * carries a {@code linkingID} of the form {@code <codeSystem>:<code>}.
     *
     * <p>The referenced substance is looked up by the code in the code system
     * and the reference's {@code refuuid}, {@code approvalID} and
     * {@code refPname} are overwritten with the resolved values. This runs
     * before {@link JsonSubstanceFactory} parses the JSON, because the model has
     * no {@code linkingID} field to carry the value through.</p>
     *
     * @param node the parsed record, may be {@code null}
     */
    private void resolveSubstanceReferences(JsonNode node) {
        if (node == null || substanceRepository == null) {
            return;
        }
        if (node.isObject()) {
            JsonNode linkingID = node.get("linkingID");
            if (linkingID != null && linkingID.isTextual()) {
                resolveSubstanceReference((ObjectNode) node, linkingID.asText());
            }
        }
        node.forEach(this::resolveSubstanceReferences);
    }

    /**
     * Resolves a single substance reference from its {@code linkingID}.
     *
     * <p>Only values of the form {@code <codeSystem>:<code>} are resolved. A
     * {@code linkingID} can also be an ordinary GSRS-derived identifier — a
     * uuid, an approval id or a substance name — and is then left untouched.
     * A name may itself contain a colon, but rarely one bounded by
     * non-whitespace on both sides, so the separator is recognised with
     * {@link #LINKING_ID_SEPARATOR} rather than by the first colon.</p>
     *
     * @param reference the JSON object holding the reference
     * @param linkingID the {@code <codeSystem>:<code>} value
     */
    private void resolveSubstanceReference(ObjectNode reference, String linkingID) {
        Matcher separator = LINKING_ID_SEPARATOR.matcher(linkingID);
        if (!separator.find()) {
            return;
        }
        int index = separator.start() + 1;
        String codeSystem = remapCodeSystem(linkingID.substring(0, index).trim());
        String code = linkingID.substring(index + 1).trim();
        SubstanceReference resolved = findReferenceByCode(code, codeSystem);
        if (resolved == null) {
            log.warn("No substance found for code '{}' in code system '{}'; leaving the reference unchanged.", code, codeSystem);
            return;
        }
        if (resolved.refuuid != null) {
            reference.put("refuuid", resolved.refuuid);
        }
        if (resolved.approvalID != null) {
            reference.put("approvalID", resolved.approvalID);
        } else {
            reference.remove("approvalID");
        }
        if (resolved.refPname != null) {
            reference.put("refPname", resolved.refPname);
        } else {
            reference.remove("refPname");
        }
    }

    /**
     * Finds the reference of the substance carrying the given code in the given
     * code system.
     *
     * @param code       the code value
     * @param codeSystem the code system name
     * @return the resolved reference, or {@code null} when no substance matches
     */
    private SubstanceReference findReferenceByCode(String code, String codeSystem) {
        Optional<SubstanceRepository.SubstanceSummary> summary =
            substanceRepository.findByCodes_CodeAndCodes_CodeSystem(code, codeSystem)
                .stream()
                .findFirst();
        return summary.map(SubstanceRepository.SubstanceSummary::toSubstanceReference).orElse(null);
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

    private String transform(String rawInput, Map<String, String> parameters, Map<String, String> namespaces)
            throws SaxonApiException, IOException {
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

        XslParameterUtils.applyParameters(transformer, parameters, namespaces);
        transformer.setParameter(new QName("input"), XdmAtomicValue.makeAtomicValue(rawInput));
        transformer.setParameter(new QName("input-format"), XdmAtomicValue.makeAtomicValue(inputFormat));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Serializer serializer = processor.newSerializer(outputStream);
        serializer.setOutputProperty(Serializer.Property.METHOD, "text");
        transformer.setDestination(serializer);
        transformer.transform();

        return outputStream.toString(StandardCharsets.UTF_8.name());
    }
}
