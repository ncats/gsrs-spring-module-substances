package example.chem;

import gov.fda.gsrs.ndsri.FeaturizeNitrosamine;
import gov.nih.ncats.molwitch.Chemical;
import gsrs.module.substance.utils.FeatureUtils;
import ix.ginas.models.v1.GinasChemicalStructure;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

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
        //properties.get(0).entrySet().forEach(e-> System.out.printf("key: %s = value: %s\n", e.getKey(), e.getValue()));
        //Assertions.assertEquals("5", properties.get(0).get("categoryScore"));
        Assertions.assertTrue(properties.isEmpty());
    }

    @Test
    void testIVACAFTORType() throws Exception {
        Chemical c1= Chemical.parse("c1ccc2c(c1)c(=O)cc[nH]2");
            List<Map<String, String>> properties = FeatureUtils.calculateFeatures( c1);
        Assertions.assertTrue(properties.isEmpty());
        /*assertEquals(1, properties.size());
        assertEquals("A. Secondary Amine" ,properties.get(0).get("type"));
        assertEquals("0,1", properties.get(0).get("Alpha-Hydrogens"));*/
    }

    @Test
    void testFostamatibib() throws Exception {
        Chemical c1= Chemical.parse("O.O.O.O.O.O.[Na+].[Na+].COC1=CC([NH:1]C2=NC=C(F)C(NC3=NC4=C(OC(C)(C)C(=O)N4COP([O-])([O-])=O)C=C3)=N2)=CC(OC)=C1OC");

        List<Map<String, String>> properties = FeatureUtils.calculateFeatures( c1);

        assertEquals(0, properties.size());
        /*assertEquals("A. Multiple Secondary Amine", properties.get(0).get("type"));
        assertEquals("0,0", properties.get(0).get("Alpha-Hydrogens"));*/
    }

    @Test
    public void testIsPiperazine() throws Exception {
        FeaturizeNitrosamine.GLOBAL_SETTINGS.DO_EXTENDED_FEATURES_TOO=true;

        Chemical c1= Chemical.parse("N1CCNCC1");
        List<Map<String, String>> properties = FeatureUtils.calculateFeatures( c1);

        assertEquals(0, properties.size());
        /*assertEquals("A. Multiple Secondary Amine", properties.get(0).get("type"));
        assertEquals("2,2", properties.get(0).get("Alpha-Hydrogens"));
        assertEquals("YES", properties.get(0).get( FeaturizeNitrosamine.FeaturePairRegistry.PIPERAZINE.getFeatureName()));*/
    }

    @Test
    public void testCarboxylicAcidOnSaltDoesNotCount() throws Exception {
        Chemical c1= Chemical.parse("O[C@H]([C@@H](O)C(O)=O)C(O)=O.COC1=CC=C(C[C@@H](C)[NH:20]C[C@H](O)C2=CC=C(O)C(NC=O)=C2)C=C1");
        List<Map<String, String>> properties = FeatureUtils.calculateFeatures( c1);
        assertEquals(0, properties.size());
        /*assertEquals("A. Secondary Amine", properties.get(0).get("type"));
        assertEquals("NO" ,properties.get(0).get(FeaturizeNitrosamine.FeaturePairRegistry.COOH.getFeatureName()));*/
    }

    @Test
    public void testBenzylLikeFeatureShouldNotFindPsuedoAromaticity() throws Exception {
        Chemical c1= Chemical.parse("C[C@H]([NH:3]C1=C2N=CNC2=NC=N1)C3=CC4=C(C(Cl)=CC=C4)C(=O)N3C5=CC=CC=C5");
        List<Map<String, String>> properties = FeatureUtils.calculateFeatures( c1);
        assertEquals(0, properties.size());
        /*assertEquals("A. Secondary Amine", properties.get(0).get("type"));
        assertEquals("NO", properties.get(0).get(FeaturizeNitrosamine.FeaturePairRegistry.ARYL_ALPHA.getFeatureName()));*/
    }

    @Test
    void testOneRealNitrosamine() throws Exception{
        String molfileText = IOUtils.toString(
                this.getClass().getResourceAsStream("/molfiles/M8LE2AF05P.mol"),
                "UTF-8"
        );
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile = molfileText;
        List<Map<String, String>> properties = FeatureUtils.calculateFeatures( structure.toChemical());
        //properties.get(0).entrySet().forEach(e-> System.out.printf("key: %s = value: %s\n", e.getKey(), e.getValue()));
        Assertions.assertEquals("5", properties.get(0).get("categoryScore"));
    }

}
