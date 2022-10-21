package gsrs.module.substance.scrubbers.basic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Scrubber Parameters
 * <p>
 * Factors that control the behavior of a Java class that removes private parts of a data object before the object is shared
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "removeDates",
        "deidentifyAuditUser",
        "removeAllLocked",
        "removeAllLockedAccessGroupsToInclude",
        "removeAllLockedRemoveElementsIfNoExportablePublicRef",
        "statusesToInclude",
        "removeElementsIfNoExportablePublicRefElementsToRemove",
        "removeCodesBySystem",
        "removeCodesBySystemCodeSystemsToRemove",
        "removeCodesBySystemCodeSystemsToKeep",
        "removeReferencesByCriteria",
        "removeReferencesByCriteriaReferenceTypesToRemove",
        "removeReferencesByCriteriaCitationPatternsToRemove",
        "removeReferencesByCriteriaExcludeReferenceByPattern",
        "substanceReferenceCleanup",
        "substanceReferenceCleanupRemoveReferencesToFilteredSubstances",
        "substanceReferenceCleanupRemoveReferencesToSubstancesNonExportedDefinitions",
        "removeNotes",
        "removeChangeReason",
        "approvalIdCleanup",
        "approvalIdCleanupRemoveApprovalId",
        "approvalIdCleanupCopyApprovalIdToCode",
        "approvalIdCleanupApprovalIdCodeSystem",
        "regenerateUUIDs",
        "changeAllStatuses",
        "changeAllStatusesNewStatusValue",
        "auditInformationCleanup",
        "auditInformationCleanupNewAuditorValue",
        "scrubbedDefinitionHandling",
        "removeBasedOnStatus",
        "scrubbedDefinitionHandlingRemoveScrubbedDefinitionalElementsEntirely",
        "scrubbedDefinitionHandlingSetScrubbedDefinitionalElementsIncomplete",
        "scrubbedDefinitionHandlingConvertScrubbedDefinitionsToConcepts",
        "scrubbedDefinitionHandlingAddNoteToScrubbedDefinitions",
        "scrubbedDefinitionHandlingTreatPartialDefinitionsAsMissingDefinitions",
        "scrubbedDefinitionHandlingRemoveScrubbedPartialDefinitionalElementsEntirely",
        "scrubbedDefinitionHandlingSetScrubbedPartialDefinitionalElementsIncomplete",
        "scrubbedDefinitionHandlingConvertScrubbedPartialDefinitionsToConcepts",
        "scrubbedDefinitionHandlingAddNoteToScrubbedPartialDefinitions"
})
@Generated("jsonschema2pojo")
public class BasicSubstanceScrubberParameters implements Serializable {

    /**
     * Remove Date
     * <p>
     */
    @JsonProperty("removeDates")
    private boolean removeDates;
    /**
     * Deidentify Audit User
     * <p>
     */
    @JsonProperty("deidentifyAuditUser")
    private boolean deidentifyAuditUser;
    /**
     * Remove all Locked
     * <p>
     */
    @JsonProperty("removeAllLocked")
    private boolean removeAllLocked;
    /**
     * Access Groups to Include
     * <p>
     */
    @JsonProperty("removeAllLockedAccessGroupsToInclude")
    private List<String> removeAllLockedAccessGroupsToInclude = null;
    /**
     * Remove Elements if no exportable selected public domain reference
     * <p>
     */
    @JsonProperty("removeAllLockedRemoveElementsIfNoExportablePublicRef")
    private boolean removeAllLockedRemoveElementsIfNoExportablePublicRef;
    /**
     * Statuses to include
     * <p>
     */
    @JsonProperty("statusesToInclude")
    private List<StatusesToInclude> statusesToInclude = null;
    /**
     * Elements to remove
     * <p>
     */
    @JsonProperty("removeElementsIfNoExportablePublicRefElementsToRemove")
    private List<String> removeElementsIfNoExportablePublicRefElementsToRemove = null;
    /**
     * Remove Codes by System
     * <p>
     */
    @JsonProperty("removeCodesBySystem")
    private boolean removeCodesBySystem;
    /**
     * Code Systems to Remove
     * <p>
     */
    @JsonProperty("removeCodesBySystemCodeSystemsToRemove")
    private List<String> removeCodesBySystemCodeSystemsToRemove = null;
    /**
     * Code Systems to Keep
     * <p>
     */
    @JsonProperty("removeCodesBySystemCodeSystemsToKeep")
    private List<String> removeCodesBySystemCodeSystemsToKeep = null;
    /**
     * Remove References by Criteria
     * <p>
     */
    @JsonProperty("removeReferencesByCriteria")
    private boolean removeReferencesByCriteria;
    /**
     * Reference Types to Remove
     * <p>
     */
    @JsonProperty("removeReferencesByCriteriaReferenceTypesToRemove")
    private List<String> removeReferencesByCriteriaReferenceTypesToRemove = null;
    /**
     * Citation Patterns to Remove
     * <p>
     */
    @JsonProperty("removeReferencesByCriteriaCitationPatternsToRemove")
    private String removeReferencesByCriteriaCitationPatternsToRemove;
    /**
     * Exclude Reference by Pattern
     * <p>
     */
    @JsonProperty("removeReferencesByCriteriaExcludeReferenceByPattern")
    private boolean removeReferencesByCriteriaExcludeReferenceByPattern;
    /**
     * Substance Reference Cleanup
     * <p>
     */
    @JsonProperty("substanceReferenceCleanup")
    private boolean substanceReferenceCleanup;
    /**
     * Remove References to Filtered Substances
     * <p>
     */
    @JsonProperty("substanceReferenceCleanupRemoveReferencesToFilteredSubstances")
    private boolean substanceReferenceCleanupRemoveReferencesToFilteredSubstances;
    /**
     * Remove References to Substances Non-Exported Definitions
     * <p>
     */
    @JsonProperty("substanceReferenceCleanupRemoveReferencesToSubstancesNonExportedDefinitions")
    private boolean substanceReferenceCleanupRemoveReferencesToSubstancesNonExportedDefinitions;
    /**
     * Remove Notes
     * <p>
     */
    @JsonProperty("removeNotes")
    private boolean removeNotes;
    /**
     * Remove Change Reason
     * <p>
     */
    @JsonProperty("removeChangeReason")
    private boolean removeChangeReason;
    /**
     * Approval Id clean-up
     * <p>
     */
    @JsonProperty("approvalIdCleanup")
    private boolean approvalIdCleanup;
    /**
     * Remove Approval Id
     * <p>
     */
    @JsonProperty("approvalIdCleanupRemoveApprovalId")
    private boolean approvalIdCleanupRemoveApprovalId;
    /**
     * Copy Approval Id to code if code not already present
     * <p>
     */
    @JsonProperty("approvalIdCleanupCopyApprovalIdToCode")
    private boolean approvalIdCleanupCopyApprovalIdToCode;
    /**
     * Remove Approval Id
     * <p>
     */
    @JsonProperty("approvalIdCleanupApprovalIdCodeSystem")
    private String approvalIdCleanupApprovalIdCodeSystem;
    /**
     * Regenerate UUIDs
     * <p>
     */
    @JsonProperty("regenerateUUIDs")
    private boolean regenerateUUIDs;
    /**
     * Change All Statuses
     * <p>
     */
    @JsonProperty("changeAllStatuses")
    private boolean changeAllStatuses;
    /**
     * New Status Value
     * <p>
     */
    @JsonProperty("changeAllStatusesNewStatusValue")
    private String changeAllStatusesNewStatusValue;
    /**
     * Audit Information clean-up
     * <p>
     */
    @JsonProperty("auditInformationCleanup")
    private boolean auditInformationCleanup;
    /**
     * New Auditor Value
     * <p>
     */
    @JsonProperty("auditInformationCleanupNewAuditorValue")
    private String auditInformationCleanupNewAuditorValue;
    /**
     * Scrubbed Definition Handling
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandling")
    private boolean scrubbedDefinitionHandling;
    /**
     * Delete records based on status
     * <p>
     */
    @JsonProperty("removeBasedOnStatus")
    private boolean removeBasedOnStatus;
    /**
     * Remove partially/fully scrubbed definitional records entirely
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingRemoveScrubbedDefinitionalElementsEntirely")
    private boolean scrubbedDefinitionHandlingRemoveScrubbedDefinitionalElementsEntirely;
    /**
     * Set partially/fully scrubbed definitional records to definitional level "Incomplete"
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingSetScrubbedDefinitionalElementsIncomplete")
    private boolean scrubbedDefinitionHandlingSetScrubbedDefinitionalElementsIncomplete;
    /**
     * Convert partially/fully scrubbed definitional records to "Concepts"
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingConvertScrubbedDefinitionsToConcepts")
    private boolean scrubbedDefinitionHandlingConvertScrubbedDefinitionsToConcepts;
    /**
     * add a note to partially/fully scrubbed definitional records
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingAddNoteToScrubbedDefinitions")
    private String scrubbedDefinitionHandlingAddNoteToScrubbedDefinitions;
    /**
     * Treat partially scrubbed definitions with same settings
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingTreatPartialDefinitionsAsMissingDefinitions")
    private boolean scrubbedDefinitionHandlingTreatPartialDefinitionsAsMissingDefinitions;
    /**
     * Remove PARTIALLY scrubbed definitional records entirely
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingRemoveScrubbedPartialDefinitionalElementsEntirely")
    private boolean scrubbedDefinitionHandlingRemoveScrubbedPartialDefinitionalElementsEntirely;
    /**
     * Set PARTIALLY scrubbed definitional records to definitional level "Incomplete"
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingSetScrubbedPartialDefinitionalElementsIncomplete")
    private boolean scrubbedDefinitionHandlingSetScrubbedPartialDefinitionalElementsIncomplete;
    /**
     * Convert PARTIALLY scrubbed definitional records to "Concepts"
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingConvertScrubbedPartialDefinitionsToConcepts")
    private boolean scrubbedDefinitionHandlingConvertScrubbedPartialDefinitionsToConcepts;
    /**
     * add a note to PARTIALLY scrubbed definitional records
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingAddNoteToScrubbedPartialDefinitions")
    private String scrubbedDefinitionHandlingAddNoteToScrubbedPartialDefinitions;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = 8826671833754141447L;

    /**
     * Remove Date
     * <p>
     */
    @JsonProperty("removeDates")
    public boolean isRemoveDates() {
        return removeDates;
    }

    /**
     * Remove Date
     * <p>
     */
    @JsonProperty("removeDates")
    public void setRemoveDates(boolean removeDates) {
        this.removeDates = removeDates;
    }

    /**
     * Deidentify Audit User
     * <p>
     */
    @JsonProperty("deidentifyAuditUser")
    public boolean isDeidentifyAuditUser() {
        return deidentifyAuditUser;
    }

    /**
     * Deidentify Audit User
     * <p>
     */
    @JsonProperty("deidentifyAuditUser")
    public void setDeidentifyAuditUser(boolean deidentifyAuditUser) {
        this.deidentifyAuditUser = deidentifyAuditUser;
    }

    /**
     * Remove all Locked
     * <p>
     */
    @JsonProperty("removeAllLocked")
    public boolean isRemoveAllLocked() {
        return removeAllLocked;
    }

    /**
     * Remove all Locked
     * <p>
     */
    @JsonProperty("removeAllLocked")
    public void setRemoveAllLocked(boolean removeAllLocked) {
        this.removeAllLocked = removeAllLocked;
    }

    /**
     * Access Groups to Include
     * <p>
     */
    @JsonProperty("removeAllLockedAccessGroupsToInclude")
    public List<String> getRemoveAllLockedAccessGroupsToInclude() {
        return removeAllLockedAccessGroupsToInclude;
    }

    /**
     * Access Groups to Include
     * <p>
     */
    @JsonProperty("removeAllLockedAccessGroupsToInclude")
    public void setRemoveAllLockedAccessGroupsToInclude(List<String> removeAllLockedAccessGroupsToInclude) {
        this.removeAllLockedAccessGroupsToInclude = removeAllLockedAccessGroupsToInclude;
    }

    /**
     * Remove Elements if no exportable selected public domain reference
     * <p>
     */
    @JsonProperty("removeAllLockedRemoveElementsIfNoExportablePublicRef")
    public boolean isRemoveAllLockedRemoveElementsIfNoExportablePublicRef() {
        return removeAllLockedRemoveElementsIfNoExportablePublicRef;
    }

    /**
     * Remove Elements if no exportable selected public domain reference
     * <p>
     */
    @JsonProperty("removeAllLockedRemoveElementsIfNoExportablePublicRef")
    public void setRemoveAllLockedRemoveElementsIfNoExportablePublicRef(boolean removeAllLockedRemoveElementsIfNoExportablePublicRef) {
        this.removeAllLockedRemoveElementsIfNoExportablePublicRef = removeAllLockedRemoveElementsIfNoExportablePublicRef;
    }

    /**
     * Statuses to include
     * <p>
     */
    @JsonProperty("statusesToInclude")
    public List<StatusesToInclude> getStatusesToInclude() {
        return statusesToInclude;
    }

    /**
     * Statuses to include
     * <p>
     */
    @JsonProperty("statusesToInclude")
    public void setStatusesToInclude(List<StatusesToInclude> statusesToInclude) {
        this.statusesToInclude = statusesToInclude;
    }

    /**
     * Elements to remove
     * <p>
     */
    @JsonProperty("removeElementsIfNoExportablePublicRefElementsToRemove")
    public List<String> getRemoveElementsIfNoExportablePublicRefElementsToRemove() {
        return removeElementsIfNoExportablePublicRefElementsToRemove;
    }

    /**
     * Elements to remove
     * <p>
     */
    @JsonProperty("removeElementsIfNoExportablePublicRefElementsToRemove")
    public void setRemoveElementsIfNoExportablePublicRefElementsToRemove(List<String> removeElementsIfNoExportablePublicRefElementsToRemove) {
        this.removeElementsIfNoExportablePublicRefElementsToRemove = removeElementsIfNoExportablePublicRefElementsToRemove;
    }

    /**
     * Remove Codes by System
     * <p>
     */
    @JsonProperty("removeCodesBySystem")
    public boolean isRemoveCodesBySystem() {
        return removeCodesBySystem;
    }

    /**
     * Remove Codes by System
     * <p>
     */
    @JsonProperty("removeCodesBySystem")
    public void setRemoveCodesBySystem(boolean removeCodesBySystem) {
        this.removeCodesBySystem = removeCodesBySystem;
    }

    /**
     * Code Systems to Remove
     * <p>
     */
    @JsonProperty("removeCodesBySystemCodeSystemsToRemove")
    public List<String> getRemoveCodesBySystemCodeSystemsToRemove() {
        return removeCodesBySystemCodeSystemsToRemove;
    }

    /**
     * Code Systems to Remove
     * <p>
     */
    @JsonProperty("removeCodesBySystemCodeSystemsToRemove")
    public void setRemoveCodesBySystemCodeSystemsToRemove(List<String> removeCodesBySystemCodeSystemsToRemove) {
        this.removeCodesBySystemCodeSystemsToRemove = removeCodesBySystemCodeSystemsToRemove;
    }

    /**
     * Code Systems to Keep
     * <p>
     */
    @JsonProperty("removeCodesBySystemCodeSystemsToKeep")
    public List<String> getRemoveCodesBySystemCodeSystemsToKeep() {
        return removeCodesBySystemCodeSystemsToKeep;
    }

    /**
     * Code Systems to Keep
     * <p>
     */
    @JsonProperty("removeCodesBySystemCodeSystemsToKeep")
    public void setRemoveCodesBySystemCodeSystemsToKeep(List<String> removeCodesBySystemCodeSystemsToKeep) {
        this.removeCodesBySystemCodeSystemsToKeep = removeCodesBySystemCodeSystemsToKeep;
    }

    /**
     * Remove References by Criteria
     * <p>
     */
    @JsonProperty("removeReferencesByCriteria")
    public boolean isRemoveReferencesByCriteria() {
        return removeReferencesByCriteria;
    }

    /**
     * Remove References by Criteria
     * <p>
     */
    @JsonProperty("removeReferencesByCriteria")
    public void setRemoveReferencesByCriteria(boolean removeReferencesByCriteria) {
        this.removeReferencesByCriteria = removeReferencesByCriteria;
    }

    /**
     * Reference Types to Remove
     * <p>
     */
    @JsonProperty("removeReferencesByCriteriaReferenceTypesToRemove")
    public List<String> getRemoveReferencesByCriteriaReferenceTypesToRemove() {
        return removeReferencesByCriteriaReferenceTypesToRemove;
    }

    /**
     * Reference Types to Remove
     * <p>
     */
    @JsonProperty("removeReferencesByCriteriaReferenceTypesToRemove")
    public void setRemoveReferencesByCriteriaReferenceTypesToRemove(List<String> removeReferencesByCriteriaReferenceTypesToRemove) {
        this.removeReferencesByCriteriaReferenceTypesToRemove = removeReferencesByCriteriaReferenceTypesToRemove;
    }

    /**
     * Citation Patterns to Remove
     * <p>
     */
    @JsonProperty("removeReferencesByCriteriaCitationPatternsToRemove")
    public String getRemoveReferencesByCriteriaCitationPatternsToRemove() {
        return removeReferencesByCriteriaCitationPatternsToRemove;
    }

    /**
     * Citation Patterns to Remove
     * <p>
     */
    @JsonProperty("removeReferencesByCriteriaCitationPatternsToRemove")
    public void setRemoveReferencesByCriteriaCitationPatternsToRemove(String removeReferencesByCriteriaCitationPatternsToRemove) {
        this.removeReferencesByCriteriaCitationPatternsToRemove = removeReferencesByCriteriaCitationPatternsToRemove;
    }

    /**
     * Exclude Reference by Pattern
     * <p>
     */
    @JsonProperty("removeReferencesByCriteriaExcludeReferenceByPattern")
    public boolean isRemoveReferencesByCriteriaExcludeReferenceByPattern() {
        return removeReferencesByCriteriaExcludeReferenceByPattern;
    }

    /**
     * Exclude Reference by Pattern
     * <p>
     */
    @JsonProperty("removeReferencesByCriteriaExcludeReferenceByPattern")
    public void setRemoveReferencesByCriteriaExcludeReferenceByPattern(boolean removeReferencesByCriteriaExcludeReferenceByPattern) {
        this.removeReferencesByCriteriaExcludeReferenceByPattern = removeReferencesByCriteriaExcludeReferenceByPattern;
    }

    /**
     * Substance Reference Cleanup
     * <p>
     */
    @JsonProperty("substanceReferenceCleanup")
    public boolean isSubstanceReferenceCleanup() {
        return substanceReferenceCleanup;
    }

    /**
     * Substance Reference Cleanup
     * <p>
     */
    @JsonProperty("substanceReferenceCleanup")
    public void setSubstanceReferenceCleanup(boolean substanceReferenceCleanup) {
        this.substanceReferenceCleanup = substanceReferenceCleanup;
    }

    /**
     * Remove References to Filtered Substances
     * <p>
     */
    @JsonProperty("substanceReferenceCleanupRemoveReferencesToFilteredSubstances")
    public boolean isSubstanceReferenceCleanupRemoveReferencesToFilteredSubstances() {
        return substanceReferenceCleanupRemoveReferencesToFilteredSubstances;
    }

    /**
     * Remove References to Filtered Substances
     * <p>
     */
    @JsonProperty("substanceReferenceCleanupRemoveReferencesToFilteredSubstances")
    public void setSubstanceReferenceCleanupRemoveReferencesToFilteredSubstances(boolean substanceReferenceCleanupRemoveReferencesToFilteredSubstances) {
        this.substanceReferenceCleanupRemoveReferencesToFilteredSubstances = substanceReferenceCleanupRemoveReferencesToFilteredSubstances;
    }

    /**
     * Remove References to Substances Non-Exported Definitions
     * <p>
     */
    @JsonProperty("substanceReferenceCleanupRemoveReferencesToSubstancesNonExportedDefinitions")
    public boolean isSubstanceReferenceCleanupRemoveReferencesToSubstancesNonExportedDefinitions() {
        return substanceReferenceCleanupRemoveReferencesToSubstancesNonExportedDefinitions;
    }

    /**
     * Remove References to Substances Non-Exported Definitions
     * <p>
     */
    @JsonProperty("substanceReferenceCleanupRemoveReferencesToSubstancesNonExportedDefinitions")
    public void setSubstanceReferenceCleanupRemoveReferencesToSubstancesNonExportedDefinitions(boolean substanceReferenceCleanupRemoveReferencesToSubstancesNonExportedDefinitions) {
        this.substanceReferenceCleanupRemoveReferencesToSubstancesNonExportedDefinitions = substanceReferenceCleanupRemoveReferencesToSubstancesNonExportedDefinitions;
    }

    /**
     * Remove Notes
     * <p>
     */
    @JsonProperty("removeNotes")
    public boolean isRemoveNotes() {
        return removeNotes;
    }

    /**
     * Remove Notes
     * <p>
     */
    @JsonProperty("removeNotes")
    public void setRemoveNotes(boolean removeNotes) {
        this.removeNotes = removeNotes;
    }

    /**
     * Remove Change Reason
     * <p>
     */
    @JsonProperty("removeChangeReason")
    public boolean isRemoveChangeReason() {
        return removeChangeReason;
    }

    /**
     * Remove Change Reason
     * <p>
     */
    @JsonProperty("removeChangeReason")
    public void setRemoveChangeReason(boolean removeChangeReason) {
        this.removeChangeReason = removeChangeReason;
    }

    /**
     * Approval Id clean-up
     * <p>
     */
    @JsonProperty("approvalIdCleanup")
    public boolean isApprovalIdCleanup() {
        return approvalIdCleanup;
    }

    /**
     * Approval Id clean-up
     * <p>
     */
    @JsonProperty("approvalIdCleanup")
    public void setApprovalIdCleanup(boolean approvalIdCleanup) {
        this.approvalIdCleanup = approvalIdCleanup;
    }

    /**
     * Remove Approval Id
     * <p>
     */
    @JsonProperty("approvalIdCleanupRemoveApprovalId")
    public boolean isApprovalIdCleanupRemoveApprovalId() {
        return approvalIdCleanupRemoveApprovalId;
    }

    /**
     * Remove Approval Id
     * <p>
     */
    @JsonProperty("approvalIdCleanupRemoveApprovalId")
    public void setApprovalIdCleanupRemoveApprovalId(boolean approvalIdCleanupRemoveApprovalId) {
        this.approvalIdCleanupRemoveApprovalId = approvalIdCleanupRemoveApprovalId;
    }

    /**
     * Copy Approval Id to code if code not already present
     * <p>
     */
    @JsonProperty("approvalIdCleanupCopyApprovalIdToCode")
    public boolean isApprovalIdCleanupCopyApprovalIdToCode() {
        return approvalIdCleanupCopyApprovalIdToCode;
    }

    /**
     * Copy Approval Id to code if code not already present
     * <p>
     */
    @JsonProperty("approvalIdCleanupCopyApprovalIdToCode")
    public void setApprovalIdCleanupCopyApprovalIdToCode(boolean approvalIdCleanupCopyApprovalIdToCode) {
        this.approvalIdCleanupCopyApprovalIdToCode = approvalIdCleanupCopyApprovalIdToCode;
    }

    /**
     * Remove Approval Id
     * <p>
     */
    @JsonProperty("approvalIdCleanupApprovalIdCodeSystem")
    public String getApprovalIdCleanupApprovalIdCodeSystem() {
        return approvalIdCleanupApprovalIdCodeSystem;
    }

    /**
     * Remove Approval Id
     * <p>
     */
    @JsonProperty("approvalIdCleanupApprovalIdCodeSystem")
    public void setApprovalIdCleanupApprovalIdCodeSystem(String approvalIdCleanupApprovalIdCodeSystem) {
        this.approvalIdCleanupApprovalIdCodeSystem = approvalIdCleanupApprovalIdCodeSystem;
    }

    /**
     * Regenerate UUIDs
     * <p>
     */
    @JsonProperty("regenerateUUIDs")
    public boolean isRegenerateUUIDs() {
        return regenerateUUIDs;
    }

    /**
     * Regenerate UUIDs
     * <p>
     */
    @JsonProperty("regenerateUUIDs")
    public void setRegenerateUUIDs(boolean regenerateUUIDs) {
        this.regenerateUUIDs = regenerateUUIDs;
    }

    /**
     * Change All Statuses
     * <p>
     */
    @JsonProperty("changeAllStatuses")
    public boolean isChangeAllStatuses() {
        return changeAllStatuses;
    }

    /**
     * Change All Statuses
     * <p>
     */
    @JsonProperty("changeAllStatuses")
    public void setChangeAllStatuses(boolean changeAllStatuses) {
        this.changeAllStatuses = changeAllStatuses;
    }

    /**
     * New Status Value
     * <p>
     */
    @JsonProperty("changeAllStatusesNewStatusValue")
    public String getChangeAllStatusesNewStatusValue() {
        return changeAllStatusesNewStatusValue;
    }

    /**
     * New Status Value
     * <p>
     */
    @JsonProperty("changeAllStatusesNewStatusValue")
    public void setChangeAllStatusesNewStatusValue(String changeAllStatusesNewStatusValue) {
        this.changeAllStatusesNewStatusValue = changeAllStatusesNewStatusValue;
    }

    /**
     * Audit Information clean-up
     * <p>
     */
    @JsonProperty("auditInformationCleanup")
    public boolean isAuditInformationCleanup() {
        return auditInformationCleanup;
    }

    /**
     * Audit Information clean-up
     * <p>
     */
    @JsonProperty("auditInformationCleanup")
    public void setAuditInformationCleanup(boolean auditInformationCleanup) {
        this.auditInformationCleanup = auditInformationCleanup;
    }

    /**
     * New Auditor Value
     * <p>
     */
    @JsonProperty("auditInformationCleanupNewAuditorValue")
    public String getAuditInformationCleanupNewAuditorValue() {
        return auditInformationCleanupNewAuditorValue;
    }

    /**
     * New Auditor Value
     * <p>
     */
    @JsonProperty("auditInformationCleanupNewAuditorValue")
    public void setAuditInformationCleanupNewAuditorValue(String auditInformationCleanupNewAuditorValue) {
        this.auditInformationCleanupNewAuditorValue = auditInformationCleanupNewAuditorValue;
    }

    /**
     * Scrubbed Definition Handling
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandling")
    public boolean isScrubbedDefinitionHandling() {
        return scrubbedDefinitionHandling;
    }

    /**
     * Scrubbed Definition Handling
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandling")
    public void setScrubbedDefinitionHandling(boolean scrubbedDefinitionHandling) {
        this.scrubbedDefinitionHandling = scrubbedDefinitionHandling;
    }

    /**
     * Delete records based on status
     * <p>
     */
    @JsonProperty("removeBasedOnStatus")
    public boolean isRemoveBasedOnStatus() {
        return removeBasedOnStatus;
    }

    /**
     * Delete records based on status
     * <p>
     */
    @JsonProperty("removeBasedOnStatus")
    public void setRemoveBasedOnStatus(boolean removeBasedOnStatus) {
        this.removeBasedOnStatus = removeBasedOnStatus;
    }

    /**
     * Remove partially/fully scrubbed definitional records entirely
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingRemoveScrubbedDefinitionalElementsEntirely")
    public boolean isScrubbedDefinitionHandlingRemoveScrubbedDefinitionalElementsEntirely() {
        return scrubbedDefinitionHandlingRemoveScrubbedDefinitionalElementsEntirely;
    }

    /**
     * Remove partially/fully scrubbed definitional records entirely
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingRemoveScrubbedDefinitionalElementsEntirely")
    public void setScrubbedDefinitionHandlingRemoveScrubbedDefinitionalElementsEntirely(boolean scrubbedDefinitionHandlingRemoveScrubbedDefinitionalElementsEntirely) {
        this.scrubbedDefinitionHandlingRemoveScrubbedDefinitionalElementsEntirely = scrubbedDefinitionHandlingRemoveScrubbedDefinitionalElementsEntirely;
    }

    /**
     * Set partially/fully scrubbed definitional records to definitional level "Incomplete"
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingSetScrubbedDefinitionalElementsIncomplete")
    public boolean isScrubbedDefinitionHandlingSetScrubbedDefinitionalElementsIncomplete() {
        return scrubbedDefinitionHandlingSetScrubbedDefinitionalElementsIncomplete;
    }

    /**
     * Set partially/fully scrubbed definitional records to definitional level "Incomplete"
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingSetScrubbedDefinitionalElementsIncomplete")
    public void setScrubbedDefinitionHandlingSetScrubbedDefinitionalElementsIncomplete(boolean scrubbedDefinitionHandlingSetScrubbedDefinitionalElementsIncomplete) {
        this.scrubbedDefinitionHandlingSetScrubbedDefinitionalElementsIncomplete = scrubbedDefinitionHandlingSetScrubbedDefinitionalElementsIncomplete;
    }

    /**
     * Convert partially/fully scrubbed definitional records to "Concepts"
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingConvertScrubbedDefinitionsToConcepts")
    public boolean isScrubbedDefinitionHandlingConvertScrubbedDefinitionsToConcepts() {
        return scrubbedDefinitionHandlingConvertScrubbedDefinitionsToConcepts;
    }

    /**
     * Convert partially/fully scrubbed definitional records to "Concepts"
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingConvertScrubbedDefinitionsToConcepts")
    public void setScrubbedDefinitionHandlingConvertScrubbedDefinitionsToConcepts(boolean scrubbedDefinitionHandlingConvertScrubbedDefinitionsToConcepts) {
        this.scrubbedDefinitionHandlingConvertScrubbedDefinitionsToConcepts = scrubbedDefinitionHandlingConvertScrubbedDefinitionsToConcepts;
    }

    /**
     * add a note to partially/fully scrubbed definitional records
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingAddNoteToScrubbedDefinitions")
    public String getScrubbedDefinitionHandlingAddNoteToScrubbedDefinitions() {
        return scrubbedDefinitionHandlingAddNoteToScrubbedDefinitions;
    }

    /**
     * add a note to partially/fully scrubbed definitional records
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingAddNoteToScrubbedDefinitions")
    public void setScrubbedDefinitionHandlingAddNoteToScrubbedDefinitions(String scrubbedDefinitionHandlingAddNoteToScrubbedDefinitions) {
        this.scrubbedDefinitionHandlingAddNoteToScrubbedDefinitions = scrubbedDefinitionHandlingAddNoteToScrubbedDefinitions;
    }

    /**
     * Treat partially scrubbed definitions with same settings
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingTreatPartialDefinitionsAsMissingDefinitions")
    public boolean isScrubbedDefinitionHandlingTreatPartialDefinitionsAsMissingDefinitions() {
        return scrubbedDefinitionHandlingTreatPartialDefinitionsAsMissingDefinitions;
    }

    /**
     * Treat partially scrubbed definitions with same settings
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingTreatPartialDefinitionsAsMissingDefinitions")
    public void setScrubbedDefinitionHandlingTreatPartialDefinitionsAsMissingDefinitions(boolean scrubbedDefinitionHandlingTreatPartialDefinitionsAsMissingDefinitions) {
        this.scrubbedDefinitionHandlingTreatPartialDefinitionsAsMissingDefinitions = scrubbedDefinitionHandlingTreatPartialDefinitionsAsMissingDefinitions;
    }

    /**
     * Remove PARTIALLY scrubbed definitional records entirely
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingRemoveScrubbedPartialDefinitionalElementsEntirely")
    public boolean isScrubbedDefinitionHandlingRemoveScrubbedPartialDefinitionalElementsEntirely() {
        return scrubbedDefinitionHandlingRemoveScrubbedPartialDefinitionalElementsEntirely;
    }

    /**
     * Remove PARTIALLY scrubbed definitional records entirely
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingRemoveScrubbedPartialDefinitionalElementsEntirely")
    public void setScrubbedDefinitionHandlingRemoveScrubbedPartialDefinitionalElementsEntirely(boolean scrubbedDefinitionHandlingRemoveScrubbedPartialDefinitionalElementsEntirely) {
        this.scrubbedDefinitionHandlingRemoveScrubbedPartialDefinitionalElementsEntirely = scrubbedDefinitionHandlingRemoveScrubbedPartialDefinitionalElementsEntirely;
    }

    /**
     * Set PARTIALLY scrubbed definitional records to definitional level "Incomplete"
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingSetScrubbedPartialDefinitionalElementsIncomplete")
    public boolean isScrubbedDefinitionHandlingSetScrubbedPartialDefinitionalElementsIncomplete() {
        return scrubbedDefinitionHandlingSetScrubbedPartialDefinitionalElementsIncomplete;
    }

    /**
     * Set PARTIALLY scrubbed definitional records to definitional level "Incomplete"
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingSetScrubbedPartialDefinitionalElementsIncomplete")
    public void setScrubbedDefinitionHandlingSetScrubbedPartialDefinitionalElementsIncomplete(boolean scrubbedDefinitionHandlingSetScrubbedPartialDefinitionalElementsIncomplete) {
        this.scrubbedDefinitionHandlingSetScrubbedPartialDefinitionalElementsIncomplete = scrubbedDefinitionHandlingSetScrubbedPartialDefinitionalElementsIncomplete;
    }

    /**
     * Convert PARTIALLY scrubbed definitional records to "Concepts"
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingConvertScrubbedPartialDefinitionsToConcepts")
    public boolean isScrubbedDefinitionHandlingConvertScrubbedPartialDefinitionsToConcepts() {
        return scrubbedDefinitionHandlingConvertScrubbedPartialDefinitionsToConcepts;
    }

    /**
     * Convert PARTIALLY scrubbed definitional records to "Concepts"
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingConvertScrubbedPartialDefinitionsToConcepts")
    public void setScrubbedDefinitionHandlingConvertScrubbedPartialDefinitionsToConcepts(boolean scrubbedDefinitionHandlingConvertScrubbedPartialDefinitionsToConcepts) {
        this.scrubbedDefinitionHandlingConvertScrubbedPartialDefinitionsToConcepts = scrubbedDefinitionHandlingConvertScrubbedPartialDefinitionsToConcepts;
    }

    /**
     * add a note to PARTIALLY scrubbed definitional records
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingAddNoteToScrubbedPartialDefinitions")
    public String getScrubbedDefinitionHandlingAddNoteToScrubbedPartialDefinitions() {
        return scrubbedDefinitionHandlingAddNoteToScrubbedPartialDefinitions;
    }

    /**
     * add a note to PARTIALLY scrubbed definitional records
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandlingAddNoteToScrubbedPartialDefinitions")
    public void setScrubbedDefinitionHandlingAddNoteToScrubbedPartialDefinitions(String scrubbedDefinitionHandlingAddNoteToScrubbedPartialDefinitions) {
        this.scrubbedDefinitionHandlingAddNoteToScrubbedPartialDefinitions = scrubbedDefinitionHandlingAddNoteToScrubbedPartialDefinitions;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
