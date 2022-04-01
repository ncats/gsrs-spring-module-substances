package importers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.dataExchange.model.MappingAction;
import gsrs.module.substance.importers.SDFImportAdapter;
import gsrs.module.substance.importers.SDFImportAdaptorFactory;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringBootConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootConfiguration
@Slf4j
public class NSRSSDFImportAdapterFactory extends SDFImportAdaptorFactory {
    @Override
    public String getAdapterName() {
        return "NSRS SDF Adapter";
    }

    @SneakyThrows
    @Override
    public AbstractImportSupportingGsrsEntityController.ImportAdapter<Substance> createAdapter(JsonNode adapterSettings) {
        List<MappingAction<Substance, SDRecordContext>> actions = getMappingActions(adapterSettings);
        AbstractImportSupportingGsrsEntityController.ImportAdapter sDFImportAdapter = new SDFImportAdapter(actions);
        return sDFImportAdapter;
    }

}
