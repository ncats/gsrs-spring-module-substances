package gsrs.module.substance.importers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.dataExchange.model.MappingAction;
import gsrs.dataExchange.model.MappingActionFactory;
import gsrs.module.substance.importers.importActionFactories.*;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;
import java.util.*;

@SpringBootConfiguration
@Configuration
@PropertySource("application.conf")
@Slf4j
public class NSRSSDFImportAdapterFactory extends SDFImportAdaptorFactory {
    @Override
    public String getAdapterName() {
        return "NSRS SDF Adapter";
    }

    @Value("${nsrs.sdfActions:blah")
    private String adapterConfigInit;

    @Value("#{${ix.gsrs.sdfActions}}")
    protected Map<String, String> defaultImportActions;

    @SneakyThrows
    @Override
    public AbstractImportSupportingGsrsEntityController.ImportAdapter<Substance> createAdapter(JsonNode adapterSettings) {
        List<MappingAction<Substance, SDRecordContext>> actions = getMappingActions(adapterSettings);
        AbstractImportSupportingGsrsEntityController.ImportAdapter sDFImportAdapter = new SDFImportAdapter(actions);
        return sDFImportAdapter;
    }

    @PostConstruct
    public void init(){
        fileImportActions =this.defaultImportActions;
        log.trace("fileImportActions: " + fileImportActions);
        log.trace("adapterConfigInit: " + adapterConfigInit);

        registry.clear();
        if(fileImportActions !=null && fileImportActions.size() >0) {
            Map<String, Object> params =  Collections.emptyMap();
            ObjectMapper mapper = new ObjectMapper();
            Set<String> actionNames=fileImportActions.keySet();
            actionNames.forEach(actionName->{
                try {
                    log.trace("processing actionName: " +actionName);
                    if(actionName.contains(":")) {
                        String paramsInner = actionName.split("\\_")[1];
                        actionName = actionName.split("\\_")[0];
                    }
                    MappingActionFactory<Substance, SDRecordContext> mappingActionFactory =
                            (MappingActionFactory<Substance, SDRecordContext>) mapper.convertValue(params,
                                    Class.forName( fileImportActions.get(actionName)));
                    registry.put(actionName, mappingActionFactory);
                    log.trace(String.format("added action %s as class with name %s", actionName, fileImportActions.get(actionName)));
                } catch (ClassNotFoundException e) {
                    log.error("error instantiating class.  message: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
        else {
            log.trace("using NSRS default actions");
            registry.put("sample_name", new NSRSSampleNameExtractorActionFactory());

            String[] casParams = {
                    "CASNumber`CAS Number`java.lang.String`true",
                    "codeType`Primary or Alternative`java.lang.String`false`PRIMARY",
                    "url`CAS Number URL`java.lang.String`false`https://commonchemistry.cas.org/detail?cas_rn="
            };
            registry.put("cas_import", new NSRSCustomCodeExtractorActionFactory(casParams));

            String[] supplierParams = {
                    "supplier`Supplier`java.lang.String`true",
                    "codeType`Primary or Alternative`java.lang.String`false`PRIMARY",
                    "url`Supplier URL`Java.lang.String`false"
            };
            registry.put("supplier_import", new NSRSCustomCodeExtractorActionFactory(supplierParams));

            String[] supplierIdParams = {
                    "supplierID`Supplier ID`java.lang.String`true",
                    "codeType`Primary or Alternative`java.lang.String`false`PRIMARY"
            };
            registry.put("supplierid_import", new NSRSCustomCodeExtractorActionFactory(supplierIdParams));

            String[] saltCodeParams = {
                    "saltCode`Salt Code`java.lang.String`true",
                    "codeType`Primary or Alternative`java.lang.String`false`PRIMARY"
            };
            registry.put("salt_code_import", new NSRSCustomCodeExtractorActionFactory(saltCodeParams));

            String[] saltEquivParams = {
                    "saltEquivalents`Salt Equivalents`java.lang.String`true",
                    "codeType`Primary or Alternative`java.lang.String`false`PRIMARY"
            };
            registry.put("salt_equiv_import", new NSRSCustomCodeExtractorActionFactory(saltEquivParams));

            //This will add a boolean flag labelled "Internally Synthesized" in the UI that will map
            // to a code of system _internal_external that will be used later on in generating a permanent ID
            String[] prefixParams = {
                    "codeValue`Internally Synthesized`java.lang.Boolean`true",
                    "codeSystem`codeSystem`java.lang.String``false`internal_external`false",
                    "codeType`Primary or Alternative`java.lang.String`false`PRIMARY`false"
            };
            registry.put("internal_external", new NSRSCustomCodeExtractorActionFactory(prefixParams));

            registry.put("structure_and_moieties", new StructureExtractorActionFactory());
            /*
            we probably do NOT want notes within SD file imports.... keeping it commented out in case there's a need.
             */
            //registry.put("note_import", new NotesExtractorActionFactory());
            /*
            we probably do NOT want properties within SD file imports.... keeping it commented out in case there's a need.
             */
            //registry.put("property_import", new PropertyExtractorActionFactory());
            registry.put(SIMPLE_REFERENCE_ACTION, new ReferenceExtractorActionFactory());
        }
    }

}
