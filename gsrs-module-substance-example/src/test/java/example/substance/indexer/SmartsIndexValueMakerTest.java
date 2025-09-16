package example.substance.indexer;

import gov.nih.ncats.molwitch.Chemical;
import gsrs.module.substance.indexers.SmartsIndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
        Assertions.assertTrue(indexedValues.stream().noneMatch(i->i.name().contains(SmartsIndexValueMaker.FACET_NAME_FULL) && i.value().equals("true")));
    }

    @Test
    void testCarboxylateGroup() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("benzoic acid");
        builder.setStructureWithDefaultReference("c1ccccc1C(=O)O");
        ChemicalSubstance nitrobenzene = builder.build();

        String acidGroupName = "carboxlic acid";
        SmartsIndexValueMaker indexer = new SmartsIndexValueMaker();
        LinkedHashMap<Integer, Map<String, Object>> config = new LinkedHashMap<>();
        Map<String, Object> configItems = new ConcurrentHashMap<>();
        configItems.put("indexableName", acidGroupName);
        Map<String, String> smarts = new LinkedHashMap<>();
        smarts.put("0", "C(O)=O");
        configItems.put("smarts", smarts);
        config.put(1, configItems);
        indexer.setRawIndexables(config);
        List<IndexableValue> indexedValues= new ArrayList<>();
        indexer.createIndexableValues(nitrobenzene, indexedValues::add);
        Assertions.assertTrue(indexedValues.stream().anyMatch(i->i.name().contains(SmartsIndexValueMaker.FACET_NAME_FULL) && i.value().equals(acidGroupName)));
    }

    @Test
    void testImidazoleOnce() throws IOException {
        String molfilePath = "/molfiles/4XXR6FT8ZA.mol";
        String molfileText = IOUtils.toString(
                this.getClass().getResourceAsStream(molfilePath),
                "UTF-8"
        );

        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("MIDD-0301");
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile= molfileText;
        builder.setStructure(structure);
        ChemicalSubstance mol1 = builder.build();

        SmartsIndexValueMaker indexer = new SmartsIndexValueMaker();
        String moleculeName = "imidazole";
        LinkedHashMap<Integer, Map<String, Object>> config = new LinkedHashMap<>();
        Map<String, Object> configItems = new ConcurrentHashMap<>();
        configItems.put("indexableName", moleculeName);
        Map<String, String> smarts = new LinkedHashMap<>();
        smarts.put("0", "c1cnc[nH]1");
        smarts.put("1", "C1=CN=CN1");
        configItems.put("smarts", smarts); //₠
        config.put(1, configItems);
        indexer.setRawIndexables(config);
        List<IndexableValue> indexedValues= new ArrayList<>();
        indexer.createIndexableValues(mol1, indexedValues::add);
        Assertions.assertEquals(1,
                indexedValues.stream().filter(v->v.name().equals(SmartsIndexValueMaker.FACET_NAME_FULL) && v.value().equals(moleculeName) ).count());
    }

    @Test
    void testTetrazoleMatch() throws IOException {
        String molfilePath = "/molfiles/D34J7PAT68.mol";
        String molfileText = IOUtils.toString(
                this.getClass().getResourceAsStream(molfilePath),
                "UTF-8"
        );

        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("MIDD-0301");
        builder.setStructureWithDefaultReference("C(O)(=O)C1N2[C@@]([C@H](NC(/C(=N\\OC)/c3nc(N)sc3)=O)C2=O)(SC=C1Cn4nc(C)nn4)[H]");
        ChemicalSubstance mol1 = builder.build();

        SmartsIndexValueMaker indexer = new SmartsIndexValueMaker();
        String moleculeName = "Cefteram, Δ2";
        LinkedHashMap<Integer, Map<String, Object>> config = new LinkedHashMap<>();
        Map<String, Object> configItems = new ConcurrentHashMap<>();
        configItems.put("indexableName", moleculeName);
        Map<String, String> smarts = new LinkedHashMap<>();
        smarts.put("2", "n(nn1)cn1");
        configItems.put("smarts",  smarts); //original smarts: "N(=NN1)C=N1"
        config.put(1, configItems);
        indexer.setRawIndexables(config);
        List<IndexableValue> indexedValues= new ArrayList<>();
        indexer.createIndexableValues(mol1, indexedValues::add);
        Assertions.assertEquals(1,
                indexedValues.stream().filter(v->v.name().equals(SmartsIndexValueMaker.FACET_NAME_FULL) && v.value().equals(moleculeName) ).count());
    }
}