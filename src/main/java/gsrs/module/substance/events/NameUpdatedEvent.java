package gsrs.module.substance.events;

import gsrs.events.AbstractEntityUpdatedEvent;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;

public class NameUpdatedEvent extends AbstractEntityUpdatedEvent<Name> {
    public NameUpdatedEvent(Name source) {
        super(source);
    }
}
