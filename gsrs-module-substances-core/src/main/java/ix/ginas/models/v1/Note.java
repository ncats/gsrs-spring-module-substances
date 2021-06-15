package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gsrs.module.substance.SubstanceOwnerReference;
import ix.ginas.models.CommonDataElementOfCollection;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.utils.JSONEntity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


@JSONEntity(title = "Note", isFinal = true)
@Entity
@Table(name="ix_ginas_note")
public class Note extends CommonDataElementOfCollection {
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JsonIgnore
    @SubstanceOwnerReference
    private Substance owner;

    public Substance fetchOwner(){
        return this.owner;
    }
    public Substance getOwner(){
        return this.owner;
    }

    public void setOwner(Substance owner) {
        this.owner = owner;
    }

    public void assignOwner(Substance own){
        this.owner=own;
    }

    @JSONEntity(title = "Note")
    @Lob
    @Basic(fetch= FetchType.EAGER)
    public String note;

    public Note () {}
    public Note (String note) {
        this.note = note;
    }
	@Override
	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();

		return temp;
	}

    @Override
    public String toString() {
        return "Note{" +
                "note='" + note + '\'' +
                '}';
    }
}
