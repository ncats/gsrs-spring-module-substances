package ix.ginas.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.core.models.Group;
import ix.core.models.Keyword;
import ix.ginas.models.serialization.GroupSerializer;
import jakarta.persistence.MappedSuperclass;
import tools.jackson.databind.annotation.JsonSerialize;

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
	 	

	
}
