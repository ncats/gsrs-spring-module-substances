package gsrs.module.substance.expanders.basic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.EntityFetcher;
import ix.ginas.exporters.RecordExpander;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
public class BasicRecordExpander implements RecordExpander<ix.ginas.models.v1.Substance> {

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    protected SubstanceEntityService substanceEntityService;

    private boolean includeDefinitionalItems = true;
    private int definitionalGenerations = 1;
    private int generationsToExpandRelated = 1;
    private boolean includeRelated = true;
    private boolean includeMediatorSubstances = true;

    private Set<String> retrievalErrors = new HashSet<>();

    public void applySettings(JsonNode settings) {
        if (!(settings instanceof ObjectNode)) {
            log.warn("in applySettings, settings parameter not of expected type (ObjectNode)");
            return;
        }
        ObjectNode objectNode = (ObjectNode) settings;
        if (objectNode.has("includeDefinitionalItems")) {
            includeDefinitionalItems = objectNode.get("includeDefinitionalItems").asBoolean();
        }
        if (objectNode.has("definitionalGenerations")) {
            definitionalGenerations = objectNode.get("definitionalGenerations").asInt();
        }
        if (objectNode.has("generationsToExpandRelated")) {
            generationsToExpandRelated = objectNode.get("generationsToExpandRelated").asInt();
        }
        if (objectNode.has("includeRelated")) {
            includeRelated = objectNode.get("includeRelated").asBoolean();
        }
        if (objectNode.has("includeMediatorSubstances")) {
            includeMediatorSubstances = objectNode.get("includeMediatorSubstances").asBoolean();
        }
    }

    @Override
    public Stream<Substance> expandRecord(Substance substance) {
        log.trace("starting in expandRecord with {}", substance.uuid.toString());
        Set<Substance> subs = new HashSet<>();
        subs.add(substance);
        retrievalErrors.clear();
        try {
            expandRecord(substance, 1, subs);
            log.trace("finished in expandRecord");
            return subs.stream();
        } catch (Exception e) {
            log.error("Error expanding record ", e);
        }
        return Stream.empty();
    }

    public void expandRecord(Substance substance, int initialGeneration, Set<Substance> subs) throws Exception {
        log.trace("starting in expandRecord, generation: {}", initialGeneration);
        if (includeDefinitionalItems) {
            appendDefinitionalSubstances(substance, initialGeneration, subs);
        }
        if (includeRelated) {
            appendRelatedSubstances(substance, initialGeneration, subs);
        }
    }

    private void appendDefinitionalSubstances(Substance substance, int generation,
                                              Set<Substance> subs) throws Exception {
        log.trace("starting in appendDefinitionalSubstances, generation: {}", generation);
        for (SubstanceReference ref : substance.getDependsOnSubstanceReferences()) {
            if (ref.refuuid != null && retrievalErrors.contains(ref.refuuid)) {
                log.trace("short-circuiting retrieval of {}", ref.refuuid);
                continue;
            }
            log.trace("before retrieval");
            Substance referred = (Substance) EntityFetcher.of(ref.getKeyForReferencedSubstance()).call();
            log.trace("completed fetch");
            if (referred == null) {
                log.warn("Error retrieving substance by ref approval id {} or uuid {}", ref.approvalID,
                        ref.refuuid);
            } else {
                subs.add(referred);
                log.trace("added substance {} to the output", referred.uuid.toString());
                if (generation < definitionalGenerations) {
                    expandRecord(referred, generation + 1, subs);
                }
            }
        }
    }

    private void appendRelatedSubstances(Substance substance, int generation,
                                         Set<Substance> subs) throws Exception {
        log.trace("starting in appendRelatedSubstances, generation: {}", generation);
        for (Relationship rel : substance.relationships) {
            if (rel.relatedSubstance != null && rel.relatedSubstance.refuuid != null) {
                if (retrievalErrors.contains(rel.relatedSubstance.refuuid)) {
                    log.trace("short-circuiting retrieval of {}", rel.relatedSubstance.refuuid);
                } else {
                    log.trace("looking for substance with UUID {}", rel.relatedSubstance.refuuid);
                    if (retrievalErrors.contains(rel.relatedSubstance.refuuid) || subs.stream().anyMatch(s -> s.getUuid().toString().equals(rel.relatedSubstance.refuuid))) {
                        log.trace("skipping retrieval of substance because it has failed before or it's already in the output set");
                    } else {
                        Substance relatedSubstance = (Substance) EntityFetcher.of(rel.relatedSubstance.getKeyForReferencedSubstance()).call();
                        if (relatedSubstance != null) {
                            subs.add(relatedSubstance);
                            if (generation < generationsToExpandRelated) {
                                expandRecord(relatedSubstance, generation + 1, subs);
                            }
                        } else {
                            log.warn("Error retrieving related substance with UUID {}", rel.relatedSubstance.refuuid);
                        }
                    }
                }
            }
            if (includeMediatorSubstances && rel.mediatorSubstance != null) {
                if ((rel.mediatorSubstance.refuuid != null && retrievalErrors.contains(rel.mediatorSubstance.refuuid))
                        || retrievalErrors.contains(rel.mediatorSubstance.refuuid)) {
                    log.trace("short-circuiting retrieval of mediator {}", rel.mediatorSubstance.refuuid);
                } else {
                    log.trace("going to fetch mediator substance ");
                    Substance mediatorSubstance = (Substance) EntityFetcher.of(rel.mediatorSubstance.getKeyForReferencedSubstance()).call();
                    log.trace("complete");
                    if (mediatorSubstance != null) {
                        log.trace("We have a mediator sub: {}", mediatorSubstance.uuid.toString());
                        subs.add(mediatorSubstance);
                        if (generation < generationsToExpandRelated) {
                            expandRecord(mediatorSubstance, generation + 1, subs);
                        }
                    } else {
                        log.warn("Error retrieving related substance with UUID {}", rel.mediatorSubstance.refuuid);
                    }
                }
            }
        }
    }
}
