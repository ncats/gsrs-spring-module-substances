package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gov.nih.ncats.molwitch.Chemical;
import ix.core.models.Indexable;
import ix.core.models.Structure;
import ix.core.validator.GinasProcessingMessage;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasSubstanceDefinitionAccess;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.*;

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
	public List<SubstanceReference> getDependsOnSubstanceReferences() {
		List<SubstanceReference> sref = new ArrayList<SubstanceReference>();
		sref.addAll(super.getDependsOnSubstanceReferences());
		if(polymer.monomers!=null) {
			polymer.monomers.forEach(s -> {
				if (s.defining) {
					sref.add(s.monomerSubstance);
				}
			});
		}
		return sref;
	}


	}
