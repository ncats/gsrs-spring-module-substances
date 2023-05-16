package gsrs.module.substance.importers.model;


import ix.ginas.models.v1.SubstanceReference;
import lombok.Data;

/*
Like SubstanceReference but disconnected from the DB and a field for role
 */
@Data
public class ImportSubstanceReference {
    public ImportSubstanceReference(){
    }

    public ImportSubstanceReference(SubstanceReference reference){
        this.refuuid=reference.refuuid;
        this.refPname=reference.refPname;
        this.substanceClass=reference.substanceClass;
        this.approvalID=reference.approvalID;
    }

    public ImportSubstanceReference(SubstanceReference reference, String role){
        this.refuuid=reference.refuuid;
        this.refPname=reference.refPname;
        this.substanceClass=reference.substanceClass;
        this.approvalID=reference.approvalID;
        this.role=role;
    }

    private String refuuid;
    private String refPname;
    private String substanceClass;
    private String approvalID;
    private String role;
}
