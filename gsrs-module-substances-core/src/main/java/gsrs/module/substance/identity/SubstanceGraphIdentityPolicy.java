package gsrs.module.substance.identity;

import ix.core.models.Keyword;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;
import ix.ginas.models.v1.*;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Defines identity normalization at substance-graph persistence boundaries.
 *
 * <p>Assigned UUIDs are preserved for create, update, and bulk import. IDs mapped
 * by Hibernate as generated values (currently chemical structure IDs) are cleared
 * for new graphs. Cloning deliberately clears all identities so that the clone
 * cannot become attached to rows owned by the source substance.</p>
 *
 * <p>Database collision detection is intentionally outside this class because it
 * requires repository access. Callers must reject supplied UUIDs that already
 * belong to a different persisted graph.</p>
 */
public class SubstanceGraphIdentityPolicy {

    public void prepareForPersistence(Substance incoming,
                                      Substance persisted,
                                      SubstanceGraphOperation operation) {
        Objects.requireNonNull(operation, "operation is required");
        if (incoming == null) {
            return;
        }

        switch (operation) {
            case CREATE, BULK_IMPORT -> prepareNewGraph(incoming);
            case UPDATE -> validateUpdateIdentity(incoming, persisted);
            case CLONE -> prepareClone(incoming);
        }
    }

    private void prepareNewGraph(Substance substance) {
        substance.version = "1";
        clearGeneratedStructureIds(substance);
    }

    private void validateUpdateIdentity(Substance incoming, Substance persisted) {
        if (persisted == null) {
            throw new IllegalArgumentException("persisted substance is required for an update");
        }
        if (incoming.getUuid() != null && persisted.getUuid() != null
                && !incoming.getUuid().equals(persisted.getUuid())) {
            throw new IllegalArgumentException("an update cannot change the substance UUID");
        }
    }

    private void prepareClone(Substance substance) {
        substance.uuid = null;
        substance.version = "1";
        resetAssignedGraphIds(substance);
        clearGeneratedStructureIds(substance);
    }

    private void clearGeneratedStructureIds(Substance substance) {
        if (substance instanceof ChemicalSubstance chemicalSubstance) {
            clearStructureIdentity(chemicalSubstance.getStructure());
            if (chemicalSubstance.getMoieties() != null) {
                for (Moiety moiety : chemicalSubstance.getMoieties()) {
                    if (moiety != null) {
                        clearStructureIdentity(moiety.structure);
                    }
                }
            }
        }
        if (substance instanceof PolymerSubstance polymerSubstance && polymerSubstance.polymer != null) {
            clearStructureIdentity(polymerSubstance.polymer.displayStructure);
            clearStructureIdentity(polymerSubstance.polymer.idealizedStructure);
        }
    }

    private void clearStructureIdentity(GinasChemicalStructure structure) {
        if (structure != null) {
            structure.id = null;
            structure.version = null;
        }
    }

    private void resetAssignedGraphIds(Substance substance) {
        resetSubstanceOwnedGraphIds(substance);
        if (substance instanceof ChemicalSubstance chemicalSubstance) {
            resetChemicalAssignedIds(chemicalSubstance);
        }
        if (substance instanceof MixtureSubstance mixtureSubstance) {
            resetMixtureGraphIds(mixtureSubstance.mixture);
        }
        if (substance instanceof ProteinSubstance proteinSubstance) {
            resetProteinGraphIds(proteinSubstance.protein);
        }
        if (substance instanceof PolymerSubstance polymerSubstance) {
            resetPolymerGraphIds(polymerSubstance.polymer);
        }
        if (substance instanceof StructurallyDiverseSubstance structurallyDiverseSubstance) {
            resetStructurallyDiverseGraphIds(structurallyDiverseSubstance.structurallyDiverse);
        }
        if (substance instanceof SpecifiedSubstanceGroup1Substance specifiedSubstanceGroup1Substance) {
            resetSpecifiedSubstanceGraphIds(specifiedSubstanceGroup1Substance.specifiedSubstance);
        }
        if (substance instanceof NucleicAcidSubstance nucleicAcidSubstance) {
            resetNucleicAcidGraphIds(nucleicAcidSubstance.nucleicAcid);
        }
    }

    private void resetSubstanceOwnedGraphIds(Substance substance) {
        if (substance.names != null) {
            for (Name name : substance.names) {
                if (name == null) {
                    continue;
                }
                name.uuid = null;
                if (name.nameOrgs != null) {
                    for (NameOrg nameOrg : name.nameOrgs) {
                        if (nameOrg != null) {
                            nameOrg.uuid = null;
                        }
                    }
                }
            }
        }
        if (substance.codes != null) {
            substance.codes.stream().filter(Objects::nonNull).forEach(code -> code.uuid = null);
        }
        if (substance.notes != null) {
            substance.notes.stream().filter(Objects::nonNull).forEach(note -> note.uuid = null);
        }
        resetPropertyGraphIds(substance.properties);
        resetRelationshipGraphIds(substance.relationships);
        remapReferenceKeywords(substance, resetReferenceGraphIds(substance.references));
        resetModificationGraphIds(substance.modifications);
    }

    private Map<String, String> resetReferenceGraphIds(java.util.List<Reference> references) {
        Map<String, String> replacements = new LinkedHashMap<>();
        if (references == null) {
            return replacements;
        }
        for (Reference reference : references) {
            if (reference == null || reference.uuid == null) {
                continue;
            }
            UUID replacement = UUID.randomUUID();
            replacements.put(reference.uuid.toString(), replacement.toString());
            reference.uuid = replacement;
        }
        return replacements;
    }

    private void remapReferenceKeywords(Substance substance, Map<String, String> replacements) {
        if (replacements.isEmpty()) {
            return;
        }
        for (GinasAccessReferenceControlled child : substance.getAllChildrenCapableOfHavingReferences()) {
            if (child == null || child.getReferences() == null || child.getReferences().isEmpty()) {
                continue;
            }
            Set<Keyword> remappedReferences = new LinkedHashSet<>();
            boolean changed = false;
            for (Keyword keyword : child.getReferences()) {
                if (keyword == null) {
                    continue;
                }
                String replacement = keyword.term != null
                        && (keyword.label == null || GinasCommonSubData.REFERENCE.equals(keyword.label))
                        ? replacements.get(keyword.term)
                        : null;
                remappedReferences.add(replacement == null
                        ? keyword
                        : new Keyword(keyword.label, replacement));
                changed |= replacement != null;
            }
            if (changed) {
                child.setReferences(remappedReferences);
            }
        }
    }

    private void resetPropertyGraphIds(java.util.List<Property> properties) {
        if (properties == null) {
            return;
        }
        for (Property property : properties) {
            if (property == null) {
                continue;
            }
            property.uuid = null;
            resetAmountId(property.getValue());
            resetSubstanceReferenceId(property.getReferencedSubstance());
            if (property.getParameters() != null) {
                for (Parameter parameter : property.getParameters()) {
                    if (parameter != null) {
                        parameter.uuid = null;
                        resetAmountId(parameter.value);
                        resetSubstanceReferenceId(parameter.referencedSubstance);
                    }
                }
            }
        }
    }

    private void resetRelationshipGraphIds(java.util.List<Relationship> relationships) {
        if (relationships == null) {
            return;
        }
        for (Relationship relationship : relationships) {
            if (relationship != null) {
                relationship.uuid = null;
                relationship.originatorUuid = null;
                resetAmountId(relationship.amount);
                resetSubstanceReferenceId(relationship.relatedSubstance);
                resetSubstanceReferenceId(relationship.mediatorSubstance);
            }
        }
    }

    private void resetModificationGraphIds(Modifications modifications) {
        if (modifications == null) {
            return;
        }
        modifications.uuid = null;
        if (modifications.agentModifications != null) {
            for (AgentModification modification : modifications.agentModifications) {
                if (modification != null) {
                    modification.uuid = null;
                    resetSubstanceReferenceId(modification.agentSubstance);
                    resetAmountId(modification.amount);
                }
            }
        }
        if (modifications.physicalModifications != null) {
            for (PhysicalModification modification : modifications.physicalModifications) {
                if (modification == null) {
                    continue;
                }
                modification.uuid = null;
                if (modification.parameters != null) {
                    for (PhysicalParameter parameter : modification.parameters) {
                        if (parameter != null) {
                            parameter.uuid = null;
                            resetAmountId(parameter.amount);
                        }
                    }
                }
            }
        }
        if (modifications.structuralModifications != null) {
            for (StructuralModification modification : modifications.structuralModifications) {
                if (modification != null) {
                    modification.uuid = null;
                    resetAmountId(modification.extentAmount);
                    resetSubstanceReferenceId(modification.molecularFragment);
                }
            }
        }
    }

    private void resetChemicalAssignedIds(ChemicalSubstance chemicalSubstance) {
        if (chemicalSubstance.getMoieties() != null) {
            for (Moiety moiety : chemicalSubstance.getMoieties()) {
                if (moiety != null) {
                    moiety.uuid = null;
                    moiety.innerUuid = null;
                    resetAmountId(moiety.getCountAmount());
                }
            }
        }
    }

    private void resetMixtureGraphIds(Mixture mixture) {
        if (mixture == null) {
            return;
        }
        mixture.uuid = null;
        resetSubstanceReferenceId(mixture.parentSubstance);
        if (mixture.getMixture() != null) {
            for (Component component : mixture.getMixture()) {
                if (component != null) {
                    component.uuid = null;
                    resetSubstanceReferenceId(component.substance);
                }
            }
        }
    }

    private void resetProteinGraphIds(Protein protein) {
        if (protein == null) {
            return;
        }
        protein.uuid = null;
        if (protein.glycosylation != null) {
            protein.glycosylation.uuid = null;
        }
        if (protein.subunits != null) {
            protein.subunits.stream().filter(Objects::nonNull).forEach(subunit -> subunit.uuid = null);
        }
        if (protein.otherLinks != null) {
            protein.otherLinks.stream().filter(Objects::nonNull).forEach(link -> link.uuid = null);
        }
    }

    private void resetPolymerGraphIds(Polymer polymer) {
        if (polymer == null) {
            return;
        }
        polymer.uuid = null;
        if (polymer.classification != null) {
            polymer.classification.uuid = null;
            resetSubstanceReferenceId(polymer.classification.parentSubstance);
        }
        if (polymer.monomers != null) {
            for (Material material : polymer.monomers) {
                if (material != null) {
                    material.uuid = null;
                    resetSubstanceReferenceId(material.monomerSubstance);
                }
            }
        }
        if (polymer.structuralUnits != null) {
            polymer.structuralUnits.stream().filter(Objects::nonNull).forEach(unit -> unit.uuid = null);
        }
    }

    private void resetStructurallyDiverseGraphIds(StructurallyDiverse diverse) {
        if (diverse != null) {
            diverse.uuid = null;
            resetSubstanceReferenceId(diverse.parentSubstance);
            resetSubstanceReferenceId(diverse.hybridSpeciesMaternalOrganism);
            resetSubstanceReferenceId(diverse.hybridSpeciesPaternalOrganism);
        }
    }

    private void resetSpecifiedSubstanceGraphIds(SpecifiedSubstanceGroup1 specified) {
        if (specified == null) {
            return;
        }
        specified.uuid = null;
        if (specified.constituents != null) {
            for (SpecifiedSubstanceComponent component : specified.constituents) {
                if (component != null) {
                    component.uuid = null;
                    resetSubstanceReferenceId(component.substance);
                }
            }
        }
    }

    private void resetNucleicAcidGraphIds(NucleicAcid nucleicAcid) {
        if (nucleicAcid == null) {
            return;
        }
        nucleicAcid.uuid = null;
        resetModificationGraphIds(nucleicAcid.getModifications());
        if (nucleicAcid.getLinkages() != null) {
            nucleicAcid.getLinkages().stream().filter(Objects::nonNull).forEach(linkage -> linkage.uuid = null);
        }
        if (nucleicAcid.getSugars() != null) {
            nucleicAcid.getSugars().stream().filter(Objects::nonNull).forEach(sugar -> sugar.uuid = null);
        }
        if (nucleicAcid.getSubunits() != null) {
            nucleicAcid.getSubunits().stream().filter(Objects::nonNull).forEach(subunit -> subunit.uuid = null);
        }
    }

    private void resetAmountId(Amount amount) {
        if (amount != null) {
            amount.uuid = null;
        }
    }

    private void resetSubstanceReferenceId(SubstanceReference reference) {
        if (reference != null) {
            reference.uuid = null;
        }
    }
}
