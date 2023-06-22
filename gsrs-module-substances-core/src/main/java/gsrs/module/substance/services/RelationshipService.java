package gsrs.module.substance.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.nih.ncats.common.Tuple;
import gsrs.DefaultDataSourceConfig;
import gsrs.EntityPersistAdapter;
import gsrs.module.substance.processors.RelationshipProcessor;
import gsrs.module.substance.processors.RemoveInverseRelationshipEvent;
import gsrs.module.substance.processors.TryToCreateInverseRelationshipEvent;
import gsrs.module.substance.processors.UpdateInverseRelationshipEvent;
import gsrs.module.substance.repository.RelationshipRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.repository.EditRepository;
import ix.core.models.Edit;
import ix.core.models.Keyword;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.Key;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.utils.RelationshipUtil;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RelationshipService {
    @Autowired
    private RelationshipRepository relationshipRepository;
    @Autowired
    private EditRepository editRepository;
    @Autowired
    private SubstanceRepository substanceRepository;

//    @Autowired
    @PersistenceContext(unitName =  DefaultDataSourceConfig.NAME_ENTITY_MANAGER)
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

        Substance owner = relationshipRepository.findById(event.getRelationshipIdThatWasUpdated()).get().fetchOwner();


        Optional<Relationship> opt = relationshipRepository.findByOriginatorUuid(event.getOriginatorUUID().toString())
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

    //TODO: This needs tests, it is unlikely to work as consistently as desired
    // TP 2022-11-29: Indeed this method was found to be the source of certain rare bugs.
    //                The main issue was that this method previously only looked at the version-1 version
    //                of the record, but sometimes the last saved version of a record isn't the immediately
    //                preceding version. It now looks at the most recent edit, and will return an empty
    //                optional if none is found.
    // However, it's still unclear what would happen if this is called after a record is created during the
    // first edit. Still need some tests for various cases. The reason this hasn't been extremely important is
    // that this code is called only when relationships are incomplete/out-of-sync for some reason.
    //
    //
    /**
     * Finds the previous type of the relationship in question based on the last saved edit. Returns empty
     * optional if none is found.
     * @param event
     * @param owner
     * @return
     */
    private Optional<String> findOldType(UpdateInverseRelationshipEvent event, Substance owner) {
        
        try {
        	Edit edit = editRepository.findFirstByKeyOrderByCreatedDesc(owner.fetchKey()).orElse(null);
        	if(edit==null)return Optional.empty();
        	Relationship oldRelationship = SubstanceBuilder.from(edit.newValue).build()
                    .relationships.stream()
                    .filter(r-> r.uuid.equals(event.getRelationshipIdThatWasUpdated()))
                    .findAny()
                    .get();
            return Optional.of(oldRelationship.type);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void updateInverseRelationshipFor(UpdateInverseRelationshipEvent event){
        Optional<Relationship> opt=Optional.empty();
        try {
            opt = findReverseRelationship(event);
        }catch(Exception e) {
            log.warn("Trouble finding inverted relationship", e);
        }
        if(!opt.isPresent()) {
        	//if no suitable inverted form is found to be updated, chances are that
        	//there was no suitable inverse yet. This can happen if the data was loaded
        	//in a particular order sometimes, or more likely when a non-invertible
        	//relationship is changed to be invertible. In such cases we need to treat
        	//this update event like it's a creation event and make the inverse
            createNewInverseRelationshipFor(event.toCreateEvent());
            return;
        }
        Relationship r1 = opt.get();
        final Substance osub = r1.fetchOwner();
        Relationship updatedInverseRelationship = relationshipRepository.findById(event.getRelationshipIdThatWasUpdated()).get();
        entityPersistAdapter.performChangeOn(osub, osub2 -> {
            Relationship toUpdate =osub2.relationships.stream()
                    .filter(r->r.uuid.equals(r1.uuid))
                    .findFirst()
                    .orElse(null);
            if(toUpdate ==null){
                //can this happen?
                return Optional.empty();
            }
            //If the inverted relationship points to the wrong substance that means the target
            //has been changed. Instead of updating the relationship, then, we need to delete
            //it and add another equivalent relationship on a different record.
            if(!osub2.uuid.toString().equals(updatedInverseRelationship.relatedSubstance.refuuid)) {
                //remove the previous relationship
                RelationshipProcessor.doWithoutEventTracking(()->{
                    relationshipRepository.delete(toUpdate);    
                });
                osub2.removeRelationship(toUpdate);
                osub2.forceUpdate();
                Substance osub3=substanceRepository.saveAndFlush(osub2);
                //trigger new creation event as if this had been a new relationship just
                //created
                createNewInverseRelationshipFor(event.toCreateEvent());
                return Optional.of(osub3);
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
            
            r1.setIsDirty("type");
            r1.setIsDirty("access");
            r1.setIsDirty("mediatorSubstance");
            r1.setIsDirty("interactionType");
            r1.setIsDirty("qualification");
            r1.setIsDirty("comments");
            r1.setIsDirty("amount");
            
            osub2.references.removeAll(refsToRemove);
            osub2.setIsDirty("references");
            osub2.setIsDirty("relationships");
           
            
            Substance otherSubstance = updatedInverseRelationship.fetchOwner();
            for (Keyword k : updatedInverseRelationship.getReferences()) {

                Reference ref = otherSubstance.getReferenceByUUID(k.getValue());
                if("SYSTEM".equals(ref.docType)){
                    continue;
                }

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
            osub2.forceUpdate();
            Substance osub3=RelationshipProcessor.doWithoutEventTracking(()->substanceRepository.saveAndFlush(osub2));
            
            return Optional.of(osub3);
        });
    }

    public void removeInverseRelationshipFor(RemoveInverseRelationshipEvent event){


        Optional<Relationship> opt = findReverseRelationship(event);
        if(!opt.isPresent()) {
            return;
        }
        Relationship r1 = opt.get();
        r1.setOkToRemove();
        final Substance osub = r1.fetchOwner();
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
                    // We never want this to trigger an event
                    Relationship rrem=rem;
                    RelationshipProcessor.doWithoutEventTracking(()->{
                        relationshipRepository.delete(rrem);    
                    });
                    osub2.removeRelationship(rem);
                }
                osub2.forceUpdate();
                substanceRepository.saveAndFlush(osub2);
//									System.out.println("Inverse should be deleted now");
                return Optional.of(osub2);
            });
        }

    }

    public void createNewInverseRelationshipFor(TryToCreateInverseRelationshipEvent event) {
        if (event.getFromSubstance() == null) {
            //TODO: Look into this
           return;
        }
        Key mkey = EntityUtils.Key.of(Substance.class, event.getFromSubstance());
        
        //we are making a new relationship with from -> to.
        //this event means we already have a to -> from relationship.
        //Due to transaction issues we can't actually check yet that we can make this relationship
        //when we make the event:
        // 1. this "from" substance might not exist yet
        // 2. the "from" substance might already have this relationship and we didn't know
            EntityUtils.EntityWrapper<?> change = entityPersistAdapter.change(
                    // TP 10/02/2021 : this form of key instantiation below is more dangerous
                    // because we TYPICALLY make keys from their "actual" classes, not their root
                    // classes. So things may be inconsistent. In the future, we could change how the
                    // EntityWrapper.getKey() method works to return a root key sometimes,
                    // or change the way the change operation works to use the root-level key,
                    // but for consistently we should get keys in a similar way every time
                    // TODO: change the event to have the Keys rather than just the IDs
                    
                    mkey
                    ,
                    s -> {
                        Substance newSub = (Substance) s;
                        Optional<Relationship> byId = relationshipRepository.findById(event.getRelationshipIdToInvert());
                        if(!byId.isPresent()){
                            return Optional.empty();
                        }
                        Relationship obj = byId.get();
                        if(!obj.isAutomaticInvertible()){
                            return Optional.empty();
                        }                    
                        Relationship r = obj.fetchInverseRelationship();
                        r.originatorUuid = event.getRelationshipIdToInvert().toString();
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
                            // TODO: Are we sure about this? This feels like a hack to make something
                            // behave as it used to in Play, but I think it's brittle. [TP]
                            newSub.updateVersion();
//                            relationshipRepository.save(r);
                            Substance upSub=newSub;
                            newSub = RelationshipProcessor.doWithoutEventTracking(()->substanceRepository.saveAndFlush(upSub));
                            
                        }
                        return Optional.ofNullable(newSub);

                    });

        }



}
