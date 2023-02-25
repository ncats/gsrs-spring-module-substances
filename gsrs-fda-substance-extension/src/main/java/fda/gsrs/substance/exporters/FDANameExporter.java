package fda.gsrs.substance.exporters;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.ginas.exporters.Exporter;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import org.springframework.transaction.annotation.Transactional;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Created by VenkataSaiRa.Chavali on 3/10/2017.
 */
public class FDANameExporter implements Exporter<Substance> {

    private final BufferedWriter bw;

    private final boolean includeBdnum;

    private final SubstanceRepository substanceRepository;

    public FDANameExporter(SubstanceRepository substanceRepository, OutputStream os, boolean includeBdnum) throws IOException{

        this.includeBdnum = includeBdnum;
        this.substanceRepository = substanceRepository;
        bw = new BufferedWriter(new OutputStreamWriter(os));
        StringBuilder sb = new StringBuilder();

        sb.append("NAME_ID").append("\t")
        .append("OWNER_UUID").append("\t")
        .append("TYPE").append("\t")
        .append("Name").append("\t")
        .append("UTF8_Name").append("\t")
        .append("Public or Private").append("\t")
        .append("This is a").append("\t")
        .append("APPROVAL_ID").append("\t");
        if(includeBdnum){
            sb.append("BDNUM").append("\t");
        }
        sb.append("DISPLAY_NAME").append("\t")
        .append("UTF8_DISPLAY_NAME").append("\t");
        if(includeBdnum){
            sb.append("PARENT_BDNUM").append("\t");
        }
        sb.append("PARENT_DISPLAY_NAME").append("\t")
        .append("UTF8_PARENT_DISPLAY_NAME");
        bw.write(sb.toString());
        bw.newLine();
    }

    /**
     * Get the "best" form of the substance. This means that if the
     * substance is a variant sub-concept, return the priority
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
    			      .filter(cd->cd.codeSystem.equals("BDNUM")&&cd.type.equals("PRIMARY"))
    			      .map(cd->cd.code)
    			      .findFirst()
    			      .orElse(null);	
    }
    
    @Override
    @Transactional(readOnly = true)
    public void export(Substance ing) throws IOException {
        // code previously did this
        // return if if(!showPrivates && !ing.getAccess().isEmpty())

        String bdnum = getBdnum(ing);
        /*
         * 1. ApprovalID of parent if subconcept
         * 2. 
         */
        
        Substance bestParent=getParentSubstance(ing);

        // code previously did this
        // return if(!showPrivates && !bestParent.getAccess().isEmpty())

        String approvalID = bestParent.getApprovalID();

        String parentBdnum = getBdnum(bestParent);

        String pt=bestParent.getDisplayName()
        		            .map(n->n.stdName)
        		            .orElse("");
        
        String ptUTF8=bestParent.getDisplayName()
        		            .map(n->n.getName())
        		            .orElse("");
        
        String ipt=ing.getDisplayName()
        		            .map(n->n.stdName)
        		            .orElse("");
        
        String iptUTF8=ing.getDisplayName()
        		            .map(n->n.getName())
        		            .orElse("");
        
        
        for ( Name n :ing.getAllNames()){

            String publicOrPrivate = (n.getAccess().isEmpty())
                ? "Public" : "Private: " + ExporterUtilities.makeAccessGroupString(n.getAccess());

            // code previously did this
            // continue if(!n.getAccess().isEmpty() && !showPrivates)

            String thisIsA  =  ing.isSubstanceVariant()? "SUB_CONCEPT->SUBSTANCE" : "parent/substance";

            StringBuilder sb = new StringBuilder();
            sb.append(n.uuid).append("\t")              // NAME_ID
            .append(bestParent.uuid).append("\t")       // OWNER_UUID
            .append(n.type).append("\t")                // TYPE
            .append(n.stdName).append("\t")             // Name
            .append(n.name).append("\t")                // UTF8_Name
            .append(publicOrPrivate).append("\t")       // Public or Private
            .append(thisIsA).append("\t")               // This is a
            .append(approvalID).append("\t");           // APPROVAL_ID
            if(includeBdnum){
                sb.append(bdnum).append("\t");          // BDNUM
            }
            sb.append(ipt).append("\t")                 // DISPLAY_NAME
            .append(iptUTF8).append("\t");              // UTF8_DISPLAY_NAME
            if(includeBdnum){
                sb.append(parentBdnum).append("\t");    // PARENT_BDNUM
            }
            sb.append(pt).append("\t")                  // PARENT_DISPLAY_NAME
            .append(ptUTF8);                            // UTF8_PARENT_DISPLAY_NAME

            bw.write(sb.toString());
            bw.newLine();
        }
    }

    @Override
    public void close() throws IOException {
        bw.close();
    }
}
