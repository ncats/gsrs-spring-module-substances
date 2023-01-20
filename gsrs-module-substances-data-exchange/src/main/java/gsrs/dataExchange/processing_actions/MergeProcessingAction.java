package gsrs.dataexchange.processing_actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.dataexchange.model.ProcessingAction;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class MergeProcessingAction implements ProcessingAction<Substance> {
    @Override
    public Substance process(Substance source, Substance existing, Map<String, Object> parameters, Consumer<String> processLog){
        log.trace("about to call existing.toBuilder() on existing: {}", existing.getUuid());
        SubstanceBuilder builder = existing.toBuilder();
        ObjectMapper mapper= new ObjectMapper();

        Objects.requireNonNull(source, "Must have a non-null source Substance to merge");
        Objects.requireNonNull(existing, "Must have a non-null existing Substance to merge");

        List<String> referencesToCopy = new ArrayList<>();//UUIDs
        if(hasTrueValue(parameters, "MergeReferences")){
            source.references.forEach(r->{
                if( existing.references.stream().anyMatch(r2->r2.docType!=null && r2.docType.equals(r.docType) && r2.citation!=null
                        && r2.citation.equals(r.citation))){
                    processLog.accept(String.format("Reference %s/%s was already present;", r.docType, r.citation));
                } else {
                    EntityUtils.EntityInfo<Reference> eics= EntityUtils.getEntityInfoFor(Reference.class);
                    try {
                        Reference newReference= eics.fromJson(mapper.writeValueAsString(r));
                        builder.addReference(newReference);
                        processLog.accept(String.format("Reference %s/%s was copied", r.docType, r.citation));
                    }
                    catch (IOException e ) {
                        processLog.accept(String.format("Error copying reference %s/%s", r.docType, r.citation));
                        log.error("Error copying reference");
                    }
                }
            });
        }
        
        if(hasTrueValue(parameters, "MergeNames")){
            source.names.forEach(n-> {
                if( existing.names.stream().anyMatch(en->en.name.equals(n.name))){
                    processLog.accept(String.format("Name %s was already present;", n.name));
                }else{
                    EntityUtils.EntityInfo<Name> eics= EntityUtils.getEntityInfoFor(Name.class);
                    Name newName;
                    try {
                        newName = eics.fromJson(n.toJson());
                        builder.addName( newName);
                        newName.getReferences().forEach(ref->{
                            log.trace("looking for reference with term {} and value {}", ref.term, ref.getValue());
                            if(builder.build().references.stream().noneMatch(r->r.getUuid().toString().equals(ref.getValue()))){
                                referencesToCopy.add(ref.getValue());
                            }
                        });

                        processLog.accept(String.format("Adding Name %s;", n.name));
                    } catch (IOException e) {
                        log.error("error copying name", e);
                        processLog.accept(String.format("Error adding Name %s;", n.name));
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        if(hasTrueValue(parameters, "MergeCodes")){
            source.codes.forEach(c-> {
                if( existing.codes.stream().anyMatch(en->en.code.equals(c.code) && en.codeSystem.equals(c.codeSystem))){
                    processLog.accept(String.format("code %s was already present;", c.code));
                }else{
                    EntityUtils.EntityInfo<Code> eics= EntityUtils.getEntityInfoFor(Code.class);
                    try {
                        Code newCode=eics.fromJson(c.toJson());
                        builder.addCode( newCode);
                        processLog.accept(String.format("Adding code %s;", c.code));
                        newCode.getReferences().forEach(ref->{
                            log.trace("looking for reference with term {} and value {}", ref.term, ref.getValue());
                            if(builder.build().references.stream().noneMatch(r->r.getUuid().toString().equals(ref.getValue()))){
                                referencesToCopy.add(ref.getValue());
                            }
                        });

                    } catch (IOException e) {
                        log.error("Error copying code", e);
                        processLog.accept(String.format("Error adding code %s of system %s;", c.code, c.codeSystem));
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        if(hasTrueValue(parameters, "MergeProperties")){
            source.properties.forEach(p-> {
                if( hasTrueValue(parameters, "PropertyNameUniqueness") &&
                        (existing.properties.stream().anyMatch(en->en.getPropertyType().equals(p.getPropertyType())
                        && en.getName().equals(p.getName()))
                        || builder.build().properties.stream().anyMatch(p2->p2.getName().equals(p.getName()) && p2.getPropertyType().equals(p.getPropertyType())))){
                    processLog.accept(String.format("property %s was already present;", p.getName()));
                }else{
                    EntityUtils.EntityInfo<Property> eics= EntityUtils.getEntityInfoFor(Property.class);
                    try {
                        Property newproperty=eics.fromJson(p.toJson());
                        builder.addProperty( newproperty);
                        processLog.accept(String.format("Adding property %s;", p.getName()));
                        newproperty.getReferences().forEach(ref->{
                            log.trace("looking for reference with term {} and value {}", ref.term, ref.getValue());
                            if(builder.build().references.stream().noneMatch(r->r.getUuid().toString().equals(ref.getValue()))){
                                referencesToCopy.add(ref.getValue());
                            }
                        });

                    } catch (IOException e) {
                        log.error("Error copying property", e);
                        processLog.accept(String.format("Error adding property %s;", p.getName()));
                        throw new RuntimeException(e);
                    }

                }
            });
        }

        if(hasTrueValue(parameters, "MergeNotes")){
            source.notes.forEach(n-> {
                if( hasTrueValue(parameters, "NoteUniqueness") &&
                        (existing.notes.stream().anyMatch(en->en.note.equals(n.note)))){
                    processLog.accept(String.format("note %s was already present;", n.note));
                }else{
                    EntityUtils.EntityInfo<Note> eics= EntityUtils.getEntityInfoFor(Note.class);
                    try {
                        Note newNote=eics.fromJson(n.toJson());
                        builder.addNote(newNote);
                        processLog.accept("Adding note;");
                        newNote.getReferences().forEach(ref->{
                            log.trace("looking for reference with term {} and value {}", ref.term, ref.getValue());
                            if(builder.build().references.stream().noneMatch(r->r.getUuid().toString().equals(ref.getValue()))){
                                referencesToCopy.add(ref.getValue());
                            }
                        });
                    } catch (IOException e) {
                        log.error("Error copying note", e);
                        processLog.accept(String.format("Error adding note %s;", n.note));
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        if(hasTrueValue(parameters, "MergeModifications")) {
            if(hasTrueValue(parameters, "MergeStructuralModifications")){
                source.modifications.structuralModifications.forEach(sm->{
                    //simple-minded copy here to start
                    //TODO: evaluate how accurate/useful this is
                    EntityUtils.EntityInfo<StructuralModification> structuralModificationEntityInfo =EntityUtils.getEntityInfoFor(StructuralModification.class);
                    try {
                        StructuralModification newStructuralModification = structuralModificationEntityInfo.fromJson(sm.toJson());
                        builder.addStructuralModification(newStructuralModification);
                        newStructuralModification.getReferences().forEach(ref->{
                            log.trace("looking for reference with term {} and value {}", ref.term, ref.getValue());
                            if(builder.build().references.stream().noneMatch(r->r.getUuid().toString().equals(ref.getValue()))){
                                referencesToCopy.add(ref.getValue());
                            }
                        });

                    } catch (IOException e){
                        log.error("Error copying StructuralModification", e);
                        processLog.accept(String.format("Error adding StructuralModification %s;", sm));
                        throw new RuntimeException(e);
                    }
                });
            }

            if(hasTrueValue(parameters, "MergeAgentModifications")){
                source.modifications.agentModifications.forEach(am->{
                    //simple-minded copy here to start
                    //TODO: evaluate how accurate/useful this is
                    EntityUtils.EntityInfo<AgentModification> agentModificationEntityInfo =EntityUtils.getEntityInfoFor(AgentModification.class);
                    try {
                        AgentModification newAgentModification = agentModificationEntityInfo.fromJson(am.toJson());
                        builder.addAgentModification(newAgentModification);
                        newAgentModification.getReferences().forEach(ref->{
                            log.trace("looking for reference with term {} and value {}", ref.term, ref.getValue());
                            if(builder.build().references.stream().noneMatch(r->r.getUuid().toString().equals(ref.getValue()))){
                                referencesToCopy.add(ref.getValue());
                            }
                        });
                    } catch (IOException e){
                        log.error("Error copying AgentModification", e);
                        processLog.accept(String.format("Error adding AgentModification %s;", am));
                        throw new RuntimeException(e);
                    }
                });
            }

            if(hasTrueValue(parameters, "MergePhysicalModifications")){
                source.modifications.physicalModifications.forEach(pm->{
                    //simple-minded copy here to start
                    //TODO: evaluate how accurate/useful this is
                    EntityUtils.EntityInfo<PhysicalModification> physicalModificationEntityInfo =EntityUtils.getEntityInfoFor(PhysicalModification.class);
                    try {
                        PhysicalModification newPhysicalModification = physicalModificationEntityInfo.fromJson(pm.toJson());
                        builder.addPhysicalModification(newPhysicalModification);
                        newPhysicalModification.getReferences().forEach(ref->{
                            log.trace("looking for reference with term {} and value {}", ref.term, ref.getValue());
                            if(builder.build().references.stream().noneMatch(r->r.getUuid().toString().equals(ref.getValue()))){
                                referencesToCopy.add(ref.getValue());
                            }
                        });
                    } catch (IOException e){
                        log.error("Error copying PhysicalModification", e);
                        processLog.accept(String.format("Error adding PhysicalModification %s;", pm));
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        if(hasTrueValue(parameters, "MergeRelationships")){
            source.relationships.forEach(n-> {
                if( hasTrueValue(parameters, "RelationshipUniqueness")
                        && existing.relationships.stream().anyMatch(en->en.type.equals(n.type) && en.relatedSubstance.refuuid.equals(n.relatedSubstance.refuuid))){
                    processLog.accept(String.format("Relationship of type %s to substance %s was already present;", n.type, n.relatedSubstance.refuuid));
                }else{
                    EntityUtils.EntityInfo<Relationship> eics= EntityUtils.getEntityInfoFor(Relationship.class);

                    try {
                        Relationship newRel = eics.fromJson(n.toJson());
                        builder.addRelationship(newRel);
                        processLog.accept(String.format("Adding Relationship of type %s to substances %s;", newRel.type,
                                newRel.relatedSubstance.refuuid));
                        newRel.getReferences().forEach(ref->{
                            log.trace("looking for reference with term {} and value {}", ref.term, ref.getValue());
                            if(builder.build().references.stream().noneMatch(r->r.getUuid().toString().equals(ref.getValue()))){
                                referencesToCopy.add(ref.getValue());
                            }
                        });
                    } catch (IOException e) {
                        log.error("error copying name", e);
                        processLog.accept(String.format("Error adding Relationship %s;", n.type));
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        if(!hasTrueValue(parameters, "SkipLevelingReferences")) {
            EntityUtils.EntityInfo<Reference> eicsRefs = EntityUtils.getEntityInfoFor(Reference.class);
            for (String refUuid : referencesToCopy) {
                if (builder.build().references.stream().noneMatch(r -> r.getUuid().toString().equals(refUuid))) {
                    Reference sourceRef = source.references.stream().filter(r -> r.getUuid().toString().equals(refUuid)).findFirst().get();
                    try {
                        Reference newReference = eicsRefs.fromJson(mapper.writeValueAsString(sourceRef));
                        builder.addReference(newReference);
                        processLog.accept(String.format("Appending Reference %s;", refUuid));
                    } catch (IOException ex) {
                        log.error("Error copying reference {}", refUuid);
                        processLog.accept(String.format("Error copying reference %s. Error: %s", refUuid, ex.getMessage()));
                    }
                }
            }
        }
        return builder.build();
    }
}
