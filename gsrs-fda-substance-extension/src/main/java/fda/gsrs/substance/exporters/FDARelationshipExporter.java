package fda.gsrs.substance.exporters;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.EntityFetcher;
import ix.core.util.EntityUtils;
import ix.ginas.exporters.Exporter;
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
    private final boolean includeBdnum;

    public FDARelationshipExporter(SubstanceRepository substanceRepository, OutputStream os, boolean includeBdnum) throws IOException{
        this.substanceRepository = substanceRepository;
        this.includeBdnum = includeBdnum;
        System.out.println("===== includeBdnum: " + includeBdnum);
        bw = new BufferedWriter(new OutputStreamWriter(os));

        StringBuilder sb = new StringBuilder();
        sb.append("Relationship Public/Private").append("\t");
        sb.append("IS_REFLEXIVE").append("\t");
        sb.append("RELATED_SUBSTANCE_UUID").append("\t");
        if(includeBdnum) {
            sb.append("RELATED_SUBSTANCE_BDNUM").append("\t");
        }
        sb.append("RELATED_SUBSTANCE_APPROVAL_ID").append("\t");
        sb.append("Related Subst. Public/Private").append("\t");
        sb.append("RELATED_SUBSTANCE_TYPE").append("\t");
        sb.append("RELATED_SUBSTANCE_DISPLAY_NAME").append("\t");
        sb.append("RELATIONSHIP_TYPE").append("\t");
        sb.append("SUBJECT_DISPLAY_NAME").append("\t");
        sb.append("SUBJECT_SUBSTANCE_TYPE").append("\t");
        sb.append("Subj. Subst. Public/Private").append("\t");
        sb.append("SUBJECT_UUID").append("\t");
        if(includeBdnum) {
            sb.append("SUBJECT_BDNUM").append("\t");
        }
        sb.append("SUBJECT_APPROVAL_ID").append("\t");
        sb.append("RELATIONSHIP_CREATED_BY").append("\t");
        sb.append("RELATIONSHIP_LAST_EDITED").append("\t");
        sb.append("RELATIONSHIP_LAST_EDITED_BY");
        bw.write(sb.toString());
        bw.newLine();
    }

    public String getBdnum(Substance s){
        return s.codes.stream()
        .filter(cd->cd.codeSystem.equals("BDNUM"))
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

            String subjectUnii = ing.getApprovalID();
            String subjectBdnum = "";
            if(includeBdnum) {
                subjectBdnum = getBdnum(ing);
            }
            String subjectDisplayName = ptUTF8;

            List<Relationship> relationships = ing.relationships;
            for (Relationship relationship : relationships) {
                String type = relationship.type;

                String relationshipPublicOrPrivate = (relationship.getAccess().isEmpty()) ? "Public" : "Private: " + ExporterUtilities.makeAccessGroupString(relationship.getAccess());

                String relatedUuid = relationship.relatedSubstance.refuuid;

                String relatedApprovalId = relationship.relatedSubstance.approvalID;
                String relatedSubstancePublicOrPrivate = "";
                String relatedBdnum = "";
                String relatedDisplayName = "";
                String relatedSubstanceType = "Not present";

                EntityUtils.Key relatedKey;
                Optional<Substance> fullRelatedSubstance;
                try {
                    relatedKey = relationship.relatedSubstance.getKeyForReferencedSubstance();
                    fullRelatedSubstance = EntityFetcher.of(relatedKey).getIfPossible().map(o -> (Substance) o);
                    relatedSubstanceType = fullRelatedSubstance.map(s -> s.substanceClass.toString()).orElse("Not present");
                    if (fullRelatedSubstance.isPresent()) {
                        relatedSubstancePublicOrPrivate = (fullRelatedSubstance.get().getAccess().isEmpty()) ? "Public" : "Private: " + ExporterUtilities.makeAccessGroupString(fullRelatedSubstance.get().getAccess());
                        if (includeBdnum) {
                            relatedBdnum = getBdnum(fullRelatedSubstance.get());
                        }
                        relatedDisplayName = fullRelatedSubstance.get().getDisplayName().map(n -> n.getName()).orElse("");
                    }
                } catch (Exception e) {
                    log.warn("Problem loading fullRelatedSubstance.", e);
                }
                String relationshipCreatedBy = relationship.createdBy.username;
                Date relationshipLastEdited = relationship.getLastEdited();
                String relationshipLastEditedBy = relationship.lastEditedBy.username;
                // What if one is null/blank?

                String isReflexive = (subjectUuid.equals(relatedUuid)) ? "Y" : "N";


                StringBuilder sb = new StringBuilder()
                .append(relationshipPublicOrPrivate).append("\t") // Relationship Public/Private
                .append(isReflexive).append("\t")                 // IS_REFLEXIVE
                .append(relatedUuid).append("\t");                 // RELATED_SUBSTANCE_UUID
                if(includeBdnum) {
                    sb.append(relatedBdnum).append("\t");               // RELATED_SUBSTANCE_BDNUM
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
                if(includeBdnum) {
                    sb.append(subjectBdnum).append("\t");                  // SUBJECT_BDNUM
                }
                sb.append(subjectUnii).append("\t")                  // SUBJECT_APPROVAL_ID
                .append(relationshipCreatedBy).append("\t")       // RELATIONSHIP_CREATED_BY
                .append(relationshipLastEdited).append("\t")      // RELATIONSHIP_LAST_EDITED
                .append(relationshipLastEditedBy);   // RELATIONSHIP_LAST_EDITED_BY
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
