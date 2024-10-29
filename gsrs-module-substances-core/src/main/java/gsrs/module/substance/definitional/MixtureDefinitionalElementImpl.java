package gsrs.module.substance.definitional;

import gsrs.module.substance.services.DefinitionalElementFactory;
import gsrs.module.substance.services.DefinitionalElementImplementation;
import ix.ginas.models.v1.Component;
import ix.ginas.models.v1.Mixture;
import ix.ginas.models.v1.MixtureSubstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Consumer;
@Slf4j
public class MixtureDefinitionalElementImpl implements DefinitionalElementImplementation {

    @Autowired
    private DefinitionalElementFactory definitionalElementFactory;

    public DefinitionalElementFactory getDefinitionalElementFactory() {
        return definitionalElementFactory;
    }

    public void setDefinitionalElementFactory(DefinitionalElementFactory definitionalElementFactory) {
        this.definitionalElementFactory = definitionalElementFactory;
    }

    @Override
    public boolean supports(Object s) {
        return s instanceof MixtureSubstance;
    }

    @Override
    public void computeDefinitionalElements(Object s, Consumer<DefinitionalElement> consumer) {
        log.trace("computeDefinitionalElements of mixture substance");
        DefinitionalElementImplementation.super.computeDefinitionalElements(s, consumer);
        MixtureSubstance mixtureSubstance = (MixtureSubstance)s;
        Mixture mix = mixtureSubstance.mixture;
        if (mix != null && !mix.components.isEmpty()) {
            log.trace("main part");
            additionalElementsFor(mixtureSubstance, consumer);
            
            log.trace("DE processing complete");
        }
    }

    private void additionalElementsFor( MixtureSubstance mixtureSubstance, Consumer<DefinitionalElement> consumer)
    {
       
        for (int i =0; i <mixtureSubstance.mixture.components.size(); i++)
        {
            Component component = mixtureSubstance.mixture.components.get(i);

            log.trace("looking at component " + i + " identified by " + component.substance.refuuid);
            DefinitionalElement componentRefUuid = DefinitionalElement.of("mixture.components.substance.refuuid",
                    component.substance.refuuid, 1);
            consumer.accept(componentRefUuid);

            DefinitionalElement componentAnyAll = DefinitionalElement.of("mixture.components.type",
                    component.type, 2);
            log.trace("component.type: " + component.type);
            consumer.accept(componentAnyAll);
            log.trace("completed component processing");
        }
        if( mixtureSubstance.mixture.parentSubstance != null && mixtureSubstance.mixture.parentSubstance.refuuid != null
                && mixtureSubstance.mixture.parentSubstance.refuuid.length() >0 )
        {
            log.trace("mix.parentSubstance");
            DefinitionalElement parentSubstanceDE = DefinitionalElement.of("mixture.parentSubstance.refuuid",
                    mixtureSubstance.mixture.parentSubstance.refuuid, 2);
            consumer.accept(parentSubstanceDE);
        }

        if (mixtureSubstance.modifications != null) {
            definitionalElementFactory.addDefinitionalElementsFor(mixtureSubstance.modifications, consumer);

        }
    }
}
