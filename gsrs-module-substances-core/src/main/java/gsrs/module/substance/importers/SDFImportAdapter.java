package gsrs.module.substance.importers;

import gov.nih.ncats.molwitch.io.ChemicalReader;
import gov.nih.ncats.molwitch.io.ChemicalReaderFactory;
import gsrs.dataExchange.model.MappingAction;
import gsrs.imports.ImportAdapter;
import gsrs.module.substance.importers.model.ChemicalBackedSDRecordContext;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class SDFImportAdapter implements ImportAdapter<Substance> {

    List<MappingAction<Substance, SDRecordContext>> actions;

    private String fileEncoding;
    
    public SDFImportAdapter(List<MappingAction<Substance, SDRecordContext>> actions){
        this.actions = actions;
    }
    
    @SneakyThrows
    @Override
    public Stream<Substance> parse(InputStream is, String encoding) {
        log.trace("Charset.defaultCharset: " + Charset.defaultCharset().name());
        ChemicalReader cr = ChemicalReaderFactory.newReader(is, encoding);
    	
        return cr.stream()
          .map(c->{
              return new ChemicalBackedSDRecordContext(c);
          })
    	  .map(sd->{
               //TODO: perhaps a builder instead?
               Substance s = new ChemicalSubstance();
               for(MappingAction<Substance, SDRecordContext> action: actions){
                   try {
                       log.trace("Before action, substance has {} names and {} codes", s.names.size(), s.codes.size());
                       s=action.act(s, sd);
                       log.trace("After action, substance has {} names and {} codes", s.names.size(), s.codes.size());
                   } catch (Exception e) {
                       log.error(e.getMessage());
                       e.printStackTrace();
                  }
               }
               log.trace("created substance has {} names and {} codes", s.names.size(), s.codes.size());
               log.trace(s.toFullJsonNode().toPrettyString());
              return s;
          });
    }

    public String getFileEncoding() {
        return fileEncoding;
    }

    public void setFileEncoding(String fileEncoding) {
        this.fileEncoding = fileEncoding;
    }
}
