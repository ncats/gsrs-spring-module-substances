package example.imports;

import gov.nih.ncats.molwitch.Chemical;
import gsrs.dataexchange.model.MappingAction;
import gsrs.module.substance.importers.importActionFactories.ReferenceExtractorActionFactory;
import gsrs.module.substance.importers.model.ChemicalBackedSDRecordContext;
import gsrs.module.substance.importers.model.PropertyBasedDataRecordContext;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Reference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class ReferenceExtractorActionFactoryTest {

    @Test
    public void testCreateReference() throws Exception {
        String smilesForHeparin="CC(=O)NC1C(O)OC(COS(=O)(=O)O)C(OC2OC(C(OC3OC(CO)C(OC4OC(C(O)C(O)C4OS(=O)(=O)O)C(=O)O)C(OS(=O)(=O)O)C3NS(=O)(=O)O)C(O)C2OS(=O)(=O)O)C(=O)O)C1O";
        String tagForRelease= "PUBLIC_DOMAIN_RELEASE";
        Chemical c = Chemical.parse(smilesForHeparin);

        ChemicalBackedSDRecordContext ctx = new ChemicalBackedSDRecordContext(c);
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("docType", "notes");
        inputParams.put("citation", "valid information");
        inputParams.put("publicDomain", true);
        String[] tags= {tagForRelease};
        inputParams.put("tags", tags);

        ChemicalSubstance chemicalSubstance = new ChemicalSubstance();
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.smiles=smilesForHeparin;

        chemicalSubstance.setStructure(structure);
        ReferenceExtractorActionFactory factory= new ReferenceExtractorActionFactory();
        MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> action= factory.create(inputParams);
        SubstanceBuilder builder= chemicalSubstance.toBuilder();
        action.act(builder, ctx);
        ChemicalSubstance newChem = builder.asChemical().build();
        Reference reference = newChem.references.get(0);
        Assertions.assertTrue(reference.isPublicDomain());
        Assertions.assertEquals(tagForRelease, reference.tags.get(0).getValue());
    }
}
