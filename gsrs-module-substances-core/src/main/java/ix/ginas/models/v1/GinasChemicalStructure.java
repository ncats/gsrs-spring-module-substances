package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import ix.core.chem.Chem;
import ix.core.controllers.EntityFactory;
import ix.core.controllers.EntityFactory.EntityMapper;
import ix.core.models.*;
import ix.ginas.converters.GinasAccessConverter;
import ix.ginas.models.*;
import ix.ginas.models.serialization.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;

import jakarta.persistence.*;
import java.util.*;

@Entity
@DiscriminatorValue("GSRS")
public class GinasChemicalStructure extends Structure implements GinasAccessReferenceControlled {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;


    private EmbeddedKeywordList internalReferences = new EmbeddedKeywordList();
	
	@CreatedDate
    public Date created=null;
    
    @ManyToOne()
    @JsonSerialize(using = PrincipalSerializer.class)
    @JsonDeserialize(using = PrincipalDeserializer.class)
    @Indexable(facet = true, name = "Created By", recurse=false)
	@CreatedBy
    public Principal createdBy;
    
    @ManyToOne()
    @JsonSerialize(using = PrincipalSerializer.class)
    @JsonDeserialize(using = PrincipalDeserializer.class)
    @Indexable(facet = true, name = "Last Edited By", recurse=false)
	@LastModifiedBy
    public Principal lastEditedBy;
	
	public GinasChemicalStructure(){
		
	}
	
	public GinasChemicalStructure(Structure s){
		this.atropisomerism=s.atropisomerism;
		this.charge=s.charge;
		this.count=s.count;
		this.definedStereo=s.definedStereo;
		this.deprecated=s.deprecated;
		this.digest=s.digest;
		this.ezCenters=s.ezCenters;
		this.formula=s.formula;
		this.id=s.id;
		this.lastEdited=s.lastEdited;
		this.links=s.links;
		this.molfile=s.molfile;
		this.mwt=s.mwt;
		this.opticalActivity=s.opticalActivity;
		this.properties=s.properties;
		this.smiles=s.smiles;
		this.stereoCenters=s.stereoCenters;
		this.stereoComments=s.stereoComments;
		this.stereoChemistry=s.stereoChemistry;
		this.version=s.version;
		Chem.setFormula(this);
	}
	
	
	
	
	
	
	@JsonIgnore
//	@OneToOne(cascade = CascadeType.ALL)
	@Basic(fetch= FetchType.LAZY)
	@Convert(converter= GinasAccessConverter.class)
    GinasAccessContainer recordAccess;


    @JsonIgnore
    public GinasAccessContainer getRecordAccess() {
    	return recordAccess;
    }

    @JsonIgnore
    public void setRecordAccess(GinasAccessContainer recordAccess) {
        this.recordAccess = new GinasAccessContainer(this);
        if(recordAccess!=null){
        this.recordAccess.setAccess(recordAccess.getAccess());
        }
    }

    @JsonProperty("access")
    @JsonDeserialize(contentUsing = GroupDeserializer.class)
    public void setAccess(Set<Group> access){
    	GinasAccessContainer recordAccess=this.getRecordAccess();
    	if(recordAccess==null){
    		recordAccess=new GinasAccessContainer(this);
    	}
    	recordAccess.setAccess(access);
		setRecordAccess(recordAccess);
    }
    
    @JsonProperty("access")
    @JsonSerialize(contentUsing = GroupSerializer.class)
    public Set<Group> getAccess(){
    	GinasAccessContainer gac=getRecordAccess();
    	if(gac!=null){
    		return gac.getAccess();
    	}
    	return new LinkedHashSet<Group>();
    }

	@JsonIgnore
    public Set<String> getAccessString(){
    	Set<String> keyset=new LinkedHashSet<String>();
    	for(Group k:getAccess()){
    		keyset.add(k.name);
    	}
    	return keyset;
    }

	public void addRestrictGroup(Group p){
		GinasAccessContainer gac=this.getRecordAccess();
		if(gac==null){
			gac=new GinasAccessContainer(this);
		}
		gac.add(p);
		this.setRecordAccess(gac);
	}
	//TODO katzelda Feb 2021 move call to group repository to controller level?
//	public void addRestrictGroup(String group){
//		addRestrictGroup(AdminFactory.registerGroupIfAbsent(new Group(group)));
//	}


    public void setId(UUID uuid){
		if(this.id == null) {
			this.id = uuid;
		}
    }
    
    
    @JsonSerialize(using = ReferenceSetSerializer.class)
    public Set<Keyword> getReferences(){
    	return new LinkedHashSet<Keyword>(internalReferences);
    }

    @JsonProperty("references")
    @JsonDeserialize(using = ReferenceSetDeserializer.class)
	@Override
	public void setReferences(Set<Keyword> references) {
    	this.internalReferences = new EmbeddedKeywordList(references);
	}
    
    public void addReference(String refUUID){
		this.internalReferences.add(new Keyword(GinasCommonSubData.REFERENCE,
				refUUID
		));
		setReferences(new LinkedHashSet<Keyword>(this.internalReferences));
	}
	
	public void addReference(Reference r){
		addReference(r.getOrGenerateUUID().toString());
	}
	@Override
	public void addReference(Reference r, Substance s){
		s.references.add(r);
		this.addReference(r);
	}
	


    
    //There's an issue here.
	public GinasChemicalStructure copy() throws Exception {
		EntityMapper em= EntityFactory.EntityMapper.FULL_ENTITY_MAPPER();
		JsonNode jsn=em.valueToTree(this);
		GinasChemicalStructure gcs=em.treeToValue(jsn, GinasChemicalStructure.class);
		gcs.id=null;
		return gcs;
	}
	public String toString(){
		return "Structure Definition";
	}
	//TODO katzelda Feb 2021: this doesn't appear to be used in GSRS 2.x
//	@JsonIgnore
//	public GinasCommonData asAuditInfo(){
//		return Util.asAuditInfo(this);
//	}
	
	@Override
	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
		return new ArrayList<GinasAccessReferenceControlled>();
	}


	
}
