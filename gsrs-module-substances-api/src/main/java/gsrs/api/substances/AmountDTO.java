package gsrs.api.substances;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ix.ginas.models.utils.JSONEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmountDTO extends SubstanceComponentBaseDTO{

    private UUID uuid;

    private String type;

    private Double average;

    private Double highLimit;

    private Double high;

    private Double lowLimit;

    private Double low;

    private String units;

    private String nonNumericValue;

    private String approvalID;
}
