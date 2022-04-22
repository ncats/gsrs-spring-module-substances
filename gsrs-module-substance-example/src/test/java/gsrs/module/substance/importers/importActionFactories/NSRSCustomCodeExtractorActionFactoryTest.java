package gsrs.module.substance.importers.importActionFactories;

import gov.nih.ncats.molwitch.Chemical;
import gsrs.dataExchange.model.MappingAction;
import gsrs.dataExchange.model.MappingActionFactoryMetadata;
import gsrs.module.substance.importers.model.ChemicalBackedSDRecordContext;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class NSRSCustomCodeExtractorActionFactoryTest {

    @Test
    void createForCasTest() throws Exception {
        String acetoneCas = "67-64-1";
        Map<String, Object> abstractParams = new HashMap<>();
        abstractParams.put("code", acetoneCas);
        abstractParams.put("codeType","PRIMARY");
        NSRSCustomCodeExtractorActionFactory factory = new NSRSCustomCodeExtractorActionFactory();
        factory.setActionName("Generic code creator");
        factory.setCodeSystem("CAS");
        factory.setActionName("CASCodeCreator");
        //factory.setCodeSystemLabel("CAS Number");
        factory.setActionLabel("CAS Number");
        //factory.setCodeValueParameterName("CASNumber");
        /*String[] params = {
                "CASNumber`CAS Number`java.lang.String`true",
                "codeType`Primary or Alternative`java.lang.Integer`false`PRIMARY",
                "url`CAS Number URL`java.lang.String`false`https://commonchemistry.cas.org/detail?cas_rn="
        };*/
        List<Map<String, Object>> casFields = new ArrayList<>();
        Map<String, Object> casCodeField = new HashMap<>();
        casCodeField.put("fieldName", "code");
        casCodeField.put("label", "CAS Number");
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

        factory.setFields(casFields);

        MappingAction<Substance, SDRecordContext> action = factory.create(abstractParams);
        Chemical chem = Chemical.createFromSmilesAndComputeCoordinates("CC(=O)C");
        SDRecordContext record = new ChemicalBackedSDRecordContext(chem);

        ChemicalSubstance test = new ChemicalSubstance();
        Substance newChem =action.act(test, record);
        Assertions.assertTrue(newChem.getCodes().stream().anyMatch(c->c.codeSystem.equals("CAS")
                && c.code.equals(acetoneCas)
                && c.url.equals("https://commonchemistry.cas.org/detail?cas_rn="+acetoneCas)));
    }

    @Test
    void createForSupplierTest() throws Exception {
        String supplierName = "Selleck";
        String supplierCodeSystem="Supplier";
        Map<String, Object> abstractParams = new HashMap<>();
        abstractParams.put("code", supplierName);
        abstractParams.put("codeType","PRIMARY");
        NSRSCustomCodeExtractorActionFactory factory = new NSRSCustomCodeExtractorActionFactory();
        factory.setActionName("Generic code creator");
        factory.setCodeSystem(supplierCodeSystem);
        factory.setActionName("SupplierCreator");
        factory.setActionLabel("Supplier");
        //factory.setActionLabel("Supplier Creator");
        //factory.se("supplier");
        /*String[] params = {
                "supplier`Supplier`java.lang.String`true",
                "codeType`Primary or Alternative`java.lang.String`false`PRIMARY",
                "url`Supplier URL`Java.lang.String`false`https://www.selleckchem.com/"
        };*/
        List<Map<String, Object>> fields = new ArrayList<>();
        Map<String, Object> supplierCodeField = new HashMap<>();
        supplierCodeField.put("fieldName", "code");
        supplierCodeField.put("label", "Supplier");
        supplierCodeField.put("valueType", "java.lang.String");
        supplierCodeField.put("required", true);
        fields.add(supplierCodeField);

        Map<String, Object> supplierCodeTypeField = new HashMap<>();
        supplierCodeTypeField.put("fieldName", "codeType");
        supplierCodeTypeField.put("label", "Primary or Alternative");
        supplierCodeTypeField.put("valueType", "java.lang.String");
        supplierCodeTypeField.put("required", false);
        supplierCodeTypeField.put("expectedToChange", false);
        supplierCodeTypeField.put("defaultValue", "PRIMARY");
        fields.add(supplierCodeTypeField);

        Map<String, Object> supplierCodeUrlField = new HashMap<>();
        supplierCodeUrlField.put("fieldName", "url");
        supplierCodeUrlField.put("label", "URL");
        supplierCodeUrlField.put("valueType", "java.lang.String");
        supplierCodeUrlField.put("required", false);
        supplierCodeUrlField.put("expectedToChange", true);
        supplierCodeUrlField.put("defaultValue", "https://www.selleckchem.com/");
        fields.add(supplierCodeUrlField);
        factory.setFields(fields);

        MappingAction<Substance, SDRecordContext> action = factory.create(abstractParams);
        Chemical chem = Chemical.createFromSmilesAndComputeCoordinates("C(=CC=C1)C(=C1)CCC(C)=O");
        SDRecordContext record = new ChemicalBackedSDRecordContext(chem);

        ChemicalSubstance test = new ChemicalSubstance();
        Substance newChem =action.act(test, record);
        Assertions.assertTrue(newChem.getCodes().stream().anyMatch(c->c.codeSystem.equals(supplierCodeSystem)
                && c.code.equals(supplierName)
                && c.url.equals("https://www.selleckchem.com/")));
    }

    @Test
    void createForSaltCodeTest() throws Exception {
        String saltCodeValue = "hydrochloride";
        String saltCodeCodeSystem="Salt Code";
        Map<String, Object> abstractParams = new HashMap<>();
        abstractParams.put("code", saltCodeValue);
        abstractParams.put("codeType","PRIMARY");
        NSRSCustomCodeExtractorActionFactory factory = new NSRSCustomCodeExtractorActionFactory();
        factory.setActionName("Generic code creator");
        factory.setCodeSystem(saltCodeCodeSystem);
        factory.setActionName("SaltCodeCreator");
        factory.setActionLabel("Salt Code Creator");
        //factory.setCodeValueParameterName("saltCode");
        /*String[] params = {
                "saltCode`Salt Code`java.lang.String`true",
                "codeType`Primary or Alternative`java.lang.String`false`PRIMARY"
        };*/
        List<Map<String, Object>> saltCodeFields = new ArrayList<>();
        Map<String, Object> saltCodeCodeField = new HashMap<>();
        saltCodeCodeField.put("fieldName", "code");
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

        factory.setFields(saltCodeFields);

        MappingAction<Substance, SDRecordContext> action = factory.create(abstractParams);
        Chemical chem = Chemical.createFromSmilesAndComputeCoordinates("C(NCC1)C(N=C2)=C(N2)1.Cl");
        SDRecordContext record = new ChemicalBackedSDRecordContext(chem);

        ChemicalSubstance test = new ChemicalSubstance();
        Substance newChem =action.act(test, record);
        Assertions.assertTrue(newChem.getCodes().stream().anyMatch(c->c.codeSystem.equals(saltCodeCodeSystem) && c.code.equals(saltCodeValue)));
    }

    @Test
    void createForSaltEquivTest() throws Exception {
        String saltEquivalentValue = "2";
        String saltEquivalentCodeSystem="Salt Equivalents";
        Map<String, Object> abstractParams = new HashMap<>();
        abstractParams.put("code", saltEquivalentValue);
        abstractParams.put("codeType","PRIMARY");
        NSRSCustomCodeExtractorActionFactory factory = new NSRSCustomCodeExtractorActionFactory();
        factory.setActionName("Generic code creator");
        factory.setCodeSystem(saltEquivalentCodeSystem);
        factory.setActionName("SaltEquivalentsCreator");
        factory.setActionLabel("Salt Equivalents");
        //factory.setCodeValueParameterName("saltEquivalents");
        String[] params = {
                "saltEquivalents`Salt Equivalents`java.lang.String`true",
                "codeType`Primary or Alternative`java.lang.String`false`PRIMARY"
        };
        List<Map<String, Object>> saltEquivFields = new ArrayList<>();
        Map<String, Object> saltEquivCodeField = new HashMap<>();
        saltEquivCodeField.put("fieldName", "code");
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

        factory.setFields(saltEquivFields);

        MappingAction<Substance, SDRecordContext> action = factory.create(abstractParams);
        Chemical chem = Chemical.createFromSmilesAndComputeCoordinates("C(NCC1)C(N=C2)=C(N2)1.Cl");
        SDRecordContext record = new ChemicalBackedSDRecordContext(chem);

        ChemicalSubstance test = new ChemicalSubstance();
        Substance newChem =action.act(test, record);
        Assertions.assertTrue(newChem.getCodes().stream().anyMatch(c->c.codeSystem.equals(saltEquivalentCodeSystem) && c.code.equals(saltEquivalentValue)
            && c.type.equals("PRIMARY")));
    }

    @Test
    void createForSupplierIDTest() throws Exception {
        String supplierId = "S9376";
        String supplierIdCodeSystem="Supplier ID";
        Map<String, Object> abstractParams = new HashMap<>();
        abstractParams.put("code", supplierId);
        abstractParams.put("codeType","PRIMARY");
        NSRSCustomCodeExtractorActionFactory factory = new NSRSCustomCodeExtractorActionFactory();
        factory.setActionName("Generic code creator");
        factory.setCodeSystem(supplierIdCodeSystem);
        factory.setActionName("SupplierIdCreator");
        factory.setActionLabel("Supplier ID");
        //factory.setCodeValueParameterName("supplierID");
        String[] params = {
                "supplierID`Supplier ID`java.lang.String`true",
                "codeType`Primary or Alternative`java.lang.String`false`PRIMARY"
        };
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

        factory.setFields(supplierIdFields);

        MappingAction<Substance, SDRecordContext> action = factory.create(abstractParams);
        Chemical chem = Chemical.createFromSmilesAndComputeCoordinates("C(=CC=C1)C(=C1)CCC(C)=O");
        SDRecordContext record = new ChemicalBackedSDRecordContext(chem);

        ChemicalSubstance test = new ChemicalSubstance();
        Substance newChem =action.act(test, record);
        Assertions.assertTrue(newChem.getCodes().stream().anyMatch(c->c.codeSystem.equals(supplierIdCodeSystem) && c.code.equals(supplierId)));
    }

    @Test
    void createForNCGCIDTest() throws Exception {
        String supplierId = "S9376";
        String supplierIdCodeSystem="Supplier ID";
        Map<String, Object> abstractParams = new HashMap<>();
        abstractParams.put("code", supplierId);
        abstractParams.put("codeType","PRIMARY");
        NSRSCustomCodeExtractorActionFactory factory = new NSRSCustomCodeExtractorActionFactory();
        factory.setActionName("Generic code creator");
        factory.setCodeSystem(supplierIdCodeSystem);
        factory.setActionName("SupplierIdCreator");
        factory.setActionLabel("Supplier ID");
        //factory.setCodeValueParameterName("supplierID");
        String[] params = {
                "supplierID`Supplier ID`java.lang.String`true",
                "codeType`Primary or Alternative`java.lang.String`false`PRIMARY"
        };
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

        factory.setFields(supplierIdFields);

        MappingAction<Substance, SDRecordContext> action = factory.create(abstractParams);
        Chemical chem = Chemical.createFromSmilesAndComputeCoordinates("C(=CC=C1)C(=C1)CCC(C)=O");
        SDRecordContext record = new ChemicalBackedSDRecordContext(chem);

        ChemicalSubstance test = new ChemicalSubstance();
        Substance newChem =action.act(test, record);
        Assertions.assertTrue(newChem.getCodes().stream().anyMatch(c->c.codeSystem.equals(supplierIdCodeSystem) && c.code.equals(supplierId)));
    }

    @Test
    public void internalOrExternalTest() throws Exception{
        /*String[] prefixParams = {
                "internalOrExternal`Internally Synthesized`java.lang.Boolean`true",
                "codeSystem`codeSystem`java.lang.String`false`internal_external`false",
                "codeType`Primary or Alternative`java.lang.String`false`PRIMARY`false"
        };*/
        List<Map<String, Object>> fields = new ArrayList<>();
        Map<String, Object> codeField = new HashMap<>();
        codeField.put("fieldName", "code");
        codeField.put("label", "Internally Synthesized");
        codeField.put("valueType", "java.lang.Boolean");
        codeField.put("required", true);
        fields.add(codeField);

        Map<String, Object> codeSystemField = new HashMap<>();
        codeSystemField.put("fieldName", "codeSystem");
        codeSystemField.put("expectedToChange", false);
        codeSystemField.put("valueType", "java.lang.String");
        codeSystemField.put("defaultValue", "internal_external");
        fields.add(codeSystemField);

        Map<String, Object> codeTypeField = new HashMap<>();
        codeTypeField.put("fieldName", "codeType");
        codeTypeField.put("label", "Primary or Alternative");
        codeTypeField.put("valueType", "java.lang.String");
        codeTypeField.put("required", false);
        codeTypeField.put("defaultValue", "PRIMARY");
        codeTypeField.put("expectedToChange", false);
        fields.add(codeTypeField);

        NSRSCustomCodeExtractorActionFactory factory = new NSRSCustomCodeExtractorActionFactory();
        factory.setFields(fields);
        factory.setActionName("internal flag generator");
        factory.setCodeSystem("internal_external");
        factory.setActionName("DummyCodeCreator");
        //factory.setCodeSystemLabel("Internal");
        factory.setActionLabel("Dummy");
        //factory.setCodeValueParameterName("internalOrExternal");
        Map<String, Object> abstractParams = new HashMap<>();
        abstractParams.put("code", "FALSE");

        MappingAction<Substance, SDRecordContext> action = factory.create(abstractParams);
        Chemical chem = Chemical.createFromSmilesAndComputeCoordinates("C(=CC=C1)C(=C1)CCC(C)=O");
        SDRecordContext record = new ChemicalBackedSDRecordContext(chem);

        ChemicalSubstance test = new ChemicalSubstance();
        Substance newChem =action.act(test, record);
        Assertions.assertTrue(newChem.codes.stream().anyMatch(c->c.codeSystem.equals("internal_external") && c.code.equalsIgnoreCase("FALSE")));
    }

    @Test
    public void getMetadataTest() {
        NSRSCustomCodeExtractorActionFactory factory = new NSRSCustomCodeExtractorActionFactory();
        factory.setActionName("Dummy code creator");
        factory.setCodeSystem("secret code");
        factory.setActionName("DummyCodeCreator");
        factory.setActionLabel("Dummy");
        String[] params = {
                "Name1`Label1`java.lang.String`true",
                "Count2`Count 2`java.lang.Integer`false`56"
        };
        List<Map<String, Object>> fields= new ArrayList<>();
        Map<String, Object> nameField = new HashMap<>();
        nameField.put("fieldName", "code");
        nameField.put("label", "Label1");
        nameField.put("valueType", "java.lang.String");
        nameField.put("required", true);
        fields.add(nameField);
        Map<String, Object> countField = new HashMap<>();
        countField.put("fieldName", "code");
        countField.put("label", "Count 2");
        countField.put("valueType", "java.lang.Integer");
        countField.put("defaultValue", 56);
        fields.add(countField);

        factory.setFields(fields);
        MappingActionFactoryMetadata mappingMetadata= factory.getMetadata();
        Assertions.assertEquals(2, mappingMetadata.getParameterFields().size());
    }
}