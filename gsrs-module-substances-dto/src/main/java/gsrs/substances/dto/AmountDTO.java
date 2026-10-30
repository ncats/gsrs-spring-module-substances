package gsrs.substances.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@EqualsAndHashCode(callSuper=false)
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
