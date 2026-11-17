
package gsrs.module.substance.importers.model;

import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.annotation.Generated;

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
        "mergeCodes",
        "mergeProperties",
        "mergePropertiesPropertyUniqueness",
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
     * Merge Codes
     * <p>
     */
    @JsonProperty("mergeCodes")
    private Boolean mergeCodes = false;
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
    @JsonProperty("mergePropertiesPropertyUniqueness")
    private Boolean mergePropertiesPropertyUniqueness = false;
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
     * No args constructor for use in serialization
     */
    public MergeProcessingActionParameters() {
    }

    /**
     * @param mergeModificationsMergeStructuralModifications
     * @param mergeCodes
     * @param mergeModificationsMergeAgentModifications
     * @param mergeModificationsMergePhysicalModifications
     * @param mergeRelationships
     * @param mergeRelationshipsRelationshipUniqueness
     * @param mergeNotes
     * @param mergeNames
     * @param skipLevelingReferences
     * @param mergeProperties
     * @param mergePropertiesPropertyUniqueness
     * @param mergeReferences
     * @param copyStructure
     * @param mergeModifications
     * @param mergeNotesNoteUniqueness
     */
    public MergeProcessingActionParameters(Boolean mergeReferences, Boolean mergeNames, Boolean mergeCodes, Boolean mergeProperties, Boolean mergePropertiesPropertyUniqueness, Boolean mergeNotes, Boolean mergeNotesNoteUniqueness, Boolean mergeRelationships, Boolean mergeRelationshipsRelationshipUniqueness, Boolean mergeModifications, Boolean mergeModificationsMergeStructuralModifications, Boolean mergeModificationsMergeAgentModifications, Boolean mergeModificationsMergePhysicalModifications, Boolean skipLevelingReferences, Boolean copyStructure) {
        super();
        this.mergeReferences = mergeReferences;
        this.mergeNames = mergeNames;
        this.mergeCodes = mergeCodes;
        this.mergeProperties = mergeProperties;
        this.mergePropertiesPropertyUniqueness = mergePropertiesPropertyUniqueness;
        this.mergeNotes = mergeNotes;
        this.mergeNotesNoteUniqueness = mergeNotesNoteUniqueness;
        this.mergeRelationships = mergeRelationships;
        this.mergeRelationshipsRelationshipUniqueness = mergeRelationshipsRelationshipUniqueness;
        this.mergeModifications = mergeModifications;
        this.mergeModificationsMergeStructuralModifications = mergeModificationsMergeStructuralModifications;
        this.mergeModificationsMergeAgentModifications = mergeModificationsMergeAgentModifications;
        this.mergeModificationsMergePhysicalModifications = mergeModificationsMergePhysicalModifications;
        this.skipLevelingReferences = skipLevelingReferences;
        this.copyStructure = copyStructure;
    }

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(MergeProcessingActionParameters.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("mergeReferences");
        sb.append('=');
        sb.append(((this.mergeReferences == null) ? "<null>" : this.mergeReferences));
        sb.append(',');
        sb.append("mergeNames");
        sb.append('=');
        sb.append(((this.mergeNames == null) ? "<null>" : this.mergeNames));
        sb.append(',');
        sb.append("mergeCodes");
        sb.append('=');
        sb.append(((this.mergeCodes == null) ? "<null>" : this.mergeCodes));
        sb.append(',');
        sb.append("mergeProperties");
        sb.append('=');
        sb.append(((this.mergeProperties == null) ? "<null>" : this.mergeProperties));
        sb.append(',');
        sb.append("mergePropertiesPropertyUniqueness");
        sb.append('=');
        sb.append(((this.mergePropertiesPropertyUniqueness == null) ? "<null>" : this.mergePropertiesPropertyUniqueness));
        sb.append(',');
        sb.append("mergeNotes");
        sb.append('=');
        sb.append(((this.mergeNotes == null) ? "<null>" : this.mergeNotes));
        sb.append(',');
        sb.append("mergeNotesNoteUniqueness");
        sb.append('=');
        sb.append(((this.mergeNotesNoteUniqueness == null) ? "<null>" : this.mergeNotesNoteUniqueness));
        sb.append(',');
        sb.append("mergeRelationships");
        sb.append('=');
        sb.append(((this.mergeRelationships == null) ? "<null>" : this.mergeRelationships));
        sb.append(',');
        sb.append("mergeRelationshipsRelationshipUniqueness");
        sb.append('=');
        sb.append(((this.mergeRelationshipsRelationshipUniqueness == null) ? "<null>" : this.mergeRelationshipsRelationshipUniqueness));
        sb.append(',');
        sb.append("mergeModifications");
        sb.append('=');
        sb.append(((this.mergeModifications == null) ? "<null>" : this.mergeModifications));
        sb.append(',');
        sb.append("mergeModificationsMergeStructuralModifications");
        sb.append('=');
        sb.append(((this.mergeModificationsMergeStructuralModifications == null) ? "<null>" : this.mergeModificationsMergeStructuralModifications));
        sb.append(',');
        sb.append("mergeModificationsMergeAgentModifications");
        sb.append('=');
        sb.append(((this.mergeModificationsMergeAgentModifications == null) ? "<null>" : this.mergeModificationsMergeAgentModifications));
        sb.append(',');
        sb.append("mergeModificationsMergePhysicalModifications");
        sb.append('=');
        sb.append(((this.mergeModificationsMergePhysicalModifications == null) ? "<null>" : this.mergeModificationsMergePhysicalModifications));
        sb.append(',');
        sb.append("skipLevelingReferences");
        sb.append('=');
        sb.append(((this.skipLevelingReferences == null) ? "<null>" : this.skipLevelingReferences));
        sb.append(',');
        sb.append("copyStructure");
        sb.append('=');
        sb.append(((this.copyStructure == null) ? "<null>" : this.copyStructure));
        sb.append(',');
        sb.append("additionalProperties");
        sb.append('=');
        sb.append(((this.additionalProperties == null) ? "<null>" : this.additionalProperties));
        sb.append(',');
        if (sb.charAt((sb.length() - 1)) == ',') {
            sb.setCharAt((sb.length() - 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

}
