package gsrs.module.substance;

import ix.ginas.models.v1.AgentModification;
import ix.ginas.models.v1.Amount;
import ix.ginas.models.v1.Modifications;
import ix.ginas.models.v1.PhysicalModification;
import ix.ginas.models.v1.PhysicalParameter;
import ix.ginas.models.v1.StructuralModification;
import ix.ginas.models.v1.SubstanceReference;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SubstanceEntityServiceModificationUuidTest {

    @Test
    void replacementUpdateShouldResetImportedModificationGraphWhenExistingGraphIsAbsent() throws Exception {
        SubstanceEntityServiceImpl service = new SubstanceEntityServiceImpl();
        Modifications imported = new Modifications();
        imported.uuid = UUID.randomUUID();

        AgentModification agent = new AgentModification();
        agent.uuid = UUID.randomUUID();
        agent.amount = amountWithUuid();
        agent.agentSubstance = referenceWithUuid();
        imported.agentModifications = new ArrayList<>(Collections.singletonList(agent));

        PhysicalModification physical = new PhysicalModification();
        physical.uuid = UUID.randomUUID();
        PhysicalParameter parameter = new PhysicalParameter();
        parameter.uuid = UUID.randomUUID();
        parameter.amount = amountWithUuid();
        physical.parameters = new ArrayList<>(Collections.singletonList(parameter));
        imported.physicalModifications = new ArrayList<>(Collections.singletonList(physical));

        StructuralModification structural = new StructuralModification();
        structural.uuid = UUID.randomUUID();
        structural.extentAmount = amountWithUuid();
        structural.molecularFragment = referenceWithUuid();
        imported.structuralModifications = new ArrayList<>(Collections.singletonList(structural));

        Method method = SubstanceEntityServiceImpl.class.getDeclaredMethod(
                "reconcileManagedModifications",
                Modifications.class,
                Modifications.class,
                UUID.class,
                List.class,
                List.class,
                List.class,
                Map.class,
                Map.class,
                Map.class,
                Map.class,
                Map.class,
                Map.class,
                Map.class,
                Map.class);
        method.setAccessible(true);

        Modifications reconciled = (Modifications) method.invoke(
                service,
                imported,
                null,
                null,
                null,
                null,
                null,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap());

        assertSame(imported, reconciled);
        assertNull(reconciled.uuid);
        assertNull(agent.uuid);
        assertNull(agent.amount.uuid);
        assertNull(agent.agentSubstance.uuid);
        assertNull(physical.uuid);
        assertNull(parameter.uuid);
        assertNull(parameter.amount.uuid);
        assertNull(structural.uuid);
        assertNull(structural.extentAmount.uuid);
        assertNull(structural.molecularFragment.uuid);
    }

    private static Amount amountWithUuid() {
        Amount amount = new Amount();
        amount.uuid = UUID.randomUUID();
        return amount;
    }

    private static SubstanceReference referenceWithUuid() {
        SubstanceReference reference = new SubstanceReference();
        reference.uuid = UUID.randomUUID();
        return reference;
    }
}
