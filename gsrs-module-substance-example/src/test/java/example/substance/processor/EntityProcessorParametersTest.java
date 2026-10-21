package example.substance.processor;

import gsrs.entityProcessor.EntityProcessorConfig;
import gsrs.module.substance.processors.CodeProcessor;
import gsrs.module.substance.processors.ConfigurableMolweightProcessor;
import ix.core.EntityProcessor;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Code;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that entity processors declared with a {@code parameters} block are actually built by
 * GSRS through Jackson ({@code ObjectMapper.convertValue}) rather than by a direct constructor call.
 *
 * <p>The other processor tests invoke the constructors directly, so they would not catch a missing
 * {@code @JsonCreator}.  These tests go through {@link EntityProcessorConfig}, which is the same
 * code path used when the application starts and reads {@code gsrs.entityProcessors.list}.</p>
 */
public class EntityProcessorParametersTest {

    @Test
    public void codeProcessorIsBuiltFromParameters() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("class", "gsrs.module.substance.datasource.DefaultCodeSystemUrlGenerator");
        parameters.put("codeSystems", java.util.Collections.singletonMap(
                "MERCK INDEX", "https://www.rsc.org/Merck-Index/monograph/mono$CODE$"));

        EntityProcessorConfig config = new EntityProcessorConfig();
        config.setProcessor(CodeProcessor.class);
        config.setParameters(parameters);

        EntityProcessor processor = config.createNewEntityProcessorInstance();
        assertTrue(processor instanceof CodeProcessor, "expected a CodeProcessor");

        Code code = new Code();
        code.code = "999999";
        code.codeSystem = "MERCK INDEX";
        code.type = "PRIMARY";
        processor.prePersist(code);

        assertEquals("https://www.rsc.org/Merck-Index/monograph/mono999999", code.url,
                "the code system data must have been deserialized from parameters");
    }

    @Test
    public void molweightProcessorIsBuiltFromParameters() throws Exception {
        List<Map<String, Object>> weights = new ArrayList<>();
        weights.add(atomicWeight("C", null, 12.511));
        weights.add(atomicWeight("Br", 81, 280.916290563));

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("atomicWeights", weights);
        parameters.put("persistenceMode", "intrinsic");
        parameters.put("decimalDigits", 2);

        EntityProcessorConfig config = new EntityProcessorConfig();
        config.setProcessor(ConfigurableMolweightProcessor.class);
        config.setParameters(parameters);

        EntityProcessor processor = config.createNewEntityProcessorInstance();
        assertNotNull(processor, "expected a ConfigurableMolweightProcessor");
        assertTrue(processor instanceof ConfigurableMolweightProcessor,
                "expected a ConfigurableMolweightProcessor");

        ChemicalSubstance chemical = new ChemicalSubstanceBuilder()
                .addName("benzene")
                .setStructureWithDefaultReference("c1c([81Br])cccc1")
                .build();
        processor.prePersist(chemical);

        double expectedMw = 6 * 12.511 + 5 * 1.008 + 280.916290563;
        assertEquals(expectedMw, chemical.getStructure().mwt, 0.001,
                "molecular weight must be computed from parameters-supplied atomic weights");
    }

    private Map<String, Object> atomicWeight(String symbol, Integer massNumber, Double value) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("symbol", symbol);
        if (massNumber != null) {
            entry.put("massNumber", massNumber);
        }
        entry.put("value", value);
        return entry;
    }
}
