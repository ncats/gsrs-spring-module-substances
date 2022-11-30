package gsrs.module.substance.importers;

import gov.nih.ncats.molwitch.io.ChemicalReader;
import gov.nih.ncats.molwitch.io.ChemicalReaderFactory;
import gsrs.dataexchange.model.MappingAction;
import gsrs.imports.ImportAdapter;
import gsrs.module.substance.importers.model.ChemicalBackedSDRecordContext;
import gsrs.module.substance.importers.model.PropertyBasedDataRecordContext;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class DelimTextImportAdapter implements ImportAdapter<Substance> {


    private String fileEncoding;

    //todo: assign real values
    private List<MappingAction<Substance, PropertyBasedDataRecordContext>> actions = new ArrayList<>();

    @Override
    public Stream<Substance> parse(InputStream is, String fileEncoding) {
        log.trace("Charset.defaultCharset: " + Charset.defaultCharset().name());
        ChemicalReader cr = null;
        try {
            cr = ChemicalReaderFactory.newReader(is, fileEncoding);
        } catch (IOException e) {
            log.error("error reading file: ", e);
            throw new RuntimeException(e);
        }

        return cr.stream()
                .map(c->{
                    return new ChemicalBackedSDRecordContext(c);
                })
                .map(sd->{
                    //TODO: perhaps a builder instead?
                    Substance s = new ChemicalSubstance();
                    for(MappingAction<Substance, PropertyBasedDataRecordContext> action: actions){
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
}
