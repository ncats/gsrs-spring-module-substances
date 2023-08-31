package example.imports;

import gov.nih.ncats.common.Tuple;
import ix.ginas.modelBuilders.SpecifiedSubstanceGroup1SubstanceBuilder;
import ix.ginas.models.GinasAccessControlled;
import ix.ginas.models.v1.SpecifiedSubstanceComponent;
import ix.ginas.models.v1.SpecifiedSubstanceGroup1;
import ix.ginas.models.v1.SpecifiedSubstanceGroup1Substance;
import ix.ginas.models.v1.SubstanceReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SSG1GetSubstanceReferencesAndParentsBeyondDependsOnTests {

    @Test
    public void testFindConstituentReference(){
        SpecifiedSubstanceGroup1SubstanceBuilder builder = new SpecifiedSubstanceGroup1SubstanceBuilder();
        builder.addName("Super Duper Mixture");

        String component1ApprovalId = "ABCDE12345";
        String component2ApprovalId = "ABCDE12346";

        SpecifiedSubstanceGroup1  specifiedSubstanceGroup1 = new SpecifiedSubstanceGroup1();
        SpecifiedSubstanceComponent component1 = new SpecifiedSubstanceComponent();
        component1.substance = new SubstanceReference();
        component1.substance.refuuid = UUID.randomUUID().toString();
        component1.substance.approvalID= component1ApprovalId;
        SpecifiedSubstanceComponent component2 = new SpecifiedSubstanceComponent();
        component2.substance = new SubstanceReference();
        component2.substance.refuuid = UUID.randomUUID().toString();
        component2.substance.approvalID= component2ApprovalId;

        specifiedSubstanceGroup1.constituents = Arrays.asList(component1, component2);
        builder.setSpecifiedSubstance(specifiedSubstanceGroup1);

        SpecifiedSubstanceGroup1Substance substance= builder.build();

        List<Tuple<GinasAccessControlled,SubstanceReference>> refs =substance.getSubstanceReferencesAndParentsBeyondDependsOn();
        Assertions.assertTrue(refs.stream().anyMatch(r->r.v().approvalID.equals(component1ApprovalId)));
        Assertions.assertTrue(refs.stream().anyMatch(r->r.v().approvalID.equals(component2ApprovalId)));
    }
}
