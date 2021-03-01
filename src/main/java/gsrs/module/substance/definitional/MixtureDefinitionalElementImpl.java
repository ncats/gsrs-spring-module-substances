package gsrs.module.substance.definitional;

import gsrs.module.substance.services.DefinitionalElementImplementation;
import ix.ginas.models.v1.Component;
import ix.ginas.models.v1.Mixture;
import ix.ginas.models.v1.MixtureSubstance;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
@Slf4j
public class MixtureDefinitionalElementImpl implements DefinitionalElementImplementation {
    @Override
    public boolean supports(Substance s) {
        return s instanceof MixtureSubstance;
    }

    @Override
    public void computeDefinitionalElements(Substance s, Consumer<DefinitionalElement> consumer) {
        log.trace("performAddition of mixture substance");
        MixtureSubstance mixtureSubstance = (MixtureSubstance)s;
        Mixture mix = mixtureSubstance.mixture;
        if (mix != null && !mix.components.isEmpty()) {
            log.trace("main part");
            List<DefinitionalElement> definitionalElements = additionalElementsFor(mixtureSubstance);
            for(DefinitionalElement de : definitionalElements)
            {
                consumer.accept(de);
            }
            log.trace("DE processing complete");
        }
    }

    private List<DefinitionalElement> additionalElementsFor( MixtureSubstance mixtureSubstance)
    {
        List<DefinitionalElement> definitionalElements = new ArrayList<>();
        for (int i =0; i <mixtureSubstance.mixture.components.size(); i++)
        {
            Component component = mixtureSubstance.mixture.components.get(i);

            log.trace("looking at component " + i + " identified by " + component.substance.refuuid);
            DefinitionalElement componentRefUuid = DefinitionalElement.of("mixture.components.substance.refuuid",
                    component.substance.refuuid, 1);
            definitionalElements.add(componentRefUuid);

            DefinitionalElement componentAnyAll = DefinitionalElement.of("mixture.components.type",
                    component.type, 2);
            log.trace("component.type: " + component.type);
            definitionalElements.add(componentAnyAll);
            log.trace("completed component processing");
        }
        if( mixtureSubstance.mixture.parentSubstance != null && mixtureSubstance.mixture.parentSubstance.refuuid != null
                && mixtureSubstance.mixture.parentSubstance.refuuid.length() >0 )
        {
            log.trace("mix.parentSubstance");
            DefinitionalElement parentSubstanceDE = DefinitionalElement.of("mixture.parentSubstance.refuuid",
                    mixtureSubstance.mixture.parentSubstance.refuuid, 2);
            definitionalElements.add(parentSubstanceDE);
        }

        if (mixtureSubstance.modifications != null) {
            definitionalElements.addAll(mixtureSubstance.modifications.getDefinitionalElements().getElements());
        }
        return definitionalElements;
    }
}
