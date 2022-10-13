package gsrs.module.substance.expanders.basic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.ginas.exporters.RecordExpander;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
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
    private boolean includeModifyingItems  = true;

    private Set<Substance> subs = new HashSet<>();
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
        if(objectNode.has("includeModifyingItems")) {
            includeModifyingItems = objectNode.get("includeModifyingItems").asBoolean();
        }
    }

    @Override
    public Stream<Substance> expandRecord(Substance substance) {
        log.trace("starting in expandRecord with {}", substance.uuid.toString());
        subs.clear();
        retrievalErrors.clear();;
        Stream<Substance> stream =expandRecord(substance, 1);
        log.trace("finished in expandRecord");
        return stream;
    }

    public Stream<Substance> expandRecord(Substance substance, int initialGeneration) {
        log.trace("starting in expandRecord, generation: {}", initialGeneration);
        subs.add(substance);
        if (includeDefinitionalItems) {
            appendDefinitionalSubstances(substance, (s) -> {
                if (subs.stream().noneMatch(s2 -> s2.getUuid().equals(s.getUuid()))) {
                    subs.add(s);
                }
            }, initialGeneration);
        }
        if (includeRelated) {
            appendRelatedSubstances(substance, (s)->{
                if (subs.stream().noneMatch(s2 -> s2.getUuid().equals(s.getUuid()))) {
                    subs.add(s);
                }
            }, initialGeneration);
        }
        return subs.stream();
    }

    private void appendDefinitionalSubstances(Substance substance, Consumer<Substance> consumer, int generation) {
        log.trace("starting in appendDefinitionalSubstances, generation: {}", generation);
        for (SubstanceReference ref : substance.getDependsOnSubstanceReferences()) {
            if(ref.refuuid!=null && retrievalErrors.contains(ref.refuuid)){
                log.trace("short-circuiting retrieval of {}", ref.refuuid);
                continue;
            }
            log.trace("before substanceRepository.findBySubstanceReference");
            Substance referred = substanceRepository.findBySubstanceReference(ref);
            log.trace("completed substanceRepository.findBySubstanceReference");
            if (referred == null) {
                log.warn("Error retrieving substance by ref approvalid {} or uuid {}", ref.approvalID,
                        ref.refuuid);
            } else {
                if(referred.uuid!=null && retrievalErrors.contains(referred.uuid.toString())) {
                    log.trace("skipping substance that we already were unable to retrieve");
                    continue;
                }
                //may be necessary to retrieve entire substance?
                Optional<Substance> optionalSubstance = substanceEntityService.get(referred.uuid);
                if( !optionalSubstance.isPresent()){
                    log.error("Error retrieving complete substance!");
                    continue;
                }
                referred =optionalSubstance.get();
                consumer.accept(referred);
                log.trace("added substance {} to the output", referred.uuid.toString());
                if (generation < definitionalGenerations) {
                    expandRecord(referred, generation+1);
                }
            }
        }
    }

    private void appendRelatedSubstances(Substance substance, Consumer<Substance> consumer, int generation) {
        log.trace("starting in appendRelatedSubstances, generation: {}", generation);
        for (Relationship rel : substance.relationships) {
            if (rel.relatedSubstance != null && rel.relatedSubstance.refuuid != null) {
                if( retrievalErrors.contains(rel.relatedSubstance.refuuid)) {
                    log.trace("short-circuiting retrieval of {}", rel.relatedSubstance.refuuid);
                } else {
                    log.trace("looking for substance with UUID {}", rel.relatedSubstance.refuuid);
                    if (retrievalErrors.contains(rel.relatedSubstance.refuuid) || subs.stream().anyMatch(s -> s.getUuid().toString().equals(rel.relatedSubstance.refuuid))) {
                        log.trace("skipping retrieval of substance because it has failed before or it's already in the output set");
                    } else {
                        Substance relatedSubstance = substanceRepository.findBySubstanceReference(rel.relatedSubstance);
                        if (relatedSubstance != null) {
                            //may be necessary to retrieve entire substance?
                            Optional<Substance> optionalSubstance = substanceEntityService.get(relatedSubstance.uuid);
                            if (!optionalSubstance.isPresent()) {
                                log.error("Error retrieving substance!");
                                continue;
                            }
                            relatedSubstance = optionalSubstance.get();
                            consumer.accept(relatedSubstance);
                            if (generation < generationsToExpandRelated) {
                                expandRecord(relatedSubstance, generation + 1);
                            }

                        } else {
                            log.warn("Error retrieving related substance with UUID {}", rel.relatedSubstance.refuuid);
                        }
                    }
                }
            }
            if( includeModifyingItems && rel.mediatorSubstance!= null ){
                if( rel.mediatorSubstance.refuuid!=null && retrievalErrors.contains(rel.mediatorSubstance.refuuid)){
                    log.trace("short-circuiting retrieval of mediator {}", rel.mediatorSubstance.refuuid);
                } else {
                    log.trace("calling ");
                    Substance mediatorSubstance = substanceRepository.findBySubstanceReference(rel.mediatorSubstance);
                    log.trace("complete");
                    if (mediatorSubstance != null) {
                        log.trace("We have a mediator sub: {}", mediatorSubstance.uuid.toString());
                        String mediatorUuid = mediatorSubstance.uuid.toString();
                        if (retrievalErrors.contains(mediatorUuid) || subs.stream().anyMatch(s -> s.getUuid().toString().equals(mediatorUuid))) {
                            log.trace("skipping retrieval of mediator substance because of previous error OR it's already in the output set");
                        } else {
                            Optional<Substance> optionalSubstance = substanceEntityService.get(mediatorSubstance.uuid);
                            if (!optionalSubstance.isPresent()) {
                                log.error("Error retrieving substance!");
                                continue;
                            }
                            mediatorSubstance = optionalSubstance.get();
                            consumer.accept(mediatorSubstance);
                            if (generation < generationsToExpandRelated) {
                                expandRecord(mediatorSubstance, generation + 1);
                            }
                        }
                    } else {
                        log.warn("Error retrieving related substance with UUID {}", rel.mediatorSubstance.refuuid);
                    }
                }
            }
        }
    }
}
