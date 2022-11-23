package gsrs.module.substance.importers;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.dataexchange.model.MappingAction;
import gsrs.imports.ImportAdapter;
import gsrs.module.substance.importers.importActionFactories.*;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SpringBootConfiguration
@Configuration
@PropertySource("application.conf")
@Slf4j

public class NSRSSDFImportAdapterFactory extends SDFImportAdapterFactory {
    @Override
    public String getAdapterName() {
        return "NSRS SDF Adapter";
    }

    @Override
    public String getAdapterKey() {
        return "SDF-NSRS";
    }


    //private String adapterConfigInit;

    protected List<Map<String, Object>> defaultImportActions;

    @SneakyThrows
    @Override
    public ImportAdapter<Substance> createAdapter(JsonNode adapterSettings) {
        List<MappingAction<Substance, SDRecordContext>> actions = getMappingActions(adapterSettings);
        ImportAdapter sDFImportAdapter = new SDFImportAdapter(actions);
        return sDFImportAdapter;
    }

    @Override
    protected void defaultInitialize() {
        log.trace("using NSRS default actions");
        registry.put("sample_name", new NSRSSampleNameExtractorActionFactory());
        /*String[] casParams = {
                "CASNumber`CAS Number`java.lang.String`true",
                "codeType`Primary or Alternative`java.lang.String`false`PRIMARY",
                "url`CAS Number URL`java.lang.String`false`https://commonchemistry.cas.org/detail?cas_rn="
        };*/
        List<Map<String, Object>> casFields = new ArrayList<>();
        Map<String, Object> casCodeField = new HashMap<>();
        casCodeField.put("fieldName", "code");
        casCodeField.put("fieldLabel", "CAS Number");
        casCodeField.put("valueType", "java.lang.String");
        casCodeField.put("required", "true");
        casFields.add(casCodeField);
        
        Map<String, Object> casCodeTypeField = new HashMap<>();
        casCodeTypeField.put("fieldName", "codeType");
        casCodeTypeField.put("label", "Primary or Alternative");
        casCodeTypeField.put("valueType", "java.lang.String");
        casCodeTypeField.put("required", true);
        casCodeTypeField.put("expectedToChange", false);
        casCodeTypeField.put("defaultValue", "PRIMARY");
        casFields.add(casCodeTypeField);

        Map<String, Object> casUrlField = new HashMap<>();
        casUrlField.put("fieldName", "url");
        casUrlField.put("label", "URL");
        casUrlField.put("valueType", "java.lang.String");
        casUrlField.put("required", false);
        casUrlField.put("expectedToChange", false);
        casUrlField.put("defaultValue", "https://commonchemistry.cas.org/detail?cas_rn=");
        casFields.add(casUrlField);

        NSRSCustomCodeExtractorActionFactory casCodeExtractorActionFactory = new NSRSCustomCodeExtractorActionFactory();
        casCodeExtractorActionFactory.setFields(casFields);
        registry.put("cas_import", casCodeExtractorActionFactory);

        /*String[] supplierParams = {
                "supplier`Supplier`java.lang.String`true",
                "codeType`Primary or Alternative`java.lang.String`false`PRIMARY",
                "url`Supplier URL`Java.lang.String`false"
        };*/
        List<Map<String, Object>> supplierFields = new ArrayList<>();
        Map<String, Object> supplierCodeField = new HashMap<>();
        supplierCodeField.put("fieldName", "code");
        supplierCodeField.put("label", "Supplier");
        supplierCodeField.put("valueType", "java.lang.String");
        supplierCodeField.put("required", true);
        supplierFields.add(supplierCodeField);

        Map<String, Object> supplierCodeTypeField = new HashMap<>();
        supplierCodeTypeField.put("fieldName", "codeType");
        supplierCodeTypeField.put("label", "Primary or Alternative");
        supplierCodeTypeField.put("valueType", "java.lang.String");
        supplierCodeTypeField.put("required", false);
        supplierCodeTypeField.put("expectedToChange", false);
        supplierCodeTypeField.put("defaultValue", "PRIMARY");
        supplierFields.add(supplierCodeTypeField);

        Map<String, Object> supplierUrlField = new HashMap<>();
        supplierUrlField .put("fieldName", "url");
        supplierUrlField .put("label", "URL");
        supplierUrlField .put("valueType", "java.lang.String");
        supplierUrlField .put("required", false);
        supplierFields.add(supplierUrlField);

        NSRSCustomCodeExtractorActionFactory supplierCodeExtractorActionFactory = new NSRSCustomCodeExtractorActionFactory();
        supplierCodeExtractorActionFactory.setFields(supplierFields);
        registry.put("supplier_import", supplierCodeExtractorActionFactory);

        /*String[] supplierIdParams = {
                "supplierID`Supplier ID`java.lang.String`true",
                "codeType`Primary or Alternative`java.lang.String`false`PRIMARY"
        };*/
        List<Map<String, Object>> supplierIdFields = new ArrayList<>();
        Map<String, Object> supplierIdCodeField = new HashMap<>();
        supplierIdCodeField.put("fieldName", "code");
        supplierIdCodeField.put("label", "Supplier ID");
        supplierIdCodeField.put("valueType", "java.lang.String");
        supplierIdCodeField.put("required", false);
        supplierIdFields.add(supplierIdCodeField);

        Map<String, Object> supplierIdCodeTypeField = new HashMap<>();
        supplierIdCodeTypeField.put("fieldName", "codeType");
        supplierIdCodeTypeField.put("label", "Primary or Alternative");
        supplierIdCodeTypeField.put("valueType", "java.lang.String");
        supplierIdCodeTypeField.put("required", false);
        supplierIdCodeTypeField.put("defaultValue", "PRIMARY");
        supplierIdCodeTypeField.put("expectedToChange", false);
        supplierIdFields.add(supplierIdCodeTypeField);
        
        Map<String, Object> supplierIdUrlField = new HashMap<>();
        supplierIdUrlField .put("fieldName", "url");
        supplierIdUrlField .put("label", "URL");
        supplierIdUrlField .put("valueType", "java.lang.String");
        supplierIdUrlField .put("required", false);
        supplierIdFields.add(supplierIdUrlField);

        NSRSCustomCodeExtractorActionFactory supplierIdCodeExtractorActionFactory = new NSRSCustomCodeExtractorActionFactory();
        supplierIdCodeExtractorActionFactory.setFields(supplierIdFields);
        registry.put("supplierid_import", supplierIdCodeExtractorActionFactory);

        /*String[] saltCodeParams = {
                "saltCode`Salt Code`java.lang.String`true",
                "codeType`Primary or Alternative`java.lang.String`false`PRIMARY"
        };*/
        List<Map<String, Object>> saltCodeFields = new ArrayList<>();
        Map<String, Object> saltCodeCodeField = new HashMap<>();
        saltCodeCodeField.put("fieldCode", "code");
        saltCodeCodeField.put("label", "Salt Code");
        saltCodeCodeField.put("valueType", "java.lang.String");
        saltCodeCodeField.put("required", true);
        saltCodeFields.add(saltCodeCodeField);

        Map<String, Object> saltCodeCodeTypeField = new HashMap<>();
        saltCodeCodeTypeField.put("fieldName", "codeType");
        saltCodeCodeTypeField.put("label", "Primary or Alternative");
        saltCodeCodeTypeField.put("valueType", "java.lang.String");
        saltCodeCodeTypeField.put("required", false);
        saltCodeCodeTypeField.put("defaultValue", "PRIMARY");
        saltCodeCodeTypeField.put("expectedToChange", false);
        saltCodeFields.add(saltCodeCodeTypeField);

        NSRSCustomCodeExtractorActionFactory saltCodeCodeExtractorActionFactory = new NSRSCustomCodeExtractorActionFactory();
        saltCodeCodeExtractorActionFactory.setFields(saltCodeFields);
        registry.put("salt_code_import", saltCodeCodeExtractorActionFactory);

        /*String[] saltEquivParams = {
                "saltEquivalents`Salt Equivalents`java.lang.String`true",
                "codeType`Primary or Alternative`java.lang.String`false`PRIMARY"
        };*/

        List<Map<String, Object>> saltEquivFields = new ArrayList<>();
        Map<String, Object> saltEquivCodeField = new HashMap<>();
        saltEquivCodeField.put("fieldCode", "code");
        saltEquivCodeField.put("label", "Salt Equivalents");
        saltEquivCodeField.put("valueType", "java.lang.Double");
        saltEquivCodeField.put("required", false);
        saltEquivFields.add(saltEquivCodeField);

        Map<String, Object> saltEquivCodeTypeField = new HashMap<>();
        saltEquivCodeTypeField.put("fieldName", "codeType");
        saltEquivCodeTypeField.put("label", "Primary or Alternative");
        saltEquivCodeTypeField.put("valueType", "java.lang.String");
        saltEquivCodeTypeField.put("required", false);
        saltEquivCodeTypeField.put("defaultValue", "PRIMARY");
        saltEquivCodeTypeField.put("expectedToChange", false);
        saltEquivFields.add(saltEquivCodeTypeField);

        NSRSCustomCodeExtractorActionFactory saltEquivCodeExtractorActionFactory = new NSRSCustomCodeExtractorActionFactory();
        saltEquivCodeExtractorActionFactory.setFields(saltEquivFields);
        registry.put("salt_equiv_import", saltEquivCodeExtractorActionFactory);


        //This will add a boolean flag labelled "Internally Synthesized" in the UI that will map
        // to a code of system _internal_external that will be used later on in generating a permanent ID
        /*String[] prefixParams = {
                "codeValue`Internally Synthesized`java.lang.Boolean`true",
                "codeSystem`codeSystem`java.lang.String``false`internal_external`false",
                "codeType`Primary or Alternative`java.lang.String`false`PRIMARY`false"
        };*/
        List<Map<String, Object>> intExtFields = new ArrayList<>();
        Map<String, Object> valueField= new HashMap<>();
        valueField.put("fieldName", "code");
        valueField.put("label", "Internally Synthesized");
        valueField.put("valueType", "java.lang.Boolean");
        valueField.put("required", true);
        intExtFields.add(valueField);

        Map<String, Object> systemField= new HashMap<>();
        systemField.put("fieldName", "codeSystem");
        systemField.put("label", "codeSystem");
        systemField.put("valueType", "java.lang.String");
        systemField.put("required", true);
        systemField.put("defaultValue", "internal_external");
        intExtFields.add(systemField);

        Map<String, Object> codeTypeField= new HashMap<>();
        codeTypeField.put("fieldName", "codeType");
        codeTypeField.put("label", "Primary or Alternative");
        codeTypeField.put("valueType", "java.lang.String");
        codeTypeField.put("required", true);
        codeTypeField.put("expectedToChange", false);
        codeTypeField.put("defaultValue", "PRIMARY");
        intExtFields.add(codeTypeField);

        NSRSCustomCodeExtractorActionFactory intExtActionFactory= new NSRSCustomCodeExtractorActionFactory();
        intExtActionFactory.setFields(intExtFields);
        registry.put("internal_external", intExtActionFactory);


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
