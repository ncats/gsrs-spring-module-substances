package gsrs.module.substance.scrubbers.basic;

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
        "accessGroupsToInclude",
        "accessGroupsToRemove",
        "removeElementsIfNoExportablePublicRef",
        "removeAllLocked",
        "removeCodesBySystem",
        "codeSystemsToRemove",
        "codeSystemsToKeep",
        "removeReferencesByCriteria",
        "referenceTypesToRemove",
        "citationPatternsToRemove",
        "excludeReferenceByPattern",
        "substanceReferenceCleanup",
        "removeReferencesToFilteredSubstances",
        "removeReferencesToSubstancesNonExportedDefinitions",
        "removeNotes",
        "removeChangeReason",
        "approvalIdCleanup",
        "removeApprovalId",
        "copyApprovalIdToCode",
        "approvalIdCodeSystem",
        "regenerateUUIDs",
        "changeAllStatuses",
        "newStatusValue",
        "AuditInformationCleanup",
        "newAuditorValue",
        "scrubbedDefinitionHandling",
        "removeScrubbedDefinitionalElementsEntirely",
        "setScrubbedDefinitionalElementsIncomplete",
        "convertScrubbedDefinitionsToConcepts",
        "addNoteToScrubbedDefinitions"
})
@Generated("jsonschema2pojo")
public class BasicSubstanceScrubberParameters {

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
     * Access Groups to Include
     * <p>
     */
    @JsonProperty("accessGroupsToInclude")
    private List<String> accessGroupsToInclude = null;
    /**
     * Access Groups to Remove
     * <p>
     */
    @JsonProperty("accessGroupsToRemove")
    private List<String> accessGroupsToRemove = null;
    /**
     * Remove Elements if no exportable selected public domain reference
     * <p>
     */
    @JsonProperty("removeElementsIfNoExportablePublicRef")
    private String removeElementsIfNoExportablePublicRef;
    /**
     * Remove all Locked
     * <p>
     */
    @JsonProperty("removeAllLocked")
    private boolean removeAllLocked;
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
    @JsonProperty("codeSystemsToRemove")
    private List<String> codeSystemsToRemove = null;
    /**
     * Code Systems to Keep
     * <p>
     */
    @JsonProperty("codeSystemsToKeep")
    private List<String> codeSystemsToKeep = null;
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
    @JsonProperty("referenceTypesToRemove")
    private List<String> referenceTypesToRemove = null;
    /**
     * Citation Patterns to Remove
     * <p>
     */
    @JsonProperty("citationPatternsToRemove")
    private String citationPatternsToRemove;
    /**
     * Exclude Reference by Pattern
     * <p>
     */
    @JsonProperty("excludeReferenceByPattern")
    private boolean excludeReferenceByPattern;
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
    @JsonProperty("removeReferencesToFilteredSubstances")
    private boolean removeReferencesToFilteredSubstances;
    /**
     * Remove References to Substances Non-Exported Definitions
     * <p>
     */
    @JsonProperty("removeReferencesToSubstancesNonExportedDefinitions")
    private boolean removeReferencesToSubstancesNonExportedDefinitions;
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
    @JsonProperty("removeApprovalId")
    private boolean removeApprovalId;
    /**
     * Copy Approval Id to code if code not already present
     * <p>
     */
    @JsonProperty("copyApprovalIdToCode")
    private boolean copyApprovalIdToCode;
    /**
     * Remove Approval Id
     * <p>
     */
    @JsonProperty("approvalIdCodeSystem")
    private String approvalIdCodeSystem;
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
    @JsonProperty("newStatusValue")
    private String newStatusValue;
    /**
     * Audit Information clean-up
     * <p>
     */
    @JsonProperty("AuditInformationCleanup")
    private boolean auditInformationCleanup;
    /**
     * New Auditor Value
     * <p>
     */
    @JsonProperty("newAuditorValue")
    private String newAuditorValue;
    /**
     * Scrubbed Definition Handling
     * <p>
     */
    @JsonProperty("scrubbedDefinitionHandling")
    private boolean scrubbedDefinitionHandling;
    /**
     * Remove partially/fully scrubbed definitional records entirely
     * <p>
     */
    @JsonProperty("removeScrubbedDefinitionalElementsEntirely")
    private boolean removeScrubbedDefinitionalElementsEntirely;
    /**
     * Set partially/fully scrubbed definitional records to definitional level "Incomplete"
     * <p>
     */
    @JsonProperty("setScrubbedDefinitionalElementsIncomplete")
    private boolean setScrubbedDefinitionalElementsIncomplete;
    /**
     * Convert partially/fully scrubbed definitional records to "Concepts"
     * <p>
     */
    @JsonProperty("convertScrubbedDefinitionsToConcepts")
    private boolean convertScrubbedDefinitionsToConcepts;
    /**
     * add a note to partially/fully scrubbed definitional records
     * <p>
     */
    @JsonProperty("addNoteToScrubbedDefinitions")
    private String addNoteToScrubbedDefinitions;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

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
     * Access Groups to Include
     * <p>
     */
    @JsonProperty("accessGroupsToInclude")
    public List<String> getAccessGroupsToInclude() {
        return accessGroupsToInclude;
    }

    /**
     * Access Groups to Include
     * <p>
     */
    @JsonProperty("accessGroupsToInclude")
    public void setAccessGroupsToInclude(List<String> accessGroupsToInclude) {
        this.accessGroupsToInclude = accessGroupsToInclude;
    }

    /**
     * Access Groups to Remove
     * <p>
     */
    @JsonProperty("accessGroupsToRemove")
    public List<String> getAccessGroupsToRemove() {
        return accessGroupsToRemove;
    }

    /**
     * Access Groups to Remove
     * <p>
     */
    @JsonProperty("accessGroupsToRemove")
    public void setAccessGroupsToRemove(List<String> accessGroupsToRemove) {
        this.accessGroupsToRemove = accessGroupsToRemove;
    }

    /**
     * Remove Elements if no exportable selected public domain reference
     * <p>
     */
    @JsonProperty("removeElementsIfNoExportablePublicRef")
    public String getRemoveElementsIfNoExportablePublicRef() {
        return removeElementsIfNoExportablePublicRef;
    }

    /**
     * Remove Elements if no exportable selected public domain reference
     * <p>
     */
    @JsonProperty("removeElementsIfNoExportablePublicRef")
    public void setRemoveElementsIfNoExportablePublicRef(String removeElementsIfNoExportablePublicRef) {
        this.removeElementsIfNoExportablePublicRef = removeElementsIfNoExportablePublicRef;
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
    @JsonProperty("codeSystemsToRemove")
    public List<String> getCodeSystemsToRemove() {
        return codeSystemsToRemove;
    }

    /**
     * Code Systems to Remove
     * <p>
     */
    @JsonProperty("codeSystemsToRemove")
    public void setCodeSystemsToRemove(List<String> codeSystemsToRemove) {
        this.codeSystemsToRemove = codeSystemsToRemove;
    }

    /**
     * Code Systems to Keep
     * <p>
     */
    @JsonProperty("codeSystemsToKeep")
    public List<String> getCodeSystemsToKeep() {
        return codeSystemsToKeep;
    }

    /**
     * Code Systems to Keep
     * <p>
     */
    @JsonProperty("codeSystemsToKeep")
    public void setCodeSystemsToKeep(List<String> codeSystemsToKeep) {
        this.codeSystemsToKeep = codeSystemsToKeep;
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
    @JsonProperty("referenceTypesToRemove")
    public List<String> getReferenceTypesToRemove() {
        return referenceTypesToRemove;
    }

    /**
     * Reference Types to Remove
     * <p>
     */
    @JsonProperty("referenceTypesToRemove")
    public void setReferenceTypesToRemove(List<String> referenceTypesToRemove) {
        this.referenceTypesToRemove = referenceTypesToRemove;
    }

    /**
     * Citation Patterns to Remove
     * <p>
     */
    @JsonProperty("citationPatternsToRemove")
    public String getCitationPatternsToRemove() {
        return citationPatternsToRemove;
    }

    /**
     * Citation Patterns to Remove
     * <p>
     */
    @JsonProperty("citationPatternsToRemove")
    public void setCitationPatternsToRemove(String citationPatternsToRemove) {
        this.citationPatternsToRemove = citationPatternsToRemove;
    }

    /**
     * Exclude Reference by Pattern
     * <p>
     */
    @JsonProperty("excludeReferenceByPattern")
    public boolean isExcludeReferenceByPattern() {
        return excludeReferenceByPattern;
    }

    /**
     * Exclude Reference by Pattern
     * <p>
     */
    @JsonProperty("excludeReferenceByPattern")
    public void setExcludeReferenceByPattern(boolean excludeReferenceByPattern) {
        this.excludeReferenceByPattern = excludeReferenceByPattern;
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
    @JsonProperty("removeReferencesToFilteredSubstances")
    public boolean isRemoveReferencesToFilteredSubstances() {
        return removeReferencesToFilteredSubstances;
    }

    /**
     * Remove References to Filtered Substances
     * <p>
     */
    @JsonProperty("removeReferencesToFilteredSubstances")
    public void setRemoveReferencesToFilteredSubstances(boolean removeReferencesToFilteredSubstances) {
        this.removeReferencesToFilteredSubstances = removeReferencesToFilteredSubstances;
    }

    /**
     * Remove References to Substances Non-Exported Definitions
     * <p>
     */
    @JsonProperty("removeReferencesToSubstancesNonExportedDefinitions")
    public boolean isRemoveReferencesToSubstancesNonExportedDefinitions() {
        return removeReferencesToSubstancesNonExportedDefinitions;
    }

    /**
     * Remove References to Substances Non-Exported Definitions
     * <p>
     */
    @JsonProperty("removeReferencesToSubstancesNonExportedDefinitions")
    public void setRemoveReferencesToSubstancesNonExportedDefinitions(boolean removeReferencesToSubstancesNonExportedDefinitions) {
        this.removeReferencesToSubstancesNonExportedDefinitions = removeReferencesToSubstancesNonExportedDefinitions;
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
    @JsonProperty("removeApprovalId")
    public boolean isRemoveApprovalId() {
        return removeApprovalId;
    }

    /**
     * Remove Approval Id
     * <p>
     */
    @JsonProperty("removeApprovalId")
    public void setRemoveApprovalId(boolean removeApprovalId) {
        this.removeApprovalId = removeApprovalId;
    }

    /**
     * Copy Approval Id to code if code not already present
     * <p>
     */
    @JsonProperty("copyApprovalIdToCode")
    public boolean isCopyApprovalIdToCode() {
        return copyApprovalIdToCode;
    }

    /**
     * Copy Approval Id to code if code not already present
     * <p>
     */
    @JsonProperty("copyApprovalIdToCode")
    public void setCopyApprovalIdToCode(boolean copyApprovalIdToCode) {
        this.copyApprovalIdToCode = copyApprovalIdToCode;
    }

    /**
     * Remove Approval Id
     * <p>
     */
    @JsonProperty("approvalIdCodeSystem")
    public String getApprovalIdCodeSystem() {
        return approvalIdCodeSystem;
    }

    /**
     * Remove Approval Id
     * <p>
     */
    @JsonProperty("approvalIdCodeSystem")
    public void setApprovalIdCodeSystem(String approvalIdCodeSystem) {
        this.approvalIdCodeSystem = approvalIdCodeSystem;
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
    @JsonProperty("newStatusValue")
    public String getNewStatusValue() {
        return newStatusValue;
    }

    /**
     * New Status Value
     * <p>
     */
    @JsonProperty("newStatusValue")
    public void setNewStatusValue(String newStatusValue) {
        this.newStatusValue = newStatusValue;
    }

    /**
     * Audit Information clean-up
     * <p>
     */
    @JsonProperty("AuditInformationCleanup")
    public boolean isAuditInformationCleanup() {
        return auditInformationCleanup;
    }

    /**
     * Audit Information clean-up
     * <p>
     */
    @JsonProperty("AuditInformationCleanup")
    public void setAuditInformationCleanup(boolean auditInformationCleanup) {
        this.auditInformationCleanup = auditInformationCleanup;
    }

    /**
     * New Auditor Value
     * <p>
     */
    @JsonProperty("newAuditorValue")
    public String getNewAuditorValue() {
        return newAuditorValue;
    }

    /**
     * New Auditor Value
     * <p>
     */
    @JsonProperty("newAuditorValue")
    public void setNewAuditorValue(String newAuditorValue) {
        this.newAuditorValue = newAuditorValue;
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
     * Remove partially/fully scrubbed definitional records entirely
     * <p>
     */
    @JsonProperty("removeScrubbedDefinitionalElementsEntirely")
    public boolean isRemoveScrubbedDefinitionalElementsEntirely() {
        return removeScrubbedDefinitionalElementsEntirely;
    }

    /**
     * Remove partially/fully scrubbed definitional records entirely
     * <p>
     */
    @JsonProperty("removeScrubbedDefinitionalElementsEntirely")
    public void setRemoveScrubbedDefinitionalElementsEntirely(boolean removeScrubbedDefinitionalElementsEntirely) {
        this.removeScrubbedDefinitionalElementsEntirely = removeScrubbedDefinitionalElementsEntirely;
    }

    /**
     * Set partially/fully scrubbed definitional records to definitional level "Incomplete"
     * <p>
     */
    @JsonProperty("setScrubbedDefinitionalElementsIncomplete")
    public boolean isSetScrubbedDefinitionalElementsIncomplete() {
        return setScrubbedDefinitionalElementsIncomplete;
    }

    /**
     * Set partially/fully scrubbed definitional records to definitional level "Incomplete"
     * <p>
     */
    @JsonProperty("setScrubbedDefinitionalElementsIncomplete")
    public void setSetScrubbedDefinitionalElementsIncomplete(boolean setScrubbedDefinitionalElementsIncomplete) {
        this.setScrubbedDefinitionalElementsIncomplete = setScrubbedDefinitionalElementsIncomplete;
    }

    /**
     * Convert partially/fully scrubbed definitional records to "Concepts"
     * <p>
     */
    @JsonProperty("convertScrubbedDefinitionsToConcepts")
    public boolean isConvertScrubbedDefinitionsToConcepts() {
        return convertScrubbedDefinitionsToConcepts;
    }

    /**
     * Convert partially/fully scrubbed definitional records to "Concepts"
     * <p>
     */
    @JsonProperty("convertScrubbedDefinitionsToConcepts")
    public void setConvertScrubbedDefinitionsToConcepts(boolean convertScrubbedDefinitionsToConcepts) {
        this.convertScrubbedDefinitionsToConcepts = convertScrubbedDefinitionsToConcepts;
    }

    /**
     * add a note to partially/fully scrubbed definitional records
     * <p>
     */
    @JsonProperty("addNoteToScrubbedDefinitions")
    public String getAddNoteToScrubbedDefinitions() {
        return addNoteToScrubbedDefinitions;
    }

    /**
     * add a note to partially/fully scrubbed definitional records
     * <p>
     */
    @JsonProperty("addNoteToScrubbedDefinitions")
    public void setAddNoteToScrubbedDefinitions(String addNoteToScrubbedDefinitions) {
        this.addNoteToScrubbedDefinitions = addNoteToScrubbedDefinitions;
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
