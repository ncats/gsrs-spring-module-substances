package gsrs.module.substance.importers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncats.molwitch.io.ChemicalReader;
import gov.nih.ncats.molwitch.io.ChemicalReaderFactory;
import gsrs.dataexchange.model.MappingAction;
import gsrs.imports.ImportAdapter;
import gsrs.module.substance.importers.model.ChemicalBackedSDRecordContext;
import gsrs.importer.PropertyBasedDataRecordContext;
import ix.core.models.Group;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class SDFImportAdapter implements ImportAdapter<Substance> {

    List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> actions;

    private String fileEncoding;
    
    public SDFImportAdapter(List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> actions){
        this.actions = actions;
    }
    
    @SneakyThrows
    @Override
    public Stream<Substance> parse(InputStream is, ObjectNode settings, JsonNode schema) {
        log.trace("Charset.defaultCharset: {}", Charset.defaultCharset().name());
        String encoding;
        if(!settings.hasNonNull("Encoding")) {
            encoding =Charset.defaultCharset().name();
        } else {
            encoding=settings.get("Encoding").textValue();
        }
        ChemicalReader cr = ChemicalReaderFactory.newReader(is, encoding);
    	
        return cr.stream()
          .map(c-> new ChemicalBackedSDRecordContext(c))
    	  .map(sd->{
               ChemicalSubstanceBuilder s = new ChemicalSubstanceBuilder();
               //for now, newly imported substances will be PROTECTED
               s.setAccess(Collections.singleton(new Group("PROTECTED")));
               for(MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> action: actions){
                   try {
                       //log.trace("Before action, substance has {} names and {} codes", s.names.size(), s.codes.size());
                       s= (ChemicalSubstanceBuilder) action.act(s, sd);
                       //log.trace("After action, substance has {} names and {} codes", s.names.size(), s.codes.size());
                   } catch (Exception e) {
                       log.error(e.getMessage());
                       e.printStackTrace();
                  }
               }
               //log.trace("created substance has {} names and {} codes", s.names.size(), s.codes.size());
               //log.trace(s.toFullJsonNode().toPrettyString());
              return s.build();
          });
    }

    public String getFileEncoding() {
        return fileEncoding;
    }

    public void setFileEncoding(String fileEncoding) {
        this.fileEncoding = fileEncoding;
    }
}
