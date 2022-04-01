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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NSRSCustomCodeExtractorActionFactoryTest {

    @Test
    void createForCasTest() throws Exception {
        String acetoneCas = "67-64-1";
        Map<String, Object> abstractParams = new HashMap<>();
        abstractParams.put("CASNumber", acetoneCas);
        abstractParams.put("codeType","PRIMARY");
        NSRSCustomCodeExtractorActionFactory factory = new NSRSCustomCodeExtractorActionFactory();
        factory.setActionName("Generic code creator");
        factory.setCodeSystem("CAS");
        factory.setActionName("CASCodeCreator");
        factory.setCodeSystemLabel("CAS Number");
        factory.setActionLabel("CAS Creator");
        factory.setCodeValueParameterName("CASNumber");
        String[] params = {
                "CASNumber`CAS Number`java.lang.String`true",
                "codeType`Primary or Alternative`java.lang.Integer`false`PRIMARY",
                "url`CAS Number URL`java.lang.String`false`https://commonchemistry.cas.org/detail?cas_rn="
        };
        factory.setParameterInfo(params);

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
        abstractParams.put("supplier", supplierName);
        abstractParams.put("codeType","PRIMARY");
        NSRSCustomCodeExtractorActionFactory factory = new NSRSCustomCodeExtractorActionFactory();
        factory.setActionName("Generic code creator");
        factory.setCodeSystem(supplierCodeSystem);
        factory.setActionName("SupplierCreator");
        factory.setCodeSystemLabel("Supplier");
        factory.setActionLabel("Supplier Creator");
        factory.setCodeValueParameterName("supplier");
        String[] params = {
                "supplier`Supplier`java.lang.String`true",
                "codeType`Primary or Alternative`java.lang.String`false`PRIMARY",
                "url`Supplier URL`Java.lang.String`false`https://www.selleckchem.com/"
        };
        factory.setParameterInfo(params);

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
        abstractParams.put("saltCode", saltCodeValue);
        abstractParams.put("codeType","PRIMARY");
        NSRSCustomCodeExtractorActionFactory factory = new NSRSCustomCodeExtractorActionFactory();
        factory.setActionName("Generic code creator");
        factory.setCodeSystem(saltCodeCodeSystem);
        factory.setActionName("SaltCodeCreator");
        factory.setCodeSystemLabel("Salt Code");
        factory.setActionLabel("Salt Code Creator");
        factory.setCodeValueParameterName("saltCode");
        String[] params = {
                "saltCode`Salt Code`java.lang.String`true",
                "codeType`Primary or Alternative`java.lang.String`false`PRIMARY"
        };
        factory.setParameterInfo(params);

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
        abstractParams.put("saltEquivalents", saltEquivalentValue);
        abstractParams.put("codeType","PRIMARY");
        NSRSCustomCodeExtractorActionFactory factory = new NSRSCustomCodeExtractorActionFactory();
        factory.setActionName("Generic code creator");
        factory.setCodeSystem(saltEquivalentCodeSystem);
        factory.setActionName("SaltEquivalentsCreator");
        factory.setCodeSystemLabel("Salt Equivalents");
        factory.setActionLabel("Salt Equivalents Creator");
        factory.setCodeValueParameterName("saltEquivalents");
        String[] params = {
                "saltEquivalents`Salt Equivalents`java.lang.String`true",
                "codeType`Primary or Alternative`java.lang.String`false`PRIMARY"
        };
        factory.setParameterInfo(params);

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
        abstractParams.put("supplierID", supplierId);
        abstractParams.put("codeType","PRIMARY");
        NSRSCustomCodeExtractorActionFactory factory = new NSRSCustomCodeExtractorActionFactory();
        factory.setActionName("Generic code creator");
        factory.setCodeSystem(supplierIdCodeSystem);
        factory.setActionName("SupplierIdCreator");
        factory.setCodeSystemLabel("Supplier ID");
        factory.setActionLabel("Supplier ID Creator");
        factory.setCodeValueParameterName("supplierID");
        String[] params = {
                "supplierID`Supplier ID`java.lang.String`true",
                "codeType`Primary or Alternative`java.lang.String`false`PRIMARY"
        };
        factory.setParameterInfo(params);

        MappingAction<Substance, SDRecordContext> action = factory.create(abstractParams);
        Chemical chem = Chemical.createFromSmilesAndComputeCoordinates("C(=CC=C1)C(=C1)CCC(C)=O");
        SDRecordContext record = new ChemicalBackedSDRecordContext(chem);

        ChemicalSubstance test = new ChemicalSubstance();
        Substance newChem =action.act(test, record);
        Assertions.assertTrue(newChem.getCodes().stream().anyMatch(c->c.codeSystem.equals(supplierIdCodeSystem) && c.code.equals(supplierId)));
    }

    @Test
    public void getMetadataTest() {
        NSRSCustomCodeExtractorActionFactory factory = new NSRSCustomCodeExtractorActionFactory();
        factory.setActionName("Dummy code creator");
        factory.setCodeSystem("secret code");
        factory.setActionName("DummyCodeCreator");
        factory.setCodeSystemLabel("Dummy");
        factory.setActionLabel("Dummy");
        String[] params = {
                "Name1`Label1`java.lang.String`true",
                "Count2`Count 2`java.lang.Integer`false`56"
        };
        factory.setParameterInfo(params);
        MappingActionFactoryMetadata mappingMetadata= factory.getMetadata();
        Assertions.assertEquals(2, mappingMetadata.getParameterFields().size());
    }
}