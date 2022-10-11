package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.core.SingleParent;
import ix.core.models.Indexable;
import ix.core.models.ParentReference;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;
import ix.ginas.models.utils.JSONEntity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ix_ginas_parameter", indexes = {@Index(name = "parameter_owner_index", columnList = "owner_uuid")})
@JSONEntity(title = "Parameter", isFinal = true)
@SingleParent
public class Parameter extends GinasCommonSubData {
	@ManyToOne(cascade = CascadeType.PERSIST)
    @ParentReference
	private Property owner;
	
	//TP: added 05-19-2016
    //Needed for some properties
	@OneToOne(cascade= CascadeType.ALL)
	public SubstanceReference referencedSubstance;
	
    @JSONEntity(title = "Parameter Name", isRequired = true)
    @Column(nullable=false)
    public String name;
    
    @JSONEntity(title = "Parameter Type", values = "JSONConstants.ENUM_PROPERTY_TYPE", isRequired = true)
    public String type;

    @Indexable(name="type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JSONEntity(title = "Parameter Value")
    @OneToOne(cascade= CascadeType.ALL)
    public Amount value;

    public Parameter () {}

    
    @Indexable(name="name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Indexable(name="value")
    public Amount getValue() {
        return value;
    }

    public void setValue(Amount value) {
        this.value = value;
    }

    public int hashCode(){
    	return (this.name+"").hashCode();
    }

    public boolean equals(Object o){
		if(!super.equals(o))return false;
    	if(o==null)return false;
    	if(o instanceof Parameter){
    		Parameter p =(Parameter)o;
    		if(!p.name.equals(this.name)){
    			return false;
    		}
    		return true;
    	}else{
    		return false;
    	}
    }
    
    public String toString(){
    	return "Property Parameter=(" + getUuid() + ")  [" +  name + "]";
    }

    @Override
   	@JsonIgnore
   	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
   		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();
   		if(this.referencedSubstance!=null){
   				temp.addAll(referencedSubstance.getAllChildrenAndSelfCapableOfHavingReferences());
   		}
   		if(this.value!=null){
				temp.addAll(value.getAllChildrenAndSelfCapableOfHavingReferences());
		}
   		return temp;
   	}
}
