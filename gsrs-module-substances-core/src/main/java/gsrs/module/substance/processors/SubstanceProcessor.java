package gsrs.module.substance.processors;

import gsrs.module.substance.repository.RelationshipRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.EntityPersistAdapter;
import ix.core.EntityProcessor;
import ix.core.models.Group;
import ix.core.models.Principal;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.Substance.SubstanceDefinitionType;
import ix.ginas.models.v1.SubstanceReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import gov.nih.ncats.common.util.TimeUtil;

import java.util.*;

/**
 * This Substance Processor makes the following changes when a Substance is saved:
 *
 * <ol>
 *     <li>If this Substance is an Alternate Definition: replace the reference to its PRIMARY definition
 *          to make sure it's up to date.</li>
 *
 *
 * </ol>
 *
 * If the Substance is newly created/ just inserted, then
 * look for dangling Relationships where previously loaded substances refer to this new substance
 * and if it finds any, add the corresponding inverse relationship.  This is probably
 * only done during partial BATCH loads.
 *
 */
@Slf4j
public class SubstanceProcessor implements EntityProcessor<Substance> {


    @Autowired
    private RelationshipRepository relationshipRepository;

    @Autowired
    private EntityPersistAdapter entityPersistAdapter;

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    

    @Autowired
    private PlatformTransactionManager transactionManager;
 
    
    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }


    private void addWaitingRelationships(Substance obj){
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
       
        
        transactionTemplate.setReadOnly(true);
        //This can't be a new isolated propagation transaction for some tests to pass. There
        //may be issues here to investigate.
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        List<Relationship> refrel = transactionTemplate.execute(stat->relationshipRepository.findByRelatedSubstance_Refuuid(obj.getOrGenerateUUID().toString()));

        for(Relationship r:refrel){
            Substance owner = r.fetchOwner();
            log.debug("finding inverse owner simple method:" + owner);
            if(owner==null) {
                owner= transactionTemplate.execute(stat->substanceRepository.findByRelationships_Uuid(r.uuid));
                log.debug("finding inverse owner direct lookup method:" + owner);
            }
            if(owner==null) {
                owner= obj.relationships
                   .stream()
                   .filter(rr->{
                       boolean keep=Objects.equals(rr.originatorUuid,r.originatorUuid);
                       
                       return keep;
                       })
                   .findFirst()
                   .map(rr->{
                       return transactionTemplate.execute(stat->substanceRepository.findBySubstanceReference(rr.relatedSubstance));
                   })
                   .orElse(null);

                log.debug("finding inverse owner convoluted method:" + owner);
            }
            if(owner!=null) {
                eventPublisher.publishEvent(
                        TryToCreateInverseRelationshipEvent.builder()
                                .creationMode(TryToCreateInverseRelationshipEvent.CreationMode.CREATE_IF_MISSING_DEEP_CHECK)
                                .originatorUUID(UUID.fromString(r.originatorUuid))
                                .toSubstance(owner.uuid)
                                .fromSubstance(obj.uuid)
                                .relationshipIdToInvert(r.uuid)
                                .build()
                        );
            }else {
                log.error("Could not find the owner of relationship:" + r.uuid + " it may be that the relationship was saved without an owner present.");
            }

        }
    }


    @Override
    @Transactional
    public void prePersist(final Substance s) {
        savingSubstance(s, true);
    }

    private void savingSubstance(final Substance s, boolean newInsert) {
        TransactionTemplate transactionTemplate2 = new TransactionTemplate(transactionManager);
        transactionTemplate2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);


        log.debug("Persisting substance:" + s);
        if (s.isAlternativeDefinition()) {

            log.debug("It's alternative");
            boolean skipSaving = false;
            
            //If it's alternative, find the primary substance (there should only be 1, but this returns a list anyway)
            //
            // Tyler Oct 1 2021:
            //TODO: edits /operations tend to get triggered and flushed from a call to the substanceRepository!
            // This is a big liability. Essentially every time a substanceRepository query method like this is called
            // it will flush out the waiting operations, including inserts and updates ... even including the very
            // statements that this "prePersist" or "preUpdate" hook is meant to pre-empt. I don't know how to deal with
            // this. We either need repository calls to NEVER flush, or we need an alternative way to get this information
            // and warn devs never to do lookups on repositories in "pre" hooks.
            
            // Due to auto flushing in hibernate, this is very tricky to avoid in newer versions of hibernate
            // https://stackoverflow.com/questions/14403498/how-to-prevent-hibernate-from-flushing-in-list/14454358
            
            // One option is to just move pre-update/pre-persist hooks like these to validation rules,
            // which effectively work as pre-pre hooks?
            
            // Just to make it work, for now, don't bother doing this lookup at all unless something changes
            // with the primary definition. This is a bad check for a lot of reasons but may work for right now
            
            // Tyler Oct 4 2021: It turns out setting the propagation settings helps isolate the session/
            // transactions okay. We may need to basic to things like this in the future:
            // transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            if( s.getPrimaryDefinitionRelationships().isPresent()) {
                Relationship r1 = s.getPrimaryDefinitionRelationships().get();
                boolean worthChecking = false;
                if (newInsert || r1.isDirty() || r1.relatedSubstance.isDirty() || r1.lastEdited == null ||
                        (r1.lastEdited != null && r1.lastEdited.getTime() > TimeUtil.getCurrentTimeMillis() - 60000)) {
                    worthChecking = true;
                }

                if (worthChecking) {


//                List<Substance> realPrimarysubs= substanceRepository.findSubstancesWithAlternativeDefinition(s);
                    //Note: trying to isolate in a transaction with propagation settings
                    // DOES prevent transaction problems.
                    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                    transactionTemplate.setReadOnly(true);
                    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    List<Substance> realPrimarysubs = transactionTemplate.execute(status -> {
                        List<Substance> subs = substanceRepository.findSubstancesWithAlternativeDefinition(s);
                        return subs;
                    });


                    log.debug("Got some relationships:" + realPrimarysubs.size());
                    Set<String> oldprimary = new HashSet<String>();
                    for (Substance pri : realPrimarysubs) {
                        oldprimary.add(pri.getUuid().toString());
                    }


                    SubstanceReference sr = s.getPrimaryDefinitionReference();
                    if (sr != null) {

                        log.debug("Enforcing bidirectional relationship");
                        //remove old references
                        for (final Substance oldPri : realPrimarysubs) {
                            if (oldPri == null) {
                                continue;
                            }
                            //no need to remove the same relationship
                            if (oldPri.getUuid().toString().equals(sr.refuuid)) {
                                skipSaving = true;
                                continue;
                            }
                            log.debug("Removing stale bidirectional relationships");


                            transactionTemplate2.executeWithoutResult(stat -> {
                                entityPersistAdapter.performChangeOn(oldPri, obj -> {
                                    List<Relationship> related = obj.removeAlternativeSubstanceDefinitionRelationship(s);
                                    for (Relationship r : related) {
                                        relationshipRepository.delete(r);
                                    }
                                    obj.forceUpdate();
                                    substanceRepository.saveAndFlush(obj);

                                    return Optional.of(obj);
                                });
                            });


                        }
                        if (!skipSaving) {
                            log.debug("Expanding reference");
                            Substance subPrimary = null;
                            try {
                                subPrimary = transactionTemplate.execute(status -> {
                                    return substanceRepository.findBySubstanceReference(sr);
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if (subPrimary != null) {
                                log.debug("Got parent sub, which is:" + EntityWrapper.of(subPrimary).getKey());
                                if (SubstanceDefinitionType.PRIMARY.equals(subPrimary.definitionType)) {

                                    log.debug("Going to save");
                                    Substance pri = subPrimary;
                                    transactionTemplate2.executeWithoutResult(stat -> {
                                        entityPersistAdapter.performChangeOn(pri, obj -> {
                                            if (!obj.addAlternativeSubstanceDefinitionRelationship(s)) {
                                                log.info("Saving alt definition, now has:"
                                                        + obj.getAlternativeDefinitionReferences().size());
                                            }
                                            obj.forceUpdate();
                                            substanceRepository.saveAndFlush(obj);
                                            return Optional.of(obj);
                                        });
                                    });


                                }
                            }
                        }

                    } else {
                        log.error("Persist error. Alternative definition has no primary relationship");
                    }
                }
            } else {
                log.warn("primary definitional relationship is missing");
            }
        }

        if(newInsert) {
            //depending on how this substance was created
            //it might have been from copying and pasting old json
            //of an already existing substance
            //which might have the change reason set so force it to be null for new inserts
            //TP: commenting out for now.
//            s.changeReason=null;

            addWaitingRelationships(s);
        }
    }



    @Override
    public void preUpdate(Substance obj) {
        savingSubstance(obj, false);
    }







}
