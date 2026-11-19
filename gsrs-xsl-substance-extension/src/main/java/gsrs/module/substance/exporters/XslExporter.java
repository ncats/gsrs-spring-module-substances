package gsrs.module.substance.exporters;

import com.fasterxml.jackson.databind.ObjectWriter;

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
    private final ObjectWriter writer =  EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().writer();
    private int row=0;

    public XslExporter(OutputStream out, String templateFile, String header, String footer, String delimiter)
            throws SaxonApiException, IOException {

        this.out = out;
        this.footer = footer;
        this.delimiter = delimiter;

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
    }

    @Override
    public void export(Substance s) throws IOException {
        if (row > 0) {
            out.write(delimiter.getBytes(StandardCharsets.UTF_8));
        }

        row = row + 1;

        try {
            transformer.setParameter(
                new QName("json-input"),
                XdmAtomicValue.makeAtomicValue(writer.writeValueAsString(s))
            );
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
