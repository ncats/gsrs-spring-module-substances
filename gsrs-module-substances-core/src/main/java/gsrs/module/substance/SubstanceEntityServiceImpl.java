package gsrs.module.substance;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ArrayNode;
import gov.nih.ncats.common.sneak.Sneak;
import gsrs.EntityPersistAdapter;
import gsrs.controller.IdHelpers;
import gsrs.events.AbstractEntityCreatedEvent;
import gsrs.events.AbstractEntityUpdatedEvent;
import gsrs.json.JsonEntityUtil;
import gsrs.module.substance.events.SubstanceCreatedEvent;
import gsrs.module.substance.events.SubstanceUpdatedEvent;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.services.SubstanceBulkLoadServiceConfiguration;
import gsrs.service.AbstractGsrsEntityService;
import gsrs.validator.GsrsValidatorFactory;
import gsrs.validator.ValidatorConfig;
import ix.core.EntityFetcher;
import ix.core.chem.StructureProcessor;
import ix.core.models.ForceUpdatableModel;
import ix.core.models.Keyword;
import ix.core.models.Structure;
import ix.core.util.EntityUtils;
import ix.core.util.LogUtil;
import ix.core.validator.*;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonData;
import ix.ginas.models.GinasCommonSubData;
import ix.ginas.models.v1.Linkage;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Amount;
import ix.ginas.models.v1.AgentModification;
import ix.ginas.models.v1.Component;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Glycosylation;
import ix.ginas.models.v1.Material;
import ix.ginas.models.v1.Moiety;
import ix.ginas.models.v1.Mixture;
import ix.ginas.models.v1.MixtureSubstance;
import ix.ginas.models.v1.Modifications;
import ix.ginas.models.v1.NucleicAcid;
import ix.ginas.models.v1.NucleicAcidSubstance;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.NameOrg;
import ix.ginas.models.v1.Note;
import ix.ginas.models.v1.OtherLinks;
import ix.ginas.models.v1.PhysicalModification;
import ix.ginas.models.v1.PhysicalParameter;
import ix.ginas.models.v1.Polymer;
import ix.ginas.models.v1.PolymerClassification;
import ix.ginas.models.v1.PolymerSubstance;
import ix.ginas.models.v1.Parameter;
import ix.ginas.models.v1.Property;
import ix.ginas.models.v1.Protein;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.SpecifiedSubstanceComponent;
import ix.ginas.models.v1.SpecifiedSubstanceGroup1;
import ix.ginas.models.v1.SpecifiedSubstanceGroup1Substance;
import ix.ginas.models.v1.StructuralModification;
import ix.ginas.models.v1.StructurallyDiverse;
import ix.ginas.models.v1.StructurallyDiverseSubstance;
import ix.ginas.models.v1.Sugar;
import ix.ginas.models.v1.Subunit;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import ix.ginas.models.v1.Unit;
import ix.ginas.utils.JsonSubstanceFactory;
import ix.ginas.utils.validation.ValidatorFactory;
import ix.ginas.utils.validation.strategy.BatchProcessingStrategy;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategy;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactory;
import ix.utils.Util;
import ix.utils.pojopatch.PojoDiff;
import ix.utils.pojopatch.PojoPatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Service
@Slf4j
public class SubstanceEntityServiceImpl extends AbstractGsrsEntityService<Substance, UUID> implements SubstanceEntityService {
    public static final String  CONTEXT = "substances";
    private static final Set<String> SERVER_MANAGED_AUDIT_FIELDS = Set.of(
            "created",
            "createdBy",
            "lastEdited",
            "lastEditedBy",
            "approved",
            "approvedBy"
    );


    public SubstanceEntityServiceImpl() {
        super(CONTEXT,  IdHelpers.UUID, "gsrs_exchange", "substance.created", "substance.updated");
    }

    @Autowired
    private SubstanceRepository repository;

    private JsonMapper  objectMapper  = JsonMapper.builderWithJackson2Defaults().build();

    @Autowired
    private StructureProcessor structureProcessor;

    @Autowired
    private GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory;

    @Autowired
    private SubstanceBulkLoadServiceConfiguration bulkLoadServiceConfiguration;

    @Autowired
    private EntityPersistAdapter entityPersistAdapter;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private GsrsValidatorFactory validatorFactoryService;

    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }

    @Override
    public UUID parseIdFromString(String idAsString) {
        return UUID.fromString(idAsString);
    }



    protected GsrsProcessingStrategy createProcessingStrategyFor(ValidatorConfig.METHOD_TYPE type){
        if(type == ValidatorConfig.METHOD_TYPE.BATCH){
            return new BatchProcessingStrategy(gsrsProcessingStrategyFactory.createNewStrategy(bulkLoadServiceConfiguration.getBatchProcessingStrategy()));
        }
        return gsrsProcessingStrategyFactory.createNewDefaultStrategy();
    }
    @Override
    protected <T> ValidatorCallback createCallbackFor(T object, ValidationResponse<T> response, ValidatorConfig.METHOD_TYPE type) {

        GsrsProcessingStrategy strategy = createProcessingStrategyFor(type);
        ValidationResponseBuilder<T> builder = new ValidationResponseBuilder<T>(object, response, strategy){
            @Override
            public void complete() {
                if(object instanceof Substance) {
                    ValidationResponse<T> resp = buildResponse();

                    List<GinasProcessingMessage> messages = resp.getValidationMessages()
                            .stream()
                            .filter(m -> m instanceof GinasProcessingMessage)
                            .map(m -> (GinasProcessingMessage) m)
                            .collect(Collectors.toList());
                    //processMessage, handleMessages, addProblems?
                    //Why all 3? because right now each of these methods might set or change fields in validation response.
                    messages.stream().forEach(strategy::processMessage);
                    resp.setValid(false);
                    if (strategy.handleMessages((Substance) object, messages)) {
                        resp.setValid(true);
                    }
                    strategy.addProblems((Substance) object, messages);

                    strategy.setIfValid(resp, messages);
                }
            }
        };
        return builder;
    }

    @Override
    protected Substance fromNewJson(JsonNode json) throws IOException {
        return JsonSubstanceFactory.makeSubstance(json);

    }

    @Override
    public Page page(Pageable pageable) {

        return repository.findAll(pageable);
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional
    protected Substance update(Substance substance) {
//        controlledVocabulary.

//        JsonSubstanceFactory.fixOwners(substance, true);
        initializeExistingStructureVersions(substance);

        //first bump version?
        substance.forceUpdate();

        markCollectionOwnersDirtyForFlush(substance);

        //postUpdate/etc only gets called on flush, apparently?
        EntityManager entityManager = getEntityManager();
        Substance merged = entityManager.contains(substance) ? substance : entityManager.merge(substance);
        entityManager.flush();
        return merged;
    }

    private void markCollectionOwnersDirtyForFlush(Substance substance) {
        if (substance.names != null) {
            for (Name name : substance.names) {
                if (name != null) {
                    name.setIsDirty("nameOrgs");
                }
            }
        }
        if (!(substance instanceof ChemicalSubstance chemicalSubstance)) {
            return;
        }
        markStructureCollectionsDirty(chemicalSubstance.getStructure());
        if (chemicalSubstance.getMoieties() != null) {
            for (Moiety moiety : chemicalSubstance.getMoieties()) {
                if (moiety != null) {
                    markStructureCollectionsDirty(moiety.structure);
                }
            }
        }
    }

    private void markStructureCollectionsDirty(GinasChemicalStructure structure) {
        if (structure == null) {
            return;
        }
        structure.setIsDirty("links");
        structure.setIsDirty("properties");
    }

    private void initializeExistingStructureVersions(Substance substance) {
        if (substance instanceof ChemicalSubstance chemicalSubstance) {
            initializeExistingStructureVersion(chemicalSubstance.getStructure());
            if (chemicalSubstance.getMoieties() != null) {
                for (Moiety moiety : chemicalSubstance.getMoieties()) {
                    if (moiety != null) {
                        initializeExistingStructureVersion(moiety.structure);
                    }
                }
            }
        }
        if (substance instanceof PolymerSubstance polymerSubstance && polymerSubstance.polymer != null) {
            initializeExistingStructureVersion(polymerSubstance.polymer.displayStructure);
            initializeExistingStructureVersion(polymerSubstance.polymer.idealizedStructure);
        }
    }

    private void initializeExistingStructureVersion(GinasChemicalStructure structure) {
        if (structure != null && structure.id != null && structure.version == null) {
            structure.version = 0L;
        }
    }

    private Long existingStructureVersionOrInitial(GinasChemicalStructure structure) {
        if (structure == null) {
            return null;
        }
        return structure.id != null && structure.version == null ? 0L : structure.version;
    }

    @Override
    protected AbstractEntityUpdatedEvent<Substance> newUpdateEvent(Substance updatedEntity) {
        return new SubstanceUpdatedEvent(updatedEntity);
    }

    @Override
    protected AbstractEntityCreatedEvent<Substance> newCreationEvent(Substance createdEntity) {
        return new SubstanceCreatedEvent(createdEntity);
    }

    @Override
    public UUID getIdFrom(Substance entity) {
        return entity.getUuid();
    }

    @Override
    protected List<Substance> fromNewJsonList(JsonNode list) throws IOException {
        List<Substance> substances = new ArrayList<>(list.size());
        for(JsonNode n : list){
            substances.add(fromNewJson(n));
        }
        return substances;
    }

    @Override
    protected Substance fromUpdatedJson(JsonNode json) throws IOException {
        return JsonSubstanceFactory.makeSubstance(scrubChemicalPayloadForUpdate(json));
    }


    @Override
    protected List<Substance> fromUpdatedJsonList(JsonNode list) throws IOException {
        List<Substance> substances = new ArrayList<>(list.size());
        for(JsonNode n : list){
            substances.add(fromUpdatedJson(n));
        }
        return substances;
    }


    @Override
    protected JsonNode toJson(Substance substance) throws IOException {
        return objectMapper.valueToTree(substance);
    }

    @Override
    protected Substance create(Substance substance) {
        normalizeCreateGraph(substance);
        try {
            EntityManager entityManager = getEntityManager();
            entityManager.persist(substance);
            entityManager.flush();
            return substance;
        }catch(Throwable t){
            t.printStackTrace();
            throw t;
        }
    }

    protected void normalizeCreateGraph(Substance substance) {
        if (substance == null) {
            return;
        }
        substance.version = "1";
        if (substance.modifications != null) {
            substance.modifications.uuid = null;
        }
        resetSubstanceOwnedGraphIds(substance);
        if (substance instanceof ChemicalSubstance chemicalSubstance) {
            resetChemicalGraphIds(chemicalSubstance);
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
            for (Code code : substance.codes) {
                if (code != null) {
                    code.uuid = null;
                }
            }
        }
        if (substance.notes != null) {
            for (Note note : substance.notes) {
                if (note != null) {
                    note.uuid = null;
                }
            }
        }
        resetPropertyGraphIds(substance.properties);
        resetRelationshipGraphIds(substance.relationships);
        remapReferenceKeywords(substance, resetReferenceGraphIds(substance.references));
        resetModificationGraphIds(substance.modifications);
    }

    private Map<String, String> resetReferenceGraphIds(List<Reference> references) {
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
        if (substance == null || replacements.isEmpty()) {
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
                String replacement = shouldRemapReferenceKeyword(keyword)
                        ? replacements.get(keyword.term)
                        : null;
                if (replacement == null) {
                    remappedReferences.add(keyword);
                    continue;
                }
                remappedReferences.add(new Keyword(keyword.label, replacement));
                changed = true;
            }
            if (changed) {
                child.setReferences(remappedReferences);
            }
        }
    }

    private boolean shouldRemapReferenceKeyword(Keyword keyword) {
        return keyword != null
                && keyword.term != null
                && (keyword.label == null || GinasCommonSubData.REFERENCE.equals(keyword.label));
    }

    private void resetPropertyGraphIds(List<Property> properties) {
        if (properties == null) {
            return;
        }
        for (Property property : properties) {
            if (property == null) {
                continue;
            }
            property.uuid = null;
            resetAmountIds(property.getValue());
            resetSubstanceReferenceIds(property.getReferencedSubstance());
            if (property.getParameters() == null) {
                continue;
            }
            for (Parameter parameter : property.getParameters()) {
                if (parameter == null) {
                    continue;
                }
                parameter.uuid = null;
                resetAmountIds(parameter.value);
                resetSubstanceReferenceIds(parameter.referencedSubstance);
            }
        }
    }

    private void resetRelationshipGraphIds(List<Relationship> relationships) {
        if (relationships == null) {
            return;
        }
        for (Relationship relationship : relationships) {
            if (relationship == null) {
                continue;
            }
            relationship.uuid = null;
            relationship.originatorUuid = null;
            resetAmountIds(relationship.amount);
            resetSubstanceReferenceIds(relationship.relatedSubstance);
            resetSubstanceReferenceIds(relationship.mediatorSubstance);
        }
    }

    private void resetModificationGraphIds(Modifications modifications) {
        if (modifications == null) {
            return;
        }
        modifications.uuid = null;
        if (modifications.agentModifications != null) {
            for (AgentModification modification : modifications.agentModifications) {
                if (modification == null) {
                    continue;
                }
                modification.uuid = null;
                resetSubstanceReferenceIds(modification.agentSubstance);
                resetAmountIds(modification.amount);
            }
        }
        if (modifications.physicalModifications != null) {
            for (PhysicalModification modification : modifications.physicalModifications) {
                if (modification == null) {
                    continue;
                }
                modification.uuid = null;
                if (modification.parameters == null) {
                    continue;
                }
                for (PhysicalParameter parameter : modification.parameters) {
                    if (parameter == null) {
                        continue;
                    }
                    parameter.uuid = null;
                    resetAmountIds(parameter.amount);
                }
            }
        }
        if (modifications.structuralModifications != null) {
            for (StructuralModification modification : modifications.structuralModifications) {
                if (modification == null) {
                    continue;
                }
                modification.uuid = null;
                resetAmountIds(modification.extentAmount);
                resetSubstanceReferenceIds(modification.molecularFragment);
            }
        }
    }

    private void resetAmountIds(Amount amount) {
        if (amount != null) {
            amount.uuid = null;
        }
    }

    private void resetMixtureGraphIds(Mixture mixture) {
        if (mixture == null) {
            return;
        }
        mixture.uuid = null;
        resetSubstanceReferenceIds(mixture.parentSubstance);
        if (mixture.getMixture() != null) {
            for (Component component : mixture.getMixture()) {
                component.uuid = null;
                resetSubstanceReferenceIds(component.substance);
            }
        }
    }

    private void resetSubstanceReferenceIds(SubstanceReference reference) {
        if (reference == null) {
            return;
        }
        reference.uuid = null;
    }

    private void resetProteinGraphIds(Protein protein) {
        if (protein == null) {
            return;
        }
        protein.uuid = null;
        if (protein.glycosylation != null) {
            resetGlycosylationGraphIds(protein.glycosylation);
        }
        if (protein.subunits != null) {
            for (Subunit subunit : protein.subunits) {
                subunit.uuid = null;
            }
        }
        if (protein.otherLinks != null) {
            for (OtherLinks otherLink : protein.otherLinks) {
                otherLink.uuid = null;
            }
        }
    }

    private void resetGlycosylationGraphIds(Glycosylation glycosylation) {
        glycosylation.uuid = null;
    }

    private void resetPolymerGraphIds(Polymer polymer) {
        if (polymer == null) {
            return;
        }
        polymer.uuid = null;
        if (polymer.classification != null) {
            resetPolymerClassificationGraphIds(polymer.classification);
        }
        resetChemicalStructureIds(polymer.displayStructure);
        resetChemicalStructureIds(polymer.idealizedStructure);
        if (polymer.monomers != null) {
            for (Material material : polymer.monomers) {
                material.uuid = null;
                resetSubstanceReferenceIds(material.monomerSubstance);
            }
        }
        if (polymer.structuralUnits != null) {
            for (Unit unit : polymer.structuralUnits) {
                unit.uuid = null;
            }
        }
    }

    private void resetPolymerClassificationGraphIds(PolymerClassification classification) {
        classification.uuid = null;
        resetSubstanceReferenceIds(classification.parentSubstance);
    }

    private void resetStructurallyDiverseGraphIds(StructurallyDiverse structurallyDiverse) {
        if (structurallyDiverse == null) {
            return;
        }
        structurallyDiverse.uuid = null;
        resetSubstanceReferenceIds(structurallyDiverse.parentSubstance);
        resetSubstanceReferenceIds(structurallyDiverse.hybridSpeciesMaternalOrganism);
        resetSubstanceReferenceIds(structurallyDiverse.hybridSpeciesPaternalOrganism);
    }

    private void resetSpecifiedSubstanceGraphIds(SpecifiedSubstanceGroup1 specifiedSubstance) {
        if (specifiedSubstance == null) {
            return;
        }
        specifiedSubstance.uuid = null;
        if (specifiedSubstance.constituents != null) {
            for (SpecifiedSubstanceComponent component : specifiedSubstance.constituents) {
                component.uuid = null;
                resetSubstanceReferenceIds(component.substance);
            }
        }
    }

    private void resetChemicalGraphIds(ChemicalSubstance chemicalSubstance) {
        if (chemicalSubstance == null) {
            return;
        }
        resetChemicalStructureIds(chemicalSubstance.getStructure());
        if (chemicalSubstance.getMoieties() != null) {
            for (Moiety moiety : chemicalSubstance.getMoieties()) {
                moiety.uuid = null;
                moiety.innerUuid = null;
                resetChemicalStructureIds(moiety.structure);
            }
        }
    }

    private void resetChemicalStructureIds(GinasChemicalStructure structure) {
        if (structure == null) {
            return;
        }
        structure.id = null;
        structure.version = null;
    }

    private JsonNode scrubChemicalPayloadForUpdate(JsonNode json) {
        if (!(json instanceof ObjectNode root)) {
            return json;
        }
        ObjectNode copy = root.deepCopy();
        scrubServerManagedAuditFields(copy);
        JsonNode substanceClassNode = root.get("substanceClass");
        if (substanceClassNode == null || !"chemical".equalsIgnoreCase(substanceClassNode.asText())) {
            return copy;
        }
        scrubChemicalStructureNode((ObjectNode) copy.get("structure"), true);

        ArrayNode moieties = (ArrayNode) copy.get("moieties");
        if (moieties != null) {
            for (JsonNode moietyNode : moieties) {
                if (moietyNode instanceof ObjectNode moietyObject) {
                    scrubChemicalStructureNode(moietyObject, true);
                }
            }
        }
        return copy;
    }

    private void scrubServerManagedAuditFields(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            SERVER_MANAGED_AUDIT_FIELDS.forEach(objectNode::remove);
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.properties().iterator();
            while (fields.hasNext()) {
                scrubServerManagedAuditFields(fields.next().getValue());
            }
            return;
        }
        if (node instanceof ArrayNode arrayNode) {
            for (JsonNode child : arrayNode) {
                scrubServerManagedAuditFields(child);
            }
        }
    }

    private void scrubChemicalStructureNode(ObjectNode structureNode, boolean preserveStructureId) {
        if (structureNode == null) {
            return;
        }
        structureNode.remove("properties");
        structureNode.remove("links");
        structureNode.remove("hash");
        structureNode.remove("_inchi");
        structureNode.remove("_inchiKey");
        if (!preserveStructureId) {
            structureNode.remove("id");
        }
    }

    private void normalizeUpdatedEntityForDiff(Substance persisted, Substance updated) {
        normalizeEmptyModificationsForDiff(persisted, updated);
        reuseUnchangedDefinitionGraphForDiff(persisted, updated);
        if (!(persisted instanceof ChemicalSubstance persistedChemical)
                || !(updated instanceof ChemicalSubstance updatedChemical)) {
            return;
        }
        normalizeUpdatedChemicalForDiff(persistedChemical, updatedChemical);
    }

    private void normalizeUpdatedChemicalForDiff(ChemicalSubstance persisted, ChemicalSubstance updated) {
        if (persisted == null || updated == null) {
            return;
        }
        if (sameChemicalStructureForDiff(persisted.getStructure(), updated.getStructure())) {
            if (sameChemicalStructureStoredFieldsForDiff(persisted.getStructure(), updated.getStructure())) {
                updated.setStructure(persisted.getStructure());
            } else {
                reusePersistedStructureComputedPropertiesForDiff(persisted.getStructure(), updated.getStructure());
                reusePersistedStructureIdentityForDiff(persisted.getStructure(), updated.getStructure());
            }
        } else if (persisted.getStructure() != null && updated.getStructure() != null) {
            reusePersistedStructureIdentityForDiff(persisted.getStructure(), updated.getStructure());
        }

        List<Moiety> remainingPersistedMoieties = persisted.getMoieties() == null
                ? new ArrayList<>()
                : new ArrayList<>(persisted.getMoieties());
        if (updated.getMoieties() == null) {
            return;
        }
        for (int i = 0; i < updated.getMoieties().size(); i++) {
            Moiety updatedMoiety = updated.getMoieties().get(i);
            Moiety matchedPersistedMoiety = findMatchingMoiety(remainingPersistedMoieties, updatedMoiety);
            if (matchedPersistedMoiety == null) {
                continue;
            }
            remainingPersistedMoieties.remove(matchedPersistedMoiety);
            if (sameMoietyStoredFieldsForDiff(matchedPersistedMoiety, updatedMoiety)) {
                updated.getMoieties().set(i, matchedPersistedMoiety);
            } else {
                reusePersistedMoietyIdentityForDiff(matchedPersistedMoiety, updatedMoiety);
                updated.getMoieties().set(i, updatedMoiety);
            }
        }
    }

    private void reusePersistedMoietyIdentityForDiff(Moiety persisted, Moiety updated) {
        if (persisted == null || updated == null) {
            return;
        }
        updated.uuid = persisted.uuid;
        updated.innerUuid = persisted.innerUuid;
        reusePersistedStructureComputedPropertiesForDiff(persisted.structure, updated.structure);
        reusePersistedStructureIdentityForDiff(persisted.structure, updated.structure);
        if (sameAmountForDiff(persisted.getCountAmount(), updated.getCountAmount())) {
            updated.setCountAmount(persisted.getCountAmount());
        }
    }

    private void reusePersistedStructureIdentityForDiff(GinasChemicalStructure persisted, GinasChemicalStructure updated) {
        if (persisted != null && updated != null) {
            updated.id = persisted.id;
            updated.version = existingStructureVersionOrInitial(persisted);
        }
    }

    private void reusePersistedStructureComputedPropertiesForDiff(GinasChemicalStructure persisted, GinasChemicalStructure updated) {
        if (persisted != null && updated != null) {
            updated.properties = persisted.properties == null ? new ArrayList<>() : new ArrayList<>(persisted.properties);
        }
    }

    private boolean sameChemicalStructureStoredFieldsForDiff(GinasChemicalStructure persisted, GinasChemicalStructure updated) {
        if (persisted == null || updated == null) {
            return persisted == updated;
        }
        return Objects.equals(persisted.molfile, updated.molfile)
                && Objects.equals(persisted.smiles, updated.smiles)
                && Objects.equals(persisted.formula, updated.formula)
                && Objects.equals(persisted.digest, updated.digest)
                && Objects.equals(persisted.stereoChemistry, updated.stereoChemistry)
                && Objects.equals(persisted.opticalActivity, updated.opticalActivity)
                && Objects.equals(persisted.atropisomerism, updated.atropisomerism)
                && Objects.equals(persisted.stereoComments, updated.stereoComments)
                && Objects.equals(persisted.stereoCenters, updated.stereoCenters)
                && Objects.equals(persisted.definedStereo, updated.definedStereo)
                && Objects.equals(persisted.ezCenters, updated.ezCenters)
                && Objects.equals(persisted.charge, updated.charge)
                && Objects.equals(persisted.count, updated.count)
                && Objects.equals(persisted.mwt, updated.mwt)
                && Objects.equals(persisted.deprecated, updated.deprecated);
    }

    private boolean sameMoietyStoredFieldsForDiff(Moiety persisted, Moiety updated) {
        if (persisted == null || updated == null) {
            return persisted == updated;
        }
        return sameChemicalStructureStoredFieldsForDiff(persisted.structure, updated.structure)
                && sameAmountForDiff(persisted.getCountAmount(), updated.getCountAmount());
    }

    private Moiety findMatchingMoiety(List<Moiety> persistedMoieties, Moiety updatedMoiety) {
        for (Moiety persistedMoiety : persistedMoieties) {
            if (sameMoietyForDiff(persistedMoiety, updatedMoiety)) {
                return persistedMoiety;
            }
        }
        return null;
    }

    private boolean sameMoietyForDiff(Moiety persisted, Moiety updated) {
        if (persisted == null || updated == null) {
            return false;
        }
        return sameChemicalStructureForDiff(persisted.structure, updated.structure)
                && sameAmountForDiff(persisted.getCountAmount(), updated.getCountAmount());
    }

    private boolean sameChemicalStructureForDiff(GinasChemicalStructure persisted, GinasChemicalStructure updated) {
        if (persisted == null || updated == null) {
            return persisted == updated;
        }
        String persistedExactHash = getExactHashForDiff(persisted);
        String updatedExactHash = getExactHashForDiff(updated);
        if (persistedExactHash != null && updatedExactHash != null) {
            return Objects.equals(persistedExactHash, updatedExactHash)
                    && Objects.equals(persisted.count, updated.count);
        }
        return Objects.equals(persisted.formula, updated.formula)
                && Objects.equals(persisted.opticalActivity, updated.opticalActivity)
                && Objects.equals(persisted.atropisomerism, updated.atropisomerism)
                && Objects.equals(persisted.stereoCenters, updated.stereoCenters)
                && Objects.equals(persisted.definedStereo, updated.definedStereo)
                && Objects.equals(persisted.ezCenters, updated.ezCenters)
                && Objects.equals(persisted.charge, updated.charge)
                && Objects.equals(persisted.count, updated.count)
                && Objects.equals(persisted.stereoChemistry, updated.stereoChemistry);
    }

    private String getExactHashForDiff(GinasChemicalStructure structure) {
        if (structure == null) {
            return null;
        }
        String exactHash = structure.getExactHash();
        if (exactHash != null && !exactHash.isBlank()) {
            return exactHash;
        }
        String structureText = firstNonBlank(structure.molfile, structure.smiles);
        if (structureText == null) {
            return null;
        }
        try {
            Structure instrumented = structureProcessor.instrument(structureText);
            return instrumented.getExactHash();
        } catch (RuntimeException e) {
            log.debug("Unable to compute exact hash for diff matching", e);
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean hasChemicalDefinitionChange(Substance persisted, Substance updated) {
        if (!(persisted instanceof ChemicalSubstance persistedChemical)
                || !(updated instanceof ChemicalSubstance updatedChemical)) {
            return false;
        }
        return !sameChemicalStructureForDiff(persistedChemical.getStructure(), updatedChemical.getStructure())
                || !sameMoietyCollectionForDiff(persistedChemical.getMoieties(), updatedChemical.getMoieties());
    }

    private boolean hasModificationsChange(Substance persisted, Substance updated) {
        Modifications persistedModifications = persisted == null ? null : persisted.modifications;
        Modifications updatedModifications = updated == null ? null : updated.modifications;
        if (isEmptyModifications(persistedModifications) && isEmptyModifications(updatedModifications)) {
            return false;
        }
        return !sameJson(persistedModifications, updatedModifications);
    }

    private void normalizeEmptyModificationsForDiff(Substance persisted, Substance updated) {
        if (persisted == null || updated == null
                || !isEmptyModifications(persisted.modifications)
                || !isEmptyModifications(updated.modifications)) {
            return;
        }
        updated.modifications = persisted.modifications;
    }

    private boolean isEmptyModifications(Modifications modifications) {
        return modifications == null
                || (isEmptyCollection(modifications.agentModifications)
                && isEmptyCollection(modifications.physicalModifications)
                && isEmptyCollection(modifications.structuralModifications));
    }

    private boolean isEmptyCollection(Collection<?> values) {
        return values == null || values.isEmpty();
    }

    private void reuseUnchangedDefinitionGraphForDiff(Substance persisted, Substance updated) {
        if (persisted instanceof SpecifiedSubstanceGroup1Substance persistedSsg1
                && updated instanceof SpecifiedSubstanceGroup1Substance updatedSsg1
                && sameJson(persistedSsg1.specifiedSubstance, updatedSsg1.specifiedSubstance)) {
            updatedSsg1.specifiedSubstance = persistedSsg1.specifiedSubstance;
        }
        if (persisted instanceof ProteinSubstance persistedProteinSubstance
                && updated instanceof ProteinSubstance updatedProteinSubstance
                && sameJson(persistedProteinSubstance.protein, updatedProteinSubstance.protein)) {
            updatedProteinSubstance.setProtein(persistedProteinSubstance.protein);
        }
        if (persisted instanceof MixtureSubstance persistedMixtureSubstance
                && updated instanceof MixtureSubstance updatedMixtureSubstance
                && sameJson(persistedMixtureSubstance.mixture, updatedMixtureSubstance.mixture)) {
            updatedMixtureSubstance.mixture = persistedMixtureSubstance.mixture;
        }
        if (persisted instanceof PolymerSubstance persistedPolymerSubstance
                && updated instanceof PolymerSubstance updatedPolymerSubstance
                && sameJson(persistedPolymerSubstance.polymer, updatedPolymerSubstance.polymer)) {
            updatedPolymerSubstance.polymer = persistedPolymerSubstance.polymer;
        }
        if (persisted instanceof StructurallyDiverseSubstance persistedStructurallyDiverseSubstance
                && updated instanceof StructurallyDiverseSubstance updatedStructurallyDiverseSubstance
                && sameJson(persistedStructurallyDiverseSubstance.structurallyDiverse,
                updatedStructurallyDiverseSubstance.structurallyDiverse)) {
            updatedStructurallyDiverseSubstance.structurallyDiverse =
                    persistedStructurallyDiverseSubstance.structurallyDiverse;
        }
        if (persisted instanceof NucleicAcidSubstance persistedNucleicAcidSubstance
                && updated instanceof NucleicAcidSubstance updatedNucleicAcidSubstance
                && sameJson(persistedNucleicAcidSubstance.nucleicAcid,
                updatedNucleicAcidSubstance.nucleicAcid)) {
            updatedNucleicAcidSubstance.setNucleicAcid(persistedNucleicAcidSubstance.nucleicAcid);
        }
    }

    private boolean sameJson(Object persisted, Object updated) {
        return Objects.equals(objectMapper.valueToTree(persisted), objectMapper.valueToTree(updated));
    }

    private boolean sameMoietyCollectionForDiff(List<Moiety> persistedMoieties, List<Moiety> updatedMoieties) {
        if (persistedMoieties == null || persistedMoieties.isEmpty()) {
            return updatedMoieties == null || updatedMoieties.isEmpty();
        }
        if (updatedMoieties == null || updatedMoieties.isEmpty()) {
            return false;
        }
        if (persistedMoieties.size() != updatedMoieties.size()) {
            return false;
        }
        List<Moiety> remainingPersistedMoieties = new ArrayList<>(persistedMoieties);
        for (Moiety updatedMoiety : updatedMoieties) {
            Moiety matchedPersistedMoiety = findMatchingMoiety(remainingPersistedMoieties, updatedMoiety);
            if (matchedPersistedMoiety == null) {
                return false;
            }
            remainingPersistedMoieties.remove(matchedPersistedMoiety);
        }
        return remainingPersistedMoieties.isEmpty();
    }

    private void normalizeUpdatedEntityForReplacement(Substance persisted, Substance updated) {
        if (!(persisted instanceof ChemicalSubstance persistedChemical)
                || !(updated instanceof ChemicalSubstance updatedChemical)) {
            return;
        }
        if (persistedChemical.getStructure() != null && updatedChemical.getStructure() != null) {
            updatedChemical.getStructure().id = persistedChemical.getStructure().id;
            updatedChemical.getStructure().version = existingStructureVersionOrInitial(persistedChemical.getStructure());
        }
        if (updatedChemical.getMoieties() == null) {
            return;
        }
        for (Moiety moiety : updatedChemical.getMoieties()) {
            if (moiety == null) {
                continue;
            }
            moiety.uuid = null;
            moiety.innerUuid = null;
            if (moiety.structure != null) {
                moiety.structure.id = null;
                moiety.structure.version = null;
            }
            if (moiety.getCountAmount() != null) {
                moiety.getCountAmount().uuid = null;
            }
        }
    }

    private Substance applyReplacementToManagedEntity(Substance managed, Substance updated) throws IOException {
        EntityManager entityManager = getEntityManager();
        GinasChemicalStructure replacementStructure = updated instanceof ChemicalSubstance updatedChemical
                ? updatedChemical.getStructure()
                : null;
        Map<UUID, GinasChemicalStructure> existingStructures = mapChemicalStructuresById(managed);
        List<Moiety> existingMoieties = managed instanceof ChemicalSubstance chemicalManaged
                ? chemicalManaged.getMoieties()
                : null;
        List<Moiety> replacementMoieties = updated instanceof ChemicalSubstance updatedChemical
                ? (updatedChemical.getMoieties() == null ? null : new ArrayList<>(updatedChemical.getMoieties()))
                : null;
        Modifications replacementModifications = updated.modifications;
        SpecifiedSubstanceGroup1 existingSpecifiedSubstance = managed instanceof SpecifiedSubstanceGroup1Substance managedSsg1
                ? managedSsg1.specifiedSubstance
                : null;
        Protein existingProtein = managed instanceof ProteinSubstance managedProteinSubstance
                ? managedProteinSubstance.protein
                : null;
        Mixture existingMixture = managed instanceof MixtureSubstance managedMixtureSubstance
                ? managedMixtureSubstance.mixture
                : null;
        Polymer existingPolymer = managed instanceof PolymerSubstance managedPolymerSubstance
                ? managedPolymerSubstance.polymer
                : null;
        StructurallyDiverse existingStructurallyDiverse =
                managed instanceof StructurallyDiverseSubstance managedStructurallyDiverseSubstance
                        ? managedStructurallyDiverseSubstance.structurallyDiverse
                        : null;
        NucleicAcid existingNucleicAcid = managed instanceof NucleicAcidSubstance managedNucleicAcidSubstance
                ? managedNucleicAcidSubstance.nucleicAcid
                : null;
        boolean preserveSpecifiedSubstance = existingSpecifiedSubstance != null;
        boolean preserveProtein = existingProtein != null;
        boolean preserveMixture = existingMixture != null;
        boolean preservePolymer = existingPolymer != null;
        boolean preserveStructurallyDiverse = existingStructurallyDiverse != null;
        boolean preserveNucleicAcid = existingNucleicAcid != null;
        List<Name> existingNameList = managed.names;
        Map<UUID, Name> existingNames = mapByUuid(existingNameList);
        Map<UUID, Code> existingCodes = mapByUuid(managed.codes);
        Map<UUID, NameOrg> existingNameOrgs = mapNameOrgsByUuid(existingNameList);
        Map<UUID, Note> existingNotes = mapByUuid(managed.notes);
        Map<UUID, Property> existingProperties = mapByUuid(managed.properties);
        Map<UUID, Parameter> existingParameters = mapByUuid(flattenParameters(managed.properties));
        Map<UUID, Relationship> existingRelationships = mapByUuid(managed.relationships);
        Map<UUID, Reference> existingReferences = mapByUuid(managed.references);
        Map<UUID, Amount> existingRelationshipAmounts = mapRelationshipAmounts(managed.relationships);
        Map<UUID, Amount> existingMoietyAmounts = mapMoietyAmounts(existingMoieties);
        Map<UUID, Amount> existingPropertyAmounts = mapPropertyAmounts(managed.properties);
        Map<UUID, Amount> existingParameterAmounts = mapParameterAmounts(managed.properties);
        Map<UUID, SubstanceReference> existingRelationshipReferences = mapRelationshipSubstanceReferences(managed.relationships);
        Map<UUID, SubstanceReference> existingPropertyReferences = mapPropertySubstanceReferences(managed.properties);
        Map<UUID, SubstanceReference> existingParameterReferences = mapParameterSubstanceReferences(managed.properties);
        Modifications existingModifications = managed.modifications;
        UUID existingModificationsUuid = existingModifications == null ? null : existingModifications.getUuid();
        List<AgentModification> existingAgentModificationList = existingModifications == null
                ? null
                : existingModifications.agentModifications;
        List<PhysicalModification> existingPhysicalModificationList = existingModifications == null
                ? null
                : existingModifications.physicalModifications;
        List<StructuralModification> existingStructuralModificationList = existingModifications == null
                ? null
                : existingModifications.structuralModifications;
        Map<UUID, AgentModification> existingAgentModifications = existingModifications == null
                ? Collections.emptyMap()
                : mapByUuid(existingModifications.agentModifications);
        Map<UUID, PhysicalModification> existingPhysicalModifications = existingModifications == null
                ? Collections.emptyMap()
                : mapByUuid(existingModifications.physicalModifications);
        Map<UUID, StructuralModification> existingStructuralModifications = existingModifications == null
                ? Collections.emptyMap()
                : mapByUuid(existingModifications.structuralModifications);
        Map<UUID, PhysicalParameter> existingPhysicalParameters = existingModifications == null
                ? Collections.emptyMap()
                : mapByUuid(flattenPhysicalParameters(existingModifications));
        Map<UUID, List<PhysicalParameter>> existingPhysicalParameterLists =
                mapPhysicalParameterListsByPhysicalModificationUuid(existingModifications);
        Map<UUID, Amount> existingPhysicalParameterAmounts = mapPhysicalParameterAmounts(existingModifications);
        Map<UUID, SubstanceReference> existingOwnedSubstanceReferences = new LinkedHashMap<>();
        existingOwnedSubstanceReferences.putAll(existingRelationshipReferences);
        existingOwnedSubstanceReferences.putAll(existingPropertyReferences);
        existingOwnedSubstanceReferences.putAll(existingParameterReferences);
        existingOwnedSubstanceReferences.putAll(mapDefinitionSubstanceReferences(managed));
        existingOwnedSubstanceReferences.putAll(mapModificationSubstanceReferences(existingModifications));
        Map<UUID, Amount> existingOwnedAmounts = new LinkedHashMap<>();
        existingOwnedAmounts.putAll(existingRelationshipAmounts);
        existingOwnedAmounts.putAll(existingMoietyAmounts);
        existingOwnedAmounts.putAll(existingPropertyAmounts);
        existingOwnedAmounts.putAll(existingParameterAmounts);
        existingOwnedAmounts.putAll(existingPhysicalParameterAmounts);
        existingOwnedAmounts.putAll(mapDefinitionAmounts(managed));
        existingOwnedAmounts.putAll(mapModificationAmounts(existingModifications));

        FlushModeType previousFlushMode = entityManager.getFlushMode();
        // Jackson creates same-id child instances before reconciliation restores managed children.
        entityManager.setFlushMode(FlushModeType.COMMIT);
        try {
            JsonNode updatedJson = objectMapper.valueToTree(updated);
            if (updatedJson instanceof ObjectNode updatedObject
                    && updated instanceof ChemicalSubstance) {
                if (replacementStructure != null) {
                    updatedObject.remove("structure");
                }
                if (replacementMoieties != null) {
                    updatedObject.remove("moieties");
                }
            }
            if (updatedJson instanceof ObjectNode updatedObject) {
                updatedObject.remove("modifications");
                if (preserveSpecifiedSubstance) {
                    updatedObject.remove("specifiedSubstance");
                }
                if (preserveProtein) {
                    updatedObject.remove("protein");
                }
                if (preserveMixture) {
                    updatedObject.remove("mixture");
                }
                if (preservePolymer) {
                    updatedObject.remove("polymer");
                }
                if (preserveStructurallyDiverse) {
                    updatedObject.remove("structurallyDiverse");
                }
                if (preserveNucleicAcid) {
                    updatedObject.remove("nucleicAcid");
                }
            }
            Substance replaced = objectMapper.readerForUpdating(managed).readValue(updatedJson);
            if (replaced instanceof ChemicalSubstance replacedChemical && replacementStructure != null) {
                replacedChemical.setStructure(reconcileManagedChemicalStructure(replacementStructure, existingStructures));
            }
            if (preserveSpecifiedSubstance && replaced instanceof SpecifiedSubstanceGroup1Substance replacedSsg1) {
                replacedSsg1.specifiedSubstance = existingSpecifiedSubstance;
            }
            if (preserveProtein && replaced instanceof ProteinSubstance replacedProteinSubstance) {
                replacedProteinSubstance.setProtein(existingProtein);
            }
            if (preserveMixture && replaced instanceof MixtureSubstance replacedMixtureSubstance) {
                replacedMixtureSubstance.mixture = existingMixture;
            }
            if (preservePolymer && replaced instanceof PolymerSubstance replacedPolymerSubstance) {
                replacedPolymerSubstance.polymer = existingPolymer;
            }
            if (preserveStructurallyDiverse
                    && replaced instanceof StructurallyDiverseSubstance replacedStructurallyDiverseSubstance) {
                replacedStructurallyDiverseSubstance.structurallyDiverse = existingStructurallyDiverse;
            }
            if (preserveNucleicAcid && replaced instanceof NucleicAcidSubstance replacedNucleicAcidSubstance) {
                replacedNucleicAcidSubstance.setNucleicAcid(existingNucleicAcid);
            }
            Modifications reconciledModifications = reconcileManagedModifications(replacementModifications, existingModifications,
                    existingModificationsUuid, existingAgentModificationList, existingPhysicalModificationList,
                    existingStructuralModificationList, existingAgentModifications, existingPhysicalModifications,
                    existingStructuralModifications, existingPhysicalParameters, existingPhysicalParameterLists,
                    existingPhysicalParameterAmounts, existingOwnedAmounts, existingOwnedSubstanceReferences);
            setManagedModifications(replaced, reconciledModifications);
            if (replaced instanceof ChemicalSubstance replacedChemical && replacementMoieties != null) {
                if (existingMoieties != null) {
                    for (Moiety existingMoiety : new ArrayList<>(existingMoieties)) {
                        existingMoiety.setOwner(null);
                        entityManager.remove(entityManager.contains(existingMoiety)
                                ? existingMoiety
                                : entityManager.merge(existingMoiety));
                    }
                }
                replacedChemical.setMoieties(replacementMoieties);
            }
            replaced.names = replaceListContents(existingNameList,
                    reconcileManagedNames(replaced.names, existingNames, existingNameOrgs, replaced));
            replaced.codes = reconcileManagedChildren(replaced.codes, existingCodes, child -> child.setOwner(replaced));
            replaced.notes = reconcileManagedChildren(replaced.notes, existingNotes, child -> child.setOwner(replaced));
            replaced.properties = reconcileManagedChildren(replaced.properties, existingProperties, child -> child.setOwner(replaced));
            replaced.relationships = reconcileManagedChildren(replaced.relationships, existingRelationships, child -> child.assignOwner(replaced));
            replaced.references = reconcileManagedChildren(replaced.references, existingReferences, child -> child.setOwner(replaced));
            reconcileNestedPropertyData(replaced.properties, existingParameters, existingPropertyAmounts, existingParameterAmounts,
                    existingPropertyReferences, existingParameterReferences);
            reconcileNestedRelationshipData(replaced.relationships, existingRelationshipAmounts, existingRelationshipReferences);
            if (replaced instanceof ChemicalSubstance replacedChemical) {
                reconcileNestedMoietyAmounts(replacedChemical.getMoieties(), existingMoietyAmounts);
            }
            return replaced;
        } finally {
            entityManager.setFlushMode(previousFlushMode);
        }
    }

    private void setManagedModifications(Substance substance, Modifications modifications) {
        if (substance instanceof ProteinSubstance proteinSubstance) {
            proteinSubstance.setModifications(modifications);
            return;
        }
        if (substance instanceof NucleicAcidSubstance nucleicAcidSubstance) {
            nucleicAcidSubstance.setModifications(modifications);
            return;
        }
        substance.modifications = modifications;
    }

    private Modifications reconcileManagedModifications(Modifications updatedModifications,
                                                        Modifications existingModifications,
                                                        UUID existingModificationsUuid,
                                                        List<AgentModification> existingAgentModificationList,
                                                        List<PhysicalModification> existingPhysicalModificationList,
                                                        List<StructuralModification> existingStructuralModificationList,
                                                        Map<UUID, AgentModification> existingAgentModifications,
                                                        Map<UUID, PhysicalModification> existingPhysicalModifications,
                                                        Map<UUID, StructuralModification> existingStructuralModifications,
                                                        Map<UUID, PhysicalParameter> existingPhysicalParameters,
                                                        Map<UUID, List<PhysicalParameter>> existingPhysicalParameterLists,
                                                        Map<UUID, Amount> existingPhysicalParameterAmounts,
                                                        Map<UUID, Amount> existingOwnedAmounts,
                                                        Map<UUID, SubstanceReference> existingOwnedSubstanceReferences)
            throws IOException {
        if (updatedModifications == null) {
            return null;
        }
        if (existingModifications == null) {
            updatedModifications.agentModifications = reconcileManagedAgentModifications(
                    updatedModifications.agentModifications, Collections.emptyMap(), existingOwnedAmounts,
                    existingOwnedSubstanceReferences, updatedModifications);
            updatedModifications.physicalModifications = reconcileManagedPhysicalModifications(
                    updatedModifications.physicalModifications, Collections.emptyMap(), existingPhysicalParameters,
                    existingPhysicalParameterLists, existingPhysicalParameterAmounts, updatedModifications);
            updatedModifications.structuralModifications = reconcileManagedStructuralModifications(
                    updatedModifications.structuralModifications, Collections.emptyMap(), existingOwnedAmounts,
                    existingOwnedSubstanceReferences, updatedModifications);
            assignModificationOwners(updatedModifications);
            return updatedModifications;
        }
        List<AgentModification> updatedAgentModifications = updatedModifications.agentModifications;
        List<PhysicalModification> updatedPhysicalModifications = updatedModifications.physicalModifications;
        List<StructuralModification> updatedStructuralModifications = updatedModifications.structuralModifications;
        if (updatedModifications != existingModifications) {
            JsonNode updatedJson = objectMapper.valueToTree(updatedModifications);
            if (updatedJson instanceof ObjectNode updatedObject) {
                updatedObject.remove("agentModifications");
                updatedObject.remove("physicalModifications");
                updatedObject.remove("structuralModifications");
            }
            objectMapper.readerForUpdating(existingModifications).readValue(updatedJson);
        }
        existingModifications.uuid = existingModificationsUuid;
        existingModifications.agentModifications = replaceListContents(existingAgentModificationList,
                reconcileManagedAgentModifications(updatedAgentModifications, existingAgentModifications,
                        existingOwnedAmounts, existingOwnedSubstanceReferences, existingModifications));
        existingModifications.physicalModifications = replaceListContents(existingPhysicalModificationList,
                reconcileManagedPhysicalModifications(updatedPhysicalModifications,
                        existingPhysicalModifications, existingPhysicalParameters, existingPhysicalParameterLists,
                        existingPhysicalParameterAmounts, existingModifications));
        existingModifications.structuralModifications = replaceListContents(existingStructuralModificationList,
                reconcileManagedStructuralModifications(updatedStructuralModifications, existingStructuralModifications,
                        existingOwnedAmounts, existingOwnedSubstanceReferences, existingModifications));
        return existingModifications;
    }

    private List<AgentModification> reconcileManagedAgentModifications(
            List<AgentModification> updatedAgentModifications,
            Map<UUID, AgentModification> existingAgentModifications,
            Map<UUID, Amount> existingOwnedAmounts,
            Map<UUID, SubstanceReference> existingOwnedSubstanceReferences,
            Modifications owner) throws IOException {
        if (updatedAgentModifications == null) {
            return null;
        }
        List<AgentModification> reconciled = new ArrayList<>(updatedAgentModifications.size());
        for (AgentModification updatedAgentModification : updatedAgentModifications) {
            if (updatedAgentModification == null) {
                continue;
            }
            AgentModification managedAgentModification = updatedAgentModification.getUuid() == null
                    ? null
                    : existingAgentModifications.get(updatedAgentModification.getUuid());
            if (managedAgentModification != null && managedAgentModification != updatedAgentModification) {
                Amount updatedAmount = updatedAgentModification.amount;
                Amount currentAmount = managedAgentModification.amount;
                SubstanceReference updatedAgentSubstance = updatedAgentModification.agentSubstance;
                SubstanceReference currentAgentSubstance = managedAgentModification.agentSubstance;
                JsonNode updatedJson = objectMapper.valueToTree(updatedAgentModification);
                if (updatedJson instanceof ObjectNode updatedObject) {
                    updatedObject.remove("amount");
                    updatedObject.remove("agentSubstance");
                }
                objectMapper.readerForUpdating(managedAgentModification).readValue(updatedJson);
                setOwnerField(managedAgentModification, owner);
                managedAgentModification.amount = reconcileOwnedAmount(updatedAmount, currentAmount, existingOwnedAmounts);
                managedAgentModification.agentSubstance = reconcileOwnedSubstanceReference(updatedAgentSubstance,
                        currentAgentSubstance, existingOwnedSubstanceReferences);
                reconciled.add(managedAgentModification);
            } else {
                setOwnerField(updatedAgentModification, owner);
                updatedAgentModification.amount = reconcileOwnedAmount(updatedAgentModification.amount, null,
                        existingOwnedAmounts);
                updatedAgentModification.agentSubstance = reconcileOwnedSubstanceReference(
                        updatedAgentModification.agentSubstance, null, existingOwnedSubstanceReferences);
                reconciled.add(updatedAgentModification);
            }
        }
        return reconciled;
    }

    private List<PhysicalModification> reconcileManagedPhysicalModifications(
            List<PhysicalModification> updatedPhysicalModifications,
            Map<UUID, PhysicalModification> existingPhysicalModifications,
            Map<UUID, PhysicalParameter> existingPhysicalParameters,
            Map<UUID, List<PhysicalParameter>> existingPhysicalParameterLists,
            Map<UUID, Amount> existingPhysicalParameterAmounts,
            Modifications owner) throws IOException {
        if (updatedPhysicalModifications == null) {
            return null;
        }
        List<PhysicalModification> reconciled = new ArrayList<>(updatedPhysicalModifications.size());
        for (PhysicalModification updatedPhysicalModification : updatedPhysicalModifications) {
            if (updatedPhysicalModification == null) {
                continue;
            }
            PhysicalModification managedPhysicalModification = updatedPhysicalModification.getUuid() == null
                    ? null
                    : existingPhysicalModifications.get(updatedPhysicalModification.getUuid());
            if (managedPhysicalModification != null && managedPhysicalModification != updatedPhysicalModification) {
                List<PhysicalParameter> updatedParameters = updatedPhysicalModification.parameters;
                JsonNode updatedJson = objectMapper.valueToTree(updatedPhysicalModification);
                if (updatedJson instanceof ObjectNode updatedObject) {
                    updatedObject.remove("parameters");
                }
                objectMapper.readerForUpdating(managedPhysicalModification).readValue(updatedJson);
                setOwnerField(managedPhysicalModification, owner);
                reconcilePhysicalParameterCollection(managedPhysicalModification, updatedParameters,
                        existingPhysicalParameters, existingPhysicalParameterLists, existingPhysicalParameterAmounts);
                reconciled.add(managedPhysicalModification);
            } else {
                setOwnerField(updatedPhysicalModification, owner);
                reconcilePhysicalParameterCollection(updatedPhysicalModification, updatedPhysicalModification.parameters,
                        existingPhysicalParameters, existingPhysicalParameterLists, existingPhysicalParameterAmounts);
                reconciled.add(updatedPhysicalModification);
            }
        }
        return reconciled;
    }

    private void reconcilePhysicalParameterCollection(PhysicalModification targetModification,
                                                      List<PhysicalParameter> updatedParameters,
                                                      Map<UUID, PhysicalParameter> existingPhysicalParameters,
                                                      Map<UUID, List<PhysicalParameter>> existingPhysicalParameterLists,
                                                      Map<UUID, Amount> existingPhysicalParameterAmounts)
            throws IOException {
        if (targetModification == null) {
            return;
        }
        List<PhysicalParameter> reconciledParameters = reconcileManagedChildren(updatedParameters,
                existingPhysicalParameters,
                child -> {
                    setOwnerField(child, targetModification);
                });
        if (reconciledParameters != null) {
            for (PhysicalParameter parameter : reconciledParameters) {
                if (parameter != null && parameter.amount != null && parameter.amount.getUuid() != null) {
                    Amount existingAmount = existingPhysicalParameterAmounts.get(parameter.amount.getUuid());
                    if (existingAmount != null) {
                        parameter.amount = existingAmount;
                    }
                }
            }
        }
        List<PhysicalParameter> targetParameters = targetModification.getUuid() == null
                ? null
                : existingPhysicalParameterLists.get(targetModification.getUuid());
        if (targetParameters == null) {
            targetParameters = targetModification.parameters;
        }
        targetModification.parameters = replaceListContents(targetParameters, reconciledParameters);
    }

    private List<StructuralModification> reconcileManagedStructuralModifications(
            List<StructuralModification> updatedStructuralModifications,
            Map<UUID, StructuralModification> existingStructuralModifications,
            Map<UUID, Amount> existingOwnedAmounts,
            Map<UUID, SubstanceReference> existingOwnedSubstanceReferences,
            Modifications owner) throws IOException {
        if (updatedStructuralModifications == null) {
            return null;
        }
        List<StructuralModification> reconciled = new ArrayList<>(updatedStructuralModifications.size());
        for (StructuralModification updatedStructuralModification : updatedStructuralModifications) {
            if (updatedStructuralModification == null) {
                continue;
            }
            StructuralModification managedStructuralModification = updatedStructuralModification.getUuid() == null
                    ? null
                    : existingStructuralModifications.get(updatedStructuralModification.getUuid());
            if (managedStructuralModification != null && managedStructuralModification != updatedStructuralModification) {
                Amount updatedExtentAmount = updatedStructuralModification.extentAmount;
                Amount currentExtentAmount = managedStructuralModification.extentAmount;
                SubstanceReference updatedMolecularFragment = updatedStructuralModification.molecularFragment;
                SubstanceReference currentMolecularFragment = managedStructuralModification.molecularFragment;
                JsonNode updatedJson = objectMapper.valueToTree(updatedStructuralModification);
                if (updatedJson instanceof ObjectNode updatedObject) {
                    updatedObject.remove("extentAmount");
                    updatedObject.remove("molecularFragment");
                }
                objectMapper.readerForUpdating(managedStructuralModification).readValue(updatedJson);
                setOwnerField(managedStructuralModification, owner);
                managedStructuralModification.extentAmount = reconcileOwnedAmount(updatedExtentAmount,
                        currentExtentAmount, existingOwnedAmounts);
                managedStructuralModification.molecularFragment = reconcileOwnedSubstanceReference(
                        updatedMolecularFragment, currentMolecularFragment, existingOwnedSubstanceReferences);
                reconciled.add(managedStructuralModification);
            } else {
                setOwnerField(updatedStructuralModification, owner);
                updatedStructuralModification.extentAmount = reconcileOwnedAmount(
                        updatedStructuralModification.extentAmount, null, existingOwnedAmounts);
                updatedStructuralModification.molecularFragment = reconcileOwnedSubstanceReference(
                        updatedStructuralModification.molecularFragment, null, existingOwnedSubstanceReferences);
                reconciled.add(updatedStructuralModification);
            }
        }
        return reconciled;
    }

    private Amount reconcileOwnedAmount(Amount updatedAmount,
                                        Amount currentAmount,
                                        Map<UUID, Amount> existingOwnedAmounts) throws IOException {
        if (updatedAmount == null) {
            return null;
        }
        UUID updatedUuid = updatedAmount.getUuid();
        if (currentAmount != null && updatedUuid != null && Objects.equals(updatedUuid, currentAmount.getUuid())) {
            JsonNode updatedJson = objectMapper.valueToTree(updatedAmount);
            objectMapper.readerForUpdating(currentAmount).readValue(updatedJson);
            existingOwnedAmounts.put(currentAmount.getUuid(), currentAmount);
            return currentAmount;
        }
        if (updatedUuid != null && existingOwnedAmounts.containsKey(updatedUuid)) {
            updatedAmount.uuid = UUID.randomUUID();
        }
        putAmountByUuid(existingOwnedAmounts, updatedAmount);
        return updatedAmount;
    }

    private SubstanceReference reconcileOwnedSubstanceReference(SubstanceReference updatedReference,
                                                               SubstanceReference currentReference,
                                                               Map<UUID, SubstanceReference> existingOwnedSubstanceReferences)
            throws IOException {
        if (updatedReference == null) {
            return null;
        }
        UUID updatedUuid = updatedReference.getUuid();
        if (currentReference != null && updatedUuid != null && Objects.equals(updatedUuid, currentReference.getUuid())) {
            JsonNode updatedJson = objectMapper.valueToTree(updatedReference);
            objectMapper.readerForUpdating(currentReference).readValue(updatedJson);
            existingOwnedSubstanceReferences.put(currentReference.getUuid(), currentReference);
            return currentReference;
        }
        if (updatedUuid != null && existingOwnedSubstanceReferences.containsKey(updatedUuid)) {
            SubstanceReference copiedReference = updatedReference.copyWithNullUUID();
            putSubstanceReferenceByUuid(existingOwnedSubstanceReferences, copiedReference);
            return copiedReference;
        }
        putSubstanceReferenceByUuid(existingOwnedSubstanceReferences, updatedReference);
        return updatedReference;
    }

    private void assignModificationOwners(Modifications modifications) {
        if (modifications == null) {
            return;
        }
        if (modifications.agentModifications != null) {
            modifications.agentModifications.forEach(child -> setOwnerField(child, modifications));
        }
        if (modifications.physicalModifications != null) {
            modifications.physicalModifications.forEach(child -> {
                setOwnerField(child, modifications);
                if (child != null && child.parameters != null) {
                    child.parameters.forEach(parameter -> setOwnerField(parameter, child));
                }
            });
        }
        if (modifications.structuralModifications != null) {
            modifications.structuralModifications.forEach(child -> setOwnerField(child, modifications));
        }
    }

    private void setOwnerField(Object target, Object owner) {
        if (target == null) {
            return;
        }
        Field ownerField = findField(target.getClass(), "owner");
        if (ownerField == null) {
            return;
        }
        try {
            ownerField.setAccessible(true);
            ownerField.set(target, owner);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to set owner on " + target.getClass().getName(), e);
        }
    }

    private Field findField(Class<?> type, String name) {
        Class<?> cursor = type;
        while (cursor != null && cursor != Object.class) {
            try {
                return cursor.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }

    private <T extends GinasCommonData> Map<UUID, T> mapByUuid(List<T> values) {
        Map<UUID, T> mapped = new LinkedHashMap<>();
        if (values == null) {
            return mapped;
        }
        for (T value : values) {
            if (value != null && value.getUuid() != null) {
                mapped.put(value.getUuid(), value);
            }
        }
        return mapped;
    }

    private Map<UUID, SubstanceReference> mapDefinitionSubstanceReferences(Substance substance) {
        Map<UUID, SubstanceReference> references = new LinkedHashMap<>();
        if (substance instanceof SpecifiedSubstanceGroup1Substance ssg1Substance
                && ssg1Substance.specifiedSubstance != null
                && ssg1Substance.specifiedSubstance.constituents != null) {
            for (SpecifiedSubstanceComponent component : ssg1Substance.specifiedSubstance.constituents) {
                putSubstanceReferenceByUuid(references, component == null ? null : component.substance);
            }
        }
        if (substance instanceof MixtureSubstance mixtureSubstance && mixtureSubstance.mixture != null) {
            putSubstanceReferenceByUuid(references, mixtureSubstance.mixture.parentSubstance);
            if (mixtureSubstance.mixture.getMixture() != null) {
                for (Component component : mixtureSubstance.mixture.getMixture()) {
                    putSubstanceReferenceByUuid(references, component == null ? null : component.substance);
                }
            }
        }
        if (substance instanceof PolymerSubstance polymerSubstance && polymerSubstance.polymer != null) {
            if (polymerSubstance.polymer.classification != null) {
                putSubstanceReferenceByUuid(references, polymerSubstance.polymer.classification.parentSubstance);
            }
            if (polymerSubstance.polymer.monomers != null) {
                for (Material material : polymerSubstance.polymer.monomers) {
                    putSubstanceReferenceByUuid(references, material == null ? null : material.monomerSubstance);
                }
            }
        }
        if (substance instanceof StructurallyDiverseSubstance structurallyDiverseSubstance
                && structurallyDiverseSubstance.structurallyDiverse != null) {
            putSubstanceReferenceByUuid(references, structurallyDiverseSubstance.structurallyDiverse.parentSubstance);
            putSubstanceReferenceByUuid(references,
                    structurallyDiverseSubstance.structurallyDiverse.hybridSpeciesMaternalOrganism);
            putSubstanceReferenceByUuid(references,
                    structurallyDiverseSubstance.structurallyDiverse.hybridSpeciesPaternalOrganism);
        }
        return references;
    }

    private Map<UUID, SubstanceReference> mapModificationSubstanceReferences(Modifications modifications) {
        Map<UUID, SubstanceReference> references = new LinkedHashMap<>();
        if (modifications == null) {
            return references;
        }
        if (modifications.agentModifications != null) {
            for (AgentModification modification : modifications.agentModifications) {
                putSubstanceReferenceByUuid(references, modification == null ? null : modification.agentSubstance);
            }
        }
        if (modifications.structuralModifications != null) {
            for (StructuralModification modification : modifications.structuralModifications) {
                putSubstanceReferenceByUuid(references, modification == null ? null : modification.molecularFragment);
            }
        }
        return references;
    }

    private void putSubstanceReferenceByUuid(Map<UUID, SubstanceReference> references,
                                             SubstanceReference reference) {
        if (reference != null && reference.getUuid() != null) {
            references.put(reference.getUuid(), reference);
        }
    }

    private Map<UUID, Amount> mapDefinitionAmounts(Substance substance) {
        Map<UUID, Amount> amounts = new LinkedHashMap<>();
        if (substance instanceof SpecifiedSubstanceGroup1Substance ssg1Substance
                && ssg1Substance.specifiedSubstance != null
                && ssg1Substance.specifiedSubstance.constituents != null) {
            for (SpecifiedSubstanceComponent component : ssg1Substance.specifiedSubstance.constituents) {
                putAmountByUuid(amounts, component == null ? null : component.amount);
            }
        }
        if (substance instanceof PolymerSubstance polymerSubstance && polymerSubstance.polymer != null) {
            if (polymerSubstance.polymer.monomers != null) {
                for (Material material : polymerSubstance.polymer.monomers) {
                    putAmountByUuid(amounts, material == null ? null : material.amount);
                }
            }
            if (polymerSubstance.polymer.structuralUnits != null) {
                for (Unit unit : polymerSubstance.polymer.structuralUnits) {
                    putAmountByUuid(amounts, unit == null ? null : unit.amount);
                }
            }
        }
        return amounts;
    }

    private Map<UUID, Amount> mapModificationAmounts(Modifications modifications) {
        Map<UUID, Amount> amounts = new LinkedHashMap<>();
        if (modifications == null) {
            return amounts;
        }
        if (modifications.agentModifications != null) {
            for (AgentModification modification : modifications.agentModifications) {
                putAmountByUuid(amounts, modification == null ? null : modification.amount);
            }
        }
        if (modifications.physicalModifications != null) {
            for (PhysicalModification modification : modifications.physicalModifications) {
                if (modification == null || modification.parameters == null) {
                    continue;
                }
                for (PhysicalParameter parameter : modification.parameters) {
                    putAmountByUuid(amounts, parameter == null ? null : parameter.amount);
                }
            }
        }
        if (modifications.structuralModifications != null) {
            for (StructuralModification modification : modifications.structuralModifications) {
                putAmountByUuid(amounts, modification == null ? null : modification.extentAmount);
            }
        }
        return amounts;
    }

    private void putAmountByUuid(Map<UUID, Amount> amounts, Amount amount) {
        if (amount != null && amount.getUuid() != null) {
            amounts.put(amount.getUuid(), amount);
        }
    }

    private Map<UUID, NameOrg> mapNameOrgsByUuid(List<Name> names) {
        List<NameOrg> nameOrgs = new ArrayList<>();
        if (names == null) {
            return mapByUuid(nameOrgs);
        }
        for (Name name : names) {
            if (name != null && name.nameOrgs != null) {
                nameOrgs.addAll(name.nameOrgs);
            }
        }
        return mapByUuid(nameOrgs);
    }

    private Map<UUID, GinasChemicalStructure> mapChemicalStructuresById(Substance substance) {
        Map<UUID, GinasChemicalStructure> mapped = new LinkedHashMap<>();
        if (!(substance instanceof ChemicalSubstance chemicalSubstance)) {
            return mapped;
        }
        putChemicalStructureById(mapped, chemicalSubstance.getStructure());
        if (chemicalSubstance.getMoieties() != null) {
            for (Moiety moiety : chemicalSubstance.getMoieties()) {
                if (moiety != null) {
                    putChemicalStructureById(mapped, moiety.structure);
                }
            }
        }
        return mapped;
    }

    private void putChemicalStructureById(Map<UUID, GinasChemicalStructure> mapped,
                                          GinasChemicalStructure structure) {
        if (structure != null && structure.id != null) {
            mapped.put(structure.id, structure);
        }
    }

    private void reconcileManagedChemicalStructures(Substance substance,
                                                    Map<UUID, GinasChemicalStructure> existingStructures)
            throws IOException {
        if (!(substance instanceof ChemicalSubstance chemicalSubstance) || existingStructures.isEmpty()) {
            return;
        }
        chemicalSubstance.setStructure(reconcileManagedChemicalStructure(chemicalSubstance.getStructure(), existingStructures));
        if (chemicalSubstance.getMoieties() == null) {
            return;
        }
        for (Moiety moiety : chemicalSubstance.getMoieties()) {
            if (moiety != null) {
                moiety.structure = reconcileManagedChemicalStructure(moiety.structure, existingStructures);
            }
        }
    }

    private GinasChemicalStructure reconcileManagedChemicalStructure(GinasChemicalStructure updatedStructure,
                                                                    Map<UUID, GinasChemicalStructure> existingStructures)
            throws IOException {
        if (updatedStructure == null || updatedStructure.id == null) {
            return updatedStructure;
        }
        GinasChemicalStructure managedStructure = existingStructures.get(updatedStructure.id);
        if (managedStructure == null || managedStructure == updatedStructure) {
            return updatedStructure;
        }
        updateManagedStructurePreservingCollections(managedStructure, updatedStructure);
        return managedStructure;
    }

    private void updateManagedStructurePreservingCollections(GinasChemicalStructure managedStructure,
                                                             GinasChemicalStructure updatedStructure)
            throws IOException {
        JsonNode updatedJson = objectMapper.valueToTree(updatedStructure);
        if (updatedJson instanceof ObjectNode updatedObject) {
            updatedObject.remove("properties");
            updatedObject.remove("links");
        }
        objectMapper.readerForUpdating(managedStructure).readValue(updatedJson);
        managedStructure.version = updatedStructure.version != null
                ? updatedStructure.version
                : existingStructureVersionOrInitial(managedStructure);
        if (updatedStructure.properties != null) {
            managedStructure.properties = replaceListContents(managedStructure.properties, updatedStructure.properties);
        }
    }

    private List<Name> reconcileManagedNames(List<Name> updatedNames,
                                             Map<UUID, Name> existingNames,
                                             Map<UUID, NameOrg> existingNameOrgs,
                                             Substance owner) throws IOException {
        if (updatedNames == null) {
            return null;
        }
        List<Name> reconciled = new ArrayList<>(updatedNames.size());
        for (Name updatedName : updatedNames) {
            if (updatedName == null) {
                continue;
            }
            Name managedName = updatedName.getUuid() == null ? null : existingNames.get(updatedName.getUuid());
            if (managedName != null && managedName != updatedName) {
                List<NameOrg> updatedNameOrgs = updatedName.nameOrgs;
                JsonNode updatedJson = objectMapper.valueToTree(updatedName);
                if (updatedJson instanceof ObjectNode updatedObject) {
                    updatedObject.remove("nameOrgs");
                }
                objectMapper.readerForUpdating(managedName).readValue(updatedJson);
                managedName.setOwner(owner);
                reconcileNameOrgCollection(managedName, updatedNameOrgs, existingNameOrgs);
                reconciled.add(managedName);
            } else {
                updatedName.setOwner(owner);
                reconcileNameOrgCollection(updatedName, updatedName.nameOrgs, existingNameOrgs);
                reconciled.add(updatedName);
            }
        }
        return reconciled;
    }

    private void reconcileNameOrgCollection(Name targetName,
                                            List<NameOrg> updatedNameOrgs,
                                            Map<UUID, NameOrg> existingNameOrgs) throws IOException {
        if (targetName == null) {
            return;
        }
        List<NameOrg> reconciledNameOrgs = reconcileManagedChildren(updatedNameOrgs, existingNameOrgs, child -> {
        });
        targetName.nameOrgs = replaceListContents(targetName.nameOrgs, reconciledNameOrgs);
    }

    private <T> List<T> replaceListContents(List<T> target, List<T> values) {
        if (values == null) {
            return null;
        }
        if (target == null) {
            return values;
        }
        if (target != values) {
            target.clear();
            target.addAll(values);
        }
        return target;
    }

    private Object resolveManagedPatchValue(Object value,
                                            Map<UUID, Name> existingNames,
                                            Map<UUID, NameOrg> existingNameOrgs,
                                            Map<UUID, GinasChemicalStructure> existingStructures,
                                            Substance managedSubstance) {
        if (value instanceof Substance substance && substance.uuid != null
                && managedSubstance != null && Objects.equals(substance.uuid, managedSubstance.uuid)) {
            return managedSubstance;
        }
        if (value instanceof Name name && name.getUuid() != null) {
            Name existingName = existingNames.get(name.getUuid());
            if (existingName != null) {
                return existingName;
            }
        }
        if (value instanceof NameOrg nameOrg && nameOrg.getUuid() != null) {
            NameOrg existingNameOrg = existingNameOrgs.get(nameOrg.getUuid());
            if (existingNameOrg != null) {
                return existingNameOrg;
            }
        }
        if (value instanceof GinasChemicalStructure structure && structure.id != null) {
            GinasChemicalStructure existingStructure = existingStructures.get(structure.id);
            if (existingStructure != null) {
                return existingStructure;
            }
        }
        if (value instanceof Moiety moiety && moiety.structure != null && moiety.structure.id != null) {
            GinasChemicalStructure existingStructure = existingStructures.get(moiety.structure.id);
            if (existingStructure != null) {
                moiety.structure = existingStructure;
            }
        }
        return value;
    }

    private <T extends GinasCommonData> List<T> reconcileManagedChildren(List<T> updatedValues,
                                                                         Map<UUID, T> existingByUuid,
                                                                         java.util.function.Consumer<T> ownerSetter) throws IOException {
        if (updatedValues == null) {
            return null;
        }
        List<T> reconciled = new ArrayList<>(updatedValues.size());
        for (T updatedValue : updatedValues) {
            if (updatedValue == null) {
                continue;
            }
            T managedValue = updatedValue.getUuid() == null ? null : existingByUuid.get(updatedValue.getUuid());
            if (managedValue != null && managedValue != updatedValue) {
                JsonNode updatedJson = objectMapper.valueToTree(updatedValue);
                objectMapper.readerForUpdating(managedValue).readValue(updatedJson);
                ownerSetter.accept(managedValue);
                reconciled.add(managedValue);
            } else {
                ownerSetter.accept(updatedValue);
                reconciled.add(updatedValue);
            }
        }
        return reconciled;
    }

    private List<Parameter> flattenParameters(List<Property> properties) {
        List<Parameter> parameters = new ArrayList<>();
        if (properties == null) {
            return parameters;
        }
        for (Property property : properties) {
            if (property != null && property.getParameters() != null) {
                parameters.addAll(property.getParameters());
            }
        }
        return parameters;
    }

    private List<PhysicalParameter> flattenPhysicalParameters(Modifications modifications) {
        List<PhysicalParameter> parameters = new ArrayList<>();
        if (modifications == null || modifications.physicalModifications == null) {
            return parameters;
        }
        for (PhysicalModification modification : modifications.physicalModifications) {
            if (modification != null && modification.parameters != null) {
                parameters.addAll(modification.parameters);
            }
        }
        return parameters;
    }

    private Map<UUID, List<PhysicalParameter>> mapPhysicalParameterListsByPhysicalModificationUuid(
            Modifications modifications) {
        Map<UUID, List<PhysicalParameter>> mapped = new LinkedHashMap<>();
        if (modifications == null || modifications.physicalModifications == null) {
            return mapped;
        }
        for (PhysicalModification modification : modifications.physicalModifications) {
            if (modification != null && modification.getUuid() != null) {
                mapped.put(modification.getUuid(), modification.parameters);
            }
        }
        return mapped;
    }

    private Map<UUID, Amount> mapRelationshipAmounts(List<Relationship> relationships) {
        Map<UUID, Amount> amounts = new LinkedHashMap<>();
        if (relationships == null) {
            return amounts;
        }
        for (Relationship relationship : relationships) {
            if (relationship != null && relationship.amount != null && relationship.amount.getUuid() != null) {
                amounts.put(relationship.amount.getUuid(), relationship.amount);
            }
        }
        return amounts;
    }

    private Map<UUID, Amount> mapPhysicalParameterAmounts(Modifications modifications) {
        Map<UUID, Amount> amounts = new LinkedHashMap<>();
        if (modifications == null || modifications.physicalModifications == null) {
            return amounts;
        }
        for (PhysicalModification modification : modifications.physicalModifications) {
            if (modification == null || modification.parameters == null) {
                continue;
            }
            for (PhysicalParameter parameter : modification.parameters) {
                if (parameter != null && parameter.amount != null && parameter.amount.getUuid() != null) {
                    amounts.put(parameter.amount.getUuid(), parameter.amount);
                }
            }
        }
        return amounts;
    }

    private Map<UUID, Amount> mapMoietyAmounts(List<Moiety> moieties) {
        Map<UUID, Amount> amounts = new LinkedHashMap<>();
        if (moieties == null) {
            return amounts;
        }
        for (Moiety moiety : moieties) {
            if (moiety != null && moiety.getCountAmount() != null && moiety.getCountAmount().getUuid() != null) {
                amounts.put(moiety.getCountAmount().getUuid(), moiety.getCountAmount());
            }
        }
        return amounts;
    }

    private Map<UUID, Amount> mapPropertyAmounts(List<Property> properties) {
        Map<UUID, Amount> amounts = new LinkedHashMap<>();
        if (properties == null) {
            return amounts;
        }
        for (Property property : properties) {
            if (property != null && property.getValue() != null && property.getValue().getUuid() != null) {
                amounts.put(property.getValue().getUuid(), property.getValue());
            }
        }
        return amounts;
    }

    private Map<UUID, Amount> mapParameterAmounts(List<Property> properties) {
        Map<UUID, Amount> amounts = new LinkedHashMap<>();
        if (properties == null) {
            return amounts;
        }
        for (Property property : properties) {
            if (property == null || property.getParameters() == null) {
                continue;
            }
            for (Parameter parameter : property.getParameters()) {
                if (parameter != null && parameter.getValue() != null && parameter.getValue().getUuid() != null) {
                    amounts.put(parameter.getValue().getUuid(), parameter.getValue());
                }
            }
        }
        return amounts;
    }

    private Map<UUID, SubstanceReference> mapRelationshipSubstanceReferences(List<Relationship> relationships) {
        Map<UUID, SubstanceReference> references = new LinkedHashMap<>();
        if (relationships == null) {
            return references;
        }
        for (Relationship relationship : relationships) {
            if (relationship == null) {
                continue;
            }
            if (relationship.relatedSubstance != null && relationship.relatedSubstance.getUuid() != null) {
                references.put(relationship.relatedSubstance.getUuid(), relationship.relatedSubstance);
            }
            if (relationship.mediatorSubstance != null && relationship.mediatorSubstance.getUuid() != null) {
                references.put(relationship.mediatorSubstance.getUuid(), relationship.mediatorSubstance);
            }
        }
        return references;
    }

    private Map<UUID, SubstanceReference> mapPropertySubstanceReferences(List<Property> properties) {
        Map<UUID, SubstanceReference> references = new LinkedHashMap<>();
        if (properties == null) {
            return references;
        }
        for (Property property : properties) {
            if (property != null && property.getReferencedSubstance() != null
                    && property.getReferencedSubstance().getUuid() != null) {
                references.put(property.getReferencedSubstance().getUuid(), property.getReferencedSubstance());
            }
        }
        return references;
    }

    private Map<UUID, SubstanceReference> mapParameterSubstanceReferences(List<Property> properties) {
        Map<UUID, SubstanceReference> references = new LinkedHashMap<>();
        if (properties == null) {
            return references;
        }
        for (Property property : properties) {
            if (property == null || property.getParameters() == null) {
                continue;
            }
            for (Parameter parameter : property.getParameters()) {
                if (parameter != null && parameter.referencedSubstance != null
                        && parameter.referencedSubstance.getUuid() != null) {
                    references.put(parameter.referencedSubstance.getUuid(), parameter.referencedSubstance);
                }
            }
        }
        return references;
    }

    private void reconcileNestedRelationshipData(List<Relationship> relationships,
                                                 Map<UUID, Amount> existingAmounts,
                                                 Map<UUID, SubstanceReference> existingReferences) {
        if (relationships == null) {
            return;
        }
        for (Relationship relationship : relationships) {
            if (relationship == null) {
                continue;
            }
            if (relationship.amount != null && relationship.amount.getUuid() != null) {
                Amount existingAmount = existingAmounts.get(relationship.amount.getUuid());
                if (existingAmount != null) {
                    relationship.amount = existingAmount;
                }
            }
            if (relationship.relatedSubstance != null && relationship.relatedSubstance.getUuid() != null) {
                SubstanceReference existingReference = existingReferences.get(relationship.relatedSubstance.getUuid());
                if (existingReference != null) {
                    relationship.relatedSubstance = existingReference;
                }
            }
            if (relationship.mediatorSubstance != null && relationship.mediatorSubstance.getUuid() != null) {
                SubstanceReference existingReference = existingReferences.get(relationship.mediatorSubstance.getUuid());
                if (existingReference != null) {
                    relationship.mediatorSubstance = existingReference;
                }
            }
        }
    }

    private void reconcileNestedPropertyData(List<Property> properties,
                                             Map<UUID, Parameter> existingParameters,
                                             Map<UUID, Amount> existingPropertyAmounts,
                                             Map<UUID, Amount> existingParameterAmounts,
                                             Map<UUID, SubstanceReference> existingPropertyReferences,
                                             Map<UUID, SubstanceReference> existingParameterReferences) throws IOException {
        if (properties == null) {
            return;
        }
        for (Property property : properties) {
            if (property == null) {
                continue;
            }
            if (property.getValue() != null && property.getValue().getUuid() != null) {
                Amount existingValue = existingPropertyAmounts.get(property.getValue().getUuid());
                if (existingValue != null) {
                    property.setValue(existingValue);
                }
            }
            if (property.getReferencedSubstance() != null && property.getReferencedSubstance().getUuid() != null) {
                SubstanceReference existingReference = existingPropertyReferences.get(property.getReferencedSubstance().getUuid());
                if (existingReference != null) {
                    property.setReferencedSubstance(existingReference);
                }
            }
            List<Parameter> reconciledParameters = reconcileManagedChildren(property.getParameters(),
                    existingParameters,
                    child -> {
                    });
            if (reconciledParameters != null) {
                for (Parameter parameter : reconciledParameters) {
                    if (parameter == null || parameter.getUuid() == null || parameter.getValue() == null
                            || parameter.getValue().getUuid() == null) {
                        if (parameter != null && parameter.referencedSubstance != null
                                && parameter.referencedSubstance.getUuid() != null) {
                            SubstanceReference existingReference = existingParameterReferences.get(parameter.referencedSubstance.getUuid());
                            if (existingReference != null) {
                                parameter.referencedSubstance = existingReference;
                            }
                        }
                        continue;
                    }
                    Amount existingValue = existingParameterAmounts.get(parameter.getValue().getUuid());
                    if (existingValue != null) {
                        parameter.setValue(existingValue);
                    }
                    if (parameter.referencedSubstance != null && parameter.referencedSubstance.getUuid() != null) {
                        SubstanceReference existingReference = existingParameterReferences.get(parameter.referencedSubstance.getUuid());
                        if (existingReference != null) {
                            parameter.referencedSubstance = existingReference;
                        }
                    }
                }
            }
            property.setParameters(reconciledParameters);
        }
    }

    private void reconcileNestedMoietyAmounts(List<Moiety> moieties, Map<UUID, Amount> existingAmounts) {
        if (moieties == null) {
            return;
        }
        for (Moiety moiety : moieties) {
            if (moiety == null || moiety.getCountAmount() == null || moiety.getCountAmount().getUuid() == null) {
                continue;
            }
            Amount existingAmount = existingAmounts.get(moiety.getCountAmount().getUuid());
            if (existingAmount != null) {
                moiety.setCountAmount(existingAmount);
            }
        }
    }

    private boolean sameAmountForDiff(ix.ginas.models.v1.Amount persisted, ix.ginas.models.v1.Amount updated) {
        if (persisted == null || updated == null) {
            return persisted == updated;
        }
        return Objects.equals(persisted.type, updated.type)
                && Objects.equals(persisted.average, updated.average)
                && Objects.equals(persisted.highLimit, updated.highLimit)
                && Objects.equals(persisted.high, updated.high)
                && Objects.equals(persisted.lowLimit, updated.lowLimit)
                && Objects.equals(persisted.low, updated.low)
                && Objects.equals(persisted.units, updated.units)
                && Objects.equals(persisted.nonNumericValue, updated.nonNumericValue)
                && Objects.equals(persisted.approvalID, updated.approvalID);
    }

    private void resetNucleicAcidGraphIds(NucleicAcid nucleicAcid) {
        if (nucleicAcid == null) {
            return;
        }
        nucleicAcid.uuid = null;
        if (nucleicAcid.getModifications() != null) {
            nucleicAcid.getModifications().uuid = null;
            nucleicAcid.getModifications().agentModifications.forEach(mod -> mod.uuid = null);
            nucleicAcid.getModifications().physicalModifications.forEach(mod -> mod.uuid = null);
            nucleicAcid.getModifications().structuralModifications.forEach(mod -> mod.uuid = null);
        }
        if (nucleicAcid.getLinkages() != null) {
            for (Linkage linkage : nucleicAcid.getLinkages()) {
                linkage.uuid = null;
            }
        }
        if (nucleicAcid.getSugars() != null) {
            for (Sugar sugar : nucleicAcid.getSugars()) {
                sugar.uuid = null;
            }
        }
        if (nucleicAcid.getSubunits() != null) {
            for (Subunit subunit : nucleicAcid.getSubunits()) {
                subunit.uuid = null;
            }
        }
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    @Transactional
    public Optional<Substance> get(UUID id) {
        return fullFetch(repository.findById(id),false);
    }

    /**
     * Fully fetch the given Substance this might be a computationally intense operation
     * and the passed in Substance is not guaranteed to be the same
     * reference of the Substance returned.
     * @param opt the Optional wrapped Substance to fully fetch.
     * @param useEF use entity fetcher to try to more efficiently fetch the Substance data.
     * @return an Optional wrapped fully fetched substance which may be empty if it could not be fetched.
     * An empty Optional passed in will always return an empty Optional.
     */
    private Optional<Substance> fullFetch(Optional<Substance> opt, boolean useEF){
        if(opt.isPresent()){
            if(useEF) {
                EntityUtils.Key k = opt.get().fetchKey();
                Optional<Substance> fetched= EntityFetcher.of(k).getIfPossible().map(o->(Substance)o);
                if(fetched.isPresent()){
                    return fetched;
                }

            }
            //if entity fetcher didn't find it fallback to full jsonnode but note
            //this might require an open transaction
            opt.get().toFullJsonNode();
        }

        return opt;
    }

    @Override
    @Transactional
    public Optional<Substance> flexLookup(String someKindOfId) {
        if(someKindOfId==null){
            return Optional.empty();
        }
        if(Util.isUUID(someKindOfId)){
            return get(UUID.fromString(someKindOfId));
        }
        //old versions of GSRS only used the first 8 chars of the uuid
        if (someKindOfId.length() == 8) { // might be uuid
            List<Substance> list = repository.findByUuidStartingWith(someKindOfId);
            if(!list.isEmpty()){
                return fullFetch(Optional.of(list.get(0)),true);
            }
        }

        Substance result = repository.findByApprovalID(someKindOfId);
        if(result !=null){
            return fullFetch(Optional.of(result),true);
        }
        List<SubstanceRepository.SubstanceSummary> summaries = repository.findByNames_NameIgnoreCase(someKindOfId);
        if(summaries !=null && !summaries.isEmpty()){

            //get the first?
            return fullFetch(repository.findById(summaries.get(0).getUuid()),true);
        }
        summaries = repository.findByCodes_CodeIgnoreCase(someKindOfId);
        if(summaries !=null && !summaries.isEmpty()){

            //get the first?
            return fullFetch(repository.findById(summaries.get(0).getUuid()),true);
        }
        return Optional.empty();
    }



    @Override
    protected Optional<UUID> flexLookupIdOnly(String someKindOfId) {
        //easiest way to avoid deduping data is to just do a full flex lookup and then return id
        Optional<Substance> found = flexLookup(someKindOfId);
        if(found.isPresent()){
            return Optional.of(found.get().uuid);
        }
        return Optional.empty();
    }


    @Override
    public UpdateResult<Substance> updateEntity(JsonNode updatedEntityJson, boolean ignoreValidation) throws Exception {
        ValidationResponse<Substance> validationResponse = null;
        if (!ignoreValidation) {
            validationResponse = validateEntity(updatedEntityJson, ValidatorCategory.CATEGORY_ALL());
            if (validationResponse != null && !validationResponse.isValid()) {
                return UpdateResult.<Substance>builder()
                        .status(UpdateResult.STATUS.ERROR)
                        .validationResponse(validationResponse)
                        .build();
            }
        }
        return performUpdateEntity(updatedEntityJson, validationResponse);
    }

    @Override
    public ValidationResponse<Substance> validateEntity(JsonNode updatedEntityJson,
                                                        ValidatorCategory validatorCategory) throws Exception {
        Substance newValue = fromUpdatedJson(updatedEntityJson);
        normalizeChemicalStructuresForValidation(newValue);
        Optional<Substance> oldValue = resolveExistingSubstanceForValidation(updatedEntityJson, newValue);
        oldValue.ifPresent(this::normalizeChemicalStructuresForValidation);
        ValidatorConfig.METHOD_TYPE methodType = oldValue.isPresent()
                ? ValidatorConfig.METHOD_TYPE.UPDATE
                : ValidatorConfig.METHOD_TYPE.CREATE;

        ValidatorFactory validatorFactory = validatorFactoryService.newFactory(CONTEXT);
        Validator<Substance> validator = validatorFactory.createValidatorFor(
                newValue,
                oldValue.orElse(null),
                methodType,
                validatorCategory);

        ValidationResponse<Substance> response = createValidationResponse(
                newValue,
                oldValue.orElse(null),
                methodType);
        ValidatorCallback callback = createCallbackFor(newValue, response, methodType);
        validator.validate(newValue, oldValue.orElse(null), callback);
        callback.complete();
        return response;
    }

    @Override
    public UpdateResult<Substance> updateEntityWithoutValidation(JsonNode updatedEntityJson) {
        return performUpdateEntity(updatedEntityJson, null);
    }

    private UpdateResult<Substance> performUpdateEntity(JsonNode updatedEntityJson,
                                                        ValidationResponse<Substance> validationResponse) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(this.getTransactionManager());

        return transactionTemplate.execute( status-> {
            try {
                Substance updatedEntity = validationResponse != null && validationResponse.getNewObject() != null
                        ? JsonEntityUtil.fixOwners((Substance) validationResponse.getNewObject(), true)
                        : JsonEntityUtil.fixOwners(fromUpdatedJson(updatedEntityJson), true);
                EntityUtils.Key oKey = EntityUtils.EntityWrapper.of(updatedEntity).getKey();
                EntityManager entityManager = oKey.getEntityManager();

                UpdateResult.UpdateResultBuilder<Substance> builder = UpdateResult.<Substance>builder();
                if (validationResponse != null) {
                    builder.validationResponse(validationResponse);
                }
                EntityUtils.EntityWrapper<Substance> savedVersion = entityPersistAdapter.change(oKey, oldEntity -> {
                        EntityUtils.EntityWrapper<Substance> og = EntityUtils.EntityWrapper.of(oldEntity);
                        String oldJson = og.toFullJson();
                        builder.oldJson(oldJson);

                        EntityUtils.EntityWrapper<Substance> oWrap = EntityUtils.EntityWrapper.of(oldEntity);
                        EntityUtils.EntityWrapper<Substance> nWrap = EntityUtils.EntityWrapper.of(updatedEntity);

                        boolean rootMolfileChange = hasRootMolfileChange(oldEntity, updatedEntity);
                        regenerateMoietiesForRootMolfileChange(oldEntity, updatedEntity);

                        // Only use POJO patch if the entities are the same substance type.
                        boolean sameSubstanceClass = Objects.equals(oldEntity.substanceClass, updatedEntity.substanceClass);
                        boolean usePojoPatch = sameSubstanceClass;
                        boolean chemicalDefinitionChange = hasChemicalDefinitionChange(oldEntity, updatedEntity);
                        boolean modificationsChange = hasModificationsChange(oldEntity, updatedEntity);
                        boolean useReplacementUpdate = chemicalDefinitionChange || rootMolfileChange || modificationsChange;
                        if (usePojoPatch && useReplacementUpdate) {
                            usePojoPatch = false;
                        }
                        if (usePojoPatch) {
                            normalizeUpdatedEntityForDiff(oldEntity, updatedEntity);
                            List<Name> existingNameListForPatch = oldEntity.names;
                            Map<UUID, Name> existingNamesForPatch = mapByUuid(existingNameListForPatch);
                            Map<UUID, NameOrg> existingNameOrgsForPatch = mapNameOrgsByUuid(existingNameListForPatch);
                            Map<UUID, GinasChemicalStructure> existingStructuresForPatch = mapChemicalStructuresById(oldEntity);
                            updatedEntity.modifications = oldEntity.modifications;
                            PojoPatch<Substance> patch = PojoDiff.getDiff(oldEntity, updatedEntity);
                            LogUtil.debug(() -> "changes = " + patch.getChanges());
                            final List<Object> removed = new ArrayList<Object>();

                            //Apply the changes, grabbing every change along the way
                            Stack changeStack = patch.apply(oldEntity, c -> {
                                if ("remove".equals(c.getOp())) {
                                    removed.add(c.getOldValue());
                                }
                                LogUtil.trace(() -> c.getOp() + "\t" + c.getOldValue() + "\t" + c.getNewValue());
                            });
                            if (changeStack.isEmpty()) {
                                throw new IllegalStateException("No change detected");
                            } else {
                                LogUtil.debug(() -> "Found:" + changeStack.size() + " changes");
                            }
                            oldEntity = fixUpdatedIfNeeded(JsonEntityUtil.fixOwners(oldEntity, true));
                            reconcileManagedChemicalStructures(oldEntity, existingStructuresForPatch);
                            oldEntity.names = replaceListContents(existingNameListForPatch,
                                    reconcileManagedNames(oldEntity.names, existingNamesForPatch, existingNameOrgsForPatch, oldEntity));
                            oldEntity = fixUpdatedIfNeeded(JsonEntityUtil.fixOwners(oldEntity, true));
                            reconcileManagedChemicalStructures(oldEntity, existingStructuresForPatch);
                            //This is the last line of defense for making sure that the patch worked
                            //Should throw an exception here if there's a major problem
                            //This is inefficient, but forces confirmation that the object is fully realized
                            String serialized = EntityUtils.EntityWrapper.of(oldEntity).toJsonDiffJson();


                            while (!changeStack.isEmpty()) {
                                Object v = changeStack.pop();
                                v = resolveManagedPatchValue(v, existingNamesForPatch, existingNameOrgsForPatch, existingStructuresForPatch, oldEntity);
                                EntityUtils.EntityWrapper<Object> ewchanged = EntityUtils.EntityWrapper.of(v);
                                if (!ewchanged.isIgnoredModel() && ewchanged.isEntity()) {
                                    Object o = ewchanged.getValue();
                                    if (o instanceof ForceUpdatableModel) {
                                        //Maybe don't do twice? IDK.
                                        ((ForceUpdatableModel) o).forceUpdate();
                                    }

                                    if (entityManager.contains(o)) {
                                        entityManager.merge(o);
                                    }
                                }
                            }

                            //explicitly delete deleted things
                            //This should ONLY delete objects which "belong"
                            //to something. That is, have a @SingleParent annotation
                            //inside

                            removed.stream()
                                    .filter(Objects::nonNull)

                                    .map(o -> EntityUtils.EntityWrapper.of(o))
                                    .filter(ew -> ew.isExplicitDeletable())
                                    .forEach(ew -> {
                                        Object o = ew.getValue();
                                        log.warn("deleting:" + o);
                                        //hibernate can only remove entities from this transaction
                                        //this logic will merge "detached" entities from outside this transaction before removing anything

                                        entityManager.remove(entityManager.contains(o) ? o : entityManager.merge(o));

                                    });

                            try {
                                Substance saved = transactionalUpdate(oldEntity, oldJson);
//                			System.out.println("updated entity = " + saved);
                                String internalJSON = EntityUtils.EntityWrapper.of(saved).toInternalJson();
//                			System.out.println("updated entity full eager fetch = " + internalJSON.hashCode());
                                builder.updatedEntity(saved);

                                builder.status(UpdateResult.STATUS.UPDATED);

                                return Optional.of(saved);
                            } catch (Throwable t) {
                                t.printStackTrace();

                                builder.status(UpdateResult.STATUS.ERROR);
                                builder.throwable(t);
                                return Optional.empty();
                            }
                        } else {
                            // NON POJOPATCH: for true chemical definition changes, merge the
                            // updated graph directly so the root structure row is updated in
                            // place. For the remaining cases, keep the legacy delete-and-save
                            // behavior.

                            Substance oldValue = (Substance) oWrap.getValue();
                            normalizeUpdatedEntityForReplacement(oldValue, updatedEntity);

                            if (useReplacementUpdate) {
                                Substance newValue = (Substance) nWrap.getValue();
                                oldValue = applyReplacementToManagedEntity(oldValue, newValue);
                                oldValue = fixUpdatedIfNeeded(JsonEntityUtil.fixOwners(oldValue, true));
                                initializeExistingStructureVersions(oldValue);
                                entityManager.flush();

                                Substance saved = transactionalUpdate(oldValue, oldJson);
                                builder.updatedEntity(saved);
                                builder.status(UpdateResult.STATUS.UPDATED);

                                return Optional.of(saved);
                            }

                            entityManager.remove(oldValue);

                            // Now need to take care of bad update pieces:
                            //	1. Version not incremented correctly (post update hooks not called)
                            //  2. Some metadata / audit data may be problematic
                            //  3. The update hooks are called explicitly now
                            //     ... and that's a weird thing to do, because the persist hooks
                            //     will get called too. Does someone really expect things to
                            //     get called twice?
                            // TODO: the above pieces are from the old codebase, but the new one
                            // has to have these evaluated too. Need unit tests.

                            entityManager.flush();
                            // if we clear here, it will cause issues for
                            // some detached entities later, but not clearing causes other issues

                            entityManager.clear();

                            Substance newValue = (Substance)nWrap.getValue();
                            if (!sameSubstanceClass) {
                                resetTypeSpecificGraphIdsForTypeChange(newValue);
                                entityManager.persist(newValue);
                            } else {
                                newValue = entityManager.merge(newValue);
                            }
                            entityManager.flush();


//                	    T saved=newValue;
                            Substance saved = transactionalUpdate(newValue, oldJson);
                            builder.updatedEntity(saved);
                            builder.status(UpdateResult.STATUS.UPDATED);

                            return Optional.of(saved); //Delete & Create
                        }
                    });
                if(savedVersion ==null){
                    status.setRollbackOnly();
                }else {
                    //IDK?
//                    if(forceMoreSave[0]) {
//                        EntityUtils.EntityWrapper<T> savedVersion2 = entityPersistAdapter.performChangeOn(savedVersion, sec -> {
//
//                        });
//                    }
                    //only publish events if we save!
                    AbstractEntityUpdatedEvent<Substance> event = newUpdateEvent(savedVersion.getValue());
                    if(event !=null) {
                        applicationEventPublisher.publishEvent(event);
                        //todo: if RabbitMq is desired, make the following work
                        /*if (gsrsRabbitMqConfiguration.isEnabled() && exchangeName != null) {
                            rabbitTemplate.convertAndSend(exchangeName, substanceUpdatedKey, event);
                        }*/
                    }
                }

                UpdateResult<Substance> updateResult= builder.build();
                if(updateResult.getThrowable() !=null){
                    Sneak.sneakyThrow( updateResult.getThrowable());
                }
                return updateResult;
            }catch(IOException e){
                status.setRollbackOnly();
                throw new UncheckedIOException(e);
            }
        });
    }

    private void resetTypeSpecificGraphIdsForTypeChange(Substance replacement) {
        if (replacement instanceof ChemicalSubstance chemicalSubstance) {
            resetChemicalGraphIds(chemicalSubstance);
        }
        if (replacement instanceof MixtureSubstance mixtureSubstance) {
            resetMixtureGraphIds(mixtureSubstance.mixture);
        }
        if (replacement instanceof ProteinSubstance proteinSubstance) {
            resetProteinGraphIds(proteinSubstance.protein);
        }
        if (replacement instanceof PolymerSubstance polymerSubstance) {
            resetPolymerGraphIds(polymerSubstance.polymer);
        }
        if (replacement instanceof StructurallyDiverseSubstance structurallyDiverseSubstance) {
            resetStructurallyDiverseGraphIds(structurallyDiverseSubstance.structurallyDiverse);
        }
        if (replacement instanceof SpecifiedSubstanceGroup1Substance specifiedSubstanceGroup1Substance) {
            resetSpecifiedSubstanceGraphIds(specifiedSubstanceGroup1Substance.specifiedSubstance);
        }
        if (replacement instanceof NucleicAcidSubstance nucleicAcidSubstance) {
            resetNucleicAcidGraphIds(nucleicAcidSubstance.nucleicAcid);
        }
    }

    private void regenerateMoietiesForRootMolfileChange(Substance persisted, Substance updated) {
        if (!hasRootMolfileChange(persisted, updated)
                || !(updated instanceof ChemicalSubstance updatedChemical)
                || updatedChemical.getStructure() == null) {
            return;
        }
        String rootMolfile = updatedChemical.getStructure().molfile;
        if (rootMolfile == null) {
            updatedChemical.setMoieties(new ArrayList<>());
            return;
        }

        List<Structure> moietyStructures = new ArrayList<>();
        structureProcessor.instrument(rootMolfile, moietyStructures, true);
        List<Moiety> regeneratedMoieties = new ArrayList<>(moietyStructures.size());
        for (Structure moietyStructure : moietyStructures) {
            Moiety moiety = new Moiety();
            moiety.structure = new GinasChemicalStructure(moietyStructure);
            moiety.setCount(moietyStructure.count);
            regeneratedMoieties.add(moiety);
        }
        preserveRootMolfileForSingleMoiety(rootMolfile, regeneratedMoieties);
        updatedChemical.setMoieties(regeneratedMoieties);
    }

    private boolean hasRootMolfileChange(Substance persisted, Substance updated) {
        if (!(persisted instanceof ChemicalSubstance persistedChemical)
                || !(updated instanceof ChemicalSubstance updatedChemical)
                || persistedChemical.getStructure() == null
                || updatedChemical.getStructure() == null) {
            return false;
        }
        return !Objects.equals(persistedChemical.getStructure().molfile, updatedChemical.getStructure().molfile);
    }

    private void preserveRootMolfileForSingleMoiety(String rootMolfile, List<Moiety> moieties) {
        if (rootMolfile == null || moieties == null || moieties.size() != 1) {
            return;
        }
        Moiety moiety = moieties.get(0);
        if (moiety != null && moiety.structure != null) {
            moiety.structure.molfile = rootMolfile;
        }
    }

	@Override
	public List<UUID> getIDs() {
		List<UUID> IDs = repository.getAllIds();		
		return IDs;
    }

    private Optional<Substance> resolveExistingSubstanceForValidation(JsonNode updatedEntityJson, Substance newValue) {
        UUID substanceId = getIdFrom(newValue);
        if (substanceId != null) {
            Optional<Substance> existing = loadExistingSubstanceForValidation(substanceId);
            if (existing.isPresent()) {
                return existing;
            }
        }

        for (String identifierField : List.of("uuid", "approvalID")) {
            JsonNode identifierNode = updatedEntityJson.get(identifierField);
            if (identifierNode == null || identifierNode.isNull()) {
                continue;
            }
            String identifier = identifierNode.asText();
            if (identifier == null || identifier.isBlank()) {
                continue;
            }
            Optional<Substance> existing = loadExistingSubstanceForValidation(identifier);
            if (existing.isPresent()) {
                return existing;
            }
        }
        return Optional.empty();
    }

    private Optional<Substance> loadExistingSubstanceForValidation(UUID substanceId) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(this.getTransactionManager());
        transactionTemplate.setReadOnly(true);
        return transactionTemplate.execute(status -> repository.findById(substanceId)
                .map(this::detachFullyFetchedSubstance));
    }

    private Optional<Substance> loadExistingSubstanceForValidation(String identifier) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(this.getTransactionManager());
        transactionTemplate.setReadOnly(true);
        return transactionTemplate.execute(status -> {
            if (Util.isUUID(identifier)) {
                return repository.findById(UUID.fromString(identifier))
                        .map(this::detachFullyFetchedSubstance);
            }
            Substance existing = repository.findByApprovalID(identifier);
            return Optional.ofNullable(existing)
                    .map(this::detachFullyFetchedSubstance);
        });
    }

    private Substance detachFullyFetchedSubstance(Substance substance) {
        return JsonSubstanceFactory.makeSubstance(substance.toFullJsonNode());
    }

    private void normalizeChemicalStructuresForValidation(Substance substance) {
        if (!(substance instanceof ChemicalSubstance chemicalSubstance)) {
            return;
        }
        chemicalSubstance.setStructure(normalizeChemicalStructureForValidation(chemicalSubstance.getStructure()));
        if (chemicalSubstance.moieties != null) {
            for (Moiety moiety : chemicalSubstance.moieties) {
                if (moiety != null) {
                    moiety.structure = normalizeChemicalStructureForValidation(moiety.structure);
                }
            }
        }
    }

    private GinasChemicalStructure normalizeChemicalStructureForValidation(GinasChemicalStructure structure) {
        if (structure == null) {
            return null;
        }
        boolean missingHashes = isBlank(structure.getExactHash()) || isBlank(structure.getStereoInsensitiveHash());
        if (!missingHashes) {
            return structure;
        }
        String structureText = firstNonBlank(structure.molfile, structure.smiles);
        if (structureText == null) {
            return structure;
        }
        try {
            Structure instrumented = structureProcessor.instrument(structureText);
            if (structure.properties == null) {
                structure.properties = new ArrayList<>();
            } else {
                structure.properties.clear();
            }
            structure.properties.addAll(instrumented.properties);
            if (structure.stereoChemistry == null) {
                structure.stereoChemistry = instrumented.stereoChemistry;
            }
            if (structure.opticalActivity == null) {
                structure.opticalActivity = instrumented.opticalActivity;
            }
            if (structure.atropisomerism == null) {
                structure.atropisomerism = instrumented.atropisomerism;
            }
            if (isBlank(structure.stereoComments)) {
                structure.stereoComments = instrumented.stereoComments;
            }
            return structure;
        } catch (RuntimeException e) {
            log.debug("Unable to normalize chemical structure for validation", e);
            return structure;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
