package gsrs.api.substances;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "substanceClass")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ConceptSubstanceDTO.class, name= "concept"),
        @JsonSubTypes.Type(value = ChemicalSubstanceDTO.class, name= "chemical"),
        @JsonSubTypes.Type(value = StructurallyDiverseSubstanceDTO.class, name= "structurallyDiverse"),
        @JsonSubTypes.Type(value = MixtureSubstanceDTO.class, name= "mixture"),
        @JsonSubTypes.Type(value = PolymerSubstanceDTO.class, name= "polymer"),
        @JsonSubTypes.Type(value = NucleicAcidSubstanceDTO.class, name= "nucleicAcid"),
        @JsonSubTypes.Type(value = SSG1SubstanceDTO.class, name= "specifiedSubstanceG1"),
        @JsonSubTypes.Type(value = ProteinSubstanceDTO.class, name= "protein"),
})
public class SubstanceDTO{

    private UUID uuid;
    //status is a String in the event someone makes a new status value...
    private String status;

    //can't extend BaseEditableDTO because of superbuilder
    private Date created;
    private Date modified;
    private boolean deprecated;

    private String approvalID;
    private String approvedBy;

    private SubstanceDefinitionType definitionType = SubstanceDefinitionType.PRIMARY;
    private SubstanceDefinitionLevel  definitionLevel= SubstanceDefinitionLevel.COMPLETE;

    private SubstanceClass substanceClass = SubstanceClass.concept;
    @Setter(AccessLevel.NONE)
    private LazyFetchedCollection _names;
    @Setter(AccessLevel.NONE)
    private LazyFetchedCollection _codes;
    @Setter(AccessLevel.NONE)
    private LazyFetchedCollection _references;
    @Setter(AccessLevel.NONE)
    private LazyFetchedCollection _modifications;

    private List<NameDTO> names = new ArrayList<>();
    private List<CodeDTO> codes= new ArrayList<>();

    private List<ReferenceDTO> references= new ArrayList<>();

    @JsonSerialize(using = ToStringSerializer.class)
    private int version;


    public enum SubstanceDefinitionType{
        PRIMARY,
        ALTERNATIVE
        ;
        @JsonValue
        public String jsonValue(){
            return name();
        }
    }

    public enum SubstanceDefinitionLevel{
        COMPLETE,
        INCOMPLETE,
        INVALID,
        REPRESENTATIVE
    }

    public enum SubstanceClass {
        chemical,
        protein,
        nucleicAcid,
        polymer,
        structurallyDiverse,
        mixture,
        specifiedSubstanceG1,
        specifiedSubstanceG2,
        specifiedSubstanceG3,
        specifiedSubstanceG4,
        unspecifiedSubstance,
        concept,
        reference
        ;

        @JsonValue
        public String jsonValue(){
            return name();
        }
    }
}
