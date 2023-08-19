package gsrs.dataexchange.processingactions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.dataexchange.model.ProcessingAction;
import gsrs.module.substance.importers.model.MergeProcessingActionParameters;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.*;
import ix.ginas.models.v1.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class MergeProcessingAction implements ProcessingAction<Substance> {

    @Autowired
    private SubstanceRepository substanceRepository;

    @Override
    public Substance process(Substance source, Substance existing, Map<String, Object> parameters, Consumer<String> processLog){
        ObjectMapper mapper = new ObjectMapper();
        if( !parameters.containsKey("mergeSettings")) {
            log.warn("no mergeSettings found!");
            return existing;
        }
        MergeProcessingActionParameters mergeParameters= mapper.convertValue(parameters.get("mergeSettings"), MergeProcessingActionParameters.class);
        log.trace("about to call existing.toBuilder() on existing: {}", existing.getUuid());
        AbstractSubstanceBuilder builder = toAbstractBuilder(existing);

        Objects.requireNonNull(source, "Must have a non-null source Substance to merge");
        Objects.requireNonNull(existing, "Must have a non-null existing Substance to merge");

        Map<String, String> referencesToCopy = new HashMap<>();//UUIDs

        if( mergeParameters.getCopyStructure()) {
            log.trace("copying structure");
            if(existing.substanceClass== Substance.SubstanceClass.chemical && source.substanceClass== Substance.SubstanceClass.chemical){
                GinasChemicalStructure structure= ((ChemicalSubstance)source).getStructure();
                EntityUtils.EntityInfo<GinasChemicalStructure> eics= EntityUtils.getEntityInfoFor(GinasChemicalStructure.class);
                try {
                    GinasChemicalStructure copiedStructure = eics.fromJson(mapper.writeValueAsString(structure));
                    Set<UUID> newRefs = new HashSet<>();
                    structure.getReferences().forEach(ref->{
                        String oldRefValue = ref.getValue();
                        String newRefValue="";
                        if(referencesToCopy.containsKey(oldRefValue)){
                            newRefValue= referencesToCopy.get(oldRefValue);
                        } else {
                            newRefValue=UUID.randomUUID().toString();
                            referencesToCopy.put(ref.getValue(), newRefValue);
                        }
                        newRefs.add(UUID.fromString(newRefValue));
                        log.trace("looking for reference with term {} and value {}", ref.term, ref.getValue());
                    });
                    copiedStructure.setReferenceUuids(newRefs);
                    copiedStructure.setId(((ChemicalSubstance) existing).getStructure().id);
                    copiedStructure.version= ((ChemicalSubstance) existing).getStructure().version;
                    ((ChemicalSubstanceBuilder)builder).setStructure(copiedStructure);

                } catch (IOException e) {
                    processLog.accept("Error copying structure " );
                    log.error("Error copying structure");
                }
            }/* else if(existing.substanceClass== Substance.SubstanceClass.protein && source.substanceClass== Substance.SubstanceClass.protein){
                Protein protein = ((ProteinSubstance)source).protein;
                EntityUtils.EntityInfo<Protein> eics= EntityUtils.getEntityInfoFor(Protein.class);
                try {
                    Protein copiedProtein = eics.fromJson(mapper.writeValueAsString(protein));
                    Set<UUID> newRefs = new HashSet<>();
                    protein.getReferences().forEach(ref->{
                        String oldRefValue = ref.getValue();
                        String newRefValue="";
                        if(referencesToCopy.containsKey(oldRefValue)){
                            newRefValue= referencesToCopy.get(oldRefValue);
                        } else {
                            newRefValue=UUID.randomUUID().toString();
                            referencesToCopy.put(ref.getValue(), newRefValue);
                        }
                        newRefs.add(UUID.fromString(newRefValue));
                        log.trace("looking for reference with term {} and value {}", ref.term, ref.getValue());
                    });
                    copiedProtein.setReferenceUuids(newRefs);
                    copiedProtein.setUuid(UUID.randomUUID());
                    ((ProteinSubstanceBuilder)builder).setProtein(copiedProtein);

                } catch (IOException e) {
                    processLog.accept("Error copying protein " );
                    log.error("Error copying protein");
                }
            }*/
        }
        if(mergeParameters.getMergeReferences()){
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
        
        if(mergeParameters.getMergeNames()){
            Set<Name> namesToCopy = new HashSet<>();
            source.names.forEach(n-> {
                if( existing.names.stream().anyMatch(en->en.name.equals(n.name))){
                    processLog.accept(String.format("Name %s was already present;", n.name));
                } else if(mergeParameters.getMergeNamesSpecificNames()!=null && !mergeParameters.getMergeNamesSpecificNames().isEmpty()
                        && !mergeParameters.getMergeNamesSpecificNames().contains(n.name)) {
                    log.trace("omitting name '{}' because it was not on the list of specific names", n.name);
                }else {
                    if(mergeParameters.getMergeNamesSkipNameMatches()){
                        List<SubstanceRepository.SubstanceSummary> sr = substanceRepository.findByNames_NameIgnoreCase(n.name.toUpperCase());
                        if( !sr.isEmpty()){
                            log.info("skipping name {} because it is used elsewhere AND a merge setting prohibits this", n.name);
                            processLog.accept(String.format("skipping name %s because it is used elsewhere AND a merge setting prohibits this", n.name));
                            return;
                        }
                    }
                    namesToCopy.add(n);
                }
            });
            copyNames(builder, namesToCopy, referencesToCopy, processLog);
        }

        if(mergeParameters.getMergeCodes()){
            source.codes.forEach(c-> {
                if( existing.codes.stream().anyMatch(en->en.code.equals(c.code) && en.codeSystem.equals(c.codeSystem))) {
                    processLog.accept(String.format("code %s was already present;", c.code));
                }else if( mergeParameters.getMergeCodesSpecificSystems()!=null && !mergeParameters.getMergeCodesSpecificSystems().isEmpty()
                        && !mergeParameters.getMergeCodesSpecificSystems().contains(c.codeSystem)) {
                    log.info("omitting code '{}' because its code system {} was not on the list of specific code systems", c.code, c.codeSystem);
                }else{
                    EntityUtils.EntityInfo<Code> eics= EntityUtils.getEntityInfoFor(Code.class);
                    try {
                        Code newCode=eics.fromJson(c.toJson());
                        newCode.setUuid(UUID.randomUUID());
                        builder.addCode( newCode);
                        processLog.accept(String.format("Adding code %s;", c.code));
                        Set<UUID> newRefs = new HashSet<>();
                        newCode.getReferences().forEach(ref->{
                            String oldRefValue = ref.getValue();
                            String newRefValue;
                            if(referencesToCopy.containsKey(oldRefValue)){
                                newRefValue= referencesToCopy.get(oldRefValue);
                            } else {
                                newRefValue=UUID.randomUUID().toString();
                                referencesToCopy.put(ref.getValue(), newRefValue);
                            }
                            newRefs.add(UUID.fromString(newRefValue));
                            log.trace("looking for reference with term {} and value {}", ref.term, ref.getValue());
                        });
                        newCode.setReferenceUuids(newRefs);

                    } catch (IOException e) {
                        log.error("Error copying code", e);
                        processLog.accept(String.format("Error adding code %s of system %s;", c.code, c.codeSystem));
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        if(mergeParameters.getMergeProperties()){
            source.properties.forEach(p-> {
                if( mergeParameters.getMergePropertiesPropertyUniqueness() &&
                        (existing.properties.stream().anyMatch(en->en.getPropertyType().equals(p.getPropertyType())
                        && en.getName().equals(p.getName()))
                        || builder.build().properties.stream().anyMatch(p2->p2.getName().equals(p.getName()) && p2.getPropertyType().equals(p.getPropertyType())))) {
                    processLog.accept(String.format("property %s was already present;", p.getName()));
                } else if(mergeParameters.getMergePropertiesSpecificPropertyNames() != null && !mergeParameters.getMergePropertiesSpecificPropertyNames().isEmpty()
                    && !mergeParameters.getMergePropertiesSpecificPropertyNames().contains(p.getName())){
                    log.trace("property with name {} was skipped because it is not on the list of properites to copy", p.getName());
                }else{
                    EntityUtils.EntityInfo<Property> eics= EntityUtils.getEntityInfoFor(Property.class);
                    try {
                        Property newProperty=eics.fromJson(p.toJson());
                        newProperty.setUuid(UUID.randomUUID());
                        builder.addProperty( newProperty);
                        processLog.accept(String.format("Adding property %s;", p.getName()));
                        Set<UUID> newRefs = new HashSet<>();
                        newProperty.getReferences().forEach(ref->{
                            String oldRefValue = ref.getValue();
                            String newRefValue;
                            if(referencesToCopy.containsKey(oldRefValue)){
                                newRefValue= referencesToCopy.get(oldRefValue);
                            } else {
                                newRefValue=UUID.randomUUID().toString();
                                referencesToCopy.put(ref.getValue(), newRefValue);
                            }
                            newRefs.add(UUID.fromString(newRefValue));
                            log.trace("looking for reference with term {} and value {}", ref.term, ref.getValue());
                        });
                        newProperty.setReferenceUuids(newRefs);

                    } catch (IOException e) {
                        log.error("Error copying property", e);
                        processLog.accept(String.format("Error adding property %s;", p.getName()));
                        throw new RuntimeException(e);
                    }

                }
            });
        }

        if(mergeParameters.getMergeNotes()){
            source.notes.forEach(n-> {
                if( mergeParameters.getMergeNotesNoteUniqueness() &&
                        (existing.notes.stream().anyMatch(en->en.note.equals(n.note)))){
                    processLog.accept(String.format("note %s was already present;", n.note));
                }else{
                    EntityUtils.EntityInfo<Note> eics= EntityUtils.getEntityInfoFor(Note.class);
                    try {
                        Note newNote=eics.fromJson(n.toJson());
                        newNote.setUuid(UUID.randomUUID());
                        builder.addNote(newNote);
                        processLog.accept("Adding note;");
                        Set<UUID> newRefs = new HashSet<>();
                        newNote.getReferences().forEach(ref->{
                            String oldRefValue = ref.getValue();
                            String newRefValue;
                            if(referencesToCopy.containsKey(oldRefValue)){
                                newRefValue= referencesToCopy.get(oldRefValue);
                            } else {
                                newRefValue=UUID.randomUUID().toString();
                                referencesToCopy.put(ref.getValue(), newRefValue);
                            }
                            newRefs.add(UUID.fromString(newRefValue));
                            log.trace("looking for reference with term {} and value {}", ref.term, ref.getValue());
                        });
                        newNote.setReferenceUuids(newRefs);
                    } catch (IOException e) {
                        log.error("Error copying note", e);
                        processLog.accept(String.format("Error adding note %s;", n.note));
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        if(mergeParameters.getMergeModifications()) {
            if(mergeParameters.getMergeModificationsMergeStructuralModifications()) {
                source.modifications.structuralModifications.forEach(sm->{
                    //simple-minded copy here to start
                    //TODO: evaluate how accurate/useful this is
                    EntityUtils.EntityInfo<StructuralModification> structuralModificationEntityInfo =EntityUtils.getEntityInfoFor(StructuralModification.class);
                    try {
                        StructuralModification newStructuralModification = structuralModificationEntityInfo.fromJson(sm.toJson());
                        newStructuralModification.setUuid(UUID.randomUUID());
                        Set<UUID>newRefs = new HashSet<>();
                        newStructuralModification.getReferences().forEach(ref->{
                            String oldRefValue = ref.getValue();
                            String newRefValue;
                            if(referencesToCopy.containsKey(oldRefValue)){
                                newRefValue= referencesToCopy.get(oldRefValue);
                            } else {
                                newRefValue=UUID.randomUUID().toString();
                                referencesToCopy.put(ref.getValue(), newRefValue);
                            }
                            newRefs.add(UUID.fromString(newRefValue));
                        });
                        newStructuralModification.setReferenceUuids(newRefs);
                        builder.addStructuralModification(newStructuralModification);
                    } catch (IOException e){
                        log.error("Error copying StructuralModification", e);
                        processLog.accept(String.format("Error adding StructuralModification %s;", sm));
                        throw new RuntimeException(e);
                    }
                });
            }

            if(mergeParameters.getMergeModificationsMergeAgentModifications()){
                source.modifications.agentModifications.forEach(am->{
                    //simple-minded copy here to start
                    //TODO: evaluate how accurate/useful this is
                    EntityUtils.EntityInfo<AgentModification> agentModificationEntityInfo =EntityUtils.getEntityInfoFor(AgentModification.class);
                    try {
                        AgentModification newAgentModification = agentModificationEntityInfo.fromJson(am.toJson());
                        newAgentModification.setUuid(UUID.randomUUID());
                        //Set<UUID>newRefs = new HashSet<>();
                        newAgentModification.getReferences().forEach(ref->{
                            String oldRefValue = ref.getValue();
                            String newRefValue;
                            if(!referencesToCopy.containsKey(oldRefValue)){
                                newRefValue=UUID.randomUUID().toString();
                                referencesToCopy.put(ref.getValue(), newRefValue);
                            }
                            //newRefs.add(UUID.fromString(newRefValue));
                        });
                        builder.addAgentModification(newAgentModification);

                    } catch (IOException e){
                        log.error("Error copying AgentModification", e);
                        processLog.accept(String.format("Error adding AgentModification %s;", am));
                        throw new RuntimeException(e);
                    }
                });
            }

            if(mergeParameters.getMergeModificationsMergePhysicalModifications()){
                source.modifications.physicalModifications.forEach(pm->{
                    //simple-minded copy here to start
                    //TODO: evaluate how accurate/useful this is
                    EntityUtils.EntityInfo<PhysicalModification> physicalModificationEntityInfo =EntityUtils.getEntityInfoFor(PhysicalModification.class);
                    try {
                        PhysicalModification newPhysicalModification = physicalModificationEntityInfo.fromJson(pm.toJson());
                        newPhysicalModification.setUuid(UUID.randomUUID());
                        //Set<UUID>newRefs = new HashSet<>();
                        newPhysicalModification.getReferences().forEach(ref->{
                            log.trace("looking for reference with term {} and value {}", ref.term, ref.getValue());
                            String oldRefValue = ref.getValue();
                            String newRefValue="";
                            if(referencesToCopy.containsKey(oldRefValue)){
                                newRefValue= referencesToCopy.get(oldRefValue);
                            } else {
                                newRefValue=UUID.randomUUID().toString();
                                referencesToCopy.put(ref.getValue(), newRefValue);
                            }
                            //newRefs.add(UUID.fromString(newRefValue));
                        });
                        builder.addPhysicalModification(newPhysicalModification);
                    } catch (IOException e){
                        log.error("Error copying PhysicalModification", e);
                        processLog.accept(String.format("Error adding PhysicalModification %s;", pm));
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        if(mergeParameters.getMergeRelationships()){
            source.relationships.forEach(n-> {
                if( mergeParameters.getMergeRelationshipsRelationshipUniqueness()
                        && existing.relationships.stream().anyMatch(en->en.type.equals(n.type) && en.relatedSubstance.refuuid.equals(n.relatedSubstance.refuuid))){
                    processLog.accept(String.format("Relationship of type %s to substance %s was already present;", n.type, n.relatedSubstance.refuuid));
                }else{
                    EntityUtils.EntityInfo<Relationship> eics= EntityUtils.getEntityInfoFor(Relationship.class);
                    try {
                        Relationship newRel = eics.fromJson(n.toJson());
                        newRel.setUuid(UUID.randomUUID());

                        processLog.accept(String.format("Adding Relationship of type %s to substances %s;", newRel.type,
                                newRel.relatedSubstance.refuuid));
                        Set<UUID>newRefs = new HashSet<>();
                        newRel.getReferences().forEach(ref->{
                            log.trace("looking for reference with term {} and value {}", ref.term, ref.getValue());
                            String oldRefValue = ref.getValue();
                            String newRefValue;
                            if(referencesToCopy.containsKey(oldRefValue)){
                                newRefValue= referencesToCopy.get(oldRefValue);
                            } else {
                                newRefValue=UUID.randomUUID().toString();
                                referencesToCopy.put(ref.getValue(), newRefValue);
                            }
                            newRefs.add(UUID.fromString(newRefValue));
                        });
                        newRel.setReferenceUuids(newRefs);
                        builder.addRelationship(newRel);
                    } catch (IOException e) {
                        log.error("error copying name", e);
                        processLog.accept(String.format("Error adding Relationship %s;", n.type));
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        if(existing.substanceClass== Substance.SubstanceClass.chemical && mergeParameters.getCopyStructure() ) {
            ((ChemicalSubstanceBuilder) builder).setStructure(((ChemicalSubstance) source).getStructure());
            ((ChemicalSubstanceBuilder) builder).clearMoieties();
            EntityUtils.EntityInfo<Moiety> eicsMoieties = EntityUtils.getEntityInfoFor(Moiety.class);

            ((ChemicalSubstance) source).moieties.forEach(m-> {
                try {
                    Moiety copiedMoiety = eicsMoieties.fromJson(mapper.writeValueAsString(m));
                    ((ChemicalSubstanceBuilder) builder).addMoiety(copiedMoiety);
                } catch (IOException e) {
                    log.error("Error copying moiety {}", m.uuid, e);
                    processLog.accept(String.format("Error copying moiety %s", m.uuid));
                }
            });
        }
        //todo: consider merging definitions
        if( !mergeParameters.getSkipLevelingReferences()) {
            EntityUtils.EntityInfo<Reference> eicsRefs = EntityUtils.getEntityInfoFor(Reference.class);
            for (String refUuid : referencesToCopy.keySet()) {
                if (builder.build().references.stream().noneMatch(r -> r.getUuid().toString().equals(refUuid))) {
                    Reference sourceRef = source.references.stream().filter(r -> r.getUuid().toString().equals(refUuid)).findFirst().get();
                    try {
                        Reference newReference = eicsRefs.fromJson(mapper.writeValueAsString(sourceRef));
                        //newReference.setUuid(UUID.fromString(referencesToCopy.get(refUuid)));
                        //below works whereas above runs but leaves the UUID unchanged!
                        newReference.uuid = UUID.fromString( referencesToCopy.get(refUuid));
                        log.trace("created ref with UUID {} to replace {}", referencesToCopy.get(refUuid), refUuid);
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

    private AbstractSubstanceBuilder toAbstractBuilder(Substance substance) {
        SubstanceBuilder builder = substance.toBuilder();
        switch (substance.substanceClass) {
            case chemical:
                ChemicalSubstanceBuilder chemicalSubstanceBuilder= builder.asChemical();
                chemicalSubstanceBuilder.setStructure( ((ChemicalSubstance)substance).getStructure());
                log.trace("builder structure id {}, version{}",
                    chemicalSubstanceBuilder.build().getStructure().id, chemicalSubstanceBuilder.build().getStructure().version);
                return chemicalSubstanceBuilder;
            case protein:
                ProteinSubstanceBuilder proteinSubstanceBuilder= builder.asProtein();
                proteinSubstanceBuilder.setProtein( ((ProteinSubstance)substance).protein);
                return proteinSubstanceBuilder;
            case nucleicAcid:
                NucleicAcidSubstanceBuilder nucleicAcidSubstanceBuilder= builder.asNucleicAcid();
                nucleicAcidSubstanceBuilder.setNucleicAcid( ((NucleicAcidSubstance)substance).nucleicAcid);
                return nucleicAcidSubstanceBuilder;
            case mixture:
                MixtureSubstanceBuilder mixtureSubstanceBuilder=builder.asMixture();
                mixtureSubstanceBuilder.setMixture(((MixtureSubstance)substance).mixture);
                return mixtureSubstanceBuilder;
            case structurallyDiverse:
                StructurallyDiverseSubstanceBuilder structurallyDiverseSubstanceBuilder= builder.asStructurallyDiverse();
                structurallyDiverseSubstanceBuilder.setStructurallyDiverse(((StructurallyDiverseSubstance)substance).structurallyDiverse);
                return structurallyDiverseSubstanceBuilder;
            case polymer:
                PolymerSubstanceBuilder polymerSubstanceBuilder= builder.asPolymer();
                polymerSubstanceBuilder.setPolymer(((PolymerSubstance)substance).polymer);
                return polymerSubstanceBuilder;
            default:
                return builder;
        }
    }

    @Override
    public String getActionName() {
        return "Merge";
    }

    @Override
    public List<String> getOptions(){
        return Arrays.asList("MergeReferences",
                "MergeNames",
                "MergeCodes",
                "MergeProperties",
                "MergeNotes",
                "NoteUniqueness",
                "MergeRelationships",
                "RelationshipUniqueness",
                "MergeModifications",
                "MergeStructuralModifications",
                "MergeAgentModifications",
                "MergePhysicalModifications",
                "SkipLevelingReferences");
    }

    @Override
    public JsonNode getAvailableSettingsSchema() {
        return schemaSupplier.get();
    }


    private final static String JSONSchema = getSchemaString();

    private final static CachedSupplier<JsonNode> schemaSupplier = CachedSupplier.of(()->{
        ObjectMapper mapper =new ObjectMapper();
        try {
            JsonNode schemaNode=mapper.readTree(JSONSchema);
            return schemaNode;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;//todo: alternate return?
    });

    @SneakyThrows
    private static String getSchemaString() {
        log.trace("starting getSchemaString");
        ClassPathResource fileResource = new ClassPathResource("schemas/mergeSchema.json");
        byte[] binaryData = FileCopyUtils.copyToByteArray(fileResource.getInputStream());
        String schemaString =new String(binaryData, StandardCharsets.UTF_8);
        //log.trace("read schema:{}", schemaString);
        return schemaString;
    }

    /* under construction...or maybe destruction  not used for now
    private boolean copyReferences(GinasCommonSubData source, GinasCommonSubData target, Map<String, String> referencesToCopy,
                                   Consumer<String> processLog, AbstractSubstanceBuilder builder ){
        EntityUtils.EntityInfo<GinasCommonSubData> eics= EntityUtils.getEntityInfoFor(GinasCommonSubData.class);
        ObjectMapper mapper = new ObjectMapper();
        try {
            GinasCommonSubData copiedItem = eics.fromJson(mapper.writeValueAsString(source));
            Set<UUID> newRefs = new HashSet<>();
            source.getReferences().forEach(ref->{
                String oldRefValue = ref.getValue();
                String newRefValue="";
                if(referencesToCopy.containsKey(oldRefValue)){
                    newRefValue= referencesToCopy.get(oldRefValue);
                } else {
                    newRefValue=UUID.randomUUID().toString();
                    referencesToCopy.put(ref.getValue(), newRefValue);
                }
                newRefs.add(UUID.fromString(newRefValue));
                log.trace("looking for reference with term {} and value {}", ref.term, ref.getValue());
            });
            copiedItem.setReferenceUuids(newRefs);
            copiedItem.setUuid(UUID.randomUUID());
            //builder.setDefinition(Sub)
            ((ProteinSubstanceBuilder)builder).setProtein(copiedItem);

        } catch (IOException e) {
            processLog.accept("Error copying protein " );
            log.error("Error copying protein");
        }

    }*/

    private void copyNames(AbstractSubstanceBuilder target, Set<Name> namesToCopy, Map<String, String> referencesToCopy,
                           Consumer<String> processLog) {
        EntityUtils.EntityInfo<Name> eics= EntityUtils.getEntityInfoFor(Name.class);
        namesToCopy.forEach(n->{
            try {
                Name newName;
                newName = eics.fromJson(n.toJson());

                newName.setUuid(UUID.randomUUID());
                Set<UUID> newRefs = new HashSet<>();
                newName.getReferences().forEach(ref->{
                    String oldRefValue = ref.getValue();
                    String newRefValue;
                    if(referencesToCopy.containsKey(oldRefValue)){
                        newRefValue= referencesToCopy.get(oldRefValue);
                    } else {
                        newRefValue=UUID.randomUUID().toString();
                        referencesToCopy.put(ref.getValue(), newRefValue);
                    }
                    newRefs.add(UUID.fromString(newRefValue));
                    log.trace("looking for reference with term {} and value {}", ref.term, ref.getValue());
                });
                newName.setReferenceUuids(newRefs);
                newName.displayName=false;//substance probably had a display name already; if not, user can make a change  later
                target.addName( newName);

                processLog.accept(String.format("Adding Name %s;", n.name));
            } catch (IOException e) {
                log.error("error copying name", e);
                processLog.accept(String.format("Error adding Name %s;", n.name));
                throw new RuntimeException(e);
            }


        });
    }
}
