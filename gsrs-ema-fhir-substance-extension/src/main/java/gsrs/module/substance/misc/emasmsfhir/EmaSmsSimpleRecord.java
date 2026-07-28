package gsrs.module.substance.misc.emasmsfhir;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.api.annotation.Extension;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hl7.fhir.r5.model.*;

// Not sure whether to use this annotation.

@EqualsAndHashCode(callSuper = true)
@Data

@ResourceDef(name = "EmaSmsSimpleRecord", profile = "http://example.org/StructureDefinition/EmaSmsSimpleRecord")
public class EmaSmsSimpleRecord  extends DomainResource {

    private static final String GSRS_SUBSTANCES_URL = "https://gsrs.ncats.nih.gov/ginas/app/substances";

    @Child(name = "smsId", min = 1, max = 1)
    @Description(shortDefinition = "SmsRecord Id")
    private StringType smsId;

    @Child(name = "substanceName", min = 1, max = 1)
    @Description(shortDefinition = "The substance name")
    private StringType substanceName;

    @Child(name = "language2", min = 1, max = 1)
    @Description(shortDefinition = "The substance name language2")
    private StringType language2;

    @Child(name = "isPreferredName", min = 1, max = 1)
    @Description(shortDefinition = "Is the substance name a preferred name")
    private BooleanType isPreferredName;

    @Child(name = "nameSource", min = 1, max = 1)
    @Description(shortDefinition = "Is the substance nameSource")
    private StringType nameSource;

    @Child(name = "substanceType", min = 1, max = 1)
    @Description(shortDefinition = "Is the substance type")
    private StringType substanceType;

    @Child(name = "evCode", min = 1, max = 1)
    @Description(shortDefinition = "Is the substance ev code")
    private StringType evCode;

    @Child(name = "unii", min = 1, max = 1)
    @Description(shortDefinition = "Is the substance UNII")
    private StringType unii;

    @Child(name = "innNumber", min = 1, max = 1)
    @Description(shortDefinition = "Is the substance innNumber")
    private StringType innNumber;

    @Child(name = "ecListNumber", min = 1, max = 1)
    @Description(shortDefinition = "Is the substance ecListNumber")
    private StringType ecListNumber;

    // This will be a quoted serialized JSON string as a payload that will allow Ema to import the Substance
    // into their GSRS instance, as opposed to their SMS.
    @Child(name = "gsrsSubstance")
    @Extension(url = GSRS_SUBSTANCES_URL, definedLocally = true, isModifier = false)
    @Description(shortDefinition = "The date the patient was registered at the clinic")
    private StringType gsrsSubstance;

    @Override
    public DomainResource copy() {
        EmaSmsSimpleRecord copy = new EmaSmsSimpleRecord();
        if (smsId != null) copy.smsId = smsId.copy();
        if (substanceName != null) copy.substanceName = substanceName.copy();
        if (language2 != null) copy.language2 = language2.copy();
        if (isPreferredName != null) copy.isPreferredName = isPreferredName.copy();
        if (nameSource != null) copy.nameSource = nameSource.copy();
        if (substanceType != null) copy.substanceType = substanceType.copy();
        if (evCode != null) copy.evCode = evCode.copy();
        if (unii != null) copy.unii = unii.copy();
        if (innNumber != null) copy.innNumber = innNumber.copy();
        if (ecListNumber != null) copy.ecListNumber = ecListNumber.copy();
        if (gsrsSubstance != null) copy.gsrsSubstance = gsrsSubstance.copy();
        copyValues(copy);
        return copy;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.Basic;
    }
}
