package gsrs.module.substance.importers;

import gov.nih.ncats.molwitch.io.ChemicalReader;
import gov.nih.ncats.molwitch.io.ChemicalReaderFactory;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.dataExchange.model.MappingAction;
import gsrs.module.substance.importers.model.ChemicalBackedSDRecordContext;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class SDFImportAdapter implements AbstractImportSupportingGsrsEntityController.ImportAdapter<Substance> {

    List<MappingAction<Substance, SDRecordContext>> actions;
    
    public SDFImportAdapter(List<MappingAction<Substance, SDRecordContext>> actions){
        this.actions = actions;
    }
    
    @SneakyThrows
    @Override
    public Stream<Substance> parse(InputStream is) {
        ChemicalReader cr = ChemicalReaderFactory.newReader(is);
    	
        return cr.stream()
          .map(c->{
              return new ChemicalBackedSDRecordContext(c);
          })
    	  .map(sd->{
               //TODO: perhaps a builder instead?
               Substance s = new ChemicalSubstance();
               for(MappingAction<Substance, SDRecordContext> action: actions){
                   try {
                       s=action.act(s, sd);
                   } catch (Exception e) {
                       log.error(e.getMessage());
                       e.printStackTrace();
                  }
               }
              return s;
          });
    }
}
