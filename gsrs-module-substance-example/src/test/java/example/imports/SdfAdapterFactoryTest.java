package example.imports;

import gsrs.dataExchange.model.MappingAction;
import gsrs.dataExchange.model.SDFRecord;
import gsrs.module.substance.importers.SDFImportAdapterFactory;
import gsrs.module.substance.importers.importActionFactories.PropertyExtractorActionFactory;
import gsrs.module.substance.importers.model.SDRecordContext;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

@Slf4j
public class SdfAdapterFactoryTest {

    @Test
    public void testParsing() throws Exception {

        Map<String, Object> meta = new HashMap<>();
        meta.put("codeSystem", "cas.rn");
        meta.put("code", "{{cas.rn}}");
        meta.put("codeType", "PRIMARY");
        Map<String, Object> resolved= SDFImportAdapterFactory.resolveParametersMap(record, meta);
        String expected="134523-00-5";
        Assertions.assertEquals(expected, (String) resolved.get("code"));
    }


    @Test
    public void testParsing2() throws Exception {
        Map<String, Object> meta = new HashMap<>();
        meta.put("name", "boiling.point.predicted");
        meta.put("valueRange", "{{boiling.point.predicted}}");
        meta.put("propertyType", "physical");
        Map<String, Object> resolved= SDFImportAdapterFactory.resolveParametersMap(record, meta);
        String expected="134523-00-5";
        Assertions.assertEquals(3, resolved.size());
    }

    @Test
    public void PropertyExtractorActionFactoryTest() throws Exception {
        Map<String, Object> absParameters = new HashMap<>();
        absParameters.put("name", "boiling.point.predicted");
        absParameters.put("valueRange", "722.2±60.0 °C    Press: 760 Torr");
        absParameters.put("propertyType", "physical");

        PropertyExtractorActionFactory factory = new PropertyExtractorActionFactory();
        MappingAction<Substance, SDRecordContext> mappingAction = factory.create(absParameters);
        ChemicalSubstance chemicalSubstance = new ChemicalSubstance();
        mappingAction.act(chemicalSubstance, record);
        chemicalSubstance.properties.forEach(p->log.trace("property: {}, amt: {}", p.getName(), p.getValue().toString()));
        Assertions.assertEquals(1, chemicalSubstance.properties.size());
    }

    private Optional<String> getSdProperty(String name) {
        String data="";
        switch(name) {
            case "cas.rn" :
                data ="134523-00-5";
                break;
            case "cas.index.name" :
                data = "1H-Pyrrole-1-heptanoic acid, 2-(4-fluorophenyl)-β,δ-dihydroxy-5-(1-methylethyl)-3-phenyl-4-[(phenylamino)carbonyl]-, (βR,δR)-";
                break;
            case "molecular.formula" :
                data = "C33H35FN2O5";
                break;
            case "molecular.weight":
                data ="558.64";
                break;
            case "melting.point.experimental":
                data ="159.1-190.6 °C";
                break;
            case "boiling.point.predicted":
                data ="722.2±60.0 °C    Press: 760 Torr";
                break;
            case "density.predicted":
                data ="1.23±0.1 g/cm3    Temp: 20 °C; Press: 760 Torr";
                break;
            case "pka.predicted":
                data ="4.29±0.10    Most Acidic Temp: 25 °C";
                break;
        }
        return Optional.of(data);
    }

    private SDRecordContext record = new SDRecordContext() {
        @Override
        public String getStructure() {
            String molfile = "Atorvastatin\\nC33H35FN2O5\\n134523-00-5 Copyright (C) 2022 ACS\\n 41 44  0  0  1  0  0  0  0  0999 V2000\\n    5.4999    5.7152    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    4.5947    4.4693    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\\n    4.8735    7.1221    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n   10.8024    6.8423    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   12.1361    6.0723    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   13.4698    6.8423    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\\n   14.8035    6.0723    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   13.4698    8.3823    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n   16.1372    6.8423    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\\n   17.4708    6.0723    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   16.1372    8.3823    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n   18.8045    6.8423    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   20.1382    6.0723    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n   18.8045    8.3823    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.7417    8.2050    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    8.8862    9.2355    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    6.2771    8.6809    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   13.8856    0.4189    0.0000 F   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.8014    4.2206    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    9.3078    4.5408    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.0314    5.5543    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    9.4688    6.0723    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\\n    8.0619    6.6987    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.1751    2.8137    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    8.0803    1.5678    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    5.6435    2.6527    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.4539    0.1610    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    5.0171    1.2459    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    5.9223    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   10.4522    3.5103    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   11.9169    3.9862    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   10.1320    2.0040    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   13.0613    2.9557    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   11.2765    0.9735    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   12.7411    1.4494    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    3.0631    4.6303    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    2.1579    3.3844    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    2.4368    6.0372    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    0.6264    3.5454    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    0.9052    6.1982    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    0.0000    4.9523    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n  1  2  1  0  0  0  0\\n  1  3  2  0  0  0  0\\n  1 21  1  0  0  0  0\\n  2 36  1  0  0  0  0\\n  4  5  1  0  0  0  0\\n  4 22  1  0  0  0  0\\n  5  6  1  0  0  0  0\\n  6  7  1  0  0  0  0\\n  6  8  1  1  0  0  0\\n  7  9  1  0  0  0  0\\n  9 10  1  0  0  0  0\\n  9 11  1  1  0  0  0\\n 10 12  1  0  0  0  0\\n 12 13  1  0  0  0  0\\n 12 14  2  0  0  0  0\\n 15 16  1  0  0  0  0\\n 15 17  1  0  0  0  0\\n 15 23  1  0  0  0  0\\n 18 35  1  0  0  0  0\\n 19 20  2  0  0  0  0\\n 19 21  1  0  0  0  0\\n 19 24  1  0  0  0  0\\n 20 22  1  0  0  0  0\\n 20 30  1  0  0  0  0\\n 21 23  2  0  0  0  0\\n 22 23  1  0  0  0  0\\n 24 25  2  0  0  0  0\\n 24 26  1  0  0  0  0\\n 25 27  1  0  0  0  0\\n 26 28  2  0  0  0  0\\n 27 29  2  0  0  0  0\\n 28 29  1  0  0  0  0\\n 30 31  2  0  0  0  0\\n 30 32  1  0  0  0  0\\n 31 33  1  0  0  0  0\\n 32 34  2  0  0  0  0\\n 33 35  2  0  0  0  0\\n 34 35  1  0  0  0  0\\n 36 37  2  0  0  0  0\\n 36 38  1  0  0  0  0\\n 37 39  1  0  0  0  0\\n 38 40  2  0  0  0  0\\n 39 41  2  0  0  0  0\\n 40 41  1  0  0  0  0\\nM  END";
            return molfile;
        }

        @Override
        public String getMolfileName() {
            return "Atorvastatin";
        }

        @Override
        public Optional<String> getProperty(String name) {
            return getSdProperty(name);
        }

        @Override
        public List<String> getProperties() {
            return Arrays.asList("cas.rn", "cas.index.name", "molecular.formula", "molecular.weight", "melting.point.experimental",
                    "boiling.point.predicted", "density.predicted", "pka.predicted");
        }

        @Override
        public Optional<String> resolveSpecial(String name) {
            return getProperty(name);
        }
    };
}
