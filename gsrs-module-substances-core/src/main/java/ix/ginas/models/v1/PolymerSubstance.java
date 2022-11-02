package ix.ginas.models.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

import gov.nih.ncats.common.Tuple;
import gov.nih.ncats.molwitch.Chemical;
import ix.core.models.Indexable;
import ix.core.models.Structure;
import ix.core.validator.GinasProcessingMessage;
import ix.ginas.models.GinasAccessControlled;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasSubstanceDefinitionAccess;
import lombok.extern.slf4j.Slf4j;

@Entity
@Inheritance
@DiscriminatorValue("POL")
@Slf4j
public class PolymerSubstance extends Substance implements GinasSubstanceDefinitionAccess
{

	@OneToOne(cascade = CascadeType.ALL)
	public Polymer polymer;

	public PolymerSubstance() {
		super(SubstanceClass.polymer);
	}

	@Override
	protected Chemical getChemicalImpl(List<GinasProcessingMessage> messages) {
		messages.add(GinasProcessingMessage
						.WARNING_MESSAGE("Polymer substance structure is for display, and is not complete in definition"));

		return polymer.idealizedStructure.toChemical(messages);
	}

	@Override
	public Optional<Structure> getStructureToRender() {
		return Optional.ofNullable(this.polymer.idealizedStructure);
	}

	@JsonIgnore
	public GinasAccessReferenceControlled getDefinitionElement() {
		return polymer;
	}

	@Override
	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
		List<GinasAccessReferenceControlled> temp = super.getAllChildrenCapableOfHavingReferences();
		if (this.polymer != null) {
			temp.addAll(this.polymer.getAllChildrenAndSelfCapableOfHavingReferences());
		}
		return temp;
	}

	@JsonIgnore
	@Indexable(indexed = false, structure = true)
	public String getStructureMolfile() {
		return polymer.idealizedStructure.molfile;
	}

	@Override
	@JsonIgnore
	public List<Tuple<GinasAccessControlled,SubstanceReference>> getNonDefiningSubstanceReferencesAndParents(){
		 List<Tuple<GinasAccessControlled,SubstanceReference>> srefs=new ArrayList<>();
	     srefs.addAll(super.getNonDefiningSubstanceReferencesAndParents());
	     if(polymer.monomers!=null) {
				polymer.monomers.forEach(s -> {
					if (!s.defining && s.monomerSubstance!=null) {
						srefs.add(Tuple.of(s,s.monomerSubstance));
					}
				});
		 }
	     return srefs;
	}

	@Override
    @JsonIgnore
    public List<Tuple<GinasAccessControlled,SubstanceReference>> getDependsOnSubstanceReferencesAndParents(){
        List<Tuple<GinasAccessControlled,SubstanceReference>> srefs=new ArrayList<>();
        srefs.addAll(super.getDependsOnSubstanceReferencesAndParents());
        if(polymer.monomers!=null) {
			polymer.monomers.forEach(s -> {
				if (s.defining) {
					srefs.add(Tuple.of(s,s.monomerSubstance));
				}
			});
		}
        if(this.polymer.classification.parentSubstance !=null){
        	srefs.add(Tuple.of(this.polymer.classification,this.polymer.classification.parentSubstance));
        }
        return srefs;
    }
    

	@Override
	@JsonIgnore
	public List<SubstanceReference> getDependsOnSubstanceReferences() {
		
		return getDependsOnSubstanceReferencesAndParents().stream().map(t->t.v()).collect(Collectors.toList());
	}


	}
