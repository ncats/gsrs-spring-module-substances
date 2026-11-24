package gsrs.module.substance.exporters;

import com.fasterxml.jackson.databind.ObjectWriter;

import gsrs.module.substance.utils.LinkingIDUpdater;
import gsrs.module.substance.utils.XslParameterUtils;
import ix.core.controllers.EntityFactory;
import ix.ginas.exporters.*;
import ix.ginas.models.v1.Substance;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.*;


public class XslExporter implements Exporter<Substance> {

    private final OutputStream out;
    private final String footer;
    private final String delimiter;
    private final Processor processor;
    private final XsltTransformer transformer;
    private final Map<String, String> parameters;
    private final Map<String, String> namespaces;
    private final LinkingIDUpdater linkingIDUpdater;
    private final ObjectWriter writer =  EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().writer();
    private int row=0;

    public XslExporter(OutputStream out, String templateFile, String header, String footer, String delimiter)
            throws SaxonApiException, IOException {
        this(out, templateFile, header, footer, delimiter, Collections.emptyMap());
    }

    public XslExporter(OutputStream out, String templateFile, String header, String footer, String delimiter,
                       Map<String, String> parameters)
            throws SaxonApiException, IOException {
        this(out, templateFile, header, footer, delimiter, parameters, null);
    }

    public XslExporter(OutputStream out, String templateFile, String header, String footer, String delimiter,
                       Map<String, String> parameters, Map<String, String> namespaces)
            throws SaxonApiException, IOException {
        this(out, templateFile, header, footer, delimiter, parameters, namespaces, null);
    }

    /**
     * Creates an exporter that optionally rewrites the {@code linkingID} of the
     * exported substance references.
     *
     * <p>The {@code linkingID} rewrite happens on the substance JSON before it
     * reaches the XSL template, so a template that keeps {@code linkingID} and
     * drops {@code refuuid} writes the resolved identifiers rather than the
     * ones GSRS derived locally.</p>
     *
     * @param linkingIDUpdater rewrites the reference linking identifiers, may be
     *                         {@code null} or disabled
     */
    public XslExporter(OutputStream out, String templateFile, String header, String footer, String delimiter,
                       Map<String, String> parameters, Map<String, String> namespaces,
                       LinkingIDUpdater linkingIDUpdater)
            throws SaxonApiException, IOException {

        this.out = out;
        this.footer = footer;
        this.delimiter = delimiter;
        this.parameters = XslParameterUtils.merge(null, parameters);
        this.namespaces = XslParameterUtils.merge(null, namespaces);
        this.linkingIDUpdater = linkingIDUpdater;

        out.write(header.getBytes(StandardCharsets.UTF_8));

        processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();

        InputStream templateStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(templateFile);
        if (templateStream == null) {
            File f = new File(templateFile);
            if (f.exists()) {
                templateStream = new FileInputStream(f);
            } else {
                throw new IOException("XSL template not found on classpath or filesystem: " + templateFile);
            }
        }
        XsltExecutable executable = compiler.compile(
            new StreamSource(templateStream)
        );

        transformer = executable.load();

        XdmNode sourceNode = processor.newDocumentBuilder()
            .build(new StreamSource(new StringReader("<root/>")));

        transformer.setInitialContextNode(sourceNode);
        XslParameterUtils.applyParameters(transformer, this.parameters, this.namespaces);
    }

    @Override
    public void export(Substance s) throws IOException {
        if (row > 0) {
            out.write(delimiter.getBytes(StandardCharsets.UTF_8));
        }

        row = row + 1;

        try {
            String jsonString = writer.writeValueAsString(s);
            if (linkingIDUpdater != null && linkingIDUpdater.isEnabled()) {
                jsonString = linkingIDUpdater.apply(jsonString);
            }
            XdmAtomicValue json = XdmAtomicValue.makeAtomicValue(jsonString);
            transformer.setParameter(new QName("input"), json);
            Serializer serializer = processor.newSerializer(out);
            serializer.setOutputProperty(Serializer.Property.METHOD, "text");
            transformer.setDestination(serializer);
            transformer.transform();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        out.write(footer.getBytes(StandardCharsets.UTF_8));
        out.close();
    }

}
