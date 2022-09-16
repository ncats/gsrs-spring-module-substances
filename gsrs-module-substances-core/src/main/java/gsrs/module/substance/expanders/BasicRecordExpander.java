package gsrs.module.substance.expanders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.ginas.exporters.RecordExpander;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.StructurallyDiverseSubstance;
import ix.ginas.models.v1.SubstanceReference;
import lombok.extern.slf4j.Slf4j;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
public class BasicRecordExpander implements RecordExpander<ix.ginas.models.v1.Substance> {

    @Autowired
    private SubstanceRepository substanceRepository;

    private boolean includeDefinitions = true;
    private int generationsToExpand =1;

    public void applySettings(JsonNode settings){
        if(! (settings instanceof ObjectNode)) {
            log.warn("in applySettings, settings parameter not of expected type (ObjectNode)");
            return;
        }
        ObjectNode objectNode =(ObjectNode) settings;
        if(objectNode.has("includeDefinitions")) {
            includeDefinitions = objectNode.get("includeDefinitions").asBoolean();
        }
        if(objectNode.has("generationsToExpand")){
            generationsToExpand = objectNode.get("generationsToExpand").asInt();
        }
    }

    @Override
    public Stream<Substance> expandRecord(Substance substance) {
        log.trace("expandRecord with {}", substance.uuid.toString());
        Set<Substance> subs = new HashSet<>();
        subs.add(substance);
        for(SubstanceReference ref:  substance.getDependsOnSubstanceReferences()) {
            if(!subs.stream().anyMatch(s->s.getUuid().toString().equals(ref.refuuid))) {
                Substance referred=substanceRepository.findBySubstanceReference(ref);
                if( referred == null){
                    log.warn("Error retrieving substance by ref approvalid {} or uuid {}", ref.approvalID,
                            ref.refuuid);
                } else {
                    subs.add(substanceRepository.findBySubstanceReference(ref));
                }
            }
        }
        /*for(Relationship rel: substance.relationships) {
            subs.add( rel.relatedSubstance.wrappedSubstance);
        }
        if( substance instanceof StructurallyDiverseSubstance) {
            StructurallyDiverseSubstance structurallyDiverseSubstance = (StructurallyDiverseSubstance)substance;
            if(structurallyDiverseSubstance.getParentSubstanceReference() != null && structurallyDiverseSubstance.getParentSubstanceReference().wrappedSubstance!=null) {
                subs.add(structurallyDiverseSubstance.getParentSubstanceReference().wrappedSubstance);
            }
        }*/
        return subs.stream();
    }
}
