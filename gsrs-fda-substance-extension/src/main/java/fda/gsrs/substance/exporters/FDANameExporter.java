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

    private final boolean showPrivates;

    private final SubstanceRepository substanceRepository;

    public FDANameExporter(SubstanceRepository substanceRepository, OutputStream os, boolean showPrivates) throws IOException{
        this.showPrivates =showPrivates;
        this.substanceRepository = substanceRepository;
        bw = new BufferedWriter(new OutputStreamWriter(os));
        bw.write("NAME_ID\tOWNER_UUID\tTYPE\tName\tPublic or Private\tThis is a\tUNII\tBDNUM\tDISPLAY_TERM\tPARENT_BDUM\tPARENT_DISPLAY_TERM");
//        bw.write("BDNUM\tName\tType\tIs Public\tPriority BDNUM\tUNII\tDisplay Name");
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
            //GSRS-699 skip substances that aren't public unless we have show private data too
            return;
        }
        String bdnum = getBdnum(ing);
        /*
         * 1. ApprovalID of parent if subconcept
         * 2. 
         */
        
        Substance bestParent=getParentSubstance(ing);
        if(!showPrivates && !bestParent.getAccess().isEmpty()){
            //GSRS-699 skip substances that aren't public unless we have show private data too
            return;
        }
        String approvalID = bestParent.getApprovalID();
        String parentBdnum = getBdnum(bestParent);
        
        String pt=bestParent.getDisplayName()
        		            .map(n->n.getName())
        		            .orElse("");
        
        
        for ( Name n :ing.getAllNames()){
            boolean isPublic = n.getAccess().isEmpty();
            boolean isPrivate = !isPublic;
            
            if(isPrivate && !showPrivates){
                continue;
            }
            String thisIsA  =  ing.isSubstanceVariant()? "SUB_CONCEPT->SUBSTANCE" : "parent/substance";

            String str = n.uuid +"\t" +bestParent.uuid +"\t" + n.type+"\t"
            		   + n.name + "\t" 

                       + (isPublic?"Public":"Private") + "\t"
                    + thisIsA +"\t"//this is a ? parent/substance ? what goes here
                    + approvalID + "\t" + bdnum + "\t"
                    +ing.getDisplayName()
                    .map(name->name.getName())
                    .orElse("") +"\t"
                       + parentBdnum+"\t"

                       + pt;
            bw.write(str);
            bw.newLine();
        }
    }

    @Override
    public void close() throws IOException {
        bw.close();
    }
}
