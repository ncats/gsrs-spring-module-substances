package gsrs.module.substance.events;

import gsrs.events.AbstractEntityUpdatedEvent;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Name;

public class CodeUpdatedEvent extends AbstractEntityUpdatedEvent<Code> {
    public CodeUpdatedEvent(Code source) {
        super(source);
    }
}
