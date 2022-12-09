package gsrs.module.substance.importers;

import gsrs.dataexchange.model.MappingAction;
import gsrs.imports.ImportAdapter;
import gsrs.module.substance.importers.model.DefaultPropertyBasedRecordContext;
import gsrs.module.substance.importers.model.PropertyBasedDataRecordContext;
import gsrs.module.substance.importers.readers.TextFileReader;
import ix.ginas.modelBuilders.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class DelimTextImportAdapter implements ImportAdapter<AbstractSubstanceBuilder> {

    private boolean removeQuotes=false;

    private String substanceTypeColumn = "SUBSTANCE_TYPE";

    private String expectedSubstanceClass;

    private String lineValueDelimiter = ",";

    //todo: assign real values
    private List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> actions = new ArrayList<>();

    public DelimTextImportAdapter(List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> actions,
                                  Map<String, Object> parameters) {
        this.actions=actions;
        if( parameters==null) {
            log.warn("no 'parameters' provided in constructor. Using default values");
            return;
        }
        if (parameters.get("substanceClass") != null) {
            this.expectedSubstanceClass = (String) parameters.get("substanceClass");
        }
        if( parameters.get("lineDelimiter") !=null){
            this.lineValueDelimiter=(String) parameters.get("lineDelimiter");
        }
        if(parameters.get("removeQuotes") !=null) {
            this.removeQuotes = (Boolean) parameters.get("removeQuotes");
        }
    }

    @Override
    public Stream<AbstractSubstanceBuilder> parse(InputStream is, String fileEncoding) {
        log.trace("Charset.defaultCharset: " + Charset.defaultCharset().name());
        TextFileReader reader = new TextFileReader();
        try {
            Stream<DefaultPropertyBasedRecordContext> contextStream = reader.readFile(is, lineValueDelimiter, removeQuotes);
            return contextStream
                    .map(r->{

                        AbstractSubstanceBuilder s;
                        switch(expectedSubstanceClass) {
                            case "Chemical":
                                s= new ChemicalSubstanceBuilder();
                                break;
                            case "Protein":
                                s= new ProteinSubstanceBuilder();
                                break;
                            case "NucleicAcid" :
                                s = new NucleicAcidSubstanceBuilder();
                                break;
                            case "Mixture" :
                                s = new MixtureSubstanceBuilder();
                                break;
                            case "StructurallyDiverse" :
                                s = new StructurallyDiverseSubstanceBuilder();
                                break;
                            case "Polymer" :
                                s = new PolymerSubstanceBuilder();
                                break;
                            case "SpecifiedSubstanceGroup1":
                                s = new SpecifiedSubstanceGroup1SubstanceBuilder();
                                break;
                            default:
                                 s = new SubstanceBuilder();
                        }

                        for(MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> action: actions){
                            try {
                                s=  action.act(s, r);
                            } catch (Exception e) {
                                log.error(e.getMessage());
                                e.printStackTrace();
                            }
                        }
                        //log.trace("created substance has {} names and {} codes", s.names.size(), s.codes.size());
                        //log.trace(s.toFullJsonNode().toPrettyString());
                        return s;
                    });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
