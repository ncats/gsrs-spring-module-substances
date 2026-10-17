package gsrs.substances.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.UUID;

@Builder
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StructurallyDiverseDTO extends SubstanceComponentBaseDTO{

    private UUID uuid;
    private String sourceMaterialClass;

    private String sourceMaterialType;

    private String sourceMaterialState;

    private String organismFamily;

    private String organismGenus;

    private String organismSpecies;

    private String organismAuthor;

    private String partLocation;

    @Builder.Default
    private ArrayList<String> part = new ArrayList<>();

    private String infraSpecificType;
    private String infraSpecificName;
    private String developmentalStage;
    private String fractionName;
    private String fractionMaterialType;

}
