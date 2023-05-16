package fda.gsrs.substance.exporters;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.EntityFetcher;
import ix.core.util.EntityUtils;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.models.v1.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

@Slf4j
public class FDARelationshipExporter implements Exporter<Substance> {

    private final BufferedWriter bw;
    private final SubstanceRepository substanceRepository;
    private final String primaryCodeSystem;

    private ExporterFactory.Parameters params;
    private boolean omitAmountData;
    private boolean omitPrimaryCodeSystem;
    private String chosenApprovalIdName;

    public FDARelationshipExporter(SubstanceRepository substanceRepository, OutputStream os, ExporterFactory.Parameters params, String primaryCodeSystem) throws IOException{
        this.substanceRepository = substanceRepository;
        this.primaryCodeSystem = primaryCodeSystem;
        this.params = params;
        JsonNode detailedParameters = params.detailedParameters();

        omitAmountData = (detailedParameters!=null
        && detailedParameters.hasNonNull(FDARelationshipExporterFactory.AMOUNT_DATA_PARAMETERS)
        && detailedParameters.get(FDARelationshipExporterFactory.AMOUNT_DATA_PARAMETERS).booleanValue());

        omitPrimaryCodeSystem = (detailedParameters!=null
        && detailedParameters.hasNonNull(FDARelationshipExporterFactory.PRIMARY_CODE_SYSTEM_PARAMETERS)
        && detailedParameters.get(FDARelationshipExporterFactory.PRIMARY_CODE_SYSTEM_PARAMETERS).booleanValue());

        chosenApprovalIdName = (detailedParameters!=null
        && detailedParameters.hasNonNull(FDARelationshipExporterFactory.APPROVAL_ID_NAME_PARAMETERS)
        && detailedParameters.get(FDARelationshipExporterFactory.APPROVAL_ID_NAME_PARAMETERS).textValue().trim().length()>0)
        ? detailedParameters.get(FDARelationshipExporterFactory.APPROVAL_ID_NAME_PARAMETERS).textValue().trim() : FDARelationshipExporterFactory.DEFAULT_APPROVAL_ID_NAME;

        bw = new BufferedWriter(new OutputStreamWriter(os));
        StringBuilder sb = new StringBuilder();
        sb.append("Relationship Public/Private").append("\t");
        sb.append("IS_REFLEXIVE").append("\t");
        sb.append("RELATED_SUBSTANCE_UUID").append("\t");
        if(!omitPrimaryCodeSystem && primaryCodeSystem!=null) {
            sb.append("RELATED_SUBSTANCE_"+primaryCodeSystem).append("\t");
        }
        sb.append("RELATED_SUBSTANCE_"+chosenApprovalIdName).append("\t");
        sb.append("Related Subst. Public/Private").append("\t");
        sb.append("RELATED_SUBSTANCE_TYPE").append("\t");
        sb.append("RELATED_SUBSTANCE_DISPLAY_NAME").append("\t");

        sb.append("RELATIONSHIP_TYPE").append("\t");
        sb.append("SUBJECT_DISPLAY_NAME").append("\t");
        sb.append("SUBJECT_SUBSTANCE_TYPE").append("\t");
        sb.append("Subj. Subst. Public/Private").append("\t");
        sb.append("SUBJECT_UUID").append("\t");
        if(!omitPrimaryCodeSystem && primaryCodeSystem!=null) {
            sb.append("SUBJECT_"+primaryCodeSystem).append("\t");
        }
        sb.append("SUBJECT_"+chosenApprovalIdName).append("\t");

        sb.append("RELATIONSHIP_QUALIFICATION").append("\t");
        sb.append("RELATIONSHIP_INTERACTION_TYPE").append("\t");

        if(!omitAmountData) {
            sb.append("RELATIONSHIP_AMOUNT_TYPE").append("\t");
            sb.append("RELATIONSHIP_AMOUNT_AVG").append("\t");
            sb.append("RELATIONSHIP_AMOUNT_LOW").append("\t");
            sb.append("RELATIONSHIP_AMOUNT_HIGH").append("\t");
            sb.append("RELATIONSHIP_AMOUNT_LOW_LIMIT").append("\t");
            sb.append("RELATIONSHIP_AMOUNT_HIGH_LIMIT").append("\t");
            sb.append("RELATIONSHIP_AMOUNT_UNIT").append("\t");
            sb.append("RELATIONSHIP_AMOUNT_NONNUMVALUE").append("\t");
        }
        sb.append("RELATIONSHIP_CREATED_BY").append("\t");
        sb.append("RELATIONSHIP_LAST_EDITED").append("\t");
        sb.append("RELATIONSHIP_LAST_EDITED_BY");
        bw.write(sb.toString());
        bw.newLine();
    }

    public String getPrimaryCodeSystemCode(Substance s){
        return s.codes.stream()
        .filter(cd->cd.codeSystem.equals(primaryCodeSystem)&&cd.type.equals("PRIMARY"))
        .map(cd->cd.code)
        .findFirst()
        .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public void export(Substance ing) throws IOException {
        try {
            // is this the right displayName?
            String ptUTF8 = ing.getDisplayName()
            .map(n -> n.getName())
            .orElse("");
            String subjectUuid = ing.getUuid().toString();
            String subjectSubstanceClass = ing.substanceClass.toString();

            String subjectSubstancePublicOrPrivate = (ing.getAccess().isEmpty()) ? "Public" : "Private: " + ExporterUtilities.makeAccessGroupString(ing.getAccess());

            String subjectApprovalId = ing.getApprovalID();
            String subjectPrimaryCodeSystemCode = "";
            if(!omitPrimaryCodeSystem && primaryCodeSystem!=null) {
                subjectPrimaryCodeSystemCode = getPrimaryCodeSystemCode(ing);
            }
            String subjectDisplayName = ptUTF8;

            List<Relationship> relationships = ing.relationships;
            for (Relationship relationship : relationships) {
                String type = relationship.type;

                String relationshipPublicOrPrivate = (relationship.getAccess().isEmpty()) ? "Public" : "Private: " + ExporterUtilities.makeAccessGroupString(relationship.getAccess());

                String relatedUuid = relationship.relatedSubstance.refuuid;

                String relatedApprovalId = relationship.relatedSubstance.approvalID;
                String relatedSubstancePublicOrPrivate = "";
                String relatedPrimaryCodeSystemCode = "";
                String relatedDisplayName = "";
                String relatedSubstanceType = "Not present";

                EntityUtils.Key relatedKey;
                Optional<Substance> fullRelatedSubstance;
                try {
                    relatedKey = relationship.relatedSubstance.getKeyForReferencedSubstance();
                    fullRelatedSubstance = EntityFetcher.of(relatedKey)
                    		                            .getIfPossible()
                    		                            .map(o -> (Substance) o)
                    		                            .map(oo->(Substance)params.getScrubber().scrub(oo).orElse(null))
                    		                            .filter(oo->oo!=null)
                    		                            ;
                    relatedSubstanceType = fullRelatedSubstance.map(s -> s.substanceClass.toString()).orElse("Not present");
                    if (fullRelatedSubstance.isPresent()) {
                        relatedSubstancePublicOrPrivate = (fullRelatedSubstance.get().getAccess().isEmpty()) ? "Public" : "Private: " + ExporterUtilities.makeAccessGroupString(fullRelatedSubstance.get().getAccess());
                        if (!omitPrimaryCodeSystem && primaryCodeSystem!=null) {
                            relatedPrimaryCodeSystemCode = getPrimaryCodeSystemCode(fullRelatedSubstance.get());
                        }
                        relatedDisplayName = fullRelatedSubstance.get().getDisplayName().map(n -> n.getName()).orElse("");
                    }
                } catch (Exception e) {
                    log.warn("Problem loading fullRelatedSubstance.", e);
                }

                String relationshipQualification = (relationship.qualification!=null) ? relationship.qualification : "";
                String relationshipInteractionType = (relationship.interactionType!=null) ? relationship.interactionType : "";
                Amount ra = relationship.amount;
                String relationshipAmountType = (ra!=null && ra.type!=null) ? ra.type: "";
                String relationshipAmountAverage = (ra!=null && ra.average!=null) ? ra.average.toString(): "";
                String relationshipAmountLow = (ra!=null && ra.low!=null) ? ra.low.toString(): "";
                String relationshipAmountHigh = (ra!=null && ra.high!=null) ? ra.high.toString(): "";
                String relationshipAmountLowLimit = (ra!=null && ra.lowLimit!=null) ? ra.lowLimit.toString(): "";
                String relationshipAmountHighLimit = (ra!=null && ra.highLimit!=null) ? ra.highLimit.toString(): "";
                String relationshipAmountUnits = (ra!=null && ra.units!=null) ? ra.units: "";
                String relationshipAmountNonNumericValue = (ra!=null && ra.nonNumericValue!=null) ? ra.nonNumericValue: "";

                String relationshipCreatedBy = relationship.createdBy.username;
                Date relationshipLastEdited = relationship.getLastEdited();
                String relationshipLastEditedBy = relationship.lastEditedBy.username;
                // What if one is null/blank?

                String isReflexive = (subjectUuid.equals(relatedUuid)) ? "Y" : "N";


                StringBuilder sb = new StringBuilder()
                .append(relationshipPublicOrPrivate).append("\t") // Relationship Public/Private
                .append(isReflexive).append("\t")                 // IS_REFLEXIVE
                .append(relatedUuid).append("\t");                 // RELATED_SUBSTANCE_UUID
                if(!omitPrimaryCodeSystem && primaryCodeSystem!=null) {
                    sb.append(relatedPrimaryCodeSystemCode).append("\t");               // relatedPrimaryCodeSystemCode
                }
                sb.append(relatedApprovalId).append("\t")           // RELATED_SUBSTANCE_APPROVAL_ID
                .append(relatedSubstancePublicOrPrivate).append("\t") // Related Subst. Public/Private
                .append(relatedSubstanceType).append("\t")        // RELATED_SUBSTANCE_TYPE
                .append(relatedDisplayName).append("\t")          // RELATED_SUBSTANCE_DISPLAY_NAME
                .append(type).append("\t")                        // RELATIONSHIP_TYPE

                .append(subjectDisplayName).append("\t")           // SUBJECT_DISPLAY_NAME
                .append(subjectSubstanceClass).append("\t")        // SUBJECT_SUBSTANCE_TYPE
                .append(subjectSubstancePublicOrPrivate).append("\t") // Subj. Subst. Public/Private
                .append(subjectUuid).append("\t");                  // SUBJECT_UUID
                if(!omitPrimaryCodeSystem && primaryCodeSystem!=null) {
                    sb.append(subjectPrimaryCodeSystemCode).append("\t");                  // subjectPrimaryCodeSystemCode
                }

                sb.append(subjectApprovalId).append("\t");                 // SUBJECT_APPROVAL_ID

                sb.append(relationshipQualification).append("\t");       // RELATIONSHIP_QUALIFICATION
                sb.append(relationshipInteractionType).append("\t");       // RELATIONSHIP_INTERACTION_TYPE

                if (!omitAmountData) {
                    sb.append(relationshipAmountType).append("\t");            // RELATIONSHIP_AMOUNT_TYPE
                    sb.append(relationshipAmountAverage).append("\t");         // RELATIONSHIP_AMOUNT_AVG
                    sb.append(relationshipAmountLow).append("\t");             // RELATIONSHIP_AMOUNT_LOW
                    sb.append(relationshipAmountHigh).append("\t");            // RELATIONSHIP_AMOUNT_HIGH
                    sb.append(relationshipAmountLowLimit).append("\t");        // RELATIONSHIP_AMOUNT_LOW_LIMIT
                    sb.append(relationshipAmountHighLimit).append("\t");       // RELATIONSHIP_AMOUNT_HIGH_LIMIT
                    sb.append(relationshipAmountUnits).append("\t");           // RELATIONSHIP_AMOUNT_UNITS
                    sb.append(relationshipAmountNonNumericValue).append("\t"); // RELATIONSHIP_AMOUNT_NONNUMVALUE
                }

                sb.append(relationshipCreatedBy).append("\t");       // RELATIONSHIP_CREATED_BY
                sb.append(relationshipLastEdited).append("\t");     // RELATIONSHIP_LAST_EDITED
                sb.append(relationshipLastEditedBy);   // RELATIONSHIP_LAST_EDITED_BY
                bw.write(sb.toString());
                bw.newLine();
            }

        } catch (Exception e) {
            if (ing.getName() != null) {
                log.warn("Exception exporting relationships of " + ing.getName());
            } else {
                log.warn("Exception exporting relationships of " + "unknown display name");
            }
        }
    }

    @Override
    public void close() throws IOException {
        bw.close();
    }
}
