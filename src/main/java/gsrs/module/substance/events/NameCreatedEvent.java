package gsrs.module.substance.events;

import gsrs.events.AbstractEntityCreatedEvent;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;

public class NameCreatedEvent extends AbstractEntityCreatedEvent<Name> {
    public NameCreatedEvent(Name source) {
        super(source);
    }
}
