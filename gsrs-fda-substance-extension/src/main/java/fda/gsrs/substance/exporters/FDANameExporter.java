package fda.gsrs.substance.exporters;
import com.fasterxml.jackson.databind.JsonNode;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.ginas.exporters.Exporter;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import ix.ginas.exporters.ExporterFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

@Slf4j
public class FDANameExporter implements Exporter<Substance> {

    private final BufferedWriter bw;
    private final SubstanceRepository substanceRepository;
    private final String primaryCodeSystem;

    private ExporterFactory.Parameters params;
    private boolean showPrimaryCodeSystem;

    public FDANameExporter(SubstanceRepository substanceRepository, OutputStream os, ExporterFactory.Parameters params, String primaryCodeSystem) throws IOException{

        this.substanceRepository = substanceRepository;
        this.primaryCodeSystem = primaryCodeSystem;
        this.params = params;
        JsonNode detailedParameters = params.detailedParameters();

        showPrimaryCodeSystem = (detailedParameters!=null
            && detailedParameters.hasNonNull(FDANameExporterFactory.PRIMARY_CODE_SYSTEM_PARAMETERS)
            && detailedParameters.get(FDANameExporterFactory.PRIMARY_CODE_SYSTEM_PARAMETERS).booleanValue());

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
        if(showPrimaryCodeSystem && primaryCodeSystem!=null){
            sb.append(primaryCodeSystem).append("\t");
        }
        sb.append("DISPLAY_NAME").append("\t")
        .append("UTF8_DISPLAY_NAME").append("\t");
        if(showPrimaryCodeSystem && primaryCodeSystem!=null){
            sb.append("PARENT_"+primaryCodeSystem).append("\t");
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
                cd.code="UNKNOWN "+primaryCodeSystem;
                cd.codeSystem=primaryCodeSystem;

                fake.codes.add(cd);

                return fake;
            }
            return parent;
        }
        return s;
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
        // code previously did this
        // return if if(!showPrivates && !ing.getAccess().isEmpty())

        String primaryCodeSystemCode = getPrimaryCodeSystemCode(ing);

        Substance bestParent=getParentSubstance(ing);

        // code previously did this
        // return if(!showPrivates && !bestParent.getAccess().isEmpty())

        String approvalID = bestParent.getApprovalID();

        String parentPrimaryCodeSystemCode = getPrimaryCodeSystemCode(bestParent);

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
            if(showPrimaryCodeSystem && primaryCodeSystem!=null){
                sb.append(primaryCodeSystemCode).append("\t");          // primaryCodeSystemCode
            }
            sb.append(ipt).append("\t")                 // DISPLAY_NAME
            .append(iptUTF8).append("\t");              // UTF8_DISPLAY_NAME
            if(showPrimaryCodeSystem && primaryCodeSystem!=null){
                sb.append(parentPrimaryCodeSystemCode).append("\t");    // parentPrimaryCodeSystemCode
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
