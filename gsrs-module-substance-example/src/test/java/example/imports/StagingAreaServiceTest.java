package example.imports;

import gsrs.GsrsFactoryConfiguration;
import gsrs.dataexchange.SubstanceStagingAreaEntityService;
import gsrs.dataexchange.extractors.*;
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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@Slf4j
public class StagingAreaServiceTest extends AbstractSubstanceJpaEntityTest {

    StagingAreaService stagingAreaService;

    String substanceContext = "ix.ginas.models.v1.Substance";

    @BeforeEach
    public void setup() throws NoSuchFieldException, IllegalAccessException {
        // gsrs.matchableCalculators.substances.KEY =


        if( stagingAreaService == null ){
            log.trace("setting up staging area service");
            stagingAreaService = new DefaultStagingAreaService();
            stagingAreaService = AutowireHelper.getInstance().autowireAndProxy(stagingAreaService);
            Map<String, Map<String, Map<String, Object>>> matchableCalculatorConfig = new HashMap<>();
            Map<String, Map<String, Object>> configs = new HashMap<>();
            Map<String, Object> casExtractor = new HashMap<>();
            casExtractor.put("matchableCalculationClass", CASNumberMatchableExtractor.class);
            LinkedHashMap<String, Object> config= new LinkedHashMap<>();
            config.put("casCodeSystems", Arrays.asList("CAS", "CASNo", "CASNumber"));
            casExtractor.put("config", config);
            configs.put("CASNumberMatchableExtractor", casExtractor);

            Map<String, Object> namesExtractor = new HashMap<>();
            namesExtractor.put("matchableCalculationClass", AllNamesMatchableExtractor.class);
            configs.put("AllNamesMatchableExtractor", namesExtractor);

            Map<String, Object> defHashExtractor = new HashMap<>();
            defHashExtractor.put("matchableCalculationClass", DefinitionalHashMatchableExtractor.class);
            configs.put("DefinitionalHashMatchableExtractor", defHashExtractor);

            Map<String, Object> selectedCodesExtractor = new HashMap<>();
            selectedCodesExtractor.put("matchableCalculationClass", SelectedCodesMatchableExtractor.class);
            LinkedHashMap<String, Object> config2= new LinkedHashMap<>();
            config2.put("codeSystems", Arrays.asList("CAS", "ChemBL", "NCI", "NSC", "EINECS"));
            selectedCodesExtractor.put("config", config2);
            configs.put("SelectedCodesMatchableExtractor", selectedCodesExtractor);

            Map<String, Object> uuidExtractor = new HashMap<>();
            uuidExtractor.put("matchableCalculationClass", UUIDMatchableExtractor.class);
            configs.put("UUIDMatchableExtractor", uuidExtractor);
            matchableCalculatorConfig.put("substances", configs);

            SubstanceStagingAreaEntityService stagingAreaEntityService = new SubstanceStagingAreaEntityService();
            stagingAreaEntityService = AutowireHelper.getInstance().autowireAndProxy(stagingAreaEntityService);
            Field factoryConfigField= stagingAreaEntityService.getClass().getDeclaredField("gsrsFactoryConfiguration");
            factoryConfigField.setAccessible(true);
            GsrsFactoryConfiguration factoryConfiguration= (GsrsFactoryConfiguration) factoryConfigField.get(stagingAreaEntityService);
            factoryConfiguration.setMatchableCalculators(matchableCalculatorConfig);
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
