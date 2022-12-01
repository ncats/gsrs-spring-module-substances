package example.imports;

import gsrs.dataexchange.SubstanceHoldingAreaEntityService;
import gsrs.holdingarea.model.MatchableKeyValueTuple;
import gsrs.holdingarea.service.DefaultHoldingAreaService;
import gsrs.holdingarea.service.HoldingAreaService;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        //create a chemical substance with as little data as possible
        UUID uuid1 = UUID.randomUUID();
        ChemicalSubstance chem= new ChemicalSubstanceBuilder()
                .setStructureWithDefaultReference("CCCC")
                .addName("chem1")
                .setUUID(uuid1)
                .build();
        List<MatchableKeyValueTuple> matchables = (List<MatchableKeyValueTuple>) testMethod.invoke(holdingAreaService, chem);
        log.trace("(matchables)");
        matchables.forEach(m-> log.trace("key: '{}'; value: '{}'; layer: {}\n", m.getKey(), m.getValue(), m.getLayer()));
        Assertions.assertTrue(matchables.stream().anyMatch(m->m.getKey().equalsIgnoreCase("Definitional Hash - Layer 1")
            && m.getValue().equals("f2f9c75f73c90d8e8f8d7701fef83e1beda60182")));
    }
}
