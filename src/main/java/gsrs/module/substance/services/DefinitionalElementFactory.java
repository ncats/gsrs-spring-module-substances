package gsrs.module.substance.services;

import gsrs.module.substance.definitional.DefinitionalElement;
import gsrs.module.substance.definitional.DefinitionalElements;
import ix.ginas.models.v1.Substance;

public interface DefinitionalElementFactory {

    DefinitionalElements computeDefinitionalElementsFor(Substance s);
}
