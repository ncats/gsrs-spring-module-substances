package gsrs.module.substance.importers;

import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import ix.ginas.models.v1.Substance;

import java.io.InputStream;
import java.util.stream.Stream;

public class SDFImportAdapter implements AbstractImportSupportingGsrsEntityController.ImportAdapter<Substance> {

    List<MappingActionFactory<Substance,SDRecordContext>> actions;
    
    public SDFImportAdapter(List<MappingActionFactory<Substance,SDRecordContext>> actions){
        this.actions = actions;
    }
    
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
               for(MappingActionFactory<Substance,SDRecordContext> act: actions){
                    s=action.act(s, sd);  
               }
              return s;
          });
    }
}
