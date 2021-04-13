package gsrs.module.substance.events;

import gsrs.events.AbstractEntityCreatedEvent;
import ix.ginas.models.v1.Substance;

public class SubstanceCreatedEvent extends AbstractEntityCreatedEvent<Substance> {
    public SubstanceCreatedEvent(Substance source) {
        super(source);
    }
}
