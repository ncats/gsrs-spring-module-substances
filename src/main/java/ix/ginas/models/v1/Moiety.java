package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gov.nih.ncats.molwitch.Chemical;
import ix.core.models.Indexable;
import ix.ginas.models.CommonDataElementOfCollection;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.NoIdGinasCommonData;
import ix.ginas.models.NoIdGinasCommonSubData;
import ix.ginas.models.serialization.MoietyDeserializer;
import ix.ginas.models.utils.JSONEntity;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@JsonDeserialize(using = MoietyDeserializer.class)
@JSONEntity(name = "moiety", title = "Moiety")
@Entity
@Table(name = "ix_ginas_moiety")
//@JsonIgnoreProperties({ "id" })
public class Moiety extends NoIdGinasCommonSubData implements Comparable<Moiety>{
	public static String JSON_NULL="JSON_NULL";
	/**
	 * The UUID of this moiety
	 */
	@Column(unique = true)
	public UUID uuid;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JsonIgnore
    private ChemicalSubstance owner;

    public ChemicalSubstance getOwner(){
        return this.owner;
    }

    public void setOwner(ChemicalSubstance owner) {
        this.owner = owner;
    }

    public void assignOwner(ChemicalSubstance own){
        this.owner=own;
    }

    @OneToOne(cascade= CascadeType.ALL)
    @JsonUnwrapped //TODO: Probably not covered well
                   // by some other tools
    public GinasChemicalStructure structure;
    
    @JSONEntity(title = "Count")
    @OneToOne(cascade= CascadeType.ALL)
    private Amount count;

    public Moiety () {}

	/**
	 * This is actually the primary key in this table
	 * the innerUuid is actually the Structure's id.
	 */
	@Column(unique=true)
    @JsonIgnore
	@Id
    public String innerUuid;

    @PrePersist
    @PreUpdate
    public void enforce(){
    	if(structure.id==null){
    		structure.id=UUID.randomUUID();
    	}
    	this.innerUuid=structure.id.toString();
    	if(uuid==null){
    		uuid= UUID.randomUUID();
		}
    }
	public String fetchGlobalId() {
		return this.uuid == null ? null : this.uuid.toString();
	}
	/**
	 * Set the Count {@link Amount} only if the count
	 * is not already set.  If you really want o
	 * set the count even if it is already set
	 * use {@link #setCount(Integer, boolean)}
	 * with {@code force = true}.
	 * This will make a new {@link Amount} object.
	 * @param i the average MOL_RATIO to use.
	 *
	 * @apiNote This is the same as {@link #setCount(Integer, boolean) setCount(i, false)}.
	 */
	public void setCount(Integer i){
		//GSRS 1627 - if the json has both a `count` and `amountCount` set
		//then depending on the order Jackson deserializes we might call setCount()
		//after the count was set to a real Amount.
		//this hack lets us ignore that 2nd call.
		setCount(i, false);
	}

	/**
	 * Set the Count to a new {@link Amount}
	 * object with the given integer value
	 * as the average MOL RATIO.  The Amount uuid
	 * is not set.
	 * If not forced, then the count will not
	 * override a pre-exisitng count.
	 * @param i the average MOL_RATIO to use.
	 * @param force force the count even
	 *              if {@link #getCount()} returns non-null value.
	 */
	public void setCount(Integer i, boolean force){
		if(force || count ==null) {
			count = intToAmount(i);
		}
	}
    public Integer getCount(){
    	if(count==null)return null;
    	if(count.average == null)return null;
    	return (int) count.average.longValue();
    }
    public static Amount intToAmount(Integer i){
    	Amount count=new Amount();
    	if(i!=null){
    		count.average=i.doubleValue();
    	}
    	count.type="MOL RATIO";
    	count.units="MOL RATIO";
    	return count;
    }

	public void setCountAmount(Amount amnt) {
		count=amnt;	
	}
	@Indexable()
	public Amount getCountAmount() {
		return count;
	}
	
	

	public UUID getUUID(){
		if(this.innerUuid!=null){
			return UUID.fromString(this.innerUuid);
		}else{
			return null;
		}
	}
	
//	@Override
//	public void forceUpdate() {
//		structure.forceUpdate();
//		super.forceUpdate();
//	}
	
	@Override
	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();
		temp.addAll(structure.getAllChildrenAndSelfCapableOfHavingReferences());
		if(count!=null){
			temp.addAll(count.getAllChildrenAndSelfCapableOfHavingReferences());
		}
		return temp;
	}

	@Override
	public int compareTo(Moiety o) {
		if(innerUuid ==null){
			if(o.innerUuid ==null) {
				return 0;
			}
			return 1;
		}
		if(o.innerUuid ==null){
			return -1;
		}
		return innerUuid.compareTo(o.innerUuid);
	}
}