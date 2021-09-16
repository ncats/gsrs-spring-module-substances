package gsrs.api.substances;

import java.util.UUID;

public class SubstanceReferenceDTO extends BaseEditableDTO{

    private UUID uuid;

    private String refPname;

    private String refuuid;

    private String approvalID;

    private String linkingID;

    private String name;

    private SubstanceDTO.SubstanceClass substanceClass;

}
