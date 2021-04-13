package gsrs.module.substance.events;

import gsrs.events.AbstractEntityUpdatedEvent;
import ix.ginas.models.v1.Substance;

public class SubstanceUpdatedEvent extends AbstractEntityUpdatedEvent<Substance> {
    public SubstanceUpdatedEvent(Substance source) {
        super(source);
    }
}
