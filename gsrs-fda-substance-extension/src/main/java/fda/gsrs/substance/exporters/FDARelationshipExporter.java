package fda.gsrs.substance.exporters;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.ginas.exporters.Exporter;
import ix.ginas.models.v1.*;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class FDARelationshipExporter implements Exporter<Substance> {

    private final BufferedWriter bw;

    private final boolean showPrivates;

    private final SubstanceRepository substanceRepository;

    public FDARelationshipExporter(SubstanceRepository substanceRepository, OutputStream os, boolean showPrivates) throws IOException{
        this.showPrivates =showPrivates;
        this.substanceRepository = substanceRepository;
        bw = new BufferedWriter(new OutputStreamWriter(os));
        bw.write("RELATIONSHIP_TYPE\tRelationship Public/Private\tIS_REFLEXIVE\tUUID\tSUBSTANCE_TYPE\tSubst. Public/Private\tUNII\tBDNUM\tDISPLAY_NAME\tRELATED_UUID\tRELATED_SUBSTANCE_TYPE\tRelated Subst. Public/Private\tRELATED_UNII\tRELATED_BDNUM\tRELATED_DISPLAY_NAME\tRELATIONSHIP_CREATED_BY\tRELATIONSHIP_LAST_EDITED\tRELATIONSHIP_LAST_EDITED_BY");
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
        if(!showPrivates && !ing.getAccess().isEmpty()){
            //Skip substances that aren't public unless we have show private data too
            return;
        }
        String bdnum = getBdnum(ing);

        Substance bestParent=getParentSubstance(ing);
        if(!showPrivates && !bestParent.getAccess().isEmpty()){
            //GSRS-699 skip substances that aren't public unless we have show private data too
            return;
        }

        // is this the right displayName?
        String ptUTF8=bestParent.getDisplayName()
        		            .map(n->n.getName())
        		            .orElse("");

        String parentUuid = bestParent.getUuid().toString();
        String parentSubstanceClass = bestParent.substanceClass.toString();
        String parentSubstancePublicOrPrivate = (bestParent.getAccess().isEmpty()) ? "Public" : "Private";
        String parentUnii = bestParent.getApprovalID();
        String parentBdnum = getBdnum(bestParent);
        String parentDisplayName = ptUTF8;

        List<Relationship> relationships = ing.relationships;
        for ( Relationship relationship : relationships) {
            String type = relationship.getDisplayType();
            String relationshipPublicOrPrivate = (relationship.getAccess().isEmpty()) ? "Public" : "Private";
            String relatedUuid = relationship.relatedSubstance.refuuid.toString();
            // Sql script used discriminator value for this? but not sure how to get that in Java.
            String relatedSubstanceType = (relationship.relatedSubstance.substanceClass==null)? "": relationship.relatedSubstance.substanceClass.toString();
            String relatedSubstancePublicOrPrivate = (relationship.relatedSubstance.getAccess().isEmpty()) ? "Public" : "Private";
            Optional<Substance> fullRelatedSubstance = substanceRepository.findById(relationship.relatedSubstance.getUuid());
            String relatedUnii = "";
            String relatedBdnum = "";
            String relatedDisplayName = "";
            relatedUnii = relationship.relatedSubstance.approvalID;
            relatedDisplayName = relationship.relatedSubstance.refPname;
            // relatedBdnum = getBdnum(fullRelatedSubstance.get());

            if(fullRelatedSubstance.isPresent()) {
                // relatedUnii = fullRelatedSubstance.get().getApprovalID();
                // relatedBdnum = getBdnum(fullRelatedSubstance.get());
                // relatedDisplayName = fullRelatedSubstance.get().getDisplayName().map(n->n.getName()).orElse("");
            }
            String relationshipCreatedBy = relationship.createdBy.username;
            Date relationshipLastEdited = relationship.getLastEdited();
            String relationshipLastEditedBy = relationship.lastEditedBy.username;
            // What if one is null/blank?
            String isReflexive = (parentUuid.equals(relatedUuid)) ? "Y" : "N";

            String str =
            type + "\t" + // RELATIONSHIP_TYPE
            relationshipPublicOrPrivate + "\t" +  // Relationship Public/Private
            isReflexive + "\t" + // IS_REFLEXIVE
            parentUuid + "\t" +   // UUID
            parentSubstanceClass + "\t" + // SUBSTANCE_TYPE
            parentSubstancePublicOrPrivate + "\t" + // Subst. Public/Private
            parentUnii + "\t" +  // UNII
            parentBdnum + "\t" +  // BDNUM
            parentDisplayName + "\t" + // DISPLAY_NAME
            relatedUuid + "\t" +  // RELATED_UUID
            relatedSubstanceType + "\t" + // RELATED_SUBSTANCE_TYPE
            relatedSubstancePublicOrPrivate + "\t" +  // Related Subst. Public/Private
            relatedUnii + "\t" +       // RELATED_UNII
            relatedBdnum + "\t" +      // RELATED_BDNUM
            relatedDisplayName + "\t" +       // RELATED_DISPLAY_NAME
            relationshipCreatedBy + "\t" +       // RELATIONSHIP_CREATED_BY
            relationshipLastEdited + "\t" +       // RELATIONSHIP_LAST_EDITED
            relationshipLastEditedBy + "\t";      // RELATIONSHIP_LAST_EDITED_BY
            bw.write(str);
            bw.newLine();


        }
    }

    @Override
    public void close() throws IOException {
        bw.close();
    }
}
