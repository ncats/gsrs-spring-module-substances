package example.exports.exporters;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.module.substance.exporters.SdfExporter;
import gsrs.module.substance.exporters.SdfExporterFactory;
import ix.ginas.exporters.DefaultParameters;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/*
This class exercises a small set of options for the SDF exporter
Note: for these tests to pass, the line
    out.flush();
within SdfExporterFactory must be uncommented
 */
@Slf4j
public class SdfExporterTest {
    SdfExporterFactory factory = new SdfExporterFactory();
    List<String> substanceNames = Arrays.asList("Substance One", "Substance #1");


    @Test
    public void testExportCodesAndNames() throws IOException {
        ByteArrayOutputStream byteArrayInputStream = new ByteArrayOutputStream(4096);
        File outputFile = new File("d:\\temp\\test1.sdf");
        if(outputFile.exists()) {
            outputFile.delete();
            log.trace("file existed and was deleted");
        }
        FileOutputStream fos = new FileOutputStream(outputFile);
        ObjectNode details = JsonNodeFactory.instance.objectNode();
        details.put(SdfExporterFactory.CODE_PARAMETERS, true);
        details.put(SdfExporterFactory.NAME_PARAMETERS, true);
        OutputFormat outputFormat = new OutputFormat("sdf", "SDF");
        BufferedOutputStream outputStream= new BufferedOutputStream(fos);
        DefaultParameters parameters = new DefaultParameters(outputFormat,false, details);
        SdfExporter exporter= (SdfExporter) factory.createNewExporter(outputStream, parameters );
        exporter.export(createSubstanceWithNamesAndCodes());
//        outputStream.flush();
//        outputStream.close();
//        fos.flush();
//        fos.close();
        Assertions.assertTrue(outputFile.exists());
        String fileData= Files.readString(outputFile.toPath());
        System.out.println("fileData: " + fileData);
        Assertions.assertTrue(fileData.contains("50-00-0"));
        Assertions.assertTrue(substanceNames.stream().allMatch(s->fileData.contains(s)));
    }

    @Test
    public void testExportCodesNoNames() throws IOException {
        ByteArrayOutputStream byteArrayInputStream = new ByteArrayOutputStream(4096);
        File outputFile = new File("d:\\temp\\test1.sdf");
        if(outputFile.exists()) {
            outputFile.delete();
            log.trace("file existed and was deleted");
        }
        FileOutputStream fos = new FileOutputStream(outputFile);
        ObjectNode details = JsonNodeFactory.instance.objectNode();
        details.put(SdfExporterFactory.CODE_PARAMETERS, true);
        details.put(SdfExporterFactory.NAME_PARAMETERS, false);
        OutputFormat outputFormat = new OutputFormat("sdf", "SDF");
        BufferedOutputStream outputStream= new BufferedOutputStream(fos);
        DefaultParameters parameters = new DefaultParameters(outputFormat,false, details);

        SdfExporter exporter= (SdfExporter) factory.createNewExporter(outputStream, parameters );
        exporter.export(createSubstanceWithNamesAndCodes());
        outputStream.flush();

        outputStream.close();
        fos.close();
        Assertions.assertTrue(outputFile.exists());
        String fileData= Files.readString(outputFile.toPath());
        System.out.println("fileData: " + fileData);
        Assertions.assertTrue(fileData.contains("50-00-0"));
        Assertions.assertFalse(fileData.contains("<NAME"));
    }

    @Test
    public void testExportNamesNoCodes() throws IOException {
        ByteArrayOutputStream byteArrayInputStream = new ByteArrayOutputStream(4096);
        File outputFile = new File("d:\\temp\\test1.sdf");
        if(outputFile.exists()) {
            outputFile.delete();
            log.trace("file existed and was deleted");
        }
        FileOutputStream fos = new FileOutputStream(outputFile);
        ObjectNode details = JsonNodeFactory.instance.objectNode();
        details.put(SdfExporterFactory.CODE_PARAMETERS, false);
        details.put(SdfExporterFactory.NAME_PARAMETERS, true);
        OutputFormat outputFormat = new OutputFormat("sdf", "SDF");
        BufferedOutputStream outputStream= new BufferedOutputStream(fos);
        DefaultParameters parameters = new DefaultParameters(outputFormat,false, details);

        SdfExporter exporter= (SdfExporter) factory.createNewExporter(outputStream, parameters );
        exporter.export(createSubstanceWithNamesAndCodes());
        outputStream.flush();

        outputStream.close();
        fos.close();
        Assertions.assertTrue(outputFile.exists());
        String fileData= Files.readString(outputFile.toPath());
        System.out.println("fileData: " + fileData);
        Assertions.assertFalse(fileData.contains("50-00-0"));
        Assertions.assertTrue(substanceNames.stream().allMatch(s->fileData.contains(s)));
    }

    @Test
    public void testExportNoCodesNoNames() throws IOException {
        ByteArrayOutputStream byteArrayInputStream = new ByteArrayOutputStream(4096);
        File outputFile = new File("d:\\temp\\test1.sdf");
        if(outputFile.exists()) {
            outputFile.delete();
            log.trace("file existed and was deleted");
        }
        FileOutputStream fos = new FileOutputStream(outputFile);
        ObjectNode details = JsonNodeFactory.instance.objectNode();
        details.put(SdfExporterFactory.CODE_PARAMETERS, false);
        details.put(SdfExporterFactory.NAME_PARAMETERS, false);
        OutputFormat outputFormat = new OutputFormat("sdf", "SDF");
        BufferedOutputStream outputStream= new BufferedOutputStream(fos);
        DefaultParameters parameters = new DefaultParameters(outputFormat,false, details);

        SdfExporter exporter= (SdfExporter) factory.createNewExporter(outputStream, parameters );
        exporter.export(createSubstanceWithNamesAndCodes());
        outputStream.flush();
        outputStream.close();
        fos.flush();
        fos.close();
        Assertions.assertTrue(outputFile.exists());
        String fileData= Files.readString(outputFile.toPath());
        System.out.println("fileData: " + fileData);
        Assertions.assertFalse(fileData.contains("50-00-0"));
        Assertions.assertFalse(fileData.contains("<NAME"));
        Assertions.assertTrue(fileData.contains("$$$$"));
    }

    private Substance createSubstanceWithNamesAndCodes() {
        SubstanceBuilder builder = new SubstanceBuilder();
        substanceNames.forEach(builder::addName);
        Code cas1= new Code();
        cas1.codeSystem="CAS";
        cas1.code="50-00-0";
        builder.addCode(cas1);
        return builder.build();
    }
}
