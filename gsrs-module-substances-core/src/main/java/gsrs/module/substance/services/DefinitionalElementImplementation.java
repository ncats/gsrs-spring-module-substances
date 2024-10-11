package gsrs.module.substance.services;

import gsrs.module.substance.definitional.DefinitionalElement;
import ix.ginas.models.v1.Parameter;
import ix.ginas.models.v1.Property;
import ix.ginas.models.v1.Substance;

import java.util.function.Consumer;
import java.util.logging.Logger;

public interface DefinitionalElementImplementation {

    boolean supports(Object s);

    default void computeDefinitionalElements(Object s, Consumer<DefinitionalElement> consumer) {
        if (s instanceof Substance) {
            Logger log= Logger.getLogger(this.getClass().getName());
            addPropertiesToDefHash((Substance) s, consumer, Logger.getLogger(getClass().getName()));
        }
    }

    default void addPropertiesToDefHash(Substance substance, Consumer<DefinitionalElement> consumer, Logger logger) {
        if( substance.properties != null ) {
            for(Property property : substance.properties) {
                if(property.isDefining() && property.getValue() != null) {
                    String defElementName = String.format("properties.%s.value",
                            property.getName());
                    DefinitionalElement propertyValueDefElement =
                            DefinitionalElement.of(defElementName, property.getValue().toString(), 2);
                    consumer.accept(propertyValueDefElement);
                    logger.fine("added def element for property " + defElementName);
                    for(Parameter parameter : property.getParameters()) {
                        defElementName = String.format("properties.%s.parameters.%s.value",
                                property.getName(), parameter.getName());
                        if( parameter.getValue() != null) {
                            DefinitionalElement propertyParamValueDefElement =
                                    DefinitionalElement.of(defElementName,
                                            parameter.getValue().toString(), 2);
                            consumer.accept(propertyParamValueDefElement);
                            logger.fine("added def element for property parameter " + defElementName);
                        }
                    }
                }
            }
        }
    }
}
