package gsrs.module.substance.services;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.ginas.models.v1.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubstanceUuidCheckServiceTest {
    private SubstanceUuidCheckService service;
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        service = new SubstanceUuidCheckService();
        entityManager = mock(EntityManager.class);
        service.entityManager = entityManager;
        service.substanceRepository = mock(SubstanceRepository.class);
    }

    @Test
    void existingNoteUsesScalarQueryWithoutLoadingEntity() {
        Substance substance = new Substance();
        Note note = new Note();
        note.uuid = UUID.randomUUID();
        substance.notes.add(note);
        expectUuidQuery(Note.class, note.uuid, true);

        assertTrue(service.doesAnyUuidExist(substance));
        verify(entityManager, never()).find(any(), any());
    }

    @Test
    void absentNoteReturnsFalse() {
        Substance substance = new Substance();
        Note note = new Note();
        note.uuid = UUID.randomUUID();
        substance.notes.add(note);
        expectUuidQuery(Note.class, note.uuid, false);

        assertFalse(service.doesAnyUuidExist(substance));
    }

    @Test
    void existingSubstanceShortCircuitsNestedQueries() {
        Substance substance = new Substance();
        substance.uuid = UUID.randomUUID();
        when(service.substanceRepository.existsById(substance.uuid)).thenReturn(true);

        assertTrue(service.doesAnyUuidExist(substance));
        verifyNoInteractions(entityManager);
    }

    @Test
    void existingPropertyParameterReturnsTrue() {
        Substance substance = new Substance();
        Property property = new Property();
        Parameter parameter = new Parameter();
        parameter.uuid = UUID.randomUUID();
        property.getParameters().add(parameter);
        substance.properties.add(property);
        expectUuidQuery(Parameter.class, parameter.uuid, true);

        assertTrue(service.doesAnyUuidExist(substance));
    }

    @Test
    void agentModificationCheckedWithoutPhysicalModifications() {
        Substance substance = new Substance();
        substance.modifications = new Modifications();
        AgentModification agent = new AgentModification();
        agent.uuid = UUID.randomUUID();
        substance.modifications.agentModifications.add(agent);
        expectUuidQuery(AgentModification.class, agent.uuid, true);

        assertTrue(service.doesAnyUuidExist(substance));
    }

    @Test
    void structuralModification() {
        Substance substance = new Substance();
        substance.modifications = new Modifications();
        StructuralModification structuralModification = new StructuralModification();
        structuralModification.uuid = UUID.randomUUID();
        substance.modifications.structuralModifications.add(structuralModification);
        expectUuidQuery(AgentModification.class, structuralModification.uuid, true);

        assertTrue(service.doesAnyUuidExist(substance));
    }

    @Test
    void moietyChecksUuidAttributeRatherThanStringPrimaryKey() {
        ChemicalSubstance substance = new ChemicalSubstance();
        Moiety moiety = new Moiety();
        moiety.uuid = UUID.randomUUID();
        substance.moieties.add(moiety);
        expectUuidQuery(Moiety.class, moiety.uuid, true);

        assertTrue(service.doesAnyUuidExist(substance));
    }

    @SuppressWarnings("unchecked")
    private <T> void expectUuidQuery(Class<T> type, UUID uuid, boolean exists) {
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        CriteriaQuery<Integer> criteria = mock(CriteriaQuery.class);
        Root<T> root = mock(Root.class);
        Path<UUID> uuidPath = mock(Path.class);
        Expression<Integer> one = mock(Expression.class);
        Predicate predicate = mock(Predicate.class);
        TypedQuery<Integer> query = mock(TypedQuery.class);

        when(entityManager.getCriteriaBuilder()).thenReturn(builder);
        when(builder.createQuery(Integer.class)).thenReturn(criteria);
        when(criteria.from(type)).thenReturn(root);
        when(builder.literal(1)).thenReturn(one);
        when(criteria.select(one)).thenReturn(criteria);
        when(root.<UUID>get("uuid")).thenReturn(uuidPath);
        when(builder.equal(uuidPath, uuid)).thenReturn(predicate);
        when(criteria.where(predicate)).thenReturn(criteria);
        when(entityManager.createQuery(criteria)).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(exists ? List.of(1) : List.of());
    }
}
