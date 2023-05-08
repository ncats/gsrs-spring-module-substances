package example.imports;

import gsrs.dataexchange.SubstanceStagingAreaEntityService;
import gsrs.stagingarea.model.MatchableKeyValueTuple;
import gsrs.stagingarea.service.DefaultStagingAreaService;
import gsrs.stagingarea.service.StagingAreaService;
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
public class StagingAreaServiceTest extends AbstractSubstanceJpaEntityTest {

    StagingAreaService stagingAreaService;

    String substanceContext = "ix.ginas.models.v1.Substance";

    @BeforeEach
    public void setup(){
        if( stagingAreaService == null ){
            log.trace("setting up staging area service");
            stagingAreaService = new DefaultStagingAreaService();
            stagingAreaService = AutowireHelper.getInstance().autowireAndProxy(stagingAreaService);
            SubstanceStagingAreaEntityService stagingAreaEntityService = new SubstanceStagingAreaEntityService();
            stagingAreaEntityService = AutowireHelper.getInstance().autowireAndProxy(stagingAreaEntityService);
            stagingAreaService.registerEntityService(stagingAreaEntityService);
        }
    }

    @Test
    public void testgetMatchables() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String testMethodName = "getMatchables";
        Method testMethod = stagingAreaService.getClass().getDeclaredMethod(testMethodName, Object.class);
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
        List<MatchableKeyValueTuple> matchables = (List<MatchableKeyValueTuple>) testMethod.invoke(stagingAreaService, chem);
        log.trace("(matchables)");
        matchables.forEach(m-> log.trace("key: '{}'; value: '{}'; layer: {}\n", m.getKey(), m.getValue(), m.getLayer()));
        Assertions.assertTrue(matchables.stream().anyMatch(m->m.getKey().equalsIgnoreCase("Definitional Hash - Layer 1")
            && m.getValue().equals("f2f9c75f73c90d8e8f8d7701fef83e1beda60182")));
    }
}
