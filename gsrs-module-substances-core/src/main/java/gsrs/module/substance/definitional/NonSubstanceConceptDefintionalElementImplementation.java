package gsrs.module.substance.definitional;

import gsrs.module.substance.services.DefinitionalElementImplementation;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;
@Slf4j
public class NonSubstanceConceptDefintionalElementImplementation implements DefinitionalElementImplementation {
    @Override
    public boolean supports(Object s) {
        return s instanceof Substance && ((Substance)s).isNonSubstanceConcept();
    }

    @Override
    public void computeDefinitionalElements(Object s, Consumer<DefinitionalElement> consumer) {
        DefinitionalElementImplementation.super.computeDefinitionalElements(s, consumer);
            String primaryName = "";
            for(Name name : ((Substance)s).getAllNames()) {
                if( name.displayName ){
                    primaryName =name.name;
                }
            }
            log.debug("going to create DE based on primary name " + primaryName);
            consumer.accept(DefinitionalElement.of("Name", primaryName, 1));
    }
}

