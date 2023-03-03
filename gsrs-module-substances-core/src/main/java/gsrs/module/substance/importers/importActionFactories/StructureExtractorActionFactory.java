package gsrs.module.substance.importers.importActionFactories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.dataexchange.model.MappingAction;
import gsrs.dataexchange.model.MappingActionFactoryMetadata;
import gsrs.dataexchange.model.MappingActionFactoryMetadataBuilder;
import gsrs.dataexchange.model.MappingParameter;
import gsrs.importer.PropertyBasedDataRecordContext;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.Amount;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Moiety;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static gsrs.module.substance.importers.SDFImportAdapterFactory.resolveParametersMap;

@Data
@Slf4j
public class StructureExtractorActionFactory extends BaseActionFactory {
    private String actionName;
    private String actionLabel;

    @Autowired
    private StructureProcessor structureProcessor;

    public MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> create(Map<String, Object> abstractParams) throws Exception {
        log.trace("create");
        return (sub, sdRec) -> {
            Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
            GinasChemicalStructure s = new GinasChemicalStructure();
            s.molfile = (String) params.get("molfile");
            doBasicsImports(s, params);
            ((ChemicalSubstanceBuilder)sub).setStructure(s);
            List<Structure> moieties = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            try {

                Structure struc = structureProcessor.taskFor(s.molfile)
                        .components(moieties)
                        .standardize(false)
                        .query(false)
                        .build()
                        .instrument()
                        .getStructure();


                for (Structure m : moieties) {
                    Moiety moiety = new Moiety();
                    moiety.structure = new GinasChemicalStructure(m);
                    log.trace("created moiety");
                    Amount c1 = Moiety.intToAmount(m.count);
                    moiety.setCountAmount(c1);
                    ((ChemicalSubstanceBuilder)sub).addMoiety(moiety);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return sub;
        };
    }

    @Override
    public MappingActionFactoryMetadata getMetadata() {
        MappingActionFactoryMetadataBuilder builder = new MappingActionFactoryMetadataBuilder();
        return builder.setLabel("Create Structure")
                .addParameterField(MappingParameter.builder()
                        .setFieldName("molfile")
                        .setValueType(String.class)
                        .setRequired(true)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("stereochemistry")
                        .setValueType(String.class)
                        .build())
                .addParameterField(MappingParameter.builder()
                        .setFieldName("opticalActivity")
                        .setValueType(String.class)
                        .build())
                .build();
    }
}
