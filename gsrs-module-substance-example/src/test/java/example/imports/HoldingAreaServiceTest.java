package example.imports;

import gsrs.dataexchange.SubstanceHoldingAreaEntityService;
import gsrs.holdingarea.model.MatchableKeyValueTuple;
import gsrs.holdingarea.service.DefaultHoldingAreaService;
import gsrs.holdingarea.service.HoldingAreaService;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.chem.StructureProcessor;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

@Slf4j
public class HoldingAreaServiceTest  extends AbstractSubstanceJpaEntityTest {

    HoldingAreaService holdingAreaService;

    String substanceContext = "ix.ginas.models.v1.Substance";

    @BeforeEach
    public void setup(){
        if( holdingAreaService == null ){
            log.trace("setting up holding area service");
            holdingAreaService = new DefaultHoldingAreaService(substanceContext);
            holdingAreaService = AutowireHelper.getInstance().autowireAndProxy(holdingAreaService);
            SubstanceHoldingAreaEntityService holdingAreaEntityService = new SubstanceHoldingAreaEntityService();
            holdingAreaEntityService = AutowireHelper.getInstance().autowireAndProxy(holdingAreaEntityService);
            holdingAreaService.registerEntityService(holdingAreaEntityService);
        }
    }

    @Test
    public void testgetMatchables() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String testMethodName = "getMatchables";
        Method testMethod = holdingAreaService.getClass().getDeclaredMethod(testMethodName, Object.class);
        if(testMethod==null) {
            Assertions.fail("Error locating testing method!");
        }
        testMethod.setAccessible(true);
        //create a basic substance
        UUID uuid1 = UUID.randomUUID();
        ChemicalSubstance chem= new ChemicalSubstanceBuilder()
                .setStructureWithDefaultReference("CCCC")
                .addName("chem1")
                .setUUID(uuid1)
                .build();
        List<MatchableKeyValueTuple> matchables = (List<MatchableKeyValueTuple>) testMethod.invoke(holdingAreaService, chem);
        System.out.println("matchables: ");
        matchables.forEach(m-> System.out.printf("key: %s; value: %s; layer: %d\n", m.getKey(), m.getValue(), m.getLayer()));
        Assertions.assertTrue(matchables.stream().anyMatch(m->m.getKey().equalsIgnoreCase("Definitional Hash")
            && m.getValue().equals("structure.properties.hash1->IJDNQMDRQITEOD@1")));
    }
}
