package ix.ginas.models.v1;

import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name="ix_ginas_ssg3_grade")
public class SpecifiedSubstanceGroup3Grade extends GinasCommonSubData {
    private String name;
    private String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {
        return Collections.singletonList(this);
    }
}
