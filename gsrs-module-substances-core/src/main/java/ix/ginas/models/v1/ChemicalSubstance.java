package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import gov.nih.ncats.molwitch.Chemical;
import ix.core.EntityMapperOptions;
import ix.core.models.BeanViews;
import ix.core.models.Indexable;
import ix.core.models.Structure;
import ix.core.validator.GinasProcessingMessage;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasSubstanceDefinitionAccess;
import ix.ginas.models.utils.JSONEntity;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("serial")
@JSONEntity(name = "chemicalSubstance", title = "Chemical Substance")
@Entity
@Inheritance
@DiscriminatorValue("CHE")
@Slf4j
public class ChemicalSubstance extends Substance implements GinasSubstanceDefinitionAccess {

    @JSONEntity(isRequired = true)
    @OneToOne(cascade= CascadeType.ALL)
//    @Column(nullable=false)
    //@JsonSerialize(using=StructureSerializer.class)
    private GinasChemicalStructure structure;


    @Indexable()
    public GinasChemicalStructure getStructure() {
        return structure;
    }

    public void setStructure(GinasChemicalStructure structure) {
        this.structure = structure;
    }

    public List<Moiety> getMoieties() {
        return moieties;
    }
    /**
     * Sets the moieties to the given List and updates their owners.
     * @param moieties the list of moieties.
     * @throws NullPointerException if moieties or any element in the list are null.
     */
    public void setMoieties(List<Moiety> moieties) {

        this.moieties = new ArrayList<>(Objects.requireNonNull(moieties));
        moieties.forEach(m-> Objects.requireNonNull(m).setOwner(this));

        this.setIsDirty("moieties");
    }

    public void addMoiety(Moiety m){
        Objects.requireNonNull(m);
        this.moieties.add(m);
        m.setOwner(this);
        this.setIsDirty("moieties");

    }
    @JSONEntity(title = "Chemical Moieties", isRequired = true, minItems = 1)
    //FIXME katzelda Sept 2019 changed mapped by from "owner" to the class that is the owner
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    @JsonView(BeanViews.Full.class)
    @EntityMapperOptions(linkoutInCompactView = true)
    public List<Moiety> moieties = new ArrayList<>();


    @Indexable(name="SubstanceStereochemistry", facet=true)
    @JsonIgnore
    public Structure.Stereo getStereochemistry () {
        return getStructure() != null ? structure.stereoChemistry : null;
    }



    public ChemicalSubstance () {
        super (SubstanceClass.chemical);
    }

    public static ChemicalSubstanceBuilder chemicalBuilder(){
        return new ChemicalSubstanceBuilder();
    }

    public ChemicalSubstanceBuilder toChemicalBuilder() {
        return new ChemicalSubstanceBuilder(this);
    }
    
    
    
    @Indexable(name="moieties")
    @JsonIgnore
    public List<GinasChemicalStructure> getMoietiesForIndexing(){
    	List<GinasChemicalStructure> mlist = new ArrayList<>();
    	for(Moiety m: this.moieties){
    		mlist.add(m.structure);
    	}
    	return mlist;
    }


    
    
  


    @Override
    protected Chemical getChemicalImpl(List<GinasProcessingMessage> messages) {
        return getStructure().toChemical(messages);
    }

    @JsonIgnore
    @Indexable(indexed=false, structure=true)
    public String getStructureMolfile(){

        GinasChemicalStructure structure = getStructure();
        if(structure ==null){
            return null;
        }
        return structure.molfile;
    }

    @JsonIgnore
    @Indexable(name = "Molecular Weight", ranges = { 0, 200, 400, 600, 800, 1000 }, format = "%1$.0f", facet=true)
    public Double getMolecularWeight(){
        GinasChemicalStructure structure = getStructure();
        if(structure ==null){
            //this shouldn't happen?
            return null;

        }
        return structure.mwt;
    }


    @JsonIgnore
    public GinasAccessReferenceControlled getDefinitionElement(){
        return getStructure();
    }

    @Override
    @JsonIgnore
    public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences(){
        List<GinasAccessReferenceControlled> temp = super.getAllChildrenCapableOfHavingReferences();
        if(this.getStructure()!=null){
            temp.addAll(this.getStructure().getAllChildrenAndSelfCapableOfHavingReferences());
        }
        if(this.moieties!=null){
            for(Moiety m: this.moieties){
                temp.addAll(m.getAllChildrenAndSelfCapableOfHavingReferences());
            }
        }
        return temp;
    }
    @JsonIgnore
    @Override
    public Optional<Structure> getStructureToRender() {
        return Optional.ofNullable(this.getStructure());
    }

    //    @Override
//    protected void additionalDefinitionalElements(Consumer<DefinitionalElement> consumer) {
//
//
//        /*
//        Key->Value
//structure.properties.lychi4->"<EXAMPLE_LYCHI>"
//structure.properties.stereoChemistry->"RACEMIC"
//structure.properties.opticalActivity->"(+/-)"
//
//For each Moiety:
//structure.moieties[<lychi4>].lychi4->"<EXAMPLE_LYCHI>"
//structure.moieties[<lychi4>].stereoChemistry->"RACEMIC"
//structure.moieties[<lychi4>].opticalActivity->"(+/-)"
//structure.moieties[<lychi4>].countAmount->"4 to 5 per mol"
//         */
//
//        addStructureDefinitialElements(structure, consumer);
//    }
//
//    private void addStructureDefinitialElements(Structure structure, Consumer<DefinitionalElement> consumer) {
//        if(structure==null){
//            //shouldn't happen unless we get invalid submission
//            return;
//        }
//        log.debug("starting addStructureDefinitialElements (ChemicalSubstance)");
//        consumer.accept(DefinitionalElement.of("structure.properties.hash1",
//                structure.getStereoInsensitiveHash(), 1));
//        log.debug("structure.getStereoInsensitiveHash(): "  + structure.getStereoInsensitiveHash());
//        consumer.accept(DefinitionalElement.of("structure.properties.hash2",
//                structure.getExactHash(), 2));
//        log.debug("structure.getExactHash(): " + structure.getExactHash());
//
//	if(structure.stereoChemistry!=null){
//		consumer.accept(DefinitionalElement.of("structure.properties.stereoChemistry",
//                	structure.stereoChemistry.toString(), 2));
//        	log.debug("structure.stereoChemistry : " + structure.stereoChemistry.toString());
//	}
//        if(structure.opticalActivity!=null){
//		consumer.accept(DefinitionalElement.of("structure.properties.opticalActivity",
//                	structure.opticalActivity.toString(), 2));
//		log.debug("structure.opticalActivity.toString(): " + structure.opticalActivity.toString());
//	}
//	if( moieties != null) {
//        for(Moiety m: moieties){
//            String mh=m.structure.getStereoInsensitiveHash();
//            log.debug("processing moiety with hash " + mh);
//            consumer.accept(DefinitionalElement.of("moiety.hash1", m.structure.getStereoInsensitiveHash(),1));
//            consumer.accept(DefinitionalElement.of("moiety.hash2", m.structure.getExactHash(),2));
//            log.debug("m.structure.getExactHash(): " + m.structure.getExactHash());
//            consumer.accept(DefinitionalElement.of("moiety[" + mh + "].stereoChemistry",
//                    m.structure.stereoChemistry.toString(), 2));
//            log.debug("m.structure.stereoChemistry.toString(): " + m.structure.stereoChemistry.toString());
//            consumer.accept(DefinitionalElement.of("moiety[" + mh + "].opticalActivity",
//                    m.structure.opticalActivity.toString(), 2));
//            log.debug("m.structure.opticalActivity.toString(): " + m.structure.opticalActivity.toString());
//            consumer.accept(DefinitionalElement.of("moiety[" + mh + "].countAmount",
//                    m.getCountAmount().toString(), 2));
//            log.debug("m.getCountAmount().toString(): " + m.getCountAmount().toString());
//        }
//	}
//    }


}
