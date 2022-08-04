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
	//TODO katzelda FEb 2021 : moved definitional hash to their own components
//
//	@Override
//	protected void additionalDefinitionalElements(Consumer<DefinitionalElement> consumer) {
//		log.debug("in polymer additionalDefinitionalElements");
//		performAddition(this.polymer, consumer, Collections.newSetFromMap(new IdentityHashMap<>()));
//	}
//
//	private void performAddition(Polymer polymer, Consumer<DefinitionalElement> consumer, Set<Polymer> visited) {
//		log.debug("in polymer performAddition");
//		if (polymer != null) {
//			visited.add(polymer);
//			List<DefinitionalElement> definitionalElements = additionalElementsFor();
//			for (DefinitionalElement de : definitionalElements) {
//				consumer.accept(de);
//			}
//			log.debug("DE processing complete");
//		}
//	}
//
//	public List<DefinitionalElement> additionalElementsFor() {
//		log.debug("in PolymerSubstance additionalElementsFor");
//		List<DefinitionalElement> definitionalElements = new ArrayList<>();
//
//		for (Material monomer : this.polymer.monomers) {
//			if( monomer.monomerSubstance != null) {
//				DefinitionalElement monomerElement = DefinitionalElement.of("polymer.monomer.monomerSubstance.refuuid",
//								monomer.monomerSubstance.refuuid, 1);
//				definitionalElements.add(monomerElement);
//				log.debug("adding monomer refuuid to the def hash: " + monomer.monomerSubstance.refuuid);
//				if (monomer.amount != null) {
//					DefinitionalElement monomerAmountElement = DefinitionalElement.of("polymer.monomer.monomerSubstance.amount",
//									monomer.amount.toString(), 2);
//					definitionalElements.add(monomerAmountElement);
//				}
//			} else {
//				log.debug("monomer does not have a substance attached.");
//			}
//		}
//
//		if (this.polymer.structuralUnits != null && !this.polymer.structuralUnits.isEmpty()) {
//			//todo: consider canonicalizing
//			List<Unit> canonicalizedUnits = this.polymer.structuralUnits;
//			for (Unit unit : canonicalizedUnits) {
//				if( unit.type == null) {
//					log.debug("skipping null unit");
//					continue;
//				}
//				//log.debug("about to process unit structure " + unit.structure);
//				String molfile = unit.structure;//prepend newline to avoid issue later on
//				Structure structure = null;
//				try	{
//					structure= StructureFactory.getStructureFrom(molfile, false);
//				}
//				catch(Exception ex) {
//					log.warn("Unable to parse structure from polymer unit molfile: " + molfile);
//					continue;
//				}
//
//				log.debug("created structure OK. looking at unit type: " + unit.type);
//				int layer = 1;
//				/* all units are part of layer 1 as of 13 March 2020 based on https://cnigsllc.atlassian.net/browse/GSRS-1361
//				if( unit.type.contains("SRU")) {
//					layer=1;
//				}*/
//
//				String currentHash = structure.getExactHash();
//				DefinitionalElement structUnitElement = DefinitionalElement.of("polymer.structuralUnit.structure.l4",
//								currentHash, layer);
//				definitionalElements.add(structUnitElement);
//
//				if (unit.amount != null) {
//					DefinitionalElement structUnitAmountElement = DefinitionalElement.of("polymer.structuralUnit["
//									+ currentHash +"].amount", unit.amount.toString(), 2);
//					definitionalElements.add(structUnitAmountElement);
//				}
//				log.debug("adding structural unit def element: " + structUnitElement);
//			}
//		}
//
//		//todo: add additional items to the definitional element list
//		if (this.modifications != null) {
//				definitionalElements.addAll(this.modifications.getDefinitionalElements().getElements());
//		}
//
//		if( this.properties != null ) {
//			for(Property property : this.properties) {
//				if(property.isDefining() && property.getValue() != null) {
//					String defElementName = String.format("properties.%s.value",
//									property.getName());
//					DefinitionalElement propertyValueDefElement =
//									DefinitionalElement.of(defElementName, property.getValue().toString(), 2);
//					definitionalElements.add(propertyValueDefElement);
//					log.debug("added def element for property " + defElementName);
//					for(Parameter parameter : property.getParameters()) {
//						defElementName = String.format("properties.%s.parameters.%s.value",
//									property.getName(), parameter.getName());
//						if( parameter.getValue() != null) {
//							DefinitionalElement propertyParamValueDefElement =
//											DefinitionalElement.of(defElementName,
//															parameter.getValue().toString(), 2);
//							definitionalElements.add(propertyParamValueDefElement);
//							log.debug("added def element for property parameter " + defElementName);
//						}
//					}
//				}
//			}
//		}
//
//		return definitionalElements;
//	}

}
