package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.core.SingleParent;
import ix.core.models.ParentReference;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;
import ix.ginas.models.utils.JSONConstants;
import ix.ginas.models.utils.JSONEntity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name="ix_ginas_nameorg", indexes = {@Index(name = "nameorg_owner_index", columnList = "owner_uuid")})
@JSONEntity(name = "nameOrg", title = "Naming Org", isFinal = true)
@SingleParent
public class NameOrg extends GinasCommonSubData {
	
	@ManyToOne(cascade = CascadeType.PERSIST)
    @ParentReference
	private Name owner;
	
	
    @JSONEntity(title = "Naming Organization", format = JSONConstants.CV_NAME_ORG, isRequired = true)
    @Column(nullable=false)
    public String nameOrg;
    
    // TODO Is this the only thing that can reasonably be deprecated?!!
    @JSONEntity(title = "Deprecated")
    public boolean deprecated;
    
    @JSONEntity(title = "Deprecated Date", format = "date")
    public Date deprecatedDate;

    public NameOrg () {}
    public NameOrg (String nameOrg) {
        this.nameOrg = nameOrg;
    }
	@Override
   	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
		return new ArrayList<>();
	}
}
