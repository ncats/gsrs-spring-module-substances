package ix.ginas.models;

import ix.core.SingleParent;
import ix.ginas.models.v1.Substance;

import javax.persistence.CascadeType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

/**
 * This abstract class is meant as a convenience tool to allow ownership for 
 * simple @OneToMany annotations.
 * 
 * @author Tyler Peryea
 *
 */
@MappedSuperclass
@SingleParent
public abstract class CommonDataElementOfCollection extends GinasCommonSubData{
	@ManyToOne(cascade = CascadeType.PERSIST)
	private Substance owner;

	public Substance getOwner(){
		return this.owner;
	}
	public void setOwner(Substance s){
		this.owner = s;
	}
	public Substance fetchOwner(){
		return this.owner;
	}
	
	public void assignOwner(Substance own){
		this.owner=own;
	}
}
