package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.core.models.DynamicFacet;
import ix.core.models.Indexable;
import ix.core.models.IndexableRoot;
import ix.core.models.ParentReference;
import ix.ginas.models.CommonDataElementOfCollection;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.utils.JSONConstants;
import ix.ginas.models.utils.JSONEntity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@JSONEntity(title = "Code", isFinal = true)
@Entity
@Table(name="ix_ginas_code")
@DynamicFacet(label="codeSystem", value="code")
@IndexableRoot
public class Code extends CommonDataElementOfCollection{
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JsonIgnore
	@ParentReference
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

    @JSONEntity(title = "Code system", format = JSONConstants.CV_CODE_SYSTEM)
    @Indexable(facet=true, name="Code System")
    public String codeSystem;
    
    @JSONEntity(title = "Code", isRequired = true)
    @Column(nullable=false)
    @Indexable(name="Code", suggest=true)
    public String code;
    
    public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}

	@JSONEntity(title = "Code Comments")
    @Lob
    @Basic(fetch= FetchType.EAGER)
	@Indexable(pathsep="//|")
    public String comments;

    @JSONEntity(title = "Code Text")
    @Lob
    @Basic(fetch= FetchType.EAGER)
    public String codeText;
    
    @JSONEntity(title = "Code Type", format = JSONConstants.CV_CODE_TYPE)
    public String type ="PRIMARY";
    
    @JSONEntity(title = "Code URL", format = "uri")
    @Lob
    @Basic(fetch= FetchType.EAGER)
    public String url;
    
    
    public Code () {}
    

    public Code(String codeSystem, String code) {
		this.codeSystem=codeSystem;
		this.code=code;
	}
	public String toString(){
		return "Code[" + this.codeSystem + "]:\"" +this.getCode()  +"\"";
	}
	
	private static Pattern splitPattern=Pattern.compile("\\|");
	
	@JsonIgnore
	public String getDisplayCode(){
		if(this.codeText!=null){
			String[] parts=splitPattern.split(this.codeText);
			return parts[parts.length-1];
		}else if(this.comments!=null){
			String[] parts=splitPattern.split(this.comments);
			return parts[parts.length-1];
		}else{
			return this.code;
		}
	}
    
	@JsonIgnore
	public boolean hasSpecialDisplay(){
		if(this.codeText!=null ||this.comments!=null){
			return true;
		}
		return false;
	}
	
	@JsonIgnore
	public boolean isClassification(){
		if(this.codeText!=null){
			if(this.codeText.contains("|"))return true;
		}
		if(this.comments!=null){
			if(this.comments.contains("|"))return true;
		}
		return false;
	}
	@Override
	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
		return new ArrayList<>();
	}

    
}
