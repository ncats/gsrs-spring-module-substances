package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;
import ix.ginas.models.utils.JSONConstants;
import ix.ginas.models.utils.JSONEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@JSONEntity(name = "amount", title = "Amount", isFinal = true)
@Entity
@Table(name="ix_ginas_amount")
public class Amount extends GinasCommonSubData {
    @JSONEntity(title = "Amount Type", format = JSONConstants.CV_AMOUNT_TYPE, isRequired = true)
    public String type;
    
    @JSONEntity(title = "Average")
    public Double average;
    
    @JSONEntity(title = "High Limit")
    public Double highLimit;
    
    @JSONEntity(title = "High")
    public Double high;
    
    @JSONEntity(title = "Low Limit")
    public Double lowLimit;
    
    @JSONEntity(title = "Low")
    public Double low;
    
    @JSONEntity(title = "Units", format = JSONConstants.CV_AMOUNT_UNIT)
    public String units;
    
    @JSONEntity(title = "Non-numeric Value")
    public String nonNumericValue;
    
    @JSONEntity(title = "Referenced Material")
    @Column(name= "approval_id", length=10)
    public String approvalID;

    public Amount () {}
    
    public String toString(){
    	String val="";
    	if(highLimit!=null && lowLimit==null && average==null){
    		val="<"+highLimit;
    	}else if(highLimit==null && lowLimit==null && average!=null){
    		val=average+"";
    	}else if(highLimit==null && lowLimit!=null && average==null){
    		val=">" + lowLimit;
    	}else if(highLimit!=null && lowLimit!=null && average!=null){
    		val=average + "[" + lowLimit + " to " + highLimit + "]";
    	}else if(highLimit!=null && lowLimit==null && average!=null){
    		val=average + "[<" + highLimit + "]";
    	}else if(highLimit==null && lowLimit!=null && average!=null){
    		val=average + "[>" + lowLimit + "]";
    	}
    	if(nonNumericValue!=null){
    		val+=" { " + nonNumericValue + " }";
    		val=val.trim();
    	}
    	if(units!=null){
    		val+= " (" + units + ")";
    		val = val.trim();
    	}
    	if(val.trim().length()<=0){
    		val="<i>empty value</i>";
    	}
    	
    	return val; 
    }
    
	@Override
	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();
		return temp;
	}

}
