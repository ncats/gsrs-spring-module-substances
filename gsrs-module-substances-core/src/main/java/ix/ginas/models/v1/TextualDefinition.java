package ix.ginas.models.v1;

import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name="ix_ginas_definition")
public class TextualDefinition extends GinasCommonSubData {
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
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
