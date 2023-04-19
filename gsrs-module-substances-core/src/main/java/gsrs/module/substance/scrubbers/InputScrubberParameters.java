package gsrs.module.substance.scrubbers;

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
 * Import Scrubber Parameters
 * <p>
 * Factors that control the behavior of a Java class that removes parts of a data object before the object is imported
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "removeCodesBySystem",
        "removeCodesBySystemCodeSystemsToRemove",
        "removeCodesBySystemCodeSystemsToKeep",
        "approvalIdCleanup",
        "approvalIdCleanupRemoveApprovalId",
        "approvalIdCleanupCopyApprovalIdToCode",
        "approvalIdCleanupApprovalIdCodeSystem",
        "UUIDCleanup",
        "regenerateUUIDs",
        "UUIdCleanupCopyUUIDIdToCode",
        "UUIDCleanupUUIDCodeSystem",
        "auditInformationCleanup",
        "auditInformationCleanupDeidentifyAuditUser",
        "auditInformationCleanupNewAuditorValue"
})
@Generated("jsonschema2pojo")
public class InputScrubberParameters {

    /**
     * Remove codes by system
     * <p>
     */
    @JsonProperty("removeCodesBySystem")
    private Boolean removeCodesBySystem;
    /**
     * Code systems to remove
     * <p>
     */
    @JsonProperty("removeCodesBySystemCodeSystemsToRemove")
    private List<String> removeCodesBySystemCodeSystemsToRemove;
    /**
     * Code systems to keep
     * <p>
     */
    @JsonProperty("removeCodesBySystemCodeSystemsToKeep")
    private List<String> removeCodesBySystemCodeSystemsToKeep;
    /**
     * Approval ID clean-up
     * <p>
     */
    @JsonProperty("approvalIdCleanup")
    private Boolean approvalIdCleanup;
    /**
     * Remove approval ID
     * <p>
     */
    @JsonProperty("approvalIdCleanupRemoveApprovalId")
    private Boolean approvalIdCleanupRemoveApprovalId;
    /**
     * Copy approval ID to code if code not already present
     * <p>
     */
    @JsonProperty("approvalIdCleanupCopyApprovalIdToCode")
    private Boolean approvalIdCleanupCopyApprovalIdToCode;
    /**
     * Copy approval ID to code with following code system (if not already present)
     * <p>
     */
    @JsonProperty("approvalIdCleanupApprovalIdCodeSystem")
    private String approvalIdCleanupApprovalIdCodeSystem;
    /**
     * Clean up (top-level) UUIDs
     * <p>
     */
    @JsonProperty("UUIDCleanup")
    private Boolean uUIDCleanup;
    /**
     * Regenerate UUIDs
     * <p>
     */
    @JsonProperty("regenerateUUIDs")
    private Boolean regenerateUUIDs;
    /**
     * Copy UUID to code if code not already present
     * <p>
     */
    @JsonProperty("UUIdCleanupCopyUUIDIdToCode")
    private Boolean uUIdCleanupCopyUUIDIdToCode;
    /**
     * Copy UUID to code with following code system (if not already present)
     * <p>
     */
    @JsonProperty("UUIDCleanupUUIDCodeSystem")
    private String uUIDCleanupUUIDCodeSystem;
    /**
     * Audit information clean-up
     * <p>
     */
    @JsonProperty("auditInformationCleanup")
    private Boolean auditInformationCleanup;
    /**
     * Deidentify audit user
     * <p>
     */
    @JsonProperty("auditInformationCleanupDeidentifyAuditUser")
    private Boolean auditInformationCleanupDeidentifyAuditUser;
    /**
     * New auditor value
     * <p>
     */
    @JsonProperty("auditInformationCleanupNewAuditorValue")
    private String auditInformationCleanupNewAuditorValue;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     */
    public InputScrubberParameters() {
    }

    /**
     * @param removeCodesBySystemCodeSystemsToKeep
     * @param removeCodesBySystem
     * @param uUIDCleanup
     * @param approvalIdCleanup
     * @param uUIDCleanupUUIDCodeSystem
     * @param approvalIdCleanupRemoveApprovalId
     * @param auditInformationCleanupDeidentifyAuditUser
     * @param auditInformationCleanupNewAuditorValue
     * @param uUIdCleanupCopyUUIDIdToCode
     * @param auditInformationCleanup
     * @param approvalIdCleanupApprovalIdCodeSystem
     * @param approvalIdCleanupCopyApprovalIdToCode
     * @param removeCodesBySystemCodeSystemsToRemove
     * @param regenerateUUIDs
     */
    public InputScrubberParameters(Boolean removeCodesBySystem, List<String> removeCodesBySystemCodeSystemsToRemove, List<String> removeCodesBySystemCodeSystemsToKeep, Boolean approvalIdCleanup, Boolean approvalIdCleanupRemoveApprovalId, Boolean approvalIdCleanupCopyApprovalIdToCode, String approvalIdCleanupApprovalIdCodeSystem, Boolean uUIDCleanup, Boolean regenerateUUIDs, Boolean uUIdCleanupCopyUUIDIdToCode, String uUIDCleanupUUIDCodeSystem, Boolean auditInformationCleanup, Boolean auditInformationCleanupDeidentifyAuditUser, String auditInformationCleanupNewAuditorValue) {
        super();
        this.removeCodesBySystem = removeCodesBySystem;
        this.removeCodesBySystemCodeSystemsToRemove = removeCodesBySystemCodeSystemsToRemove;
        this.removeCodesBySystemCodeSystemsToKeep = removeCodesBySystemCodeSystemsToKeep;
        this.approvalIdCleanup = approvalIdCleanup;
        this.approvalIdCleanupRemoveApprovalId = approvalIdCleanupRemoveApprovalId;
        this.approvalIdCleanupCopyApprovalIdToCode = approvalIdCleanupCopyApprovalIdToCode;
        this.approvalIdCleanupApprovalIdCodeSystem = approvalIdCleanupApprovalIdCodeSystem;
        this.uUIDCleanup = uUIDCleanup;
        this.regenerateUUIDs = regenerateUUIDs;
        this.uUIdCleanupCopyUUIDIdToCode = uUIdCleanupCopyUUIDIdToCode;
        this.uUIDCleanupUUIDCodeSystem = uUIDCleanupUUIDCodeSystem;
        this.auditInformationCleanup = auditInformationCleanup;
        this.auditInformationCleanupDeidentifyAuditUser = auditInformationCleanupDeidentifyAuditUser;
        this.auditInformationCleanupNewAuditorValue = auditInformationCleanupNewAuditorValue;
    }

    /**
     * Remove codes by system
     * <p>
     */
    @JsonProperty("removeCodesBySystem")
    public Boolean getRemoveCodesBySystem() {
        return removeCodesBySystem;
    }

    /**
     * Remove codes by system
     * <p>
     */
    @JsonProperty("removeCodesBySystem")
    public void setRemoveCodesBySystem(Boolean removeCodesBySystem) {
        this.removeCodesBySystem = removeCodesBySystem;
    }

    /**
     * Code systems to remove
     * <p>
     */
    @JsonProperty("removeCodesBySystemCodeSystemsToRemove")
    public List<String> getRemoveCodesBySystemCodeSystemsToRemove() {
        return removeCodesBySystemCodeSystemsToRemove;
    }

    /**
     * Code systems to remove
     * <p>
     */
    @JsonProperty("removeCodesBySystemCodeSystemsToRemove")
    public void setRemoveCodesBySystemCodeSystemsToRemove(List<String> removeCodesBySystemCodeSystemsToRemove) {
        this.removeCodesBySystemCodeSystemsToRemove = removeCodesBySystemCodeSystemsToRemove;
    }

    /**
     * Code systems to keep
     * <p>
     */
    @JsonProperty("removeCodesBySystemCodeSystemsToKeep")
    public List<String> getRemoveCodesBySystemCodeSystemsToKeep() {
        return removeCodesBySystemCodeSystemsToKeep;
    }

    /**
     * Code systems to keep
     * <p>
     */
    @JsonProperty("removeCodesBySystemCodeSystemsToKeep")
    public void setRemoveCodesBySystemCodeSystemsToKeep(List<String> removeCodesBySystemCodeSystemsToKeep) {
        this.removeCodesBySystemCodeSystemsToKeep = removeCodesBySystemCodeSystemsToKeep;
    }

    /**
     * Approval ID clean-up
     * <p>
     */
    @JsonProperty("approvalIdCleanup")
    public Boolean getApprovalIdCleanup() {
        return approvalIdCleanup;
    }

    /**
     * Approval ID clean-up
     * <p>
     */
    @JsonProperty("approvalIdCleanup")
    public void setApprovalIdCleanup(Boolean approvalIdCleanup) {
        this.approvalIdCleanup = approvalIdCleanup;
    }

    /**
     * Remove approval ID
     * <p>
     */
    @JsonProperty("approvalIdCleanupRemoveApprovalId")
    public Boolean getApprovalIdCleanupRemoveApprovalId() {
        return approvalIdCleanupRemoveApprovalId;
    }

    /**
     * Remove approval ID
     * <p>
     */
    @JsonProperty("approvalIdCleanupRemoveApprovalId")
    public void setApprovalIdCleanupRemoveApprovalId(Boolean approvalIdCleanupRemoveApprovalId) {
        this.approvalIdCleanupRemoveApprovalId = approvalIdCleanupRemoveApprovalId;
    }

    /**
     * Copy approval ID to code if code not already present
     * <p>
     */
    @JsonProperty("approvalIdCleanupCopyApprovalIdToCode")
    public Boolean getApprovalIdCleanupCopyApprovalIdToCode() {
        return approvalIdCleanupCopyApprovalIdToCode;
    }

    /**
     * Copy approval ID to code if code not already present
     * <p>
     */
    @JsonProperty("approvalIdCleanupCopyApprovalIdToCode")
    public void setApprovalIdCleanupCopyApprovalIdToCode(Boolean approvalIdCleanupCopyApprovalIdToCode) {
        this.approvalIdCleanupCopyApprovalIdToCode = approvalIdCleanupCopyApprovalIdToCode;
    }

    /**
     * Copy approval ID to code with following code system (if not already present)
     * <p>
     */
    @JsonProperty("approvalIdCleanupApprovalIdCodeSystem")
    public String getApprovalIdCleanupApprovalIdCodeSystem() {
        return approvalIdCleanupApprovalIdCodeSystem;
    }

    /**
     * Copy approval ID to code with following code system (if not already present)
     * <p>
     */
    @JsonProperty("approvalIdCleanupApprovalIdCodeSystem")
    public void setApprovalIdCleanupApprovalIdCodeSystem(String approvalIdCleanupApprovalIdCodeSystem) {
        this.approvalIdCleanupApprovalIdCodeSystem = approvalIdCleanupApprovalIdCodeSystem;
    }

    /**
     * Clean up (top-level) UUIDs
     * <p>
     */
    @JsonProperty("UUIDCleanup")
    public Boolean getUUIDCleanup() {
        return uUIDCleanup;
    }

    /**
     * Clean up (top-level) UUIDs
     * <p>
     */
    @JsonProperty("UUIDCleanup")
    public void setUUIDCleanup(Boolean uUIDCleanup) {
        this.uUIDCleanup = uUIDCleanup;
    }

    /**
     * Regenerate UUIDs
     * <p>
     */
    @JsonProperty("regenerateUUIDs")
    public Boolean getRegenerateUUIDs() {
        return regenerateUUIDs;
    }

    /**
     * Regenerate UUIDs
     * <p>
     */
    @JsonProperty("regenerateUUIDs")
    public void setRegenerateUUIDs(Boolean regenerateUUIDs) {
        this.regenerateUUIDs = regenerateUUIDs;
    }

    /**
     * Copy UUID to code if code not already present
     * <p>
     */
    @JsonProperty("UUIdCleanupCopyUUIDIdToCode")
    public Boolean getUUIdCleanupCopyUUIDIdToCode() {
        return uUIdCleanupCopyUUIDIdToCode;
    }

    /**
     * Copy UUID to code if code not already present
     * <p>
     */
    @JsonProperty("UUIdCleanupCopyUUIDIdToCode")
    public void setUUIdCleanupCopyUUIDIdToCode(Boolean uUIdCleanupCopyUUIDIdToCode) {
        this.uUIdCleanupCopyUUIDIdToCode = uUIdCleanupCopyUUIDIdToCode;
    }

    /**
     * Copy UUID to code with following code system (if not already present)
     * <p>
     */
    @JsonProperty("UUIDCleanupUUIDCodeSystem")
    public String getUUIDCleanupUUIDCodeSystem() {
        return uUIDCleanupUUIDCodeSystem;
    }

    /**
     * Copy UUID to code with following code system (if not already present)
     * <p>
     */
    @JsonProperty("UUIDCleanupUUIDCodeSystem")
    public void setUUIDCleanupUUIDCodeSystem(String uUIDCleanupUUIDCodeSystem) {
        this.uUIDCleanupUUIDCodeSystem = uUIDCleanupUUIDCodeSystem;
    }

    /**
     * Audit information clean-up
     * <p>
     */
    @JsonProperty("auditInformationCleanup")
    public Boolean getAuditInformationCleanup() {
        return auditInformationCleanup;
    }

    /**
     * Audit information clean-up
     * <p>
     */
    @JsonProperty("auditInformationCleanup")
    public void setAuditInformationCleanup(Boolean auditInformationCleanup) {
        this.auditInformationCleanup = auditInformationCleanup;
    }

    /**
     * Deidentify audit user
     * <p>
     */
    @JsonProperty("auditInformationCleanupDeidentifyAuditUser")
    public Boolean getAuditInformationCleanupDeidentifyAuditUser() {
        return auditInformationCleanupDeidentifyAuditUser;
    }

    /**
     * Deidentify audit user
     * <p>
     */
    @JsonProperty("auditInformationCleanupDeidentifyAuditUser")
    public void setAuditInformationCleanupDeidentifyAuditUser(Boolean auditInformationCleanupDeidentifyAuditUser) {
        this.auditInformationCleanupDeidentifyAuditUser = auditInformationCleanupDeidentifyAuditUser;
    }

    /**
     * New auditor value
     * <p>
     */
    @JsonProperty("auditInformationCleanupNewAuditorValue")
    public String getAuditInformationCleanupNewAuditorValue() {
        return auditInformationCleanupNewAuditorValue;
    }

    /**
     * New auditor value
     * <p>
     */
    @JsonProperty("auditInformationCleanupNewAuditorValue")
    public void setAuditInformationCleanupNewAuditorValue(String auditInformationCleanupNewAuditorValue) {
        this.auditInformationCleanupNewAuditorValue = auditInformationCleanupNewAuditorValue;
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
        sb.append(InputScrubberParameters.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("removeCodesBySystem");
        sb.append('=');
        sb.append(((this.removeCodesBySystem == null) ? "<null>" : this.removeCodesBySystem));
        sb.append(',');
        sb.append("removeCodesBySystemCodeSystemsToRemove");
        sb.append('=');
        sb.append(((this.removeCodesBySystemCodeSystemsToRemove == null) ? "<null>" : this.removeCodesBySystemCodeSystemsToRemove));
        sb.append(',');
        sb.append("removeCodesBySystemCodeSystemsToKeep");
        sb.append('=');
        sb.append(((this.removeCodesBySystemCodeSystemsToKeep == null) ? "<null>" : this.removeCodesBySystemCodeSystemsToKeep));
        sb.append(',');
        sb.append("approvalIdCleanup");
        sb.append('=');
        sb.append(((this.approvalIdCleanup == null) ? "<null>" : this.approvalIdCleanup));
        sb.append(',');
        sb.append("approvalIdCleanupRemoveApprovalId");
        sb.append('=');
        sb.append(((this.approvalIdCleanupRemoveApprovalId == null) ? "<null>" : this.approvalIdCleanupRemoveApprovalId));
        sb.append(',');
        sb.append("approvalIdCleanupCopyApprovalIdToCode");
        sb.append('=');
        sb.append(((this.approvalIdCleanupCopyApprovalIdToCode == null) ? "<null>" : this.approvalIdCleanupCopyApprovalIdToCode));
        sb.append(',');
        sb.append("approvalIdCleanupApprovalIdCodeSystem");
        sb.append('=');
        sb.append(((this.approvalIdCleanupApprovalIdCodeSystem == null) ? "<null>" : this.approvalIdCleanupApprovalIdCodeSystem));
        sb.append(',');
        sb.append("uUIDCleanup");
        sb.append('=');
        sb.append(((this.uUIDCleanup == null) ? "<null>" : this.uUIDCleanup));
        sb.append(',');
        sb.append("regenerateUUIDs");
        sb.append('=');
        sb.append(((this.regenerateUUIDs == null) ? "<null>" : this.regenerateUUIDs));
        sb.append(',');
        sb.append("uUIdCleanupCopyUUIDIdToCode");
        sb.append('=');
        sb.append(((this.uUIdCleanupCopyUUIDIdToCode == null) ? "<null>" : this.uUIdCleanupCopyUUIDIdToCode));
        sb.append(',');
        sb.append("uUIDCleanupUUIDCodeSystem");
        sb.append('=');
        sb.append(((this.uUIDCleanupUUIDCodeSystem == null) ? "<null>" : this.uUIDCleanupUUIDCodeSystem));
        sb.append(',');
        sb.append("auditInformationCleanup");
        sb.append('=');
        sb.append(((this.auditInformationCleanup == null) ? "<null>" : this.auditInformationCleanup));
        sb.append(',');
        sb.append("auditInformationCleanupDeidentifyAuditUser");
        sb.append('=');
        sb.append(((this.auditInformationCleanupDeidentifyAuditUser == null) ? "<null>" : this.auditInformationCleanupDeidentifyAuditUser));
        sb.append(',');
        sb.append("auditInformationCleanupNewAuditorValue");
        sb.append('=');
        sb.append(((this.auditInformationCleanupNewAuditorValue == null) ? "<null>" : this.auditInformationCleanupNewAuditorValue));
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
