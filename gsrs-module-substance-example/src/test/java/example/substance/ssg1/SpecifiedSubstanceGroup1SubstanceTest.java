package example.substance.ssg1;

import gov.nih.ncats.common.Tuple;
import ix.ginas.modelBuilders.SpecifiedSubstanceGroup1SubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.GinasAccessControlled;
import ix.ginas.models.v1.SpecifiedSubstanceGroup1Substance;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SpecifiedSubstanceGroup1SubstanceTest {

    @Test
    public void SSG1getDependsOnSubstanceReferencesAndParentsTest() {
        SubstanceBuilder basicSubstancBuilder = new SubstanceBuilder();
        basicSubstancBuilder.addName("SSG1 Basis");
        Substance basicSubstance = basicSubstancBuilder.build();
        SpecifiedSubstanceGroup1SubstanceBuilder builder = new SpecifiedSubstanceGroup1SubstanceBuilder(basicSubstance);
        //intentionally laving definition blank
        SpecifiedSubstanceGroup1Substance  substance = builder.build();
        List<Tuple<GinasAccessControlled, SubstanceReference>> defs= substance.getDependsOnSubstanceReferencesAndParents();
        Assertions.assertNotNull(defs);//just getting here with no NPE proves the 20-January-2023 fix works

    }

    @Test
    public void SSG1getDependsOnSubstanceReferencesTest() {
        SubstanceBuilder basicSubstancBuilder = new SubstanceBuilder();
        basicSubstancBuilder.addName("SSG1 Basis");
        Substance basicSubstance = basicSubstancBuilder.build();
        SpecifiedSubstanceGroup1SubstanceBuilder builder = new SpecifiedSubstanceGroup1SubstanceBuilder(basicSubstance);
        //intentionally laving definition blank
        SpecifiedSubstanceGroup1Substance  substance = builder.build();
        List<SubstanceReference> refs= substance.getDependsOnSubstanceReferences();
        Assertions.assertNotNull(refs);//just getting here with no NPE proves the 20-January-2023 fix works

    }
}
