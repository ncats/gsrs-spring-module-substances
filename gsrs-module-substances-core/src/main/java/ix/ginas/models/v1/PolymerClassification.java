package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import ix.core.models.Indexable;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;
import ix.ginas.models.serialization.KeywordDeserializer;
import ix.ginas.models.serialization.KeywordListSerializer;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="polymer_classification")
public class PolymerClassification extends GinasCommonSubData {
    @Indexable(facet=true,name="Polymer Class")
    public String polymerClass;
    @Indexable(facet=true,name="Polymer Geometry")
    public String polymerGeometry;
    
    @JsonSerialize(using= KeywordListSerializer.class)
    @JsonDeserialize(contentUsing = KeywordDeserializer.SubClassDeserializer.class)
    @Basic(fetch= FetchType.LAZY)
    public EmbeddedKeywordList polymerSubclass = new EmbeddedKeywordList();
    
    
    public String sourceType;
    
    @OneToOne(cascade= CascadeType.ALL)
    public SubstanceReference parentSubstance;

    public PolymerClassification () {}
    
    
    @PreUpdate
	public void updateImmutables(){
		super.updateImmutables();
		this.polymerSubclass= new EmbeddedKeywordList(this.polymerSubclass);
	}

    @Override
   	@JsonIgnore
   	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
   		List<GinasAccessReferenceControlled> temp = new ArrayList<GinasAccessReferenceControlled>();
   		if(this.parentSubstance!=null){
   				temp.addAll(parentSubstance.getAllChildrenAndSelfCapableOfHavingReferences());
   		}
   		return temp;
   	}
}
