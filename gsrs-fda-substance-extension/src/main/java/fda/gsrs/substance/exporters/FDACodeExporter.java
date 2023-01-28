package fda.gsrs.substance.exporters;

import ix.ginas.exporters.Exporter;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Created by VenkataSaiRa.Chavali on 3/10/2017.
 */
public class FDACodeExporter implements Exporter<Substance> {

    private String primaryCodeSystem;
    BufferedWriter bw;

    public FDACodeExporter(OutputStream os, String primaryCodeSystem) throws IOException{
        this.primaryCodeSystem = primaryCodeSystem;
        bw = new BufferedWriter(new OutputStreamWriter(os));
        bw.write("Approval ID" + ((primaryCodeSystem!=null) ? "\t"+primaryCodeSystem : "") + "\tCode Public/Private\tCode\tCode System\tCode Type\tCode Text\tComments");
        bw.newLine();
    }

    @Override
    public void export(Substance obj) throws IOException {
        // The substance corresponds to one line of "scrubbed" data.
        // Don't we need to filter on type = PRIMARY ???? It wasn't doing that before. Or is primary always the first one?
        String priCode = (primaryCodeSystem != null) ?
        obj.codes.stream().filter(cd -> cd.codeSystem.equals(primaryCodeSystem) && cd.type.equals("PRIMARY")).map(cd -> cd.code).findFirst().orElse(null)
        : "";
        for (Code c : obj.getCodes()) {
            String publicOrPrivate = (c.getAccess().isEmpty())
                ? "Public" : "Private: " + ExporterUtilities.makeAccessGroupString(c.getAccess());
            String str = obj.approvalID;
            if (primaryCodeSystem != null) {
                str += "\t" + priCode;
            }
            str +=
            "\t" + publicOrPrivate
            + "\t" + c.code
            + "\t" + c.codeSystem
            + "\t" + c.type
            + "\t" + ((c.codeText != null) ? c.codeText : "")
            + "\t" + ((c.comments != null) ? ExporterUtilities.replaceAllLinefeedsWithPipes(c.comments) : "");
            bw.write(str);
            bw.newLine();
        }
      }


    @Override
    public void close() throws IOException {
        bw.close();
    }
}
