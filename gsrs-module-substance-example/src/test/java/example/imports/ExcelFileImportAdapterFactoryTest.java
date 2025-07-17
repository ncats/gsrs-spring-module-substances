package example.imports;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import example.GsrsModuleSubstanceApplication;
import gsrs.imports.ActionConfig;
import gsrs.imports.ActionConfigImpl;
import gsrs.imports.CodeProcessorFieldImpl;
import gsrs.imports.ImportAdapter;
import gsrs.module.substance.importers.*;
import gsrs.module.substance.importers.importActionFactories.*;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
public class ExcelFileImportAdapterFactoryTest extends AbstractSubstanceJpaFullStackEntityTest {

/*
Confirm ability to read data
*/
    @Test
    public void testParse() throws IOException {
        List<ActionConfig> simpleConfig = new ArrayList<>();

        ActionConfigImpl proteinActionConfig = new ActionConfigImpl();
        proteinActionConfig.setActionClass(ProteinSequenceExtractorActionFactory.class);
        proteinActionConfig.setActionName("protein_import");
        List<CodeProcessorFieldImpl> fields = new ArrayList<>();
        CodeProcessorFieldImpl codeProcessorField = new CodeProcessorFieldImpl();
        codeProcessorField.setFieldName("proteinSequence");
        codeProcessorField.setRequired(true);
        codeProcessorField.setFieldLabel("Protein Sequence Field");
        codeProcessorField.setFieldType(String.class);
        codeProcessorField.setExpectedToChange(true);
        codeProcessorField.setDefaultValue("PROTEIN_SEQUENCE");
        fields.add(codeProcessorField);
        proteinActionConfig.setFields(fields);
        Map<String, Object> actionParameters = new HashMap<>();
        actionParameters.put("subunitDelimiter", "\\|");
        proteinActionConfig.setParameters(actionParameters);
        simpleConfig.add(proteinActionConfig);

        ActionConfigImpl nameActionConfig = new ActionConfigImpl();
        nameActionConfig.setActionClass(NameExtractorActionFactory.class);
        nameActionConfig.setActionName("common_name");
        List<CodeProcessorFieldImpl> fieldsName = new ArrayList<>();
        CodeProcessorFieldImpl nameField = new CodeProcessorFieldImpl();
        nameField.setFieldName("name");
        nameField.setRequired(true);
        nameField.setFieldLabel("Name");
        nameField.setFieldType(String.class);
        nameField.setExpectedToChange(true);
        fieldsName.add(nameField);

        CodeProcessorFieldImpl nameTypeField = new CodeProcessorFieldImpl();
        nameTypeField.setFieldName("nameType");
        nameTypeField.setRequired(true);
        nameTypeField.setFieldLabel("Name Type");
        nameTypeField.setFieldType(String.class);
        nameTypeField.setExpectedToChange(true);
        nameTypeField.setDefaultValue("cn");
        fieldsName.add(nameTypeField);
        nameActionConfig.setFields(fieldsName);
        simpleConfig.add(nameActionConfig);

        ActionConfigImpl rnCodeActionConfig = new ActionConfigImpl();
        rnCodeActionConfig.setActionClass(CodeExtractorActionFactory.class);
        rnCodeActionConfig.setActionName("cas_code");
        List<CodeProcessorFieldImpl> fieldsRn = new ArrayList<>();
        CodeProcessorFieldImpl rnField = new CodeProcessorFieldImpl();
        rnField.setFieldName("code");
        rnField.setRequired(true);
        rnField.setFieldLabel("Code");
        rnField.setFieldType(String.class);
        rnField.setExpectedToChange(true);
        fieldsRn.add(rnField);

        CodeProcessorFieldImpl rnTypeField = new CodeProcessorFieldImpl();
        rnTypeField.setFieldName("codeType");
        rnTypeField.setRequired(false);
        rnTypeField.setFieldLabel("Code Type");
        rnTypeField.setFieldType(String.class);
        rnTypeField.setExpectedToChange(false);
        rnTypeField.setDefaultValue("PRIMARY");
        fieldsRn.add(rnTypeField);

        CodeProcessorFieldImpl rnSystemField = new CodeProcessorFieldImpl();
        rnSystemField.setFieldName("codeSystem");
        rnSystemField.setRequired(true);
        rnSystemField.setFieldLabel("Code System");
        rnSystemField.setFieldType(String.class);
        rnSystemField.setExpectedToChange(false);
        rnSystemField.setDefaultValue("CAS");
        fieldsRn.add(rnSystemField);
        rnCodeActionConfig.setFields(fieldsRn);
        simpleConfig.add(rnCodeActionConfig);

        ActionConfigImpl referenceActionConfig = new ActionConfigImpl();
        referenceActionConfig.setActionClass(ReferenceExtractorActionFactory.class);
        referenceActionConfig.setActionName("public_reference");
        List<CodeProcessorFieldImpl> fieldsRef = new ArrayList<>();
        CodeProcessorFieldImpl publicDomainField = new CodeProcessorFieldImpl();
        publicDomainField.setDefaultValue("true");
        publicDomainField.setExpectedToChange(false);
        publicDomainField.setFieldType(Boolean.class);
        publicDomainField.setFieldLabel("Public?");
        publicDomainField.setFieldName("publicDomain");
        fieldsRef.add(publicDomainField);
        CodeProcessorFieldImpl citationField = new CodeProcessorFieldImpl();
        citationField.setDefaultValue(SDFImportAdapterFactory.REFERENCE_INSTRUCTION);
        citationField.setExpectedToChange(true);
        citationField.setFieldType(String.class);
        citationField.setFieldLabel("Citation");
        citationField.setFieldName("citation");
        fieldsRef.add(citationField);

        CodeProcessorFieldImpl docTypeField = new CodeProcessorFieldImpl();
        docTypeField.setDefaultValue("wikipedia");
        docTypeField.setExpectedToChange(true);
        docTypeField.setFieldType(String.class);
        docTypeField.setFieldLabel("docType");
        docTypeField.setFieldName("docType");
        fieldsRef.add(docTypeField);
        referenceActionConfig.setFields(fieldsRef);
        simpleConfig.add(referenceActionConfig);

        ExcelFileImportAdapterFactory factory = new ExcelFileImportAdapterFactory();
        factory.setFileImportActions(simpleConfig);
        factory.initialize();

        ArrayNode actionListNode = JsonNodeFactory.instance.arrayNode();
        ObjectNode actionNode = JsonNodeFactory.instance.objectNode();
        TextNode actionNameNode = JsonNodeFactory.instance.textNode("protein_import");
        actionNode.set("actionName", actionNameNode);
        ObjectNode adapter1Parameters = JsonNodeFactory.instance.objectNode();
        adapter1Parameters.put("proteinSequence","{{PROTEIN_SEQUENCE}}");
        actionNode.set("actionParameters", adapter1Parameters);
        actionListNode.add(actionNode);

        ObjectNode nameNode = JsonNodeFactory.instance.objectNode();
        TextNode nameActionNameNode = JsonNodeFactory.instance.textNode("common_name");
        nameNode.set("actionName", nameActionNameNode);
        ObjectNode adapter2Parameters = JsonNodeFactory.instance.objectNode();
        adapter2Parameters.put("name","{{DISPLAY_NAME}}");
        adapter2Parameters.put("displayName", true);
        adapter2Parameters.put("referenceUUIDs", "[[UUID_1]]");
        nameNode.set("actionParameters", adapter2Parameters);
        actionListNode.add(nameNode);

        ObjectNode rnNode = JsonNodeFactory.instance.objectNode();
        TextNode rnActionNameNode = JsonNodeFactory.instance.textNode("cas_code");
        rnNode.set("actionName", rnActionNameNode);
        ObjectNode adapterRn = JsonNodeFactory.instance.objectNode();
        adapterRn.put("code","{{RN}}");
        adapterRn.put("codeSystem","CAS");
        adapterRn.put("codeType","PRIMARY");
        rnNode.set("actionParameters", adapterRn);
        actionListNode.add(rnNode);

        ObjectNode refNode = JsonNodeFactory.instance.objectNode();
        TextNode refActionNameNode = JsonNodeFactory.instance.textNode("public_reference");
        refNode.set("actionName", refActionNameNode);
        refNode.set("actionClass", JsonNodeFactory.instance.textNode("gsrs.module.substance.importers.importActionFactories.ReferenceExtractorActionFactory"));
        ObjectNode adapterRef = JsonNodeFactory.instance.objectNode();
        adapterRef.put("docType","CATALOG");
        adapterRef.put("citation","INSERT REFERENCE CITATION HERE");
        adapterRef.put("referenceID", "INSERT REFERENCE ID HERE");
        adapterRef.put("uuid", "[[UUID_1]]");
        refNode.set("actionParameters", adapterRef);
        actionListNode.add(refNode);

        ObjectNode adapterSettings = JsonNodeFactory.instance.objectNode();
        adapterSettings.set("actions", actionListNode);
        ObjectNode generalParameters = JsonNodeFactory.instance.objectNode();
        generalParameters.put("substanceClassName", "Protein");
        generalParameters.put("dataSheetName", "Sheet0");
        generalParameters.put("fieldRow", 0);
        adapterSettings.set("parameters", generalParameters);

        ImportAdapter<Substance> importAdapter= factory.createAdapter(adapterSettings);
        Assertions.assertTrue(importAdapter instanceof ExcelFileImportAdapter);
        ExcelFileImportAdapter excelFileImportAdapter = (ExcelFileImportAdapter) importAdapter;

        String fileName = "testExcelFile/export-03-01-2023_19-45-32.xlsx";
        File dataFile = new ClassPathResource(fileName).getFile();
        log.trace("using dataFile.getAbsoluteFile(): " + dataFile.getAbsoluteFile());
        InputStream fis = Files.newInputStream(dataFile.getAbsoluteFile().toPath());
        String fileEncoding = "UTF-8";
        ObjectNode settingsNode = JsonNodeFactory.instance.objectNode();
        settingsNode.put("Encoding", fileEncoding);
        settingsNode.put("dataSheetName", "Sheet0");
        Stream<Substance> substanceBuilderStream= excelFileImportAdapter.parse(fis, settingsNode, null);
        List<ProteinSubstance> proteinSubstances = substanceBuilderStream
                .map(p->((ProteinSubstance)p))
                .collect(Collectors.toList());
        Assertions.assertTrue(proteinSubstances.stream().anyMatch(p->p.names.get(0).name.equals("D-ALANINE AMINOTRANSFERASE (STAPHYLOCOCCUS EPIDERMIDIS (STRAIN ATCC 12228))")
                && p.protein.subunits.stream().anyMatch(s->s.sequence.equals("MTKVFINGEFVNEEDAKVSYEDRGYVFGDGIYEYIRAYDGKLFTVKEHFERFLRSAEEIGLDLNYTIEELIELVRRLLKENNVVNGGIYIQATRGAAPRNHSFPTPPVKPVIMAFTKSYDRPYEELEQGVYAITTEDIRWLRCDIKSLNLLGNVLAKEYAVKYNAAEAIQHRGDIVTEGASSNVYAIKDGVIYTHPVNNFILNGITRRVIKWIAEDEQIPFKEEKFTVEFLKSADEVIISSTSAEVMPITKIDGENVQDGQVGTITRQLQQGFEKYIQSHSI"))
                ));
        Assertions.assertTrue(proteinSubstances.stream().anyMatch(p->p.protein.subunits.stream().anyMatch(s->s.sequence.equals("EVQLVESGGGLVQPGGSLRLSCAASGFNIKDTYIHWVRQAPGKGLEWVARIYPTNGYTRYADSVKGRFTISADTSKNTAYLQMNSLRAEDTAVYYCSRWGGDGFYAMDYWGQGTLVTVSSXSTKGPSVFPLAPSSKSTSGGTAALGCLVKDYFPEPVTVSWNSGALTSGVHTFPAVLQSSGLYSLSSVVTVPSSSLGTQTYICNVNHKPSNTKVDKKVEPKSCDKTHTCPPCPAPELLGGPSVFLFPPKPKDTLMISRTPEVTCVVVDVSHEDPEVKFNWYVDGVEVHNAKTKPREEQYNSTYRVVSVLTVLHQDWLNGKEYKCKVSNKALPAPIEKTISKAKGQPREPQVYTLPPSRDELTKNQVSLTCLVKGFYPSDIAVEWESNGQPENNYKTTPPVLDSDGSFFLYSKLTVDKSRWQQGNVFSCSVMHEALHNHYTQKSLSLSPG")) &&
                p.protein.subunits.stream().anyMatch(s->s.sequence.equals("DIQMTQSPSSLSASVGDRVTITCRASQDVNTAVAWYQQKPGKAPKLLIYSASFLYSGVPSRFSGSRSGTDFTLTISSLQPEDFATYYCQQHYTTPPTFGQGTKVEIKRTVAAPSVFIFPPSDEQLKSGTASVVCLLNNFYPREAKVQWKVDNALQSGNSQESVTEQDSKDSTYSLSSTLTLSKADYEKHKVYACEVTHQGLSSPVTKSFNRGEC"))));
        Assertions.assertEquals(3, proteinSubstances.size());
    }

    @Test
    public void testParseSmiles() throws IOException {
        ObjectNode adapterSettings = JsonNodeFactory.instance.objectNode();
        ObjectNode generalParameters = JsonNodeFactory.instance.objectNode();
        generalParameters.put("substanceClassName", "Chemical");
        generalParameters.put("dataSheetName", "Sheet0");
        generalParameters.put("fieldRow", 0);
        generalParameters.put("lineValueDelimiter", "\t");
        adapterSettings.set("parameters", generalParameters);
        ArrayNode actionListNode = JsonNodeFactory.instance.arrayNode();

        List<ActionConfig> simpleConfig = new ArrayList<>();
        ActionConfig idCodeConfig = new ActionConfigImpl();
        idCodeConfig.setActionClass(CodeExtractorActionFactory.class);
        idCodeConfig.setActionName("pubchem_code");
        CodeProcessorFieldImpl idField = new CodeProcessorFieldImpl();
        idField.setFieldName("code");
        idField.setRequired(true);
        idField.setFieldLabel("Code");
        idField.setFieldType(String.class);
        idField.setExpectedToChange(true);
        idCodeConfig.setFields(Collections.singletonList(idField));
        simpleConfig.add(idCodeConfig);

        ActionConfig structureFieldActionConfig = new ActionConfigImpl();
        structureFieldActionConfig.setActionClass(StructureExtractorActionFactory.class);
        structureFieldActionConfig.setActionName("structure_and_moieties_from_text");
        simpleConfig.add(structureFieldActionConfig);

        ObjectNode structureActionConfig = JsonNodeFactory.instance.objectNode();
        structureActionConfig.put("actionClass", StructureExtractorActionFactory.class.getName());
        structureActionConfig.put("actionName", "structure_and_moieties_from_text");
        ObjectNode structureActionParameters = JsonNodeFactory.instance.objectNode();
        structureActionParameters.put("smiles","{{PUBCHEM_OPENEYE_CAN_SMILES}}");

        CodeProcessorFieldImpl structureField = new CodeProcessorFieldImpl();
        structureField.setFieldName("smiles");
        structureField.setRequired(true);
        structureField.setFieldLabel("SMILES");
        structureField.setFieldType(String.class);
        structureField.setExpectedToChange(true);
        ObjectNode structureFieldNode = JsonNodeFactory.instance.objectNode();
        structureFieldNode.put("fieldName", "smiles");
        structureFieldNode.put("required", true);
        structureFieldNode.put("fieldLabel", "SMILES");
        structureFieldNode.put("fieldType", "String");
        structureFieldNode.put("expectedToChange", true);

        ArrayNode fieldList = JsonNodeFactory.instance.arrayNode();
        fieldList.add(structureFieldNode);
        structureActionConfig.set("fields", fieldList);
        structureActionConfig.set("actionParameters", structureActionParameters);
        actionListNode.add(structureActionConfig);
        ObjectNode idActionFields = JsonNodeFactory.instance.objectNode();
        idActionFields.put("code","{{id}}");
        idActionFields.put("codeSystem", "pubchem");
        idActionFields.put("codeType", "PRIMARY");
        ObjectNode idConfigNode = JsonNodeFactory.instance.objectNode();
        idConfigNode.put("fields", idActionFields);
        idConfigNode.put("actionName", "pubchem_code");
        idConfigNode.put("actionClass", CodeExtractorActionFactory.class.getName());
        ObjectNode idActionParameters = JsonNodeFactory.instance.objectNode();
        idActionParameters.put("code","{{id}}");
        idActionParameters.put("codeSystem","pubchem");
        idActionParameters.put("codeType","PRIMARY");
        idConfigNode.put("actionParameters", idActionParameters);
        actionListNode.add(idConfigNode);
        List<CodeProcessorFieldImpl> fieldsRn = new ArrayList<>();
        CodeProcessorFieldImpl rnField = new CodeProcessorFieldImpl();
        fieldsRn.add(rnField);
        ChemicalDelimTextImportAdapterFactory factory = new ChemicalDelimTextImportAdapterFactory();
        factory.setFileImportActions(simpleConfig);
        factory.initialize();
        adapterSettings.set("actions", actionListNode);
        factory.setFileImportActions(simpleConfig);
        ImportAdapter<Substance> importAdapter= factory.createAdapter(adapterSettings);
        ChemicalDelimTextImportAdapter testFieldAdapter = (ChemicalDelimTextImportAdapter) importAdapter;

        String delim = "\t";
        String testData = "id\tPUBCHEM_OPENEYE_CAN_SMILES\n137695\tCOC1=CC(=CC=C1)[Se]C";
        InputStream inputStream = new ByteArrayInputStream(testData.getBytes());
        ObjectNode settingsNode = JsonNodeFactory.instance.objectNode();
        String fileEncoding = "UTF-8";
        settingsNode.put("Encoding", fileEncoding);
        settingsNode.put("dataSheetName", "Sheet0");

        Stream<Substance> chemStream = testFieldAdapter.parse(inputStream, settingsNode,  null);
        ChemicalSubstance result = (ChemicalSubstance) chemStream.findFirst().get();
        Assertions.assertEquals("C8H10OSe", result.toChemical().getFormula());
        Assertions.assertEquals("137695", result.codes.get(0).code);
    }

    @Test
    public void testParseSmilesWithBlankLine() throws IOException {
        ObjectNode adapterSettings = JsonNodeFactory.instance.objectNode();
        ObjectNode generalParameters = JsonNodeFactory.instance.objectNode();
        generalParameters.put("substanceClassName", "Chemical");
        generalParameters.put("dataSheetName", "Sheet0");
        generalParameters.put("fieldRow", 0);
        generalParameters.put("lineValueDelimiter", "\t");
        adapterSettings.set("parameters", generalParameters);
        ArrayNode actionListNode = JsonNodeFactory.instance.arrayNode();

        List<ActionConfig> simpleConfig = new ArrayList<>();
        ActionConfig idCodeConfig = new ActionConfigImpl();
        idCodeConfig.setActionClass(CodeExtractorActionFactory.class);
        idCodeConfig.setActionName("pubchem_code");
        CodeProcessorFieldImpl idField = new CodeProcessorFieldImpl();
        idField.setFieldName("code");
        idField.setRequired(true);
        idField.setFieldLabel("Code");
        idField.setFieldType(String.class);
        idField.setExpectedToChange(true);
        idCodeConfig.setFields(Collections.singletonList(idField));
        simpleConfig.add(idCodeConfig);

        ActionConfig structureFieldActionConfig = new ActionConfigImpl();
        structureFieldActionConfig.setActionClass(StructureExtractorActionFactory.class);
        structureFieldActionConfig.setActionName("structure_and_moieties_from_text");
        simpleConfig.add(structureFieldActionConfig);

        ObjectNode structureActionConfig = JsonNodeFactory.instance.objectNode();
        structureActionConfig.put("actionClass", StructureExtractorActionFactory.class.getName());
        structureActionConfig.put("actionName", "structure_and_moieties_from_text");
        ObjectNode structureActionParameters = JsonNodeFactory.instance.objectNode();
        structureActionParameters.put("smiles","{{PUBCHEM_OPENEYE_CAN_SMILES}}");

        CodeProcessorFieldImpl structureField = new CodeProcessorFieldImpl();
        structureField.setFieldName("smiles");
        structureField.setRequired(true);
        structureField.setFieldLabel("SMILES");
        structureField.setFieldType(String.class);
        structureField.setExpectedToChange(true);
        ObjectNode structureFieldNode = JsonNodeFactory.instance.objectNode();
        structureFieldNode.put("fieldName", "smiles");
        structureFieldNode.put("required", true);
        structureFieldNode.put("fieldLabel", "SMILES");
        structureFieldNode.put("fieldType", "String");
        structureFieldNode.put("expectedToChange", true);

        ArrayNode fieldList = JsonNodeFactory.instance.arrayNode();
        fieldList.add(structureFieldNode);
        structureActionConfig.set("fields", fieldList);
        structureActionConfig.set("actionParameters", structureActionParameters);
        actionListNode.add(structureActionConfig);
        ObjectNode idActionFields = JsonNodeFactory.instance.objectNode();
        idActionFields.put("code","{{id}}");
        idActionFields.put("codeSystem", "pubchem");
        idActionFields.put("codeType", "PRIMARY");
        ObjectNode idConfigNode = JsonNodeFactory.instance.objectNode();
        idConfigNode.put("fields", idActionFields);
        idConfigNode.put("actionName", "pubchem_code");
        idConfigNode.put("actionClass", CodeExtractorActionFactory.class.getName());
        ObjectNode idActionParameters = JsonNodeFactory.instance.objectNode();
        idActionParameters.put("code","{{id}}");
        idActionParameters.put("codeSystem","pubchem");
        idActionParameters.put("codeType","PRIMARY");
        idConfigNode.put("actionParameters", idActionParameters);
        actionListNode.add(idConfigNode);
        List<CodeProcessorFieldImpl> fieldsRn = new ArrayList<>();
        CodeProcessorFieldImpl rnField = new CodeProcessorFieldImpl();
        fieldsRn.add(rnField);
        ChemicalDelimTextImportAdapterFactory factory = new ChemicalDelimTextImportAdapterFactory();
        factory.setFileImportActions(simpleConfig);
        factory.initialize();
        adapterSettings.set("actions", actionListNode);
        factory.setFileImportActions(simpleConfig);
        ImportAdapter<Substance> importAdapter= factory.createAdapter(adapterSettings);
        ChemicalDelimTextImportAdapter testFieldAdapter = (ChemicalDelimTextImportAdapter) importAdapter;

        String delim = "\t";
        String testData = "id\tPUBCHEM_OPENEYE_CAN_SMILES\n\n137695\tCOC1=CC(=CC=C1)[Se]C";
        InputStream inputStream = new ByteArrayInputStream(testData.getBytes());
        ObjectNode settingsNode = JsonNodeFactory.instance.objectNode();
        String fileEncoding = "UTF-8";
        settingsNode.put("Encoding", fileEncoding);
        settingsNode.put("dataSheetName", "Sheet0");

        Stream<Substance> chemStream = testFieldAdapter.parse(inputStream, settingsNode,  null);
        ChemicalSubstance result = (ChemicalSubstance) chemStream.findFirst().get();
        Assertions.assertEquals("C8H10OSe", result.toChemical().getFormula());
        Assertions.assertEquals("137695", result.codes.get(0).code);
    }

}
