package gsrs.module.substance.importers;

import gov.nih.ncats.molwitch.io.ChemicalReader;
import gov.nih.ncats.molwitch.io.ChemicalReaderFactory;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import jdk.internal.jline.internal.Log;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

public class SDFImportAdapter implements AbstractImportSupportingGsrsEntityController.ImportAdapter<Substance> {

    List<SDFImportAdaptorFactory.MappingAction<Substance, SDFImportAdaptorFactory.SDRecordContext>> actions;
    
    public SDFImportAdapter(List<SDFImportAdaptorFactory.MappingAction<Substance, SDFImportAdaptorFactory.SDRecordContext>> actions){
        this.actions = actions;
    }
    
    @SneakyThrows
    @Override
    public Stream<Substance> parse(InputStream is) {
        ChemicalReader cr = ChemicalReaderFactory.newReader(is);
    	
        return cr.stream()
          .map(c->{
              return new SDFImportAdaptorFactory.ChemicalBackedSDRecordContext(c);
          })
    	  .map(sd->{
               //TODO: perhaps a builder instead?
               Substance s = new ChemicalSubstance();
               for(SDFImportAdaptorFactory.MappingAction<Substance, SDFImportAdaptorFactory.SDRecordContext> action: actions){
                   try {
                       s=action.act(s, sd);
                   } catch (Exception e) {
                       Log.error(e);
                  }
               }
              return s;
          });
    }
}
