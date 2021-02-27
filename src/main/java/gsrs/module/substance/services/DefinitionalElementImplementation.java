package gsrs.module.substance.services;

import gsrs.module.substance.definitional.DefinitionalElement;
import ix.ginas.models.v1.Substance;

import java.util.function.Consumer;

public interface DefinitionalElementImplementation {

    boolean supports(Substance s);

    void computeDefinitionalElements(Substance s, Consumer<DefinitionalElement> consumer);
}
