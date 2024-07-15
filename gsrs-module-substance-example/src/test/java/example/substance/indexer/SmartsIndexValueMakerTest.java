package example.substance.indexer;

import gsrs.module.substance.indexers.SmartsIndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
class SmartsIndexValueMakerTest {

    @Test
    void testNitroGroup() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("Nitrobenzene");
        builder.setStructureWithDefaultReference("[O-][N+](=O)c1ccccc1");
        ChemicalSubstance nitrobenzene = builder.build();

        SmartsIndexValueMaker indexer = new SmartsIndexValueMaker();
        List<IndexableValue> indexedValues= new ArrayList<>();
        indexer.createIndexableValues(nitrobenzene, indexedValues::add);
        Assertions.assertTrue(indexedValues.stream().anyMatch(i->i.name().contains("StructureFacet") && i.value().equals("nitro")));
    }

    @Test
    void testNoNitroGroup() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("Toluene");
        builder.setStructureWithDefaultReference("Cc1ccccc1");
        ChemicalSubstance nitrobenzene = builder.build();

        SmartsIndexValueMaker indexer = new SmartsIndexValueMaker();
        List<IndexableValue> indexedValues= new ArrayList<>();
        indexer.createIndexableValues(nitrobenzene, indexedValues::add);
        Assertions.assertTrue(indexedValues.stream().anyMatch(i->i.name().contains("StructureFacet") && i.value().equals("false")));
    }

    @Test
    void testCarboxylateGroup() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("Nitrobenzene");
        builder.setStructureWithDefaultReference("c1ccccc1C(=O)O");
        ChemicalSubstance nitrobenzene = builder.build();

        SmartsIndexValueMaker indexer = new SmartsIndexValueMaker();
        Map<String, String> config = new HashMap<>();
        String acidGroupName = "carboxlic acid";
        config.put(acidGroupName, "C(O)=O");
        indexer.setRawNamedSmarts(config);
        List<IndexableValue> indexedValues= new ArrayList<>();
        indexer.createIndexableValues(nitrobenzene, indexedValues::add);
        Assertions.assertTrue(indexedValues.stream().anyMatch(i->i.name().contains("StructureFacet") && i.value().equals(acidGroupName)));
    }

}
