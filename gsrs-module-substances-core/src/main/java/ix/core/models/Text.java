package ix.core.models;

import javax.persistence.*;
import org.hibernate.annotations.Type;

@Entity
@DiscriminatorValue("TXT")
public class Text extends Value {
    @Lob
    @Type(type="org.hibernate.type.TextType")
    @Basic(fetch= FetchType.EAGER)
    public String text;

    public Text () {}
    public Text (String label) {
        super (label);
    }
    public Text (String label, String value) {
        super (label);
        text = value;
    }

    public String getText () { return text; }
    public void setText (String text) { this.text = text; }

    @Override
    public String getValue () { return text; }
}
