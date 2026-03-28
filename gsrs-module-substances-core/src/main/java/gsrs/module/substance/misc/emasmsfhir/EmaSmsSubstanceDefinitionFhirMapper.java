package gsrs.module.substance.misc.emasmsfhir;

import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import lombok.Data;
import org.hl7.fhir.r5.model.*;
import java.util.*;
import java.util.stream.Collectors;

@Data
public class EmaSmsSubstanceDefinitionFhirMapper {
    private static final String DEFAULT_NAME_SOURCE = "FDA SUBSTANCE REGISTRATION SYSTEM";
    private static final String DEFAULT_LANGUAGE = "en";
    private static final String SUBTANCE_TYPE_SYSTEM_URL = "http://example.europa.eu/fhir/SubstanceType";
    private static final String CODE_SYSTEM_URL="http://example.europa.eu/fhir/Substance";

    private static final String UNII_SYSTEM_URL="https://example.europa.eu/fhir/Substance#---UNII";
    private static final String EV_SYSTEM_URL="https://example.europa.eu/fhir/Substance#---EV";
    private static final String INN_SYSTEM_URL="https://example.europa.eu/fhir/Substance#---INN";
    private static final String ECHA_SYSTEM_URL="https://example.europa.eu/fhir/Substance#----ECHA";
    private static final String STATUS_SYSTEM_URL="https://example.europa.eu/fhir/Substance#--status";

    private static final String GSRS_SUBSTANCE_EXTENSION_URL="https://gsrs.ncats.nih.gov/api/v1/substances";

    private static final Map<String, Map<String, String>> SUBSTANCE_CLASS_MAP;
    private static final Locale[] locales = Locale.getAvailableLocales();
    // delete private static Map<String, String> LANGUAGE_MAP;

    static  {

        SUBSTANCE_CLASS_MAP = new HashMap<>();

        Map chemicalMap = new HashMap<>();
        chemicalMap.put("display", "Chemical");
        chemicalMap.put("code", "100000075670");
        chemicalMap.put("system", "http://example.europa.eu/fhir/SubstanceType");
        SUBSTANCE_CLASS_MAP.put("chemical", chemicalMap);

        Map conceptMap = new HashMap<>();
        conceptMap.put("display", "Concept");
        conceptMap.put("code", "?");
        conceptMap.put("system", "http://example.europa.eu/fhir/SubstanceType");
        SUBSTANCE_CLASS_MAP.put("concept", conceptMap);
    }

    private boolean includeGsrsSubstanceExtension = false;

    public SubstanceDefinition generateEmaSmsSubstanceDefinitionFromSubstance(Substance substance) {



        SubstanceDefinition substanceDefinition = new SubstanceDefinition();

        Optional<Name> optionalDisplayName = substance.getDisplayName();

        String substanceClassName = substance.substanceClass.name();

        substanceDefinition.setId("example");

        // Name, Do we want all Names?
        optionalDisplayName.ifPresent(displayName -> substanceDefinition.addName(makeSubstanceDefinitionNameComponent(displayName)));

        // classification
        substanceDefinition.getClassification()
            .add(new CodeableConcept()
            .addCoding(new Coding()
                .setCode(querySubstanceClassMap(substanceClassName, "code"))
                .setSystem(SUBTANCE_TYPE_SYSTEM_URL)
                .setDisplay("")
        ));

        // e.g. SUB99611MIG
        substanceDefinition.addCode(
            new SubstanceDefinition.SubstanceDefinitionCodeComponent()
            .setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setCode(EmaSmsFhrUtils.findCodeByCodeSystem("EVMPD", substance))
                .setSystem(EV_SYSTEM_URL)
            )
        ));

        // e.g. UNII; should we use approval ID instead?
        substanceDefinition.addCode(
            new SubstanceDefinition.SubstanceDefinitionCodeComponent()
            .setCode(new CodeableConcept()
                .addCoding(new Coding()
                    .setCode(EmaSmsFhrUtils.findCodeByCodeSystem("FDA UNII", substance))
                    .setSystem(UNII_SYSTEM_URL)
                    .setDisplay("UNII")
                )
        ));

        // INN
        substanceDefinition.addCode(
            new SubstanceDefinition.SubstanceDefinitionCodeComponent()
            .setCode(new CodeableConcept()
                .addCoding(new Coding()
                    .setCode(EmaSmsFhrUtils.findCodeByCodeSystem("INN", substance))
                    .setSystem(INN_SYSTEM_URL)
                )
        ));

        // ECHA (EC/EINECS)
        substanceDefinition.addCode(
            new SubstanceDefinition.SubstanceDefinitionCodeComponent()
            .setCode(new CodeableConcept()
                .addCoding(new Coding()
                    .setCode(EmaSmsFhrUtils.findCodeByCodeSystem("ECHA (EC/EINECS)", substance))
                    .setSystem(ECHA_SYSTEM_URL)
                )
            ));

        // Add Extension for GSRS Substance Json
        if(includeGsrsSubstanceExtension) {
            substanceDefinition.addExtension(
                new Extension().setUrl(GSRS_SUBSTANCE_EXTENSION_URL)
                .setValue(new StringType(EmaSmsFhrUtils.gsrsSubstanceToQuotedJson(substance)))
            );
        }
        return substanceDefinition;
    }

   private SubstanceDefinition.SubstanceDefinitionNameComponent makeSubstanceDefinitionNameComponent(Name name) {
       return new SubstanceDefinition.SubstanceDefinitionNameComponent()
           .setName(name.getName())
           .setLanguage(
               name.languages.stream().map(kw-> {
                   return new CodeableConcept()
                       .addCoding(new Coding()
                           .setCode(kw.term)
                           .setSystem("urn:ietf:bcp:47")
                           // Should we get from CV?
                           .setDisplay(Locale.forLanguageTag(kw.term).getDisplayLanguage())
                       );
               }).collect(Collectors.toList())
           )
           // Should we use name.preferred instead?
           .setPreferred(name.isDisplayName())
           .setStatus(new CodeableConcept()
               .addCoding(new Coding()
                   .setCode("200000005004")
                   .setSystem(STATUS_SYSTEM_URL)
                   .setDisplay("Current")
            )
       );
   }

   private String querySubstanceClassMap (String substanceClass, String key) {
        Map<String, String> map = SUBSTANCE_CLASS_MAP.get(substanceClass);
        String value = null;
        if(map!=null) {
            value = map.get(key);
        }
        if(value!=null) {
            return value;
        }
        return "";
    }

}



