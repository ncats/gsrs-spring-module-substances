package gsrs.module.substance.services;

import gsrs.module.substance.repository.*;
import ix.ginas.models.v1.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


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
                        if( org.uuid != null && entityManager.find(NameOrg.class, org.uuid) != null )  {
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
                if(note.uuid != null && entityManager.find(Note.class, note.uuid)!=null) {
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
                    if( moiety.uuid != null && entityManager.find(Moiety.class, moiety.uuid.toString()) != null ) {
                        log.trace("chemical moiety ID exists");
                        return true;
                    }
                }
            }
        } else if( substance instanceof PolymerSubstance polymerSubstance) {
            if( polymerSubstance.polymer.uuid != null && entityManager.find(Polymer.class, polymerSubstance.polymer.uuid)!=null) {
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
                    && entityManager.find(PolymerClassification.class, polymerSubstance.polymer.classification.uuid)!=null ) {
                log.trace("polymer classification uuid exists");
                return true;
            }
        } else if( substance instanceof ProteinSubstance proteinSubstance) {
            if( proteinSubstance.protein.uuid != null && entityManager.find(Protein.class, proteinSubstance.protein.uuid)!=null) {
                log.trace("protein UUID exists");
                return true;
            }
        } else if( substance instanceof StructurallyDiverseSubstance structurallyDiverseSubstance) {
            if( structurallyDiverseSubstance.structurallyDiverse.uuid != null
                    && entityManager.find(StructurallyDiverse.class, structurallyDiverseSubstance.structurallyDiverse.uuid)!=null) {
                log.trace("str div UUID exists");
                return true;
            }
        } else if( substance instanceof NucleicAcidSubstance nucleicAcidSubstance) {
            if( nucleicAcidSubstance.nucleicAcid.uuid != null
                    && entityManager.find(NucleicAcid.class, nucleicAcidSubstance.nucleicAcid.uuid)!=null) {
                log.trace("nucleic acid UUID exists");
                return true;
            }
        } else if( substance instanceof MixtureSubstance mixtureSubstance) {
            if( mixtureSubstance.mixture.uuid != null
                    && entityManager.find(Mixture.class, mixtureSubstance.mixture.uuid)!=null) {
                log.trace("mixture UUID exists");
                return true;
            }
            if( mixtureSubstance.mixture.components != null  && !mixtureSubstance.mixture.components.isEmpty()) {
                for(Component component : mixtureSubstance.mixture.components) {
                    if( component.uuid != null && entityManager.find(Component.class, component.uuid)!=null)  {
                        log.trace("mixture componenet uuid exists");
                        return true;
                    }
                }
            }
        } else if( substance instanceof SpecifiedSubstanceGroup1Substance specifiedSubstanceGroup1Substance) {
            if( specifiedSubstanceGroup1Substance.specifiedSubstance.uuid != null
                    && entityManager.find(SpecifiedSubstanceGroup1.class, specifiedSubstanceGroup1Substance.specifiedSubstance.uuid)!=null) {
                log.trace("specified substance UUID exists");
                return true;
            }
            if( specifiedSubstanceGroup1Substance.specifiedSubstance.constituents != null
                    && !specifiedSubstanceGroup1Substance.specifiedSubstance.constituents.isEmpty()) {
                for(SpecifiedSubstanceComponent constituent : specifiedSubstanceGroup1Substance.specifiedSubstance.constituents) {
                    if( constituent.uuid != null && entityManager.find(SpecifiedSubstanceComponent.class, constituent.uuid)!=null)  {
                        log.trace("SSG1 componenet uuid exists");
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
                    && entityManager.find(SubstanceReference.class, relationship.relatedSubstance.uuid) != null) {
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
                if( property.uuid != null && entityManager.find(Property.class, property.uuid) != null){
                    log.trace("property exists");
                    return true;
                }
                if( property.getValue() != null &&  property.getValue().uuid != null
                        && entityManager.find(Amount.class, property.getValue().uuid) != null) {
                    log.trace("property value exists");
                    return true;
                }
                if(anyParameterExists(property.getParameters())) {
                    log.trace("property parameter uuid exists");
                }

                if( property.getReferencedSubstance() != null && property.getReferencedSubstance().uuid != null
                    && entityManager.find(SubstanceReference.class, property.getReferencedSubstance().uuid) != null ) {
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

            for (PhysicalModification physicalModification : substance.modifications.physicalModifications) {
                if (physicalModification.uuid != null && entityManager.find(PhysicalModification.class, physicalModification.uuid) != null) {
                    log.trace("physical modifications ID exists");
                    return true;
                }
                if (physicalModification.parameters != null && !physicalModification.parameters.isEmpty()) {
                    for (PhysicalParameter physicalParameter : physicalModification.parameters) {
                        if (physicalParameter.uuid != null && entityManager.find(PhysicalParameter.class, physicalParameter.uuid) != null) {
                            log.trace("physical parameter exists");
                            return true;
                        }
                    }
                }

                for (AgentModification agentModification : substance.modifications.agentModifications) {
                    log.trace("going to look up agentModification.uuid {}", agentModification.uuid);
                    if (agentModification.uuid != null && entityManager.find(AgentModification.class, agentModification.uuid) != null) {
                        log.trace("agent modification UUID exists");
                        return true;
                    }
                    if( agentModification.agentSubstance.uuid != null
                            && entityManager.find(SubstanceReference.class, agentModification.agentSubstance.uuid) != null) {
                        log.trace("agent modification referenced substance UUID exists");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean anyParameterExists(List<Parameter> parameters) {
        if( parameters == null || parameters.isEmpty()){
            return false;
        }
        for( Parameter parameter : parameters) {
            if(parameter.uuid != null && entityManager.find(Parameter.class, parameter.uuid) != null) {
                return true;
            }
        }
        return false;
    }


}
