package ix.ginas.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import ix.core.models.Keyword;
import ix.ginas.models.serialization.ReferenceSetDeserializer;
import ix.ginas.models.serialization.ReferenceSetSerializer;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;

import javax.persistence.Convert;
import javax.persistence.MappedSuperclass;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;
import java.util.LinkedHashSet;
import java.util.Set;

@MappedSuperclass
//@Access(AccessType.FIELD)
public abstract class NoIdGinasCommonSubData extends NoIdGinasCommonData implements GinasAccessReferenceControlled {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	//@JsonIgnore
    @Convert(converter = EmbeddedKeywordList.Converter.class)
	private EmbeddedKeywordList internalReferences = new EmbeddedKeywordList();

    public NoIdGinasCommonSubData() {
    }
    
    @JsonProperty("references")
    @JsonSerialize(using = ReferenceSetSerializer.class)
    @Transient
    public Set<Keyword> getReferences(){
    	return new LinkedHashSet<Keyword>(internalReferences);
    }

    
    @JsonProperty("references")
    @JsonDeserialize(using = ReferenceSetDeserializer.class)
	@Override
	public void setReferences(Set<Keyword> references) {
    	this.internalReferences = new EmbeddedKeywordList(references);
	}


	@Override
	public void addReference(String refUUID){
    	//dup check
		boolean isDup = hasDupReference(refUUID);

		if(!isDup) {
			this.internalReferences.add(new Keyword(NoIdGinasCommonSubData.REFERENCE,
					refUUID
			));
			setReferences(new LinkedHashSet<>(this.internalReferences));
		}
	}

	private boolean hasDupReference(String refUUID){
		if(refUUID==null){
			return false;
		}
		//can't use java 8 here because our ebean class enhancer is java 7
		for(Keyword k : internalReferences){
			if(!NoIdGinasCommonSubData.REFERENCE.equals(k.label)){
				continue;
			}
			if(refUUID.equals(k.term)){
				return true;
			}
		}
		return false;
	}
	@Override
	public void addReference(Reference r){
		addReference(r.getOrGenerateUUID().toString());
	}
	@Override
	public void addReference(Reference r, Substance s){
		s.references.add(r);
		this.addReference(r);
	}
	
	public String toJson(){
		ObjectMapper om = new ObjectMapper();
		return om.valueToTree(this).toString();
	}
	

	/**
	 * This is needed to ensure that any pieces marked as immutable
	 * are properly re-initialized
	 */
	@PreUpdate
	public void updateImmutables(){
		this.internalReferences = new EmbeddedKeywordList(internalReferences);
	}
}