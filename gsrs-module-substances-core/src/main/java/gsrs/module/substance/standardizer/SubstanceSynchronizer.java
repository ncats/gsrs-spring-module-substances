package gsrs.module.substance.standardizer;

import gov.nih.ncats.common.Tuple;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.tasks.SubstanceReferenceState;
import gsrs.service.GsrsEntityService;
import ix.core.EntityFetcher;
import ix.core.util.EntityUtils;
import ix.ginas.models.GinasAccessControlled;
import ix.ginas.models.v1.PolymerSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import ix.ginas.utils.validation.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
public class SubstanceSynchronizer {

    @Autowired
    SubstanceRepository substanceRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private SubstanceLegacySearchService searchService;

    @Autowired
    protected SubstanceEntityService substanceEntityService;

    public void fixSubstanceReferences(Substance startingSubstance, Consumer<String> actionRecorder,
                                       String refUuidCodeSystem, String refApprovalIdCodeSystem) {
        log.trace("In fixSubstanceReferences");
        AtomicBoolean substanceNeedsToSave = new AtomicBoolean(false);
        startingSubstance.relationships.forEach(r -> {
            SubstanceReferenceState state = resolveSubstanceReference(r.relatedSubstance, actionRecorder, refUuidCodeSystem, refApprovalIdCodeSystem);
            if (state == SubstanceReferenceState.JUST_RESOLVED) {
                String message = String.format("Referenced substance %s for relationship of type %s was found on substance %s!",
                        r.relatedSubstance.refuuid, r.type, startingSubstance.uuid.toString());
                actionRecorder.accept(message);
                substanceNeedsToSave.set(true);
            } else if (state == SubstanceReferenceState.UNRESOLVABLE) {
                String message = String.format("Referenced substance %s for relationship of type %s was not found on substance %s!",
                        r.relatedSubstance.refuuid, r.type, startingSubstance.uuid.toString());
                actionRecorder.accept(message);
            }
            if (r.mediatorSubstance != null) {
                state = resolveSubstanceReference(r.mediatorSubstance, actionRecorder, refUuidCodeSystem, refApprovalIdCodeSystem);
                if (state == SubstanceReferenceState.JUST_RESOLVED) {
                    substanceNeedsToSave.set(true);
                } else if (state == SubstanceReferenceState.UNRESOLVABLE) {
                    String message = String.format("Mediator substance %s for relationship of type %s was not found on substance %s!",
                            r.relatedSubstance.refuuid, r.type, startingSubstance.uuid.toString());
                    actionRecorder.accept(message);
                }
            }
        });
        List<Tuple<GinasAccessControlled, SubstanceReference>> refs = getBaseRefs(startingSubstance);
        refs.forEach(r -> {
            SubstanceReferenceState state = resolveSubstanceReference(r.v(), actionRecorder, refUuidCodeSystem, refApprovalIdCodeSystem);
            if (state == SubstanceReferenceState.JUST_RESOLVED) {
                substanceNeedsToSave.set(true);
            }
            if (state == SubstanceReferenceState.UNRESOLVABLE) {
                String message = String.format("Referenced substance %s for %s of %s was not found!", r.v().refuuid, r.k().getClass().getName(),
                        startingSubstance.uuid.toString());
                actionRecorder.accept(message);
            }
        });
        if (substanceNeedsToSave.get()) {
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tx.executeWithoutResult(s -> {
                try {
                    log.trace("going to save substance {}", startingSubstance.getUuid().toString());
                    GsrsEntityService.UpdateResult<Substance> updateResult = substanceEntityService.updateEntityWithoutValidation(startingSubstance.toFullJsonNode());
                    if (updateResult.getStatus() == GsrsEntityService.UpdateResult.STATUS.ERROR) {
                        log.error("Error updating substance: {}", updateResult.getValidationResponse().toString());
                        if (updateResult.getThrowable() != null) {
                            log.error(updateResult.getThrowable().getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("Error saving substance {}", startingSubstance.uuid, e);
                }
            });
        }
    }

    public SubstanceReferenceState resolveSubstanceReference(SubstanceReference substanceReference,
                                                             Consumer<String> actionRecorder,
                                                             String refUuidCodeSystem, String refApprovalIdCodeSystem) {
        SubstanceReferenceState substanceReferenceState = SubstanceReferenceState.UNRESOLVABLE;
        if (substanceReference == null) {
            log.info("resolveSubstanceReference called with null parameter");
            return substanceReferenceState;
        }
        if (substanceRepository.exists(substanceReference) && substanceReference.refuuid!=null && substanceReference.refuuid.length()>0
                && substanceRepository.getOne(UUID.fromString(substanceReference.refuuid)) !=null
                && substanceRepository.getOne(UUID.fromString(substanceReference.refuuid)).getUuid() !=null
                && substanceRepository.getOne(UUID.fromString(substanceReference.refuuid)).getUuid().toString().equals(substanceReference.refuuid)) {
            return SubstanceReferenceState.ALREADY_RESOLVED;
        }
        //Substance idMatch= substanceRepository.getOne(UUID.fromString(substanceReference.refuuid));
        EntityFetcher fetcher = EntityFetcher.of(EntityUtils.Key.of(Substance.class, substanceReference.refuuid));
        Optional<Substance> idMatch =fetcher.getIfPossible();
        if( idMatch.isPresent() && idMatch.get().uuid!=null && idMatch.get().uuid.toString().equals(substanceReference.refuuid)) {
            log.trace("found substance by UUID");
            substanceReference.wrappedSubstance=idMatch.get();
            substanceReferenceState = SubstanceReferenceState.JUST_RESOLVED;
            return substanceReferenceState;
        }
        boolean currentReferenceResolved = false;
        List<Substance> uuidMatches = refUuidCodeSystem != null && refUuidCodeSystem.length() > 0
                && substanceReference.refuuid!=null && substanceReference.refuuid.length() > 0
                ? ValidationUtils.findSubstancesByCode(refUuidCodeSystem, substanceReference.refuuid, transactionManager, searchService)
                : Collections.emptyList();

        if (uuidMatches.size() > 1) {
            String message = String.format("More than one record found with UUID code %s and code system %s. Using first matching record",
                    substanceReference.refuuid, refUuidCodeSystem);
            log.warn(message);
            actionRecorder.accept(message);
            substanceReference.refuuid = uuidMatches.get(0).uuid.toString();
            substanceReference.wrappedSubstance=uuidMatches.get(0);
            currentReferenceResolved = true;
            substanceReferenceState = SubstanceReferenceState.JUST_RESOLVED;
        } else if (uuidMatches.size() == 1) {
            substanceReference.refuuid = uuidMatches.get(0).uuid.toString();
            substanceReference.wrappedSubstance=uuidMatches.get(0);
            currentReferenceResolved = true;
            substanceReferenceState = SubstanceReferenceState.JUST_RESOLVED;
            String message = String.format("Resolved record UUID %s by code (code system %s)",
                    substanceReference.refuuid, refUuidCodeSystem);
            actionRecorder.accept(message);
        }
        if (!currentReferenceResolved && substanceReference.approvalID != null && substanceReference.approvalID.length() > 0) {
            Substance approvalIdMatch = substanceRepository.findByApprovalID(substanceReference.approvalID);
            if (approvalIdMatch != null) {
                substanceReference.refuuid = approvalIdMatch.uuid.toString();
                substanceReference.wrappedSubstance=approvalIdMatch;
                currentReferenceResolved = true;
                substanceReferenceState = SubstanceReferenceState.JUST_RESOLVED;
                String message = String.format("Resolved record UUID %s by Approval ID %s",
                        substanceReference.refuuid, substanceReference.approvalID);
                actionRecorder.accept(message);
            } else {
                List<Substance> approvalIdMatches = ValidationUtils.findSubstancesByCode(refApprovalIdCodeSystem,
                        substanceReference.approvalID, transactionManager, searchService);
                if (approvalIdMatches != null && !approvalIdMatches.isEmpty()) {
                    if (approvalIdMatches.size() > 1) {
                        String message = String.format("More than one record found with Approval ID code %s and code system %s. Using first matching record",
                                substanceReference.refuuid, refApprovalIdCodeSystem);
                        log.warn(message);
                        actionRecorder.accept(message);
                    }
                    substanceReference.refuuid = approvalIdMatches.get(0).uuid.toString();
                    substanceReference.wrappedSubstance=approvalIdMatches.get(0);
                    currentReferenceResolved = true;
                    substanceReferenceState = SubstanceReferenceState.JUST_RESOLVED;
                    String message = String.format("Resolved record UUID %s/Approval ID %s by code (code system %s)",
                            substanceReference.refuuid, substanceReference.approvalID, refApprovalIdCodeSystem);
                    actionRecorder.accept(message);
                }
            }
        }
        if (!currentReferenceResolved && substanceReference.refPname != null && substanceReference.refPname.length() > 0) {
            //use the name
            List<Substance> nameMatches = ValidationUtils.findSubstancesByName(substanceReference.refPname, transactionManager,
                    searchService);
            if (nameMatches != null && !nameMatches.isEmpty()) {
                if (nameMatches.size() > 1) {
                    String message = String.format("More than one record found with Name %s. Using first matching record",
                            substanceReference.refPname);
                    log.warn(message);
                    actionRecorder.accept(message);
                }
                substanceReference.refuuid = nameMatches.get(0).uuid.toString();
                substanceReference.wrappedSubstance= nameMatches.get(0);
                substanceReferenceState = SubstanceReferenceState.JUST_RESOLVED;
                String message = String.format("Resolved record UUID %s/name %s by name",
                        substanceReference.refuuid, substanceReference.refPname);
                actionRecorder.accept(message);
            }
        }
        return substanceReferenceState;
    }

    private List<Tuple<GinasAccessControlled, SubstanceReference>> getBaseRefs(Substance substance) {
        //at the moment, polymers are the old substance where calling the correct method makes a difference
        if (substance instanceof PolymerSubstance) {
            return substance.getSubstanceReferencesAndParentsBeyondDependsOn();
        }
        return substance.getSubstanceReferencesAndParentsBeyondDependsOn();
    }
}