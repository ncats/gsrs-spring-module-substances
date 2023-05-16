package gsrs.module.substance.importers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.dataexchange.model.MappingAction;
import gsrs.importer.DefaultPropertyBasedRecordContext;
import gsrs.importer.PropertyBasedDataRecordContext;
import ix.ginas.importers.ExcelSpreadsheetReader;
import ix.ginas.modelBuilders.*;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class ExcelFileImportAdapter extends DelimTextImportAdapter {
    private String dataSheetName;

    private Integer fieldRow=0;

    public ExcelFileImportAdapter(List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> actions, Map<String, Object> parameters) {

        super(actions, parameters);
        if (parameters.get("substanceClassName") != null) {
            this.substanceClassName = (String) parameters.get("substanceClassName");
            log.trace("got substanceClassName: {}", substanceClassName);
        }

        if (parameters.get("dataSheetName") != null) {
            this.dataSheetName = (String) parameters.get("dataSheetName");
            log.trace("got sheetName: {}", dataSheetName);
        }

        if(parameters.get("fieldRow")!= null){
            this.fieldRow= (Integer) parameters.get("fieldRow");
        }
    }

    @Override
    public Stream<Substance> parse(InputStream is, ObjectNode settings, JsonNode schema) {
        ExcelSpreadsheetReader reader = new ExcelSpreadsheetReader(is);
        String sheetWithData = settings == null || !settings.hasNonNull("dataSheetName") ? this.dataSheetName
                : settings.get("dataSheetName").textValue();
        log.trace("using sheetWithData {}", sheetWithData);
        Stream<DefaultPropertyBasedRecordContext> contextStream = reader.readSheet(sheetWithData, null, fieldRow);
        return contextStream
                .map(r -> {

                    AbstractSubstanceBuilder s;
                    switch (substanceClassName) {
                        case "Chemical":
                            s = new ChemicalSubstanceBuilder();
                            break;
                        case "Protein":
                            s = new ProteinSubstanceBuilder();
                            break;
                        case "NucleicAcid":
                            s = new NucleicAcidSubstanceBuilder();
                            break;
                        case "Mixture":
                            s = new MixtureSubstanceBuilder();
                            break;
                        case "StructurallyDiverse":
                            s = new StructurallyDiverseSubstanceBuilder();
                            break;
                        case "Polymer":
                            s = new PolymerSubstanceBuilder();
                            break;
                        case "SpecifiedSubstanceGroup1":
                            s = new SpecifiedSubstanceGroup1SubstanceBuilder();
                            break;
                        default:
                            s = new SubstanceBuilder();
                    }

                    for (MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> action : actions) {
                        try {
                            s = action.act(s, r);
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
}
