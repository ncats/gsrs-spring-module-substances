package gsrs.module.substance.events;

import gsrs.events.AbstractEntityUpdatedEvent;
import ix.ginas.models.v1.Reference;

public class ReferenceUpdatedEvent extends AbstractEntityUpdatedEvent<Reference> {
    public ReferenceUpdatedEvent(Reference source) {
        super(source);
    }
}
