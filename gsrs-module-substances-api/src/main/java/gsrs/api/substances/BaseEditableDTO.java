package gsrs.api.substances;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
public abstract class BaseEditableDTO {
    private Date created;
    private Date modified;
    private boolean deprecated;
}
