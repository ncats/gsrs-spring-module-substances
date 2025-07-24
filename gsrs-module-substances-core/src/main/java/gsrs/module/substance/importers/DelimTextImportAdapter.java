package gsrs.module.substance.importers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.dataexchange.model.MappingAction;
import gsrs.importer.DefaultPropertyBasedRecordContext;
import gsrs.importer.PropertyBasedDataRecordContext;
import gsrs.imports.ImportAdapter;
import ix.ginas.importers.TextFileReader;
import ix.ginas.modelBuilders.*;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class DelimTextImportAdapter implements ImportAdapter<Substance> {

    private boolean removeQuotes=false;

    protected String substanceClassName = "concept";

    private String lineValueDelimiter = ",";

    private int linesToSkip=0;

    protected List<String> fileFields;

    //todo: assign real values
    protected List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> actions = new ArrayList<>();

    public DelimTextImportAdapter(List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> actions,
                                  Map<String, Object> parameters) {
        this.actions=actions;
        if( parameters==null) {
            log.warn("no 'parameters' provided in constructor. Using default values");
            return;
        }
        if (parameters.get("substanceClassName") != null) {
            this.substanceClassName = (String) parameters.get("substanceClassName");
            log.trace("got substanceClassName: {}", substanceClassName);
        }
        if( parameters.get("lineValueDelimiter") !=null){
            this.lineValueDelimiter=(String) parameters.get("lineValueDelimiter");
        }
        if(parameters.get("removeQuotes") !=null) {
            try {
                Object rawValue = parameters.get("removeQuotes");
                if( rawValue instanceof String){
                    this.removeQuotes = Boolean.parseBoolean( (String)rawValue);
                } else if( rawValue instanceof Boolean) {
                    this.removeQuotes = (Boolean)rawValue;
                }
            }
            catch (Exception ex){
                log.warn("Error creating boolean from {}", parameters.get("removeQuotes"));
            }
        }
        if(parameters.get("fileFields") != null) {
            this.fileFields = new ArrayList<>();
            fileFields.addAll((List<String>)parameters.get("fileFields"));
        }
        if(parameters.get("linesToSkip") !=null) {
            try {
                Object rawValue=parameters.get("linesToSkip");
                if(rawValue instanceof String) {
                    this.linesToSkip = Integer.parseInt((String) rawValue);
                } else if( rawValue instanceof Integer){
                    this.linesToSkip = (Integer) rawValue;
                }
            }
            catch(ClassCastException | NumberFormatException e){
                log.warn("Error creating Integer from {}", parameters.get("linesToSkip"));
            }
        }
    }

    @Override
    public Stream<Substance> parse(InputStream is, ObjectNode settings, JsonNode schema) {
        log.trace("Charset.defaultCharset: " + Charset.defaultCharset().name());
        TextFileReader reader = new TextFileReader();
        try {
            Stream<DefaultPropertyBasedRecordContext> contextStream = reader.readFile(is, lineValueDelimiter, removeQuotes, fileFields);
            return contextStream
                    .peek(r-> {
                        log.trace("in parse, record has {} properties", r.getProperties().size());
                        r.getProperties().forEach(p-> log.trace("   property: {}; value: {}", p, r.getProperty(p).isPresent() ? r.getProperty(p).get() : "absent"));
                    } )
                    .filter(r->r.getProperties() != null && r.getProperties().size() >0
                            /*&& r.getProperties().stream().anyMatch(p-> r.getProperty(p).isPresent() && r.getProperty(p).get().length()>0)*/)
                    .map(r->{
                        log.trace("in map that follows filter");
                        AbstractSubstanceBuilder s;
                         switch(substanceClassName) {
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
                        return s.build();
                    });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
