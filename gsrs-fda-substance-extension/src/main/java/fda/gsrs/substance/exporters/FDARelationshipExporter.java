package fda.gsrs.substance.exporters;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.EntityFetcher;
import ix.core.models.Group;
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
import java.util.stream.Collectors;
@Slf4j
public class FDARelationshipExporter implements Exporter<Substance> {

    private final BufferedWriter bw;

    private final SubstanceRepository substanceRepository;

    private final ShowPrivates showPrivates;

    public FDARelationshipExporter(SubstanceRepository substanceRepository, OutputStream os, booleanshowPrivates) throws IOException{
        this.substanceRepository = substanceRepository;
        this.showPrivates = showPrivates;

        bw = new BufferedWriter(new OutputStreamWriter(os));
        bw.write(
        "Relationship Public/Private\tIS_REFLEXIVE\tRELATED_SUBSTANCE_UUID\tRELATED_SUBSTANCE_BDNUM\tRELATED_SUBSTANCE_APPROVAL_ID\tRelated Subst. Public/Private\tRELATED_SUBSTANCE_TYPE\tRELATED_SUBSTANCE_DISPLAY_NAME\tRELATIONSHIP_TYPE\tSUBJECT_DISPLAY_NAME\tSUBJECT_SUBSTANCE_TYPE\tSubj. Subst. Public/Private\tSUBJECT_UUID\tSUBJECT_BDNUM\tSUBJECT_APPROVAL_ID\tRELATIONSHIP_CREATED_BY\tRELATIONSHIP_LAST_EDITED\tRELATIONSHIP_LAST_EDITED_BY");
        );
        bw.newLine();
    }

    /**
     * Get the "best" form of the substance. This means that if the
     * substance is a varient sub-concept, return the priority
     * substance version. Otherwise, just return the supplied
     * substance.
     * @param s
     * @return
     */
    public Substance getParentSubstance(Substance s) {
        return s;
    }
    public Substance disabled_getParentSubstance(Substance s){
    	if(s.isSubstanceVariant()){
    		SubstanceReference sr= s.getParentSubstanceReference();
    		Substance parent=substanceRepository.findBySubstanceReference(sr);
    		if(parent == null){
    			Substance fake = new Substance();
    			fake.approvalID=sr.approvalID;
    			Name n = new Name();
    			n.setName(sr.refPname);
    		
    			fake.names.add(n); 
    			
    			Code cd = new Code();
    			cd.code="UNKNOWN BDNUM";
    			cd.codeSystem="BDNUM";
    			
    			fake.codes.add(cd);
    			
    			return fake;
    		}
    		return parent;
    	}
    	return s;
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
            Substance bestParent = getParentSubstance(ing);
            // is this the right displayName?
            String ptUTF8 = bestParent.getDisplayName()
            .map(n -> n.getName())
            .orElse("");
            String parentUuid = bestParent.getUuid().toString();
            String parentSubstanceClass = bestParent.substanceClass.toString();

            String parentSubstancePublicOrPrivate = (bestParent.getAccess().isEmpty()) ? "Public" : "Private: " + makeAccessGroupString(bestParent.getAccess());

            String parentUnii = bestParent.getApprovalID();
            String parentBdnum = getBdnum(bestParent);
            String parentDisplayName = ptUTF8;

            List<Relationship> relationships = ing.relationships;
            for (Relationship relationship : relationships) {
                String type = relationship.type;

                String relationshipPublicOrPrivate = (relationship.getAccess().isEmpty()) ? "Public" : "Private: " + makeAccessGroupString(relationship.getAccess());

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
                        relatedSubstancePublicOrPrivate = (fullRelatedSubstance.get().getAccess().isEmpty()) ? "Public" : "Private: " + makeAccessGroupString(fullRelatedSubstance.get().getAccess());
                        relatedBdnum = getBdnum(fullRelatedSubstance.get());
                        relatedDisplayName = fullRelatedSubstance.get().getDisplayName().map(n -> n.getName()).orElse("");
                    }
                } catch (Exception e) {
                    log.warn("Problem loading fullRelatedSubstance.", e);
                }
                String relationshipCreatedBy = relationship.createdBy.username;
                Date relationshipLastEdited = relationship.getLastEdited();
                String relationshipLastEditedBy = relationship.lastEditedBy.username;
                // What if one is null/blank?

                String isReflexive = (parentUuid.equals(relatedUuid)) ? "Y" : "N";


                StringBuilder sb = new StringBuilder()
                .append(relationshipPublicOrPrivate).append("\t") // Relationship Public/Private
                .append(isReflexive).append("\t")                 // IS_REFLEXIVE
                .append(relatedUuid).append("\t")                 // RELATED_SUBSTANCE_UUID
                .append(relatedBdnum).append("\t")                // RELATED_SUBSTANCE_BDNUM
                .append(relatedApprovalId).append("\t")           // RELATED_SUBSTANCE_APPROVAL_ID
                .append(relatedSubstancePublicOrPrivate).append("\t") // Related Subst. Public/Private
                .append(relatedSubstanceType).append("\t")        // RELATED_SUBSTANCE_TYPE

                .append(relatedDisplayName).append("\t")          // RELATED_SUBSTANCE_DISPLAY_NAME
                .append(type).append("\t")                        // RELATIONSHIP_TYPE
                .append(parentDisplayName).append("\t")           // SUBJECT_DISPLAY_NAME
                .append(parentSubstanceClass).append("\t")        // SUBJECT_SUBSTANCE_TYPE
                .append(parentSubstancePublicOrPrivate).append("\t") // Subj. Subst. Public/Private
                .append(parentUuid).append("\t")                  // SUBJECT_UUID
                .append(parentBdnum).append("\t")                  // SUBJECT_BDNUM
                .append(parentUnii).append("\t")                  // SUBJECT_APPROVAL_ID

                .append(relationshipCreatedBy).append("\t")       // RELATIONSHIP_CREATED_BY
                .append(relationshipLastEdited).append("\t")      // RELATIONSHIP_LAST_EDITED
                .append(relationshipLastEditedBy).append("\t");   // RELATIONSHIP_LAST_EDITED_BY
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

    public String makeAccessGroupString(Set<Group> s) {
        return (String) s.stream().map(o->o.name).sorted().collect(Collectors.joining(", "));
    }

    @Override
    public void close() throws IOException {
        bw.close();
    }
}
