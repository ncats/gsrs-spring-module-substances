package gsrs.module.substance.exporters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncats.molwitch.Chemical;
import ix.core.validator.GinasProcessingMessage;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class SdfExporter implements Exporter<Substance> {
    @FunctionalInterface
    public interface ChemicalModifier {
        void modify(Chemical c, Substance parentSubstance, List<GinasProcessingMessage> messages, JsonNode detailedParameters);

    }

    private static final ChemicalModifier NO_OP_MODIFIER = (c, s, messages, detailedParameters) ->{};
    private final BufferedWriter out;

    private final ChemicalModifier modifier;

    private final ExporterFactory.Parameters parameters;

    public SdfExporter(OutputStream out, ChemicalModifier modifier, ExporterFactory.Parameters parameters){
        Objects.requireNonNull(out);
        Objects.requireNonNull(modifier);

        this.out = new BufferedWriter(new OutputStreamWriter(out));
        this.modifier  = modifier;
        this.parameters=parameters;
    }
    public SdfExporter(OutputStream out, ChemicalModifier modifier){
        this(out, modifier, null);
    }
    public SdfExporter(OutputStream out){
       this(out, NO_OP_MODIFIER);
    }

    public SdfExporter(File outputFile) throws IOException{
        this(new BufferedOutputStream(new FileOutputStream(outputFile)));
    }

    @Override
    public void export(Substance s) throws IOException {
        log.trace("starting export");
        List<GinasProcessingMessage> warnings = new ArrayList<>();

        Chemical chem = s.toChemical( warnings::add);
        System.out.println("properties");
        chem.getProperties().keySet().forEach(k-> System.out.println(k));


        modifier.modify(chem, s, warnings, parameters.detailedParameters());
        try {

            String content = formatMolfile(chem);
            out.write(content);
            out.newLine();

        }catch(Exception e){
            throw new IOException("error exporting to sdf file", e);
        }
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    private static String formatMolfile(Chemical c) throws Exception {
        String mol = c.toSd();
        String[] lines = mol.split("\n");
        lines[1] = " G-SRS " + lines[1];
        return String.join("\n", lines);
    }
}