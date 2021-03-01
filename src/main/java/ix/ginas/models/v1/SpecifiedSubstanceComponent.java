package ix.ginas.models.v1;

import javax.persistence.*;

@Entity
@Inheritance
@DiscriminatorValue("SPEC")
public class SpecifiedSubstanceComponent extends Component {
    //TODO katzelda Feb 2021: do they mean column unique = true?
//    @OneToOne(cascade= CascadeType.ALL)
    public String role;
    @OneToOne(cascade= CascadeType.ALL)
    public Amount amount;

    public SpecifiedSubstanceComponent() {}
}
