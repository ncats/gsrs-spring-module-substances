package gsrs.module.substance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import gsrs.validator.ValidatorConfig;
import ix.core.EntityFetcher;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.models.ForceUpdatableModel;
import ix.core.util.EntityUtils;
import ix.core.util.LogUtil;
import ix.core.validator.*;
import ix.ginas.models.v1.Linkage;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.GinasCommonData;
import ix.ginas.models.v1.Amount;
import ix.ginas.models.v1.Component;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Glycosylation;
import ix.ginas.models.v1.Material;
import ix.ginas.models.v1.Moiety;
import ix.ginas.models.v1.Mixture;
import ix.ginas.models.v1.MixtureSubstance;
import ix.ginas.models.v1.NucleicAcid;
import ix.ginas.models.v1.NucleicAcidSubstance;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Note;
import ix.ginas.models.v1.OtherLinks;
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
import ix.ginas.models.v1.StructurallyDiverse;
import ix.ginas.models.v1.StructurallyDiverseSubstance;
import ix.ginas.models.v1.Sugar;
import ix.ginas.models.v1.Subunit;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import ix.ginas.models.v1.Unit;
import ix.ginas.utils.JsonSubstanceFactory;
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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Service
@Slf4j
public class SubstanceEntityServiceImpl extends AbstractGsrsEntityService<Substance, UUID> implements SubstanceEntityService {
    public static final String  CONTEXT = "substances";


    public SubstanceEntityServiceImpl() {
        super(CONTEXT,  IdHelpers.UUID, "gsrs_exchange", "substance.created", "substance.updated");
    }

    @Autowired
    private SubstanceRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

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
        //first bump version?
        substance.forceUpdate();

        //postUpdate/etc only gets called on flush, apparently?
        return repository.saveAndFlush(getEntityManager().merge(substance));
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
            if (substance instanceof NucleicAcidSubstance
                    || substance instanceof MixtureSubstance
                    || substance instanceof ChemicalSubstance
                    || substance instanceof ProteinSubstance
                    || substance instanceof PolymerSubstance
                    || substance instanceof StructurallyDiverseSubstance
                    || substance instanceof SpecifiedSubstanceGroup1Substance) {
                EntityManager entityManager = getEntityManager();
                entityManager.persist(substance);
                entityManager.flush();
                return substance;
            }
            return repository.saveAndFlush(substance);
        }catch(Throwable t){
            t.printStackTrace();
            throw t;
        }
    }

    private void normalizeCreateGraph(Substance substance) {
        if (substance == null) {
            return;
        }
        substance.uuid = null;
        substance.version = "1";
        if (substance.modifications != null) {
            substance.modifications.uuid = null;
        }
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
        JsonNode substanceClassNode = root.get("substanceClass");
        if (substanceClassNode == null || !"chemical".equalsIgnoreCase(substanceClassNode.asText())) {
            return json;
        }

        ObjectNode copy = root.deepCopy();
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
            updated.setStructure(persisted.getStructure());
        } else if (persisted.getStructure() != null && updated.getStructure() != null) {
            updated.getStructure().id = persisted.getStructure().id;
            updated.getStructure().version = persisted.getStructure().version;
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
            updated.getMoieties().set(i, matchedPersistedMoiety);
        }
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
        return !sameChemicalStructureForDiff(persistedChemical.getStructure(), updatedChemical.getStructure());
    }

    private void normalizeUpdatedEntityForReplacement(Substance persisted, Substance updated) {
        if (!(persisted instanceof ChemicalSubstance persistedChemical)
                || !(updated instanceof ChemicalSubstance updatedChemical)) {
            return;
        }
        if (persistedChemical.getStructure() != null && updatedChemical.getStructure() != null) {
            updatedChemical.getStructure().id = persistedChemical.getStructure().id;
            updatedChemical.getStructure().version = persistedChemical.getStructure().version;
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
        GinasChemicalStructure existingStructure = managed instanceof ChemicalSubstance chemicalManaged
                ? chemicalManaged.getStructure()
                : null;
        List<Moiety> existingMoieties = managed instanceof ChemicalSubstance chemicalManaged
                ? chemicalManaged.getMoieties()
                : null;
        Map<UUID, Name> existingNames = mapByUuid(managed.names);
        Map<UUID, Code> existingCodes = mapByUuid(managed.codes);
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
        JsonNode updatedJson = objectMapper.valueToTree(updated);
        Substance replaced = objectMapper.readerForUpdating(managed).readValue(updatedJson);
        if (replaced instanceof ChemicalSubstance replacedChemical && existingStructure != null) {
            GinasChemicalStructure updatedStructure = replacedChemical.getStructure();
            if (updatedStructure != null && updatedStructure != existingStructure
                    && Objects.equals(updatedStructure.id, existingStructure.id)) {
                JsonNode updatedStructureJson = objectMapper.valueToTree(updatedStructure);
                objectMapper.readerForUpdating(existingStructure).readValue(updatedStructureJson);
                existingStructure.version = updatedStructure.version != null
                        ? updatedStructure.version
                        : existingStructure.version;
                replacedChemical.setStructure(existingStructure);
            }
        }
        replaced.names = reconcileManagedChildren(replaced.names, existingNames, child -> child.setOwner(replaced));
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
    public UpdateResult<Substance> updateEntityWithoutValidation(JsonNode updatedEntityJson) {
        return performUpdateEntity(updatedEntityJson, null);
    }

    private UpdateResult<Substance> performUpdateEntity(JsonNode updatedEntityJson,
                                                        ValidationResponse<Substance> validationResponse) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(this.getTransactionManager());

        return transactionTemplate.execute( status-> {
            try {
                Substance updatedEntity = JsonEntityUtil.fixOwners(fromUpdatedJson(updatedEntityJson), true);
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

                        boolean usePojoPatch = false;
                        //only use POJO patch if the entities are the same type
                        if (oWrap.getEntityClass().equals(nWrap.getEntityClass())) {
                            usePojoPatch = true;
                        }
                        if (usePojoPatch && hasChemicalDefinitionChange(oldEntity, updatedEntity)) {
                            usePojoPatch = false;
                        }
                        boolean chemicalDefinitionChange = hasChemicalDefinitionChange(oldEntity, updatedEntity);
                        if (usePojoPatch) {
                            normalizeUpdatedEntityForDiff(oldEntity, updatedEntity);
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
                            //This is the last line of defense for making sure that the patch worked
                            //Should throw an exception here if there's a major problem
                            //This is inefficient, but forces confirmation that the object is fully realized
                            String serialized = EntityUtils.EntityWrapper.of(oldEntity).toJsonDiffJson();


                            while (!changeStack.isEmpty()) {
                                Object v = changeStack.pop();
                                EntityUtils.EntityWrapper<Object> ewchanged = EntityUtils.EntityWrapper.of(v);
                                if (!ewchanged.isIgnoredModel() && ewchanged.isEntity()) {
                                    Object o = ewchanged.getValue();
                                    if (o instanceof ForceUpdatableModel) {
                                        //Maybe don't do twice? IDK.
                                        ((ForceUpdatableModel) o).forceUpdate();
                                    }

                                    entityManager.merge(o);
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

                            if (chemicalDefinitionChange) {
                                Substance newValue = (Substance) nWrap.getValue();
                                oldValue = applyReplacementToManagedEntity(oldValue, newValue);
                                oldValue = fixUpdatedIfNeeded(JsonEntityUtil.fixOwners(oldValue, true));
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
                            newValue = entityManager.merge(newValue);
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

	@Override
	public List<UUID> getIDs() {
		List<UUID> IDs = repository.getAllIds();		
		return IDs;
	}

}
