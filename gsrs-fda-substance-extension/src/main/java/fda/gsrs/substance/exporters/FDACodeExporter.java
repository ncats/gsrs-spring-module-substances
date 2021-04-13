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

    BufferedWriter bw;

    private final boolean showPrivates;

    public FDACodeExporter(OutputStream os, boolean showPrivates) throws IOException{
        this.showPrivates =showPrivates;

        bw = new BufferedWriter(new OutputStreamWriter(os));
        bw.write("UNII\tBDNUM\tCode\tCode System\tCode Type\tCode Text\tComments\tisPublic");
        bw.newLine();
    }

    @Override
    public void export(Substance obj) throws IOException {
        //GSRS-699 skip substances that aren't public unless we have show private data too
        if(!showPrivates && !obj.getAccess().isEmpty()){
            return;
        }
        String bdnum = obj.codes.stream().filter(cd->cd.codeSystem.equals("BDNUM")).map(cd->cd.code).findFirst().orElse(null);
        for ( Code c :obj.getCodes()){
            if(c.codeSystem.equals("BDNUM")){
                continue;
            }
            boolean isPublic = c.getAccess().isEmpty();
            if(!showPrivates && !isPublic){
                continue;
            }
            String str = obj.approvalID 
                         + "\t"+ bdnum 
                         + "\t"+ c.code 
                         + "\t" + c.codeSystem 
                         + "\t" + c.type
                         + "\t" + ((c.comments!=null)?c.comments:"") 
                         + "\t" + ((c.codeText!=null)?c.codeText:"") 
                         + "\t" + isPublic;
            bw.write(str);
            bw.newLine();
        }
    }

    @Override
    public void close() throws IOException {
        bw.close();
    }
}
