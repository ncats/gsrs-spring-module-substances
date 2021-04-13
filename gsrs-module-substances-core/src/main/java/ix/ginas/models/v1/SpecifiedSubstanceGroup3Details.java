package ix.ginas.models.v1;

import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasCommonSubData;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="ix_ginas_ssg3")
public class SpecifiedSubstanceGroup3Details extends GinasCommonSubData {

    @OneToOne(cascade= CascadeType.ALL)
    private SubstanceReference parentSubstance;

    @OneToOne(cascade= CascadeType.ALL)
    private SpecifiedSubstanceGroup3Grade grade;

    @OneToOne(cascade= CascadeType.ALL)
    private TextualDefinition definition;

    public SpecifiedSubstanceGroup3Grade getGrade() {
        return grade;
    }

    public void setGrade(SpecifiedSubstanceGroup3Grade grade) {
        this.grade = grade;
    }

    public TextualDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(TextualDefinition definition) {
        this.definition = definition;
    }

    public SubstanceReference getParentSubstance() {
        return parentSubstance;
    }

    public void setParentSubstance(SubstanceReference parent) {
        this.parentSubstance = parent;
    }

    @Override
    public List<GinasAccessReferenceControlled> getAllChildrenCapableOfHavingReferences() {

        List<GinasAccessReferenceControlled> list = new ArrayList<>();
        list.addAll(parentSubstance.getAllChildrenCapableOfHavingReferences());
        return list;
    }
}
