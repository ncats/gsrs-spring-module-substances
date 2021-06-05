package ix.ginas.models.v1;

import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.Type;

@Entity
@Table(name="ix_ginas_definition")
public class TextualDefinition extends GinasCommonSubData {
    @Lob
    @Type(type="org.hibernate.type.TextType")
    private String definition;

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }


    @Override
    public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
        return Collections.singletonList(this);
    }
}
