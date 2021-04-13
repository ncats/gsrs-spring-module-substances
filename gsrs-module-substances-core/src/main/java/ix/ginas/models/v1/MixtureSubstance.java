package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasSubstanceDefinitionAccess;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.*;

@Entity
@Inheritance
@DiscriminatorValue("MIX")
@Slf4j
public class MixtureSubstance extends Substance implements GinasSubstanceDefinitionAccess
{

	@OneToOne(cascade = CascadeType.ALL)
	public Mixture mixture;
	public static String MixtureDefinitionPrefix = "mixture.definition";

	public MixtureSubstance()
	{
		super(SubstanceClass.mixture);
	}

	@JsonIgnore
	public List<SubstanceReference> getDependsOnSubstanceReferences()
	{

		List<SubstanceReference> sref = new ArrayList<SubstanceReference>();
		sref.addAll(super.getDependsOnSubstanceReferences());
		for (Component c : mixture.getMixture())
		{
			sref.add(c.substance);
		}

		return sref;
	}
//TODO katzelda Feb 2021 : delete handled in controller
//	@Override
//	public void delete()
//	{
//		super.delete();
//		for (Component c : mixture.components)
//		{
//			c.delete();
//		}
//
//	}

	@JsonIgnore
	public GinasAccessReferenceControlled getDefinitionElement()
	{
		return mixture;
	}

	@Override
	@JsonIgnore
	public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences()
	{
		List<GinasAccessReferenceControlled> temp = super.getAllChildrenCapableOfHavingReferences();
		if (this.mixture != null)
		{
			temp.addAll(this.mixture.getAllChildrenAndSelfCapableOfHavingReferences());
		}
		return temp;
	}
	//TODO katzelda Feb 2021 : moved DefintionalElements to it's own
//	@Override
//	protected void additionalDefinitionalElements(Consumer<DefinitionalElement> consumer)
//	{
//		log.trace("in additionalDefinitionalElements");
//		//addMixtureDefinitionalElementsFor(mixture, "mixture.properties", consumer, Collections.newSetFromMap(new IdentityHashMap<>()));
//		performAddition(this.mixture, consumer, Collections.newSetFromMap(new IdentityHashMap<>()));
//	}
//
//	private void performAddition(Mixture mix, Consumer<DefinitionalElement> consumer, Set<Mixture> visited)
//	{
//		log.trace("performAddition of mixture substance");
//		if (mix != null && !mix.components.isEmpty())
//		{
//			visited.add(mix);
//			log.trace("main part");
//			List<DefinitionalElement>	definitionalElements = additionalElementsFor();
//			for(DefinitionalElement de : definitionalElements)
//			{
//				consumer.accept(de);
//			}
//			log.trace("DE processing complete");
//		}
//		/*List<DefinitionalElement> definitionalElementsAdd = additionalElementsFor();
//		for(int i =0; i< definitionalElementsAdd.size(); i++)
//		{
//			DefinitionalElement de = definitionalElementsAdd.get(i);
//			consumer.accept(de);
//		}*/
//	}
//
//	public List<DefinitionalElement> additionalElementsFor()
//	{
//		List<DefinitionalElement> definitionalElements = new ArrayList<>();
//		for (int i =0; i <this.mixture.components.size(); i++)
//		{
//			Component component = this.mixture.components.get(i);
//
//			log.trace("looking at component " + i + " identified by " + component.substance.refuuid);
//			DefinitionalElement componentRefUuid = DefinitionalElement.of("mixture.components.substance.refuuid",
//                                                                component.substance.refuuid, 1);
//			definitionalElements.add(componentRefUuid);
//
//			DefinitionalElement componentAnyAll = DefinitionalElement.of("mixture.components.type",
//							component.type, 2);
//			log.trace("component.type: " + component.type);
//			definitionalElements.add(componentAnyAll);
//			log.trace("completed component processing");
//		}
//		if( this.mixture.parentSubstance != null && this.mixture.parentSubstance.refuuid != null
//						&& this.mixture.parentSubstance.refuuid.length() >0 )
//		{
//			log.trace("mix.parentSubstance");
//			DefinitionalElement parentSubstanceDE = DefinitionalElement.of("mixture.parentSubstance.refuuid",
//							this.mixture.parentSubstance.refuuid, 2);
//			definitionalElements.add(parentSubstanceDE);
//		}
//
//		if (this.modifications != null) {
//				definitionalElements.addAll(this.modifications.getDefinitionalElements().getElements());
//		}
//		return definitionalElements;
//	}

}
