package gsrs.module.substance.services;

import gsrs.module.substance.repository.*;
import ix.ginas.models.v1.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Service
@Transactional(readOnly = true)
@Slf4j
public class SubstanceUuidCheckService {
    
    @Autowired
    SubstanceRepository substanceRepository;

    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    NameRepository nameRepository;
    
    @Autowired
    CodeRepository codeRepository;
    
    @Autowired
    ReferenceRepository referenceRepository;
    
    @Autowired
    RelationshipRepository relationshipRepository;

    @Autowired
    StructureRepository structureRepository;

    @Autowired
    ModificationRepository modificationRepository;

    public boolean doesAnyUuidExist(Substance substance) {
        Objects.requireNonNull(substance, "substance must not be null");
        // Check supplied entity UUIDs against the database.
        if( substance.uuid != null && substanceRepository.existsById(substance.uuid)) {
          log.trace("substance uuid exists");
          return true;
        }
        if( substance.names != null ) {
            for( Name name : substance.names) {
                if(name.uuid != null && nameRepository.existsById(name.uuid)) {
                    log.trace("name UUID exists");
                    return true;
                }
                if( name.nameOrgs != null && !name.nameOrgs.isEmpty()) {
                    for(NameOrg org : name.nameOrgs) {
                        if( org.uuid != null && uuidExists(NameOrg.class, org.uuid) )  {
                            log.trace("name org exists");
                            return true;
                        }
                    }
                }
            }
        }

        if( substance.codes != null ) {
            for( Code code : substance.codes) {
                if(code.uuid != null && codeRepository.existsById(code.uuid)) {
                    log.trace("code exists");
                    return true;
                }
            }
        }

        if(substance.notes != null &&!substance.notes.isEmpty()) {
            for(Note note: substance.notes) {
                log.trace("going to look up note.uuid {}", note.uuid);
                if(note.uuid != null && uuidExists(Note.class, note.uuid)) {
                    log.trace("note UUID exists");
                    return true;
                }
            }
        }

        if( substance instanceof ChemicalSubstance chemicalSubstance) {
            if( chemicalSubstance.getStructure() != null && chemicalSubstance.getStructure().id != null
            &&  structureRepository.existsById(chemicalSubstance.getStructure().id)) {
                log.trace("chemical structure ID exists");
                return true;
            }
            if(chemicalSubstance.getMoieties() != null && !chemicalSubstance.getMoieties().isEmpty()) {
                for(Moiety moiety : chemicalSubstance.getMoieties()) {
                    if( moiety.uuid != null && uuidExists(Moiety.class, moiety.uuid) ) {
                        log.trace("chemical moiety ID exists");
                        return true;
                    }
                }
            }
        } else if( substance instanceof PolymerSubstance polymerSubstance) {
            if( polymerSubstance.polymer.uuid != null && uuidExists(Polymer.class, polymerSubstance.polymer.uuid)) {
                log.trace("polymer UUID exists");
                return true;
            }
            if( polymerSubstance.polymer.displayStructure != null && polymerSubstance.polymer.displayStructure.id != null
                && structureRepository.existsById(polymerSubstance.polymer.displayStructure.id)){
                log.trace("polymer display structure ID exists");
                return true;
            }
            if( polymerSubstance.polymer.idealizedStructure != null && polymerSubstance.polymer.idealizedStructure.id != null
                    && structureRepository.existsById(polymerSubstance.polymer.idealizedStructure.id)){
                log.trace("polymer idealized structure ID exists");
                return true;
            }
            if(polymerSubstance.polymer.classification != null
                    && uuidExists(PolymerClassification.class, polymerSubstance.polymer.classification.uuid) ) {
                log.trace("polymer classification uuid exists");
                return true;
            }
        } else if( substance instanceof ProteinSubstance proteinSubstance) {
            if( proteinSubstance.protein.uuid != null && uuidExists(Protein.class, proteinSubstance.protein.uuid)) {
                log.trace("protein UUID exists");
                return true;
            }
        } else if( substance instanceof StructurallyDiverseSubstance structurallyDiverseSubstance) {
            if( structurallyDiverseSubstance.structurallyDiverse.uuid != null
                    && uuidExists(StructurallyDiverse.class, structurallyDiverseSubstance.structurallyDiverse.uuid)) {
                log.trace("str div UUID exists");
                return true;
            }
        } else if( substance instanceof NucleicAcidSubstance nucleicAcidSubstance) {
            if( nucleicAcidSubstance.nucleicAcid.uuid != null
                    && uuidExists(NucleicAcid.class, nucleicAcidSubstance.nucleicAcid.uuid)) {
                log.trace("nucleic acid UUID exists");
                return true;
            }
        } else if( substance instanceof MixtureSubstance mixtureSubstance) {
            if( mixtureSubstance.mixture.uuid != null
                    && uuidExists(Mixture.class, mixtureSubstance.mixture.uuid)) {
                log.trace("mixture UUID exists");
                return true;
            }
            if( mixtureSubstance.mixture.components != null  && !mixtureSubstance.mixture.components.isEmpty()) {
                for(Component component : mixtureSubstance.mixture.components) {
                    if( component.uuid != null && uuidExists(Component.class, component.uuid))  {
                        log.trace("mixture component uuid exists");
                        return true;
                    }
                }
            }
        } else if( substance instanceof SpecifiedSubstanceGroup1Substance specifiedSubstanceGroup1Substance) {
            if( specifiedSubstanceGroup1Substance.specifiedSubstance.uuid != null
                    && uuidExists(SpecifiedSubstanceGroup1.class, specifiedSubstanceGroup1Substance.specifiedSubstance.uuid)) {
                log.trace("specified substance UUID exists");
                return true;
            }
            if( specifiedSubstanceGroup1Substance.specifiedSubstance.constituents != null
                    && !specifiedSubstanceGroup1Substance.specifiedSubstance.constituents.isEmpty()) {
                for(SpecifiedSubstanceComponent constituent : specifiedSubstanceGroup1Substance.specifiedSubstance.constituents) {
                    if( constituent.uuid != null && uuidExists(SpecifiedSubstanceComponent.class, constituent.uuid))  {
                        log.trace("SSG1 component uuid exists");
                        return true;
                    }
                }
            }
        }

        if( substance.relationships != null ){
            for(Relationship relationship : substance.relationships) {
                if( relationship.uuid != null && relationshipRepository.existsById(relationship.uuid)) {
                    log.trace("relationship UUID exists");
                    return true;
                }
                if( relationship.relatedSubstance != null && relationship.relatedSubstance.uuid != null
                    && uuidExists(SubstanceReference.class, relationship.relatedSubstance.uuid)) {
                    log.trace("substance ref id exists");
                    return true;
                }
            }
        }

        if( substance.references != null){
            for(Reference reference : substance.references){
                if( reference.uuid != null && referenceRepository.existsById(reference.uuid)) {
                    log.trace("reference uuid exists");
                    return true;
                }
            }
        }

        if( substance.properties != null ) {
            for( Property property : substance.properties) {
                if( property.uuid != null && uuidExists(Property.class, property.uuid)){
                    log.trace("property exists");
                    return true;
                }
                if( property.getValue() != null &&  property.getValue().uuid != null
                        && uuidExists(Amount.class, property.getValue().uuid)) {
                    log.trace("property value exists");
                    return true;
                }
                if(anyParameterExists(property.getParameters())) {
                    log.trace("property parameter uuid exists");
                    return true;
                }

                if( property.getReferencedSubstance() != null && property.getReferencedSubstance().uuid != null
                    && uuidExists(SubstanceReference.class, property.getReferencedSubstance().uuid) ) {
                    log.trace("property substance ref uuid exists");
                    return true;
                }
            }
        }

        if( substance.hasModifications()) {
            if (substance.modifications.uuid != null && modificationRepository.existsById(substance.modifications.uuid)) {
                log.trace("modifications parent ID exists");
                return true;
            }

            for (AgentModification agentModification : substance.modifications.agentModifications) {
                log.trace("going to look up agentModification.uuid {}", agentModification.uuid);
                if (agentModification.uuid != null && uuidExists(AgentModification.class, agentModification.uuid)) {
                    log.trace("agent modification UUID exists");
                    return true;
                }
                if( agentModification.agentSubstance != null && agentModification.agentSubstance.uuid != null
                        && uuidExists(SubstanceReference.class, agentModification.agentSubstance.uuid)) {
                    log.trace("agent modification referenced substance UUID exists");
                    return true;
                }
            }

            for (PhysicalModification physicalModification : substance.modifications.physicalModifications) {
                if (physicalModification.uuid != null && uuidExists(PhysicalModification.class, physicalModification.uuid)) {
                    log.trace("physical modifications ID exists");
                    return true;
                }
                if (physicalModification.parameters != null && !physicalModification.parameters.isEmpty()) {
                    for (PhysicalParameter physicalParameter : physicalModification.parameters) {
                        if (physicalParameter.uuid != null && uuidExists(PhysicalParameter.class, physicalParameter.uuid)) {
                            log.trace("physical parameter exists");
                            return true;
                        }
                    }
                }
            }

            for( StructuralModification structuralModification : substance.modifications.structuralModifications) {
                if(structuralModification.uuid != null && uuidExists(StructuralModification.class, structuralModification.uuid)) {
                    log.trace("structural modification UUID exists");
                    return true;
                }
                if(structuralModification.molecularFragment != null && structuralModification.molecularFragment.uuid != null
                    && uuidExists(SubstanceReference.class, structuralModification.molecularFragment.uuid)) {
                    log.trace("str mod ref substance exists");
                    return true;
                }
            }

        }
        return false;
    }

    /**
     * Select a scalar so Hibernate does not hydrate the entity or fetch its associations.
     * Match the UUID attribute explicitly: Moiety's primary key is innerUuid, not uuid.
     */
    private <T> boolean uuidExists(Class<T> entityType, UUID uuid) {
        if (uuid == null) {
            return false;
        }
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Integer> query = builder.createQuery(Integer.class);
        Root<T> root = query.from(entityType);
        query.select(builder.literal(1))
                .where(builder.equal(root.get("uuid"), uuid));
        return !entityManager.createQuery(query)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
    }

    private boolean anyParameterExists(List<Parameter> parameters) {
        if( parameters == null || parameters.isEmpty()){
            return false;
        }
        for( Parameter parameter : parameters) {
            if(parameter.uuid != null && uuidExists(Parameter.class, parameter.uuid)) {
                return true;
            }
        }
        return false;
    }


}
