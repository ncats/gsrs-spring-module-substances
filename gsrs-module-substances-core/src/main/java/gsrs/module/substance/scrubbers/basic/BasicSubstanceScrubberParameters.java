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

import lombok.Data;

/**
 * Scrubber Parameters
 * <p>
 * Factors that control the behavior of a Java class that removes private parts
 * of a data object before the object is shared
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "removeDates", "auditInformationCleanupDeidentifyAuditUser", "removeAllLocked", "removeAllLockedAccessGroupsToInclude",
		"removeAllLockedRemoveElementsIfNoExportablePublicRef", "statusesToInclude",
		"removeElementsIfNoExportablePublicRefElementsToRemove", "removeCodesBySystem",
		"removeCodesBySystemCodeSystemsToRemove", "removeCodesBySystemCodeSystemsToKeep", "removeReferencesByCriteria",
		"removeReferencesByCriteriaReferenceTypesToRemove", "removeReferencesByCriteriaCitationPatternsToRemove",
		"removeReferencesByCriteriaExcludeReferenceByPattern", "substanceReferenceCleanup",
		"substanceReferenceCleanupActionForDefinitionalDependentScrubbedSubstanceReferences",
		"substanceReferenceCleanupActionForRelationalScrubbedSubstanceReferences", "removeNotes", "removeChangeReason",
		"approvalIdCleanup", "approvalIdCleanupRemoveApprovalId", "approvalIdCleanupCopyApprovalIdToCode",
		"approvalIdCleanupApprovalIdCodeSystem", "regenerateUUIDs", "changeAllStatuses",
		"changeAllStatusesNewStatusValue", "auditInformationCleanup", "auditInformationCleanupNewAuditorValue",
		"removeBasedOnStatus", "scrubbedDefinitionHandling",
		"scrubbedDefinitionHandlingRemoveScrubbedDefinitionalElementsEntirely",
		"scrubbedDefinitionHandlingSetScrubbedDefinitionalElementsIncomplete",
		"scrubbedDefinitionHandlingConvertScrubbedDefinitionsToConcepts",
		"scrubbedDefinitionHandlingAddNoteToScrubbedDefinitions",
		"scrubbedDefinitionHandlingTreatPartialDefinitionsAsMissingDefinitions",
		"scrubbedDefinitionHandlingRemoveScrubbedPartialDefinitionalElementsEntirely",
		"scrubbedDefinitionHandlingSetScrubbedPartialDefinitionalElementsIncomplete",
		"scrubbedDefinitionHandlingConvertScrubbedPartialDefinitionsToConcepts",
		"scrubbedDefinitionHandlingAddNoteToScrubbedPartialDefinitions" })
@Generated("jsonschema2pojo")
@Data
public class BasicSubstanceScrubberParameters {

	/**
	 * Remove Date
	 * <p>
	 *
	 *
	 */
	@JsonProperty("removeDates")
	public Boolean removeDates = false;
	/**
	 * Deidentify Audit User
	 * <p>
	 *
	 *
	 */
	@JsonProperty("auditInformationCleanupDeidentifyAuditUser")
	public Boolean auditInformationCleanupDeidentifyAuditUser = false;
	/**
	 * Remove all Locked
	 * <p>
	 *
	 *
	 */
	@JsonProperty("removeAllLocked")
	public Boolean removeAllLocked= false;
	/**
	 * Access Groups to Include
	 * <p>
	 *
	 *
	 */
	@JsonProperty("removeAllLockedAccessGroupsToInclude")
	public List<String> removeAllLockedAccessGroupsToInclude = null;
	/**
	 * Remove Elements if no exportable selected public domain reference
	 * <p>
	 *
	 *
	 */
	@JsonProperty("removeAllLockedRemoveElementsIfNoExportablePublicRef")
	public Boolean removeAllLockedRemoveElementsIfNoExportablePublicRef= false;
	/**
	 * Statuses to include
	 * <p>
	 *
	 *
	 */
	@JsonProperty("statusesToInclude")
	public List<String> statusesToInclude = null;
	/**
	 * Elements to remove
	 * <p>
	 *
	 *
	 */
	@JsonProperty("removeElementsIfNoExportablePublicRefElementsToRemove")
	public List<String> removeElementsIfNoExportablePublicRefElementsToRemove = null;
	/**
	 * Remove Codes by System
	 * <p>
	 *
	 *
	 */
	@JsonProperty("removeCodesBySystem")
	public Boolean removeCodesBySystem = false;
	/**
	 * Code Systems to Remove
	 * <p>
	 *
	 *
	 */
	@JsonProperty("removeCodesBySystemCodeSystemsToRemove")
	public List<String> removeCodesBySystemCodeSystemsToRemove = null;
	/**
	 * Code Systems to Keep
	 * <p>
	 *
	 *
	 */
	@JsonProperty("removeCodesBySystemCodeSystemsToKeep")
	public List<String> removeCodesBySystemCodeSystemsToKeep = null;
	/**
	 * Remove References by Criteria
	 * <p>
	 *
	 *
	 */
	@JsonProperty("removeReferencesByCriteria")
	public Boolean removeReferencesByCriteria = false;
	/**
	 * Reference Types to Remove
	 * <p>
	 *
	 *
	 */
	@JsonProperty("removeReferencesByCriteriaReferenceTypesToRemove")
	public List<String> removeReferencesByCriteriaReferenceTypesToRemove = null;
	/**
	 * Citation Patterns to Remove
	 * <p>
	 *
	 *
	 */
	@JsonProperty("removeReferencesByCriteriaCitationPatternsToRemove")
	public String removeReferencesByCriteriaCitationPatternsToRemove;
	/**
	 * Exclude Reference by Pattern
	 * <p>
	 *
	 *
	 */
	@JsonProperty("removeReferencesByCriteriaExcludeReferenceByPattern")
	public Boolean removeReferencesByCriteriaExcludeReferenceByPattern = false;
	/**
	 * Substance Reference Cleanup
	 * <p>
	 *
	 *
	 */
	@JsonProperty("substanceReferenceCleanup")
	public Boolean substanceReferenceCleanup = false;
	/**
	 * What action should be taken when a definition depends on a referenced
	 * substance which would be scrubbed out?
	 * <p>
	 *
	 *
	 */
	@JsonProperty("substanceReferenceCleanupActionForDefinitionalDependentScrubbedSubstanceReferences")
	public String substanceReferenceCleanupActionForDefinitionalDependentScrubbedSubstanceReferences;
	/**
	 * What action should be taken when a record references a scrubbed-out substance
	 * in a non-defining way (e.g. relationships)?
	 * <p>
	 *
	 *
	 */
	@JsonProperty("substanceReferenceCleanupActionForRelationalScrubbedSubstanceReferences")
	public String substanceReferenceCleanupActionForRelationalScrubbedSubstanceReferences;
	/**
	 * Remove Notes
	 * <p>
	 *
	 *
	 */
	@JsonProperty("removeNotes")
	public Boolean removeNotes = false;
	/**
	 * Remove Change Reason
	 * <p>
	 *
	 *
	 */
	@JsonProperty("removeChangeReason")
	public Boolean removeChangeReason = false;
	/**
	 * Approval Id clean-up
	 * <p>
	 *
	 *
	 */
	@JsonProperty("approvalIdCleanup")
	public Boolean approvalIdCleanup = false;
	/**
	 * Remove Approval Id
	 * <p>
	 *
	 *
	 */
	@JsonProperty("approvalIdCleanupRemoveApprovalId")
	public Boolean approvalIdCleanupRemoveApprovalId = false;
	/**
	 * Copy Approval Id to code if code not already present
	 * <p>
	 *
	 *
	 */
	@JsonProperty("approvalIdCleanupCopyApprovalIdToCode")
	public Boolean approvalIdCleanupCopyApprovalIdToCode = false;
	/**
	 * Remove Approval Id
	 * <p>
	 *
	 *
	 */
	@JsonProperty("approvalIdCleanupApprovalIdCodeSystem")
	public String approvalIdCleanupApprovalIdCodeSystem;
	/**
* Clean up (top-level) UUIDs
* <p>
*
*
*/
@JsonProperty("UUIDCleanup")
public Boolean UUIDCleanup=false;
/**
	 * Regenerate UUIDs
	 * <p>
	 *
	 *
	 */
	@JsonProperty("regenerateUUIDs")
public Boolean regenerateUUIDs =false;
/**
* Copy UUID to code if code not already present
* <p>
*
*
*/
@JsonProperty("UUIdCleanupCopyUUIDIdToCode")
public Boolean uUIdCleanupCopyUUIDIdToCode = false;
/**
* Copy UUID to code with following code system (if not already present)
* <p>
*
*
*/
@JsonProperty("UUIDCleanupUUIDCodeSystem")
public String UUIDCleanupUUIDCodeSystem ="";
	/**
	 * Change All Statuses
	 * <p>
	 *
	 *
	 */
	@JsonProperty("changeAllStatuses")
	public Boolean changeAllStatuses = false;
	/**
	 * New Status Value
	 * <p>
	 *
	 *
	 */
	@JsonProperty("changeAllStatusesNewStatusValue")
	public String changeAllStatusesNewStatusValue;
	/**
	 * Audit Information clean-up
	 * <p>
	 *
	 *
	 */
	@JsonProperty("auditInformationCleanup")
	public Boolean auditInformationCleanup = false;
	/**
	 * New Auditor Value
	 * <p>
	 *
	 *
	 */
	@JsonProperty("auditInformationCleanupNewAuditorValue")
	public String auditInformationCleanupNewAuditorValue;
	/**
	 * Delete records based on status
	 * <p>
	 *
	 *
	 */
	@JsonProperty("removeBasedOnStatus")
	public Boolean removeBasedOnStatus = false;
	/**
	 * Scrubbed Definition Handling
	 * <p>
	 *
	 *
	 */
	@JsonProperty("scrubbedDefinitionHandling")
	public Boolean scrubbedDefinitionHandling = false;
	/**
	 * Remove partially/fully scrubbed definitional records entirely
	 * <p>
	 *
	 *
	 */
	@JsonProperty("scrubbedDefinitionHandlingRemoveScrubbedDefinitionalElementsEntirely")
	public Boolean scrubbedDefinitionHandlingRemoveScrubbedDefinitionalElementsEntirely = false;
	/**
	 * Set partially/fully scrubbed definitional records to definitional level
	 * "Incomplete"
	 * <p>
	 *
	 *
	 */
	@JsonProperty("scrubbedDefinitionHandlingSetScrubbedDefinitionalElementsIncomplete")
	public Boolean scrubbedDefinitionHandlingSetScrubbedDefinitionalElementsIncomplete = false;
	/**
	 * Convert partially/fully scrubbed definitional records to "Concepts"
	 * <p>
	 *
	 *
	 */
	@JsonProperty("scrubbedDefinitionHandlingConvertScrubbedDefinitionsToConcepts")
	public Boolean scrubbedDefinitionHandlingConvertScrubbedDefinitionsToConcepts = false;
	/**
	 * add a note to partially/fully scrubbed definitional records
	 * <p>
	 *
	 *
	 */
	@JsonProperty("scrubbedDefinitionHandlingAddNoteToScrubbedDefinitions")
	public String scrubbedDefinitionHandlingAddNoteToScrubbedDefinitions;
	/**
	 * Treat partially scrubbed definitions with same settings
	 * <p>
	 *
	 *
	 */
	@JsonProperty("scrubbedDefinitionHandlingTreatPartialDefinitionsAsMissingDefinitions")
	public Boolean scrubbedDefinitionHandlingTreatPartialDefinitionsAsMissingDefinitions = false;
	/**
	 * Remove PARTIALLY scrubbed definitional records entirely
	 * <p>
	 *
	 *
	 */
	@JsonProperty("scrubbedDefinitionHandlingRemoveScrubbedPartialDefinitionalElementsEntirely")
	public Boolean scrubbedDefinitionHandlingRemoveScrubbedPartialDefinitionalElementsEntirely = false;
	/**
	 * Set PARTIALLY scrubbed definitional records to definitional level
	 * "Incomplete"
	 * <p>
	 *
	 *
	 */
	@JsonProperty("scrubbedDefinitionHandlingSetScrubbedPartialDefinitionalElementsIncomplete")
	public Boolean scrubbedDefinitionHandlingSetScrubbedPartialDefinitionalElementsIncomplete = false;
	/**
	 * Convert PARTIALLY scrubbed definitional records to "Concepts"
	 * <p>
	 *
	 *
	 */
	@JsonProperty("scrubbedDefinitionHandlingConvertScrubbedPartialDefinitionsToConcepts")
	public Boolean scrubbedDefinitionHandlingConvertScrubbedPartialDefinitionsToConcepts = false;
	/**
	 * add a note to PARTIALLY scrubbed definitional records
	 * d
	 *
	 *
	 */
	@JsonProperty("scrubbedDefinitionHandlingAddNoteToScrubbedPartialDefinitions")
	public String scrubbedDefinitionHandlingAddNoteToScrubbedPartialDefinitions;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}

	@JsonAnySetter
	public void setAdditionalProperty(String name, Object value) {
		this.additionalProperties.put(name, value);
	}
}