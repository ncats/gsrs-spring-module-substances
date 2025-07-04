package gsrs.module.substance.importers;

import gsrs.dataexchange.model.MappingAction;
import gsrs.importer.PropertyBasedDataRecordContext;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;

import java.util.List;
import java.util.Map;

public class ChemicalDelimTextImportAdapter extends DelimTextImportAdapter{

    public ChemicalDelimTextImportAdapter(List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> actions, Map<String, Object> parameters) {
        super(actions, parameters);
        this.substanceClassName="Chemical";
    }
}
