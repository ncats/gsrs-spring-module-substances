package gsrs.module.substance.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.nih.ncats.common.Tuple;
import gsrs.EntityPersistAdapter;
import gsrs.module.substance.processors.CreateInverseRelationshipEvent;
import gsrs.module.substance.processors.RemoveInverseRelationshipEvent;
import gsrs.module.substance.processors.UpdateInverseRelationshipEvent;
import gsrs.module.substance.repository.RelationshipRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.repository.EditRepository;
import ix.core.models.Edit;
import ix.core.models.Keyword;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.utils.RelationshipUtil;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RelationshipService {
    @Autowired
    private RelationshipRepository relationshipRepository;
    @Autowired
    private EditRepository editRepository;
    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityPersistAdapter entityPersistAdapter;

    private Optional<Relationship> findReverseRelationship(RemoveInverseRelationshipEvent event){



        Optional<Relationship> opt = relationshipRepository.findByOriginatorUuid(event.getRelationshipOriginatorIdToRemove().toString())
                .stream()
                .filter(r-> !event.getRelationshipIdThatWasRemoved().equals(r.uuid))
                .findAny();

        if(opt.isPresent()){
            return opt;
        }
        //GSRS-860 sometimes when grabbing substance json from public data
        //and loading it on local system and then making edits without pulling latest version from GSRS
        //we edit/ remove references that were system generated or point to other substances
        //that weren't also loaded so check to make sure we have the related substance
        Optional<Substance> relatedSubstance = substanceRepository.findById(UUID.fromString(event.getRelatedSubstanceRefId()));
        if(relatedSubstance.isPresent()){
            List<Relationship> relationships = relatedSubstance.get().relationships;
            if(relationships !=null){
                List<Relationship> candidates = relationships.stream()
                        .filter(r->event.getSubstanceRefIdOfRemovedRelationship().equals(r.relatedSubstance.refuuid))
                        .filter(r -> r.isAutomaticInvertible() && RelationshipUtil.reverseRelationship(r.type).equals(event.getRelationshipTypeThatWasRemoved()))
                        .collect(Collectors.toList());
                if(candidates.size() ==1){
                    return Optional.of(candidates.get(0));
                }
                //It's a bigger deal to accidentally delete a relationship you're not sure about, so don't do it if
                //there's some ambiguity
            }
        }
        return Optional.empty();
    }

    private Optional<Relationship> findReverseRelationship(UpdateInverseRelationshipEvent event){

        Substance owner = relationshipRepository.findById(event.getRelationshipIdThatWasUpdated()).get().getOwner();


        Optional<Relationship> opt = relationshipRepository.findByOriginatorUuid(event.getOriginatorIdToUpdate().toString())
                .stream()
                .filter(r-> !event.getRelationshipIdThatWasUpdated().equals(r.uuid))
                .findAny();

        if(opt.isPresent()){
            return opt;
        }
        Optional<String> oldType = findOldType(event, owner);
        if(!oldType.isPresent()){
            return Optional.empty();
        }
        //GSRS-860 sometimes when grabbing substance json from public data
        //and loading it on local system and then making edits without pulling latest version from GSRS
        //we edit/ remove references that were system generated or point to other substances
        //that weren't also loaded so check to make sure we have the related substance
        Optional<Substance> relatedSubstance = substanceRepository.findById(event.getSubstanceIdToUpdate());
        if(relatedSubstance.isPresent()){
            List<Relationship> relationships = relatedSubstance.get().relationships;
            if(relationships !=null){
                List<Relationship> candidates = relationships.stream()
                        .filter(r->event.getRelationshipIdThatWasUpdated().equals(r.relatedSubstance.refuuid))
                        .filter(r -> r.isAutomaticInvertible() && RelationshipUtil.reverseRelationship(oldType.get()).equals(r.type))
                        .collect(Collectors.toList());
                if(candidates.size() ==1){
                    return Optional.of(candidates.get(0));
                }
                //It's a bigger deal to accidentally delete a relationship you're not sure about, so don't do it if
                //there's some ambiguity
            }
        }
        return Optional.empty();
    }

    private Optional<String> findOldType(UpdateInverseRelationshipEvent event, Substance owner) {
        Edit edit = editRepository.findByRefidAndVersion(owner.uuid.toString(), Integer.toString(Integer.parseInt(owner.version)-1)).get();
        try {
            Relationship oldRelationship = SubstanceBuilder.from(edit.newValue).build()
                    .relationships.stream()
                    .filter(r-> r.uuid.equals(event.getRelationshipIdThatWasUpdated()))
                    .findAny()
                    .get();
            return Optional.of(oldRelationship.type);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public void updateInverseRelationshipFor(UpdateInverseRelationshipEvent event){
        Optional<Relationship> opt = findReverseRelationship(event);
        if(!opt.isPresent()) {
            return;
        }
        Relationship r1 = opt.get();
        final Substance osub = r1.getOwner();
        Relationship updatedInverseRelationship = relationshipRepository.findById(event.getRelationshipIdThatWasUpdated()).get();
        entityPersistAdapter.performChangeOn(osub, osub2 -> {
            Relationship toUpdate =null;
            for (Relationship r : osub2.relationships) {
                if (r.uuid.equals(r1.uuid)) {
                    toUpdate = r;
                    break;
                }
            }
            if(toUpdate ==null){
                //can this happen?
                return Optional.empty();
            }

            //logic mostly borrowed from GSRS 2.x RelationshipProcessor
            //we make a new one each time because it does some clone stuff
            //and we don't want to reuse reference objects..I don't think
            //there shouldn't be more than 1 anyway unless there's an error
            //so it's not much of a performance hit to do it inside the loop

            Relationship inverse = updatedInverseRelationship.fetchInverseRelationship();

            List<Reference> refsToRemove = new ArrayList<>();

            //TODO: fix this to remove the actual references from the substance
            Set<Keyword> keepRefs= r1.getReferences()
                    .stream()
                    .map(r->osub2.getReferenceByUUID(r.term))
                    .map(r-> Tuple.of("SYSTEM".equals(r.docType),r))
                    .filter(t->{
                        if(!t.k()){
                            Reference toRemove=t.v();
                            long dependencies=toRemove.getElementsReferencing()
                                    .stream()
                                    .map(elm-> EntityUtils.EntityWrapper.of(elm))
                                    .filter(ew->!r1.uuid.equals(ew.getId().orElse(null)))
                                    .count();
                            if(dependencies<=0){
                                refsToRemove.add(toRemove);
                            }
                        }
                        return t.k();
                    })
                    .map(t->t.v())
                    .map(ref->ref.asKeyword())
                    .collect(Collectors.toSet());


            r1.setComments(inverse.comments);
            r1.type = new String(inverse.type);
            r1.amount = inverse.amount;

            //GSRS-684 and GSRS-730 copy over qualification and interactionType
            if(inverse.qualification !=null){
                //new String so ebean sees it's a new object
                //just in case...
                r1.qualification = new String(inverse.qualification);
            }
            if(inverse.interactionType !=null){
                //new String so ebean sees it's a new object
                //just in case...
                r1.interactionType = new String(inverse.interactionType);
            }
            if(inverse.mediatorSubstance !=null){
                r1.mediatorSubstance = inverse.mediatorSubstance.copyWithNullUUID();
            }


            r1.setReferences(keepRefs);
            r1.setAccess(inverse.getAccess()); //Should take care of access problem


            osub2.references.removeAll(refsToRemove);

            Substance otherSubstance = updatedInverseRelationship.getOwner();
            for (Keyword k : updatedInverseRelationship.getReferences()) {

                Reference ref = otherSubstance.getReferenceByUUID(k.getValue());
                if("SYSTEM".equals(ref.docType)){
                    continue;
                }

//												System.out.println("adding ref" +  ref);
                if(ref!=null){
                    try {
                        Reference newRef = EntityUtils.EntityWrapper.of(ref).getClone();
                        newRef.uuid =null;
                        r1.addReference(newRef, osub2);
                        this.entityManager.merge(newRef);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }
            }
//            if(forceToBeGenerator.get()){
//                r1.originatorUuid=obj.getOrGenerateUUID().toString();
//            }
            r1.forceUpdate();
            osub2.forceUpdate();
            return Optional.of(osub2);
        });
    }

    public void removeInverseRelationshipFor(RemoveInverseRelationshipEvent event){


        Optional<Relationship> opt = findReverseRelationship(event);
        if(!opt.isPresent()) {
            return;
        }
        Relationship r1 = opt.get();
        r1.setOkToRemove();
        final Substance osub = r1.getOwner();
        if (osub != null) {
            entityPersistAdapter.performChangeOn(osub, osub2 -> {
//									System.out.println("Okay, going to delete the inverse");

                Relationship rem = null;
                for (Relationship r : osub2.relationships) {
                    if (r.uuid.equals(r1.uuid)) {
                        rem = r;
                    }
                }
                if (rem != null) {

                    relationshipRepository.delete(rem);
                    osub2.removeRelationship(rem);
                }
                osub2.forceUpdate();
                substanceRepository.save(osub2);
//									System.out.println("Inverse should be deleted now");
                return Optional.of(osub2);
            });
        }

    }

    public void createNewInverseRelationshipFor(CreateInverseRelationshipEvent event) {
        if (event.getFromSubstance() == null) {
            //TODO: Look into this
           return;
        }
        //we are making a new relationship with from -> to.
        //this event means we already have a to -> from relationship.
        //Due to transaction issues we can't actually check yet that we can make this relationship
        //when we make the event:
        //1.  this "from" substance might not exist yet
        // 2. the "from" substance might already have this relationship and we didn't know
            EntityUtils.EntityWrapper<?> change = entityPersistAdapter.change(
                    EntityUtils.Key.of(Substance.class, event.getFromSubstance()),
                    s -> {
                        Substance newSub = (Substance) s;
//						System.out.println("Adding directly now");
                        Relationship obj = relationshipRepository.findById(event.getRelationshipIdToInvert()).get();

                        Relationship r = obj.fetchInverseRelationship();
                        r.originatorUuid = event.getOriginatorSubstance().toString();
                        Optional<Substance> otherSubstanceOpt = substanceRepository.findById(event.getToSubstance());
                        if(!otherSubstanceOpt.isPresent()){
                            return Optional.empty();
                        }

                        Substance otherSubstance = otherSubstanceOpt.get();
                        r.relatedSubstance = otherSubstance.asSubstanceReference();

                        if (!event.getCreationMode().shouldAdd(r, newSub, otherSubstance)) {
                            return Optional.empty();
                        }
                        Reference ref1 = Reference.SYSTEM_GENERATED();
                        ref1.citation = "Generated from relationship on:'" + r.relatedSubstance.refPname + "'";


                        r.addReference(ref1, newSub);
                        newSub.addRelationship(r);
                        //GSRS-736 copy over references
                        //with new UUIDs

                        for (Keyword kw : obj.getReferences()) {
                            Reference origRef = obj.fetchOwner().getReferenceByUUID(kw.getValue());
                            try {
                                Reference newRef = EntityUtils.EntityWrapper.of(origRef).getClone();
                                newRef.uuid = null; //blank out UUID so it generates a new one on save
                                r.addReference(newRef, newSub);
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }
                        }

                        if (newSub != null) {
                            newSub.updateVersion();
                            relationshipRepository.save(r);
                            newSub = substanceRepository.save(newSub);
                        }

                        return Optional.ofNullable(newSub);

                    });

        }



}
