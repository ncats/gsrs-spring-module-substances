package example.substance.export;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.module.substance.exporters.SubstancePropertyExporter;
import gsrs.module.substance.exporters.SubstancePropertyExporterFactory;
import ix.ginas.exporters.DefaultParameters;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

public class PropertyExporterTest {

    @Test
    void testGetFormats() {
        SubstancePropertyExporterFactory factory = new SubstancePropertyExporterFactory();
        Set<OutputFormat> formats= factory.getSupportedFormats();
        boolean actual =formats.stream().anyMatch(f->f.getExtension().equalsIgnoreCase("properties.txt")
                && f.getDisplayName().equalsIgnoreCase("Substance Property File(.properties.txt)"));
        Assertions.assertTrue(actual);
    }

    @Test
    void testSupports() {
        ExporterFactory.Parameters parameters = new DefaultParameters( new OutputFormat("properties.txt", "Substance Property File(.properties.txt)"), false);
        SubstancePropertyExporterFactory factory = new SubstancePropertyExporterFactory();
        Assertions.assertTrue(factory.supports(parameters));
    }

    @Test
    void testSupportsNot() {
        ExporterFactory.Parameters parameters = new DefaultParameters( new OutputFormat("txt", "Regular Text File(.txt)"), false);
        SubstancePropertyExporterFactory factory = new SubstancePropertyExporterFactory();
        Assertions.assertFalse(factory.supports(parameters));
    }

    @Test
    void testCreateExporter() throws IOException {
        SubstancePropertyExporterFactory factory = new SubstancePropertyExporterFactory();
        ObjectNode additionalParameters = JsonNodeFactory.instance.objectNode();
        additionalParameters.put("someField", "some value");
        ExporterFactory.Parameters parameters = new DefaultParameters( new OutputFormat("properties.txt",
                "Substance Property File(.properties.txt)"), false, additionalParameters);
        File file = new File("output1.txt");
        FileOutputStream stream = new FileOutputStream(file);

        Exporter<Substance> exporter = factory.createNewExporter(stream, parameters);
        Assertions.assertTrue(exporter instanceof SubstancePropertyExporter);
    }
}
