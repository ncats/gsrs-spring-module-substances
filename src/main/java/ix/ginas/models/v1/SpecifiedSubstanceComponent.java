package ix.ginas.models.v1;

import javax.persistence.*;

@Entity
@Inheritance
@DiscriminatorValue("SPEC")
public class SpecifiedSubstanceComponent extends Component {
    @OneToOne(cascade= CascadeType.ALL)
    public String role;
    @OneToOne(cascade= CascadeType.ALL)
    public Amount amount;

    public SpecifiedSubstanceComponent() {}
}
