package example.substance.indexer;

import gsrs.module.substance.indexers.MixtureStructureHashIndexValueMaker;
import gsrs.module.substance.indexers.ModificationStructureHashIndexValueMaker;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.models.Value;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Component;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Mixture;
import ix.ginas.models.v1.MixtureSubstance;
import ix.ginas.models.v1.Modifications;
import ix.ginas.models.v1.StructuralModification;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

class StructureHashReferenceIndexValueMakerTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    private StructureProcessor structureProcessor;

    @Test
    void mixtureIndexerRecomputesReferencedChemicalHashesWithoutLazyStructureProperties() throws Exception {
        SubstanceReference reference = reference();
        ChemicalSubstance component = chemicalWithLazyStructureProperties();
        SubstanceRepository repository = repositoryReturning(reference, component);

        MixtureStructureHashIndexValueMaker indexer = new MixtureStructureHashIndexValueMaker();
        ReflectionTestUtils.setField(indexer, "substanceRepository", repository);
        ReflectionTestUtils.setField(indexer, "structureProcessor", structureProcessor);

        MixtureSubstance mixtureSubstance = new MixtureSubstance();
        mixtureSubstance.mixture = new Mixture();
        Component mixtureComponent = new Component();
        mixtureComponent.substance = reference;
        mixtureSubstance.mixture.components.add(mixtureComponent);

        List<IndexableValue> values = new ArrayList<>();
        Assertions.assertDoesNotThrow(() -> indexer.createIndexableValues(mixtureSubstance, values::add));

        assertHasRootStructureHash(values);
    }

    @Test
    void modificationIndexerRecomputesReferencedChemicalHashesWithoutLazyStructureProperties() throws Exception {
        SubstanceReference reference = reference();
        ChemicalSubstance component = chemicalWithLazyStructureProperties();
        SubstanceRepository repository = repositoryReturning(reference, component);

        ModificationStructureHashIndexValueMaker indexer = new ModificationStructureHashIndexValueMaker();
        ReflectionTestUtils.setField(indexer, "substanceRepository", repository);
        ReflectionTestUtils.setField(indexer, "structureProcessor", structureProcessor);

        Substance modifiedSubstance = new Substance();
        modifiedSubstance.modifications = new Modifications();
        StructuralModification structuralModification = new StructuralModification();
        structuralModification.molecularFragment = reference;
        modifiedSubstance.modifications.structuralModifications.add(structuralModification);

        List<IndexableValue> values = new ArrayList<>();
        Assertions.assertDoesNotThrow(() -> indexer.createIndexableValues(modifiedSubstance, values::add));

        assertHasRootStructureHash(values);
    }

    private ChemicalSubstance chemicalWithLazyStructureProperties() throws Exception {
        Structure processed = structureProcessor.taskFor("CCO")
                .standardize(true)
                .build()
                .instrument()
                .getStructure();
        GinasChemicalStructure structure = new GinasChemicalStructure(processed);
        structure.properties = lazyFailureList();

        ChemicalSubstance chemicalSubstance = new ChemicalSubstance();
        chemicalSubstance.setStructure(structure);
        return chemicalSubstance;
    }

    private SubstanceRepository repositoryReturning(SubstanceReference reference, ChemicalSubstance component) {
        SubstanceRepository repository = Mockito.mock(SubstanceRepository.class);
        Mockito.doReturn(component).when(repository).findBySubstanceReference(reference);
        return repository;
    }

    private SubstanceReference reference() {
        SubstanceReference reference = new SubstanceReference();
        reference.refuuid = UUID.randomUUID().toString();
        return reference;
    }

    private List<Value> lazyFailureList() {
        return new AbstractList<Value>() {
            @Override
            public Value get(int index) {
                throw lazyFailure();
            }

            @Override
            public int size() {
                throw lazyFailure();
            }

            @Override
            public Iterator<Value> iterator() {
                throw lazyFailure();
            }
        };
    }

    private LazyInitializationException lazyFailure() {
        return new LazyInitializationException("could not initialize proxy - no Session");
    }

    private void assertHasRootStructureHash(List<IndexableValue> values) {
        Assertions.assertTrue(values.stream()
                        .anyMatch(value -> "root_structure_properties_STEREO_INSENSITIVE_HASH".equals(value.name())),
                "Expected root structure hash values to be indexed");
    }
}
