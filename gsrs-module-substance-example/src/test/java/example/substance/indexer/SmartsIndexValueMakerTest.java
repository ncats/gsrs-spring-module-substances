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
        Assertions.assertTrue(indexedValues.stream().noneMatch(i->i.name().contains(SmartsIndexValueMaker.FACET_NAME_FULL) && i.value().equals("true")));
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