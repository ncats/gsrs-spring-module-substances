package example.chem;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.module.substance.definitional.ChemicalSubstanceDefinitionalElementImpl;
import gsrs.module.substance.definitional.DefinitionalElement;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DefHashCalcTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    private StructureProcessor structureProcessor;

    @Test
    public void testOpticalInDefinitionalHashCalcNeg() throws Exception {
        String opticalActivityKey="structure.properties.opticalActivity";
        String structureJson = "{\n" +
                "    \"opticalActivity\": \"UNSPECIFIED\",\n" +
                "    \"molfile\": \"\\n  ACCLDraw07282209012D\\n\\n  5  4  0  0  0  0  0  0  0  0999 V2000\\n   10.5000   -8.5938    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   11.5229   -8.0032    0.0000 C   0  0  3  0  0  0  0  0  0  0  0  0\\n   11.5229   -6.8217    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\\n   12.5460   -8.5939    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   13.5692   -8.0032    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n  1  2  1  0  0  0  0\\n  2  3  1  0  0  0  0\\n  2  4  1  0  0  0  0\\n  4  5  1  0  0  0  0\\nM  END\",\n" +
                "    \"stereoCenters\": 1,\n" +
                "    \"definedStereo\": 0,\n" +
                "    \"ezCenters\": 0,\n" +
                "    \"charge\": 0,\n" +
                "    \"mwt\": 92.56726,\n" +
                "    \"count\": 1,\n" +
                "    \"stereochemistry\": \"RACEMIC\"\n" +
                "}\n" +
                "";
        ObjectMapper om = new ObjectMapper();
        Structure rawStructure = om.readValue(structureJson, Structure.class);
        Structure instrumentedStructure =structureProcessor.instrument(rawStructure.toChemical(), true);
        GinasChemicalStructure ginasChemicalStructure = new GinasChemicalStructure(instrumentedStructure);

        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chem =builder
                .setStructure(ginasChemicalStructure)
                .addName("2-chlorobutane")
                        .build();
        ChemicalSubstanceDefinitionalElementImpl defHashCalculator = new ChemicalSubstanceDefinitionalElementImpl();
        List<DefinitionalElement> definitionalElements = new ArrayList<>();
        defHashCalculator.computeDefinitionalElements(chem, definitionalElements::add);
        definitionalElements.forEach(de-> System.out.printf("key: %s = %s\n", de.getKey(), de.getValue()));
        Assertions.assertTrue(definitionalElements.stream().noneMatch(de->de.getKey().equals(opticalActivityKey)));
    }

    @Test
    public void testOpticalInDefinitionalHashCalcPos() throws Exception {
        String opticalActivityKey="structure.properties.opticalActivity";
        String structureJson = "{\n" +
                "    \"opticalActivity\": \"POS\",\n" +
                "    \"molfile\": \"\\n  ACCLDraw07282209012D\\n\\n  5  4  0  0  0  0  0  0  0  0999 V2000\\n   10.5000   -8.5938    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   11.5229   -8.0032    0.0000 C   0  0  3  0  0  0  0  0  0  0  0  0\\n   11.5229   -6.8217    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\\n   12.5460   -8.5939    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   13.5692   -8.0032    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n  1  2  1  0  0  0  0\\n  2  3  1  0  0  0  0\\n  2  4  1  0  0  0  0\\n  4  5  1  0  0  0  0\\nM  END\",\n" +
                "    \"stereoCenters\": 1,\n" +
                "    \"definedStereo\": 0,\n" +
                "    \"ezCenters\": 0,\n" +
                "    \"charge\": 0,\n" +
                "    \"mwt\": 92.56726,\n" +
                "    \"count\": 1,\n" +
                "    \"stereochemistry\": \"MIXED\"\n" +
                "}\n" +
                "";
        ObjectMapper om = new ObjectMapper();
        Structure rawStructure = om.readValue(structureJson, Structure.class);
        Structure instrumentedStructure =structureProcessor.instrument(rawStructure.toChemical(), true);
        GinasChemicalStructure ginasChemicalStructure = new GinasChemicalStructure(instrumentedStructure);


        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chem =builder
                .setStructure(ginasChemicalStructure)
                .addName("2-chlorobutane")
                .build();
        chem.getStructure().setStereoChemistry(Structure.Stereo.MIXED);
        ChemicalSubstanceDefinitionalElementImpl defHashCalculator = new ChemicalSubstanceDefinitionalElementImpl();
        List<DefinitionalElement> definitionalElements = new ArrayList<>();
        defHashCalculator.computeDefinitionalElements(chem, definitionalElements::add);
        definitionalElements.forEach(de-> System.out.printf("key: %s = %s\n", de.getKey(), de.getValue()));
        Assertions.assertTrue(definitionalElements.stream().anyMatch(de->de.getKey().equals(opticalActivityKey)));
    }


    @Test
    public void testStereoCommentsInDefinitionalHashCalcPos() throws Exception {
        String additionalStereochemistryKey="structure.properties.stereoComments";
        String dataFileName ="R0NL28355M.json";
        File proteinFile = new ClassPathResource("testJSON/" + dataFileName).getFile();
        ChemicalSubstanceBuilder builder = SubstanceBuilder.from(proteinFile);

        ChemicalSubstance chem =builder.build();
        System.out.printf("atropisomerism: %s\n", chem.getStructure().atropisomerism);
        ChemicalSubstanceDefinitionalElementImpl defHashCalculator = new ChemicalSubstanceDefinitionalElementImpl();
        List<DefinitionalElement> definitionalElements = new ArrayList<>();
        defHashCalculator.computeDefinitionalElements(chem, definitionalElements::add);
        definitionalElements.forEach(de-> System.out.printf("key: %s = %s\n", de.getKey(), de.getValue()));
        Assertions.assertTrue(definitionalElements.stream().anyMatch(de->de.getKey().equals(additionalStereochemistryKey)));
    }

    @Test
    public void testStereoCommentsInDefinitionalHashCalcNeg() throws Exception {
        String additionalStereochemistryKey="structure.properties.stereoComments";
        String dataFileName ="R0NL28355M.json";
        File proteinFile = new ClassPathResource("testJSON/" + dataFileName).getFile();
        ChemicalSubstanceBuilder builder = SubstanceBuilder.from(proteinFile);

        ChemicalSubstance chem =builder.build();
        chem.getStructure().atropisomerism= Structure.NYU.No;
        System.out.printf("atropisomerism: %s\n", chem.getStructure().atropisomerism);
        ChemicalSubstanceDefinitionalElementImpl defHashCalculator = new ChemicalSubstanceDefinitionalElementImpl();
        List<DefinitionalElement> definitionalElements = new ArrayList<>();
        defHashCalculator.computeDefinitionalElements(chem, definitionalElements::add);
        definitionalElements.forEach(de-> System.out.printf("key: %s = %s\n", de.getKey(), de.getValue()));
        Assertions.assertFalse(definitionalElements.stream().anyMatch(de->de.getKey().equals(additionalStereochemistryKey)));
    }

    @Test
    public void testStereoCommentsInDefinitionalHashCalcNeg2() throws Exception {
        String additionalStereochemistryKey="structure.properties.stereoComments";
        String dataFileName ="R0NL28355M.json";
        File proteinFile = new ClassPathResource("testJSON/" + dataFileName).getFile();
        ChemicalSubstanceBuilder builder = SubstanceBuilder.from(proteinFile);

        ChemicalSubstance chem =builder.build();
        chem.getStructure().stereoComments = null;
        ChemicalSubstanceDefinitionalElementImpl defHashCalculator = new ChemicalSubstanceDefinitionalElementImpl();
        List<DefinitionalElement> definitionalElements = new ArrayList<>();
        defHashCalculator.computeDefinitionalElements(chem, definitionalElements::add);
        definitionalElements.forEach(de-> System.out.printf("key: %s = %s\n", de.getKey(), de.getValue()));
        Assertions.assertFalse(definitionalElements.stream().anyMatch(de->de.getKey().equals(additionalStereochemistryKey)));
    }


}
