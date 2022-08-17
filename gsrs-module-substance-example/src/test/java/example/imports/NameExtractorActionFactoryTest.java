package example.imports;

import gov.nih.ncats.molwitch.Chemical;
import gsrs.dataexchange.model.MappingAction;
import gsrs.module.substance.importers.importActionFactories.NameExtractorActionFactory;
import gsrs.module.substance.importers.model.ChemicalBackedSDRecordContext;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.core.models.Structure;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NameExtractorActionFactoryTest {

    @Test
    public void testCreateName() throws Exception {
        NameExtractorActionFactory nameExtractorActionFactory = new NameExtractorActionFactory();
        String smilesForDiclofenac="O=C(O)Cc1ccccc1Nc2c(Cl)cccc2Cl";
        String nameDiclofenac= "diclofenac";
        Chemical c = Chemical.parse(smilesForDiclofenac);

        ChemicalBackedSDRecordContext ctx = new ChemicalBackedSDRecordContext(c);
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("name", nameDiclofenac);
        inputParams.put("nameType", "bn");
        inputParams.put("lang", "en");

        ChemicalSubstance chemicalSubstance = new ChemicalSubstance();
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.smiles=smilesForDiclofenac;

        chemicalSubstance.setStructure(structure);
        MappingAction<Substance, SDRecordContext> action= nameExtractorActionFactory.create(inputParams);
        action.act(chemicalSubstance, ctx);
        Name newlyCreatedName = chemicalSubstance.names.get(0);
        Assertions.assertEquals(nameDiclofenac, newlyCreatedName.name);
        Assertions.assertEquals("en",newlyCreatedName.languages.get(0).getValue());
        Assertions.assertEquals("bn", newlyCreatedName.type);
    }

    @Test
    public void testCreateDisplayName() throws Exception {
        NameExtractorActionFactory nameExtractorActionFactory = new NameExtractorActionFactory();
        String smilesForDiclofenac="O=C(O)Cc1ccccc1Nc2c(Cl)cccc2Cl";
        String nameDiclofenac= "diclofenac";
        Chemical c = Chemical.parse(smilesForDiclofenac);

        ChemicalBackedSDRecordContext ctx = new ChemicalBackedSDRecordContext(c);
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("name", nameDiclofenac);
        inputParams.put("nameType", "bn");
        inputParams.put("lang", "en");
        inputParams.put("displayName", true);

        ChemicalSubstance chemicalSubstance = new ChemicalSubstance();
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.smiles=smilesForDiclofenac;

        chemicalSubstance.setStructure(structure);
        MappingAction<Substance, SDRecordContext> action= nameExtractorActionFactory.create(inputParams);
        action.act(chemicalSubstance, ctx);
        Name newlyCreatedName = chemicalSubstance.names.get(0);
        Assertions.assertTrue(newlyCreatedName.isDisplayName());
    }

    @Test
    public void testCreateNameWithRefs() throws Exception {
        NameExtractorActionFactory nameExtractorActionFactory = new NameExtractorActionFactory();
        String smilesForDiclofenac="O=C(O)Cc1ccccc1Nc2c(Cl)cccc2Cl";
        String nameDiclofenac= "diclofenac";
        Chemical c = Chemical.parse(smilesForDiclofenac);

        ChemicalBackedSDRecordContext ctx = new ChemicalBackedSDRecordContext(c);
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("name", nameDiclofenac);
        inputParams.put("nameType", "bn");
        inputParams.put("lang", "en");
        String[] refs = {"[[UUID_1]]"};
        inputParams.put("referenceUUIDs", refs);

        ChemicalSubstance chemicalSubstance = new ChemicalSubstance();
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.smiles=smilesForDiclofenac;

        chemicalSubstance.setStructure(structure);
        MappingAction<Substance, SDRecordContext> action= nameExtractorActionFactory.create(inputParams);
        action.act(chemicalSubstance, ctx);
        Name newlyCreatedName = chemicalSubstance.names.get(0);
        newlyCreatedName.getReferences().stream().anyMatch(r->  r.term.length()>100);
    }
}
