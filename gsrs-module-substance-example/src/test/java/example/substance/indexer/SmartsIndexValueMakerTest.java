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
        Assertions.assertTrue(indexedValues.stream().anyMatch(i->i.name().contains(SmartsIndexValueMaker.FACET_NAME_FULL) && i.value().equals("nitro")));
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
        Assertions.assertTrue(indexedValues.stream().anyMatch(i->i.name().contains(SmartsIndexValueMaker.FACET_NAME_FULL) && i.value().equals("false")));
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
        Assertions.assertTrue(indexedValues.stream().anyMatch(i->i.name().contains(SmartsIndexValueMaker.FACET_NAME_FULL) && i.value().equals(acidGroupName)));
    }

    @Test
    void testImidazoleOnce() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("MIDD-0301");
        builder.setStructureWithDefaultReference("C[C@@H]1c2c(C(=O)O)ncn2-c3ccc(cc3C(=N1)c4ccccc4F)Br");
        ChemicalSubstance mol1 = builder.build();

        SmartsIndexValueMaker indexer = new SmartsIndexValueMaker();
        Map<String, String> config = new HashMap<>();
        String moleculeName = "imidazole";
        config.put(moleculeName, "c1cnc[nH]1€C1=CN=CN1");
        indexer.setRawNamedSmarts(config);
        List<IndexableValue> indexedValues= new ArrayList<>();
        indexer.createIndexableValues(mol1, indexedValues::add);
        Assertions.assertEquals(1,
                indexedValues.stream().filter(v->v.name().equals(SmartsIndexValueMaker.FACET_NAME_FULL) && v.value().equals(moleculeName) ).count());
    }
}