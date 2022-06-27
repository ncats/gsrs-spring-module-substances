package gsrs.substances.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NameDTO extends SubstanceComponentBaseDTO{

    private UUID uuid;
    private String name;
    private String stdName;
    private boolean displayName;
    private boolean preferred;
    private String type;

}
