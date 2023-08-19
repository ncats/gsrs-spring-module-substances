package gsrs.module.substance.importers.model;

import java.util.LinkedHashMap;
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
 * Import Merge Parameters
 * <p>
 * Options when data from one substance is merged into another
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "mergeReferences",
        "mergeNames",
        "mergeNames_specificNames",
        "mergeNames_skipNameMatches",
        "mergeNames_mergeNamesOnStdName",
        "mergeCodes",
        "mergeCodes_specificSystems",
        "mergeProperties",
        "mergePropertiesPropertyUniqueness",
        "mergeProperties_specificPropertyNames",
        "mergeNotes",
        "mergeNotesNoteUniqueness",
        "mergeRelationships",
        "mergeRelationshipsRelationshipUniqueness",
        "mergeModifications",
        "mergeModificationsMergeStructuralModifications",
        "mergeModificationsMergeAgentModifications",
        "mergeModificationsMergePhysicalModifications",
        "skipLevelingReferences",
        "copyStructure"
})
@Generated("jsonschema2pojo")
public class MergeProcessingActionParameters {

    /**
     * Merge References
     * <p>
     */
    @JsonProperty("mergeReferences")
    private Boolean mergeReferences = false;
    /**
     * Merge Names
     * <p>
     */
    @JsonProperty("mergeNames")
    private Boolean mergeNames = false;
    /**
     * Specific Names to Include
     * <p>
     */
    @JsonProperty("mergeNames_specificNames")
    private List<String> mergeNamesSpecificNames;
    /**
     * Skip Names with any duplicate (including other GSRS records)
     * <p>
     */
    @JsonProperty("mergeNames_skipNameMatches")
    private Boolean mergeNamesSkipNameMatches = true;
    /**
     * Merge only Names that have a standardized name match within the matching substance
     * <p>
     */
    @JsonProperty("mergeNames_mergeNamesOnStdName")
    private Boolean mergeNamesMergeNamesOnStdName = false;
    /**
     * Merge Codes
     * <p>
     */
    @JsonProperty("mergeCodes")
    private Boolean mergeCodes = false;
    /**
     * Specific Code Systems to Include
     * <p>
     */
    @JsonProperty("mergeCodes_specificSystems")
    private List<String> mergeCodesSpecificSystems;
    /**
     * Merge Properties
     * <p>
     */
    @JsonProperty("mergeProperties")
    private Boolean mergeProperties = false;
    /**
     * Property Name Uniqueness
     * <p>
     */
    @JsonProperty("mergeProperties_PropertyUniqueness")
    private Boolean mergePropertiesPropertyUniqueness = false;
    /**
     * Specific Properties to Include
     * <p>
     */
    @JsonProperty("mergeProperties_specificPropertyNames")
    private List<String> mergePropertiesSpecificPropertyNames;
    /**
     * Merge Notes
     * <p>
     */
    @JsonProperty("mergeNotes")
    private Boolean mergeNotes = false;
    /**
     * Note uniqueness
     * <p>
     */
    @JsonProperty("mergeNotesNoteUniqueness")
    private Boolean mergeNotesNoteUniqueness = false;
    /**
     * Merge Relationships
     * <p>
     */
    @JsonProperty("mergeRelationships")
    private Boolean mergeRelationships = false;
    /**
     * Relationship Uniqueness
     * <p>
     */
    @JsonProperty("mergeRelationshipsRelationshipUniqueness")
    private Boolean mergeRelationshipsRelationshipUniqueness = false;
    /**
     * Copy modifications from new substance into existing substance
     * <p>
     */
    @JsonProperty("mergeModifications")
    private Boolean mergeModifications = false;
    /**
     * Merge Structural Modifications
     * <p>
     */
    @JsonProperty("mergeModificationsMergeStructuralModifications")
    private Boolean mergeModificationsMergeStructuralModifications = false;
    /**
     * Merge Agent Modifications
     * <p>
     */
    @JsonProperty("mergeModificationsMergeAgentModifications")
    private Boolean mergeModificationsMergeAgentModifications = false;
    /**
     * Merge Physical Modifications
     * <p>
     */
    @JsonProperty("mergeModificationsMergePhysicalModifications")
    private Boolean mergeModificationsMergePhysicalModifications = false;
    /**
     * Skip Leveling References
     * <p>
     */
    @JsonProperty("skipLevelingReferences")
    private Boolean skipLevelingReferences = false;
    /**
     * Copy chemical structure
     * <p>
     */
    @JsonProperty("copyStructure")
    private Boolean copyStructure = false;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Merge References
     * <p>
     */
    @JsonProperty("mergeReferences")
    public Boolean getMergeReferences() {
        return mergeReferences;
    }

    /**
     * Merge References
     * <p>
     */
    @JsonProperty("mergeReferences")
    public void setMergeReferences(Boolean mergeReferences) {
        this.mergeReferences = mergeReferences;
    }

    /**
     * Merge Names
     * <p>
     */
    @JsonProperty("mergeNames")
    public Boolean getMergeNames() {
        return mergeNames;
    }

    /**
     * Merge Names
     * <p>
     */
    @JsonProperty("mergeNames")
    public void setMergeNames(Boolean mergeNames) {
        this.mergeNames = mergeNames;
    }

    /**
     * Specific Names to Include
     * <p>
     */
    @JsonProperty("mergeNames_specificNames")
    public List<String> getMergeNamesSpecificNames() {
        return mergeNamesSpecificNames;
    }

    /**
     * Specific Names to Include
     * <p>
     */
    @JsonProperty("mergeNames_specificNames")
    public void setMergeNamesSpecificNames(List<String> mergeNamesSpecificNames) {
        this.mergeNamesSpecificNames = mergeNamesSpecificNames;
    }

    /**
     * Skip Names with any duplicate (including other GSRS records)
     * <p>
     */
    @JsonProperty("mergeNames_skipNameMatches")
    public Boolean getMergeNamesSkipNameMatches() {
        return mergeNamesSkipNameMatches;
    }

    /**
     * Skip Names with any duplicate (including other GSRS records)
     * <p>
     */
    @JsonProperty("mergeNames_skipNameMatches")
    public void setMergeNamesSkipNameMatches(Boolean mergeNamesSkipNameMatches) {
        this.mergeNamesSkipNameMatches = mergeNamesSkipNameMatches;
    }

    /**
     * Merge only Names that have a standardized name match within the matching substance
     * <p>
     */
    @JsonProperty("mergeNames_mergeNamesOnStdName")
    public Boolean getMergeNamesMergeNamesOnStdName() {
        return mergeNamesMergeNamesOnStdName;
    }

    /**
     * Merge only Names that have a standardized name match within the matching substance
     * <p>
     */
    @JsonProperty("mergeNames_mergeNamesOnStdName")
    public void setMergeNamesMergeNamesOnStdName(Boolean mergeNamesMergeNamesOnStdName) {
        this.mergeNamesMergeNamesOnStdName = mergeNamesMergeNamesOnStdName;
    }

    /**
     * Merge Codes
     * <p>
     */
    @JsonProperty("mergeCodes")
    public Boolean getMergeCodes() {
        return mergeCodes;
    }

    /**
     * Merge Codes
     * <p>
     */
    @JsonProperty("mergeCodes")
    public void setMergeCodes(Boolean mergeCodes) {
        this.mergeCodes = mergeCodes;
    }

    /**
     * Specific Code Systems to Include
     * <p>
     */
    @JsonProperty("mergeCodes_specificSystems")
    public List<String> getMergeCodesSpecificSystems() {
        return mergeCodesSpecificSystems;
    }

    /**
     * Specific Code Systems to Include
     * <p>
     */
    @JsonProperty("mergeCodes_specificSystems")
    public void setMergeCodesSpecificSystems(List<String> mergeCodesSpecificSystems) {
        this.mergeCodesSpecificSystems = mergeCodesSpecificSystems;
    }

    /**
     * Merge Properties
     * <p>
     */
    @JsonProperty("mergeProperties")
    public Boolean getMergeProperties() {
        return mergeProperties;
    }

    /**
     * Merge Properties
     * <p>
     */
    @JsonProperty("mergeProperties")
    public void setMergeProperties(Boolean mergeProperties) {
        this.mergeProperties = mergeProperties;
    }

    /**
     * Property Name Uniqueness
     * <p>
     */
    @JsonProperty("mergePropertiesPropertyUniqueness")
    public Boolean getMergePropertiesPropertyUniqueness() {
        return mergePropertiesPropertyUniqueness;
    }

    /**
     * Property Name Uniqueness
     * <p>
     */
    @JsonProperty("mergePropertiesPropertyUniqueness")
    public void setMergePropertiesPropertyUniqueness(Boolean mergePropertiesPropertyUniqueness) {
        this.mergePropertiesPropertyUniqueness = mergePropertiesPropertyUniqueness;
    }

    /**
     * Specific Properties to Include
     * <p>
     */
    @JsonProperty("mergeProperties_specificPropertyNames")
    public List<String> getMergePropertiesSpecificPropertyNames() {
        return mergePropertiesSpecificPropertyNames;
    }

    /**
     * Specific Properties to Include
     * <p>
     */
    @JsonProperty("mergeProperties_specificPropertyNames")
    public void setMergePropertiesSpecificPropertyNames(List<String> mergePropertiesSpecificPropertyNames) {
        this.mergePropertiesSpecificPropertyNames = mergePropertiesSpecificPropertyNames;
    }

    /**
     * Merge Notes
     * <p>
     */
    @JsonProperty("mergeNotes")
    public Boolean getMergeNotes() {
        return mergeNotes;
    }

    /**
     * Merge Notes
     * <p>
     */
    @JsonProperty("mergeNotes")
    public void setMergeNotes(Boolean mergeNotes) {
        this.mergeNotes = mergeNotes;
    }

    /**
     * Note uniqueness
     * <p>
     */
    @JsonProperty("mergeNotesNoteUniqueness")
    public Boolean getMergeNotesNoteUniqueness() {
        return mergeNotesNoteUniqueness;
    }

    /**
     * Note uniqueness
     * <p>
     */
    @JsonProperty("mergeNotesNoteUniqueness")
    public void setMergeNotesNoteUniqueness(Boolean mergeNotesNoteUniqueness) {
        this.mergeNotesNoteUniqueness = mergeNotesNoteUniqueness;
    }

    /**
     * Merge Relationships
     * <p>
     */
    @JsonProperty("mergeRelationships")
    public Boolean getMergeRelationships() {
        return mergeRelationships;
    }

    /**
     * Merge Relationships
     * <p>
     */
    @JsonProperty("mergeRelationships")
    public void setMergeRelationships(Boolean mergeRelationships) {
        this.mergeRelationships = mergeRelationships;
    }

    /**
     * Relationship Uniqueness
     * <p>
     */
    @JsonProperty("mergeRelationshipsRelationshipUniqueness")
    public Boolean getMergeRelationshipsRelationshipUniqueness() {
        return mergeRelationshipsRelationshipUniqueness;
    }

    /**
     * Relationship Uniqueness
     * <p>
     */
    @JsonProperty("mergeRelationshipsRelationshipUniqueness")
    public void setMergeRelationshipsRelationshipUniqueness(Boolean mergeRelationshipsRelationshipUniqueness) {
        this.mergeRelationshipsRelationshipUniqueness = mergeRelationshipsRelationshipUniqueness;
    }

    /**
     * Copy modifications from new substance into existing substance
     * <p>
     */
    @JsonProperty("mergeModifications")
    public Boolean getMergeModifications() {
        return mergeModifications;
    }

    /**
     * Copy modifications from new substance into existing substance
     * <p>
     */
    @JsonProperty("mergeModifications")
    public void setMergeModifications(Boolean mergeModifications) {
        this.mergeModifications = mergeModifications;
    }

    /**
     * Merge Structural Modifications
     * <p>
     */
    @JsonProperty("mergeModificationsMergeStructuralModifications")
    public Boolean getMergeModificationsMergeStructuralModifications() {
        return mergeModificationsMergeStructuralModifications;
    }

    /**
     * Merge Structural Modifications
     * <p>
     */
    @JsonProperty("mergeModificationsMergeStructuralModifications")
    public void setMergeModificationsMergeStructuralModifications(Boolean mergeModificationsMergeStructuralModifications) {
        this.mergeModificationsMergeStructuralModifications = mergeModificationsMergeStructuralModifications;
    }

    /**
     * Merge Agent Modifications
     * <p>
     */
    @JsonProperty("mergeModificationsMergeAgentModifications")
    public Boolean getMergeModificationsMergeAgentModifications() {
        return mergeModificationsMergeAgentModifications;
    }

    /**
     * Merge Agent Modifications
     * <p>
     */
    @JsonProperty("mergeModificationsMergeAgentModifications")
    public void setMergeModificationsMergeAgentModifications(Boolean mergeModificationsMergeAgentModifications) {
        this.mergeModificationsMergeAgentModifications = mergeModificationsMergeAgentModifications;
    }

    /**
     * Merge Physical Modifications
     * <p>
     */
    @JsonProperty("mergeModificationsMergePhysicalModifications")
    public Boolean getMergeModificationsMergePhysicalModifications() {
        return mergeModificationsMergePhysicalModifications;
    }

    /**
     * Merge Physical Modifications
     * <p>
     */
    @JsonProperty("mergeModificationsMergePhysicalModifications")
    public void setMergeModificationsMergePhysicalModifications(Boolean mergeModificationsMergePhysicalModifications) {
        this.mergeModificationsMergePhysicalModifications = mergeModificationsMergePhysicalModifications;
    }

    /**
     * Skip Leveling References
     * <p>
     */
    @JsonProperty("skipLevelingReferences")
    public Boolean getSkipLevelingReferences() {
        return skipLevelingReferences;
    }

    /**
     * Skip Leveling References
     * <p>
     */
    @JsonProperty("skipLevelingReferences")
    public void setSkipLevelingReferences(Boolean skipLevelingReferences) {
        this.skipLevelingReferences = skipLevelingReferences;
    }

    /**
     * Copy chemical structure
     * <p>
     */
    @JsonProperty("copyStructure")
    public Boolean getCopyStructure() {
        return copyStructure;
    }

    /**
     * Copy chemical structure
     * <p>
     */
    @JsonProperty("copyStructure")
    public void setCopyStructure(Boolean copyStructure) {
        this.copyStructure = copyStructure;
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
