package gsrs.module.substance.events;

import gsrs.events.AbstractEntityCreatedEvent;
import ix.ginas.models.v1.Reference;

public class ReferenceCreatedEvent extends AbstractEntityCreatedEvent<Reference> {
    public ReferenceCreatedEvent(Reference source) {
        super(source);
    }
}
