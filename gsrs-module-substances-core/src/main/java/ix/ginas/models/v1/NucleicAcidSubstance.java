package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gsrs.module.substance.services.SubstanceSequenceFileSupportService;
import gsrs.sequence.SequenceFileSupport;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.models.SequenceEntity;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasSubstanceDefinitionAccess;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.*;
import java.util.List;
import java.util.stream.Stream;

@Entity
@Inheritance
@DiscriminatorValue("NA")
@Slf4j
//@JSONEntity(name = "nucleicAcidSubstance", title = "Nucleic Acid Substance")
public class NucleicAcidSubstance extends Substance implements GinasSubstanceDefinitionAccess, SequenceFileSupport {
	@OneToOne(cascade= CascadeType.ALL)
	public NucleicAcid nucleicAcid;

	
	public NucleicAcidSubstance(){
    	super(SubstanceClass.nucleicAcid);
	}
	
	
	@Override
    public Modifications getModifications(){
		if(this.nucleicAcid ==null){
			return null;
		}
    	return this.nucleicAcid.getModifications();
    }
    
	
	
    
    @Transient
    private boolean _dirtyModifications=false;
    
    
    public void setModifications(Modifications m){
    	if(this.nucleicAcid==null){
    		this.nucleicAcid = new NucleicAcid();
    		_dirtyModifications=true;
    	}
    	this.nucleicAcid.setModifications(m);
    	this.modifications=m;
    }
    
    public void setNucleicAcid(NucleicAcid p){
    	this.nucleicAcid=p;
    	if(_dirtyModifications){
    		this.nucleicAcid.setModifications(this.modifications);
    		_dirtyModifications=false;
    	}
    }

	//TODO katzelda Feb 2021: delete handled in controller
//    @Override
//    public void delete(){
//    	Modifications old=this.modifications;
//    	this.modifications=null;
//    	super.delete();
//    	for(Subunit su:this.nucleicAcid.subunits){
//    		su.delete();
//    	}
//    	if(old!=null){
//    		old.delete();
//    	}
//    }
    
    public int getTotalSites(boolean includeEnds){
    	int tot=0;
    	for(Subunit s:this.nucleicAcid.getSubunits()){
    		tot+=s.getLength();
    		if(!includeEnds){
    			tot--;
    		}
    	}
    	return tot;
    }

	@JsonIgnore
	public GinasAccessReferenceControlled getDefinitionElement(){
		return nucleicAcid;
	}
    
//	public NucleicAcid getNucleicAcid() {
//		return nucleicAcid;
//	}
//
//	public void setNucleicAcid(NucleicAcid nucleicAcid) {
//		this.nucleicAcid = nucleicAcid;
//	}
	
	
/*
	public void setFromMap(Map m) {
		super.setFromMap(m);
		nucleicAcid = toDataHolder(
				m.get("nucleicAcid"),
				new DataHolderFactory<gov.nih.ncats.informatics.ginas.shared.model.v1.NucleicAcid>() {
					@Override
					public gov.nih.ncats.informatics.ginas.shared.model.v1.NucleicAcid make() {
						return new gov.nih.ncats.informatics.ginas.shared.model.v1.NucleicAcid();
					}
				});
	}

	@Override
	public Map addAttributes(Map m) {
		super.addAttributes(m);

		if (nucleicAcid != null)
			m.put("nucleicAcid", nucleicAcid.toMap());
		return m;
	}
*/

	@Override
	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences(){
		List<GinasAccessReferenceControlled> temp = super.getAllChildrenCapableOfHavingReferences();
		if(this.nucleicAcid!=null){
			temp.addAll(this.nucleicAcid.getAllChildrenAndSelfCapableOfHavingReferences());
		}
		return temp;
	}

	@Override
	public boolean hasSequenceFiles() {
		SubstanceSequenceFileSupportService supportService =  StaticContextAccessor.getBean(SubstanceSequenceFileSupportService.class);
		if(supportService ==null){
			return false;
		}
		return supportService.hasSequenceFiles(this);
	}

	@JsonIgnore
	@Override
	public Stream<SequenceFileData> getSequenceFileData() {
		SubstanceSequenceFileSupportService supportService =  StaticContextAccessor.getBean(SubstanceSequenceFileSupportService.class);
		if(supportService ==null){
			return Stream.empty();
		}
		return supportService.getSequenceFileDataFor(this, SequenceEntity.SequenceType.NUCLEIC_ACID);
	}
}
