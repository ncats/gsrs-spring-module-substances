package gsrs.module.substance.definitional;

import ix.ginas.models.v1.Amount;
import ix.ginas.models.v1.Material;
import ix.ginas.models.v1.Polymer;
import ix.ginas.models.v1.PolymerSubstance;
import ix.ginas.models.v1.SubstanceReference;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolymerDefinitionalElementImplTest {

    @Test
    void definingMonomerContributesReferenceAndAmountAtExpectedLayers() {
        Material monomer = monomer(true);

        List<DefinitionalElement> elements = computeElements(monomer);

        assertEquals(Arrays.asList(
                DefinitionalElement.of("polymer.monomer.monomerSubstance.refuuid",
                        monomer.monomerSubstance.refuuid, 1),
                DefinitionalElement.of("polymer.monomer.monomerSubstance.amount",
                        monomer.amount.toString(), 2)), elements);
    }

    @Test
    void nonDefiningMonomerContributesNeitherReferenceNorAmount() {
        Material monomer = monomer(false);

        List<DefinitionalElement> elements = computeElements(monomer);

        assertTrue(elements.isEmpty(), "Non-defining monomers must not contribute definitional elements");
    }

    private static Material monomer(boolean defining) {
        Material monomer = new Material();
        monomer.defining = defining;
        monomer.monomerSubstance = new SubstanceReference();
        monomer.monomerSubstance.refuuid = "8116157b-1998-487a-9346-5c97be8626b2";
        monomer.amount = new Amount();
        monomer.amount.average = 25.0;
        monomer.amount.units = "mol %";
        return monomer;
    }

    private static List<DefinitionalElement> computeElements(Material monomer) {
        PolymerSubstance substance = new PolymerSubstance();
        substance.polymer = new Polymer();
        substance.polymer.monomers.add(monomer);
        List<DefinitionalElement> elements = new ArrayList<>();

        new PolymerDefinitionalElementImpl().computeDefinitionalElements(substance, elements::add);

        return elements;
    }
}
