package gsrs.module.substance.indexers;

import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class SmartsIndexValueMaker implements IndexValueMaker<Substance> {

    private  final static String FACET_NAME_STEMP="_Structure_Facet";

    private String configData = "[$([NX3](=O)=O),$([NX3+](=O)[O-])][!#8]_nitro€[NX2]=[OX1]_nitroso";
    List<SmartsIndexable> indexables;
    public SmartsIndexValueMaker(){
        indexables = new ArrayList<>();
        String[] tokens = configData.split("\\€");
        for(String token : tokens) {
            String[] parts = token.split("\\_");
            SmartsIndexable indexable = new SmartsIndexable(parts[0], parts[1]);
            indexables.add(indexable);
        }
    }
    @Override
    public Class<Substance> getIndexedEntityClass() {
        return Substance.class;
    }

    @Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {
        if( substance instanceof ChemicalSubstance) {
            ChemicalSubstance chemical = (ChemicalSubstance) substance;
            chemical.getStructure().m
        }
    }
}
