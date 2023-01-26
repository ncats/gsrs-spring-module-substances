package fda.gsrs.substance.exporters;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.EntityFetcher;
import ix.core.models.Group;
import ix.core.util.EntityUtils;
import ix.ginas.exporters.Exporter;
import ix.ginas.models.v1.*;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.stream.Collectors;

public class FDARelationshipExporter implements Exporter<Substance> {

    private final BufferedWriter bw;

    private final boolean showPrivates;

    private final SubstanceRepository substanceRepository;

    public FDARelationshipExporter(SubstanceRepository substanceRepository, OutputStream os, boolean showPrivates) throws IOException{
        // publicOnly/ShowPrivates is not longer used in favor of scrubber.
        this.showPrivates =showPrivates;
        this.substanceRepository = substanceRepository;
        bw = new BufferedWriter(new OutputStreamWriter(os));
        bw.write("Relationship Public/Private\tIS_REFLEXIVE\tSUBJECT_UUID\tSUBJECT_SUBSTANCE_TYPE\tSubj. Subst. Public/Private\tSUBJECT_APPROVAL_ID\tSUBJECT_BDNUM\tSUBJECT_DISPLAY_NAME\tRELATIONSHIP_TYPE\tRELATED_SUBSTANCE_DISPLAY_NAME\tRELATED_SUBSTANCE_UUID\tRELATED_SUBSTANCE_TYPE\tRelated Subst. Public/Private\tRELATED_SUBSTANCE_APPROVAL_ID\tRELATED_SUBSTANCE_BDNUM\tRELATIONSHIP_CREATED_BY\tRELATIONSHIP_LAST_EDITED\tRELATIONSHIP_LAST_EDITED_BY");
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
    public Substance getParentSubstance(Substance s){
    	
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

        Substance bestParent=getParentSubstance(ing);

        // is this the right displayName?
        String ptUTF8=bestParent.getDisplayName()
        		            .map(n->n.getName())
        		            .orElse("");
        String parentUuid = bestParent.getUuid().toString();
        String parentSubstanceClass = bestParent.substanceClass.toString();

        String parentSubstancePublicOrPrivate = (bestParent.getAccess().isEmpty()) ? "Public" : "Private: " + makeAccessGroupString(bestParent.getAccess());

        String parentUnii = bestParent.getApprovalID();
        String parentBdnum = getBdnum(bestParent);
        String parentDisplayName = ptUTF8;

        List<Relationship> relationships = ing.relationships;
        for ( Relationship relationship : relationships) {
            String type = relationship.type;

            String relationshipPublicOrPrivate = (relationship.getAccess().isEmpty()) ? "Public" : "Private: " + makeAccessGroupString(relationship.getAccess());

            String relatedUuid = relationship.relatedSubstance.refuuid;
            EntityUtils.Key relatedKey = relationship.relatedSubstance.getKeyForReferencedSubstance();
            Optional<Substance> fullRelatedSubstance = EntityFetcher.of(relatedKey).getIfPossible().map(o->(Substance) o);
            String relatedSubstanceType = fullRelatedSubstance.map(s->s.substanceClass.toString()).orElse("Not present");

            String relatedApprovalId = relationship.relatedSubstance.approvalID;
            String relatedSubstancePublicOrPrivate = "";
            String relatedBdnum = "";
            String relatedDisplayName = "";
            if(fullRelatedSubstance.isPresent()) {
                relatedSubstancePublicOrPrivate = (fullRelatedSubstance.get().getAccess().isEmpty()) ? "Public" : "Private: " + makeAccessGroupString(fullRelatedSubstance.get().getAccess());
                relatedBdnum = getBdnum(fullRelatedSubstance.get());
                relatedDisplayName = fullRelatedSubstance.get().getDisplayName().map(n->n.getName()).orElse("");
            }
            String relationshipCreatedBy = relationship.createdBy.username;
            Date relationshipLastEdited = relationship.getLastEdited();
            String relationshipLastEditedBy = relationship.lastEditedBy.username;
            // What if one is null/blank?
            String isReflexive = (parentUuid.equals(relatedUuid)) ? "Y" : "N";

            String str =
            relationshipPublicOrPrivate + "\t" +  // Relationship Public/Private
            isReflexive + "\t" + // IS_REFLEXIVE
            parentUuid + "\t" +   // SUBJECT_UUID
            parentSubstanceClass + "\t" + // SUBJECT_SUBSTANCE_TYPE
            parentSubstancePublicOrPrivate + "\t" + // Subj. Subst. Public/Private (note this is checked)
            parentUnii + "\t" +  // SUBJECT_APPROVAL_ID
            parentBdnum + "\t" +  // SUBJECT_BDNUM
            parentDisplayName + "\t" + // SUBJECT_DISPLAY_NAME
            type + "\t" + // RELATIONSHIP_TYPE
            relatedDisplayName + "\t" +       // RELATED_SUBSTANCE_DISPLAY_NAME
            relatedUuid + "\t" +  // RELATED_SUBSTANCE_UUID
            relatedSubstanceType + "\t" + // RELATED_SUBSTANCE_TYPE
            relatedSubstancePublicOrPrivate + "\t" +  // Related Subst. Public/Private
            relatedApprovalId + "\t" +       // RELATED_SUBSTANCE_APPROVAL_ID
            relatedBdnum + "\t" +      // RELATED_SUBSTANCE_BDNUM
            relationshipCreatedBy + "\t" +       // RELATIONSHIP_CREATED_BY
            relationshipLastEdited + "\t" +       // RELATIONSHIP_LAST_EDITED
            relationshipLastEditedBy + "\t";      // RELATIONSHIP_LAST_EDITED_BY
            bw.write(str);
            bw.newLine();
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
