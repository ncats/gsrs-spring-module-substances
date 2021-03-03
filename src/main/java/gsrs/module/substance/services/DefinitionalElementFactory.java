package gsrs.module.substance.services;

import gsrs.module.substance.definitional.DefinitionalElement;
import gsrs.module.substance.definitional.DefinitionalElements;
import ix.ginas.models.v1.Substance;

import java.util.function.Consumer;

public interface DefinitionalElementFactory {

    DefinitionalElements computeDefinitionalElementsFor(Substance s);
    void addDefinitionalElementsFor(Object o, Consumer<DefinitionalElement> consumer);
}
