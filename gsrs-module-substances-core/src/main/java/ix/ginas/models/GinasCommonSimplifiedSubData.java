package ix.ginas.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import ix.core.models.Group;
import ix.core.models.Indexable;
import ix.core.models.Keyword;
import ix.core.models.Principal;
import ix.ginas.models.serialization.GroupSerializer;
import ix.ginas.models.serialization.PrincipalDeserializer;

import jakarta.persistence.MappedSuperclass;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("serial")
@MappedSuperclass
public abstract class GinasCommonSimplifiedSubData extends GinasCommonSubData implements GinasAccessReferenceControlled {
	 	@JsonIgnore
	    public Set<Keyword> getReferences(){
	    	return super.getReferences();
	    }
	 	
	 	@JsonIgnore
	 	public UUID getUuid() {
			return super.getUuid();
		}



	 	@JsonIgnore
		public Date getLastEdited() {
			return super.getLastEdited();
		}


	 	@JsonIgnore
		public boolean isDeprecated() {
			return super.isDeprecated();
		}

	 	@JsonIgnore
		public Date getCreated() {
			return super.getCreated();
		}
	 	
	 	
	 	 @JsonIgnore
	 	 @JsonSerialize(contentUsing = GroupSerializer.class)
	     public Set<Group> getAccess(){
	     	return super.getAccess();
	     }
	 	
//	 	@JsonProperty("_self")
//	 	@JsonIgnore
//	    @Indexable(indexed=false)
//	    public String getself () {
//	 		return super.getself();
//	    }
	 	
	 	
	 	
	
}
