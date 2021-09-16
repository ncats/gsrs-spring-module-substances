package gsrs.api.substances;


import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class StructurallyDiverseDTO extends BaseEditableDTO{

    private UUID uuid;

    private String sourceMaterialClass;

    private String sourceMaterialType;

    private String sourceMaterialState;

    private String organismFamily;

    private String organismGenus;

    private String organismSpecies;

    private String organismAuthor;

    private String partLocation;

    private ArrayList<String> part = new ArrayList<>();

    private String infraSpecificType;
    private String infraSpecificName;
    private String developmentalStage;
    private String fractionName;
    private String fractionMaterialType;

    private Set<String> access = new LinkedHashSet<>();
    private Set<UUID> references = new LinkedHashSet<>();
}
