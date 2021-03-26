package gsrs.module.substance.events;

import gsrs.events.AbstractEntityCreatedEvent;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Name;

public class CodeCreatedEvent extends AbstractEntityCreatedEvent<Code> {
    public CodeCreatedEvent(Code source) {
        super(source);
    }
}
