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
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
public class BasicRecordExpander implements RecordExpander<ix.ginas.models.v1.Substance> {

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    protected SubstanceEntityService substanceEntityService;

    private boolean includeDefinitions = true;
    private int generationsToExpand = 1;
    private boolean includeRelated = true;

    public void applySettings(JsonNode settings) {
        if (!(settings instanceof ObjectNode)) {
            log.warn("in applySettings, settings parameter not of expected type (ObjectNode)");
            return;
        }
        ObjectNode objectNode = (ObjectNode) settings;
        if (objectNode.has("includeDefinitions")) {
            includeDefinitions = objectNode.get("includeDefinitions").asBoolean();
        }
        if (objectNode.has("generationsToExpand")) {
            generationsToExpand = objectNode.get("generationsToExpand").asInt();
        }
        if (objectNode.has("includeRelated")) {
            includeRelated = objectNode.get("includeRelated").asBoolean();
        }
    }

    @Override
    public Stream<Substance> expandRecord(Substance substance) {
        log.trace("expandRecord with {}", substance.uuid.toString());
        Set<Substance> subs = new HashSet<>();
        subs.add(substance);
        if (includeDefinitions) {
            appendDefinitionalSubstances(substance, (s) -> {
                if (subs.stream().noneMatch(s2 -> s2.getUuid().equals(s.getUuid()))) {
                    subs.add(s);
                }
            }, 1);
        }
        if (includeRelated) {
            appendRelatedSubstances(substance, (s)->{
                if (subs.stream().noneMatch(s2 -> s2.getUuid().equals(s.getUuid()))) {
                    subs.add(s);
                }
            }, 1);
        }
        return subs.stream();
    }

    private void appendDefinitionalSubstances(Substance substance, Consumer<Substance> consumer, int generation) {
        log.trace("starting in appendDefinitionalSubstances, generation: {}", generation);
        for (SubstanceReference ref : substance.getDependsOnSubstanceReferences()) {
            Substance referred = substanceRepository.findBySubstanceReference(ref);

            if (referred == null) {
                log.warn("Error retrieving substance by ref approvalid {} or uuid {}", ref.approvalID,
                        ref.refuuid);
            } else {
                //may be necessary to retrieve entire substance?
                Optional<Substance> optionalSubstance = substanceEntityService.get(referred.uuid);
                if( !optionalSubstance.isPresent()){
                    log.error("Error retrieving complete substance!");
                    continue;
                }
                referred =optionalSubstance.get();
                //log.trace("accepting substance {}", referred.uuid.toString());
                consumer.accept(referred);
                if (generation < generationsToExpand) {
                    appendDefinitionalSubstances(referred, consumer, generation+1);
                }
            }
        }
    }

    private void appendRelatedSubstances(Substance substance, Consumer<Substance> consumer, int generation) {
        log.trace("starting in appendRelatedSubstances, generation: {}", generation);
        for (Relationship rel : substance.relationships) {

            if (rel.relatedSubstance != null && rel.relatedSubstance.refuuid != null) {
                log.trace("looking for substance with UUID {}", rel.relatedSubstance.refuuid);
                Substance relatedSubstance = substanceRepository.findBySubstanceReference(rel.relatedSubstance);
                if(relatedSubstance!=null) {
                    //may be necessary to retrieve entire substance?
                    Optional<Substance> optionalSubstance=substanceEntityService.get(relatedSubstance.uuid);
                    if(!optionalSubstance.isPresent()){
                        log.error("Error retrieving substance!");
                        continue;
                    }
                    relatedSubstance =optionalSubstance.get();
                    consumer.accept(relatedSubstance);
                    if (generation < generationsToExpand) {
                        appendRelatedSubstances(relatedSubstance, consumer, generation+1);
                    }
                }
                else {
                    log.warn("Error retrieving related substance with UUID {}", rel.relatedSubstance.refuuid);
                }
            }
            if( rel.mediatorSubstance!= null ){
                Substance mediatorSubstance = substanceRepository.findBySubstanceReference (rel.mediatorSubstance);
                if(mediatorSubstance!=null) {
                    log.trace("We have a mediator sub: {}", mediatorSubstance.uuid.toString());
                    Optional<Substance> optionalSubstance=substanceEntityService.get(mediatorSubstance.uuid);
                    if(!optionalSubstance.isPresent()){
                        log.error("Error retrieving substance!");
                        continue;
                    }
                    mediatorSubstance = optionalSubstance.get();
                    consumer.accept(mediatorSubstance);
                    if (generation < generationsToExpand) {
                        appendRelatedSubstances(mediatorSubstance, consumer, generation+1);
                    }
                } else {
                    log.warn("Error retrieving related substance with UUID {}", rel.mediatorSubstance.refuuid);
                }

            }
        }

    }
}
