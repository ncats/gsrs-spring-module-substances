package gsrs.substances.dto;

import lombok.Data;

import java.util.Date;

@Data
public abstract class BaseEditableDTO {
    private Date created;
    private Date modified;
    private boolean deprecated;
}
