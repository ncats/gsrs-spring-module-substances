package example.chem;

import gsrs.module.substance.utils.FeatureUtils;
import ix.core.chem.InchiStandardizer;
import ix.ginas.models.v1.GinasChemicalStructure;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class FeatureGenerationTest {

    @Test
    void testOneMol() throws Exception{
        String molfileText = IOUtils.toString(
                this.getClass().getResourceAsStream("/molfiles/1~{H}-quinolin-4-one.mol"),
                "UTF-8"
        );
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile = molfileText;
        List<Map<String, String>> properties = FeatureUtils.calculateFeatures( structure.toChemical());

        properties.get(0).entrySet().forEach(e-> System.out.printf("key: %s = value: %s\n", e.getKey(), e.getValue()));
        Assertions.assertEquals("5", properties.get(0).get("categoryScore"));

    }
}
