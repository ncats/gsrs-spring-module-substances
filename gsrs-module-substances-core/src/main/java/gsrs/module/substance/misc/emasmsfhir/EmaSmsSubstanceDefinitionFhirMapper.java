package gsrs.module.substance.misc.emasmsfhir;

import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Data
@Component
@Slf4j
public class EmaSmsSubstanceDefinitionFhirMapper {

    @Autowired
    private EmaSmsFhirConfiguration emaSmsFhirConfiguration;

    private static final String DEFAULT_NAME_SOURCE = "FDA SUBSTANCE REGISTRATION SYSTEM";
    private static final String DEFAULT_LANGUAGE = "en";

    private static final String GSRS_SUBSTANCE_EXTENSION_URL="https://gsrs.ncats.nih.gov/api/v1/substances";

    private static final Locale[] locales = Locale.getAvailableLocales();

    private boolean includeGsrsSubstanceExtension = false;

    public SubstanceDefinition generateEmaSmsSubstanceDefinitionFromSubstance(Substance substance) {

        SubstanceDefinition substanceDefinition = new SubstanceDefinition();

        Optional<Name> optionalDisplayName = substance.getDisplayName();

        String substanceClassName = substance.substanceClass.name();
        String emaSmsSubstanceType = EmaSmsFhrUtils.getEmaSmsSubstanceTypeFromGsrsSubstanceClass(substanceClassName);

        substanceDefinition.setId("example");

        // Name, Do we want all Names?
        optionalDisplayName.ifPresent(displayName -> substanceDefinition.addName(makeSubstanceDefinitionNameComponent(displayName)));

        // Classification
        substanceDefinition.getClassification()
            .add(new CodeableConcept()
            .addCoding(new Coding()
                .setCode(emaSmsFhirConfiguration.getSubstanceTypeConfigs().get(emaSmsSubstanceType).get("SMS Term ID"))
                .setSystem(emaSmsFhirConfiguration.getSubstanceTypeConfigs().get(emaSmsSubstanceType).get("SMS URL"))
            ));

        // Codes
        addCodeToSubstanceDefinition(substanceDefinition, "ecListNumber", substance);
        addCodeToSubstanceDefinition(substanceDefinition, "evCode", substance);
        addCodeToSubstanceDefinition(substanceDefinition, "innNumber", substance);
        addCodeToSubstanceDefinition(substanceDefinition, "smsId", substance);
        addCodeToSubstanceDefinition(substanceDefinition, "unii", substance);

        // Extension for GSRS Substance Json
        if(includeGsrsSubstanceExtension) {
            substanceDefinition.addExtension(
                new Extension().setUrl(GSRS_SUBSTANCE_EXTENSION_URL)
                .setValue(new StringType(EmaSmsFhrUtils.gsrsSubstanceToQuotedJson(substance)))
            );
        }

        return substanceDefinition;
    }

    /* Helper methods */

    private void addCodeToSubstanceDefinition(SubstanceDefinition sd, String smsKey, Substance gsrsSubstance) {
        SubstanceDefinition.SubstanceDefinitionCodeComponent codeComponent
            = makeSubstanceDefinitionCodeComponent(smsKey, gsrsSubstance);
        if (codeComponent!=null) {
            sd.addCode(codeComponent);
        }
    }

    private SubstanceDefinition.SubstanceDefinitionCodeComponent makeSubstanceDefinitionCodeComponent(String smsKey, Substance gsrsSubstance) {
         SubstanceDefinition.SubstanceDefinitionCodeComponent component = null;
         if (emaSmsFhirConfiguration.getCodeConfigs().get(smsKey)==null) {
            log.warn("Warning, configuration associated with smsKey: {}, not found", smsKey);
            return null;
        } else {
             String code = emaSmsFhirConfiguration.getCodeConfigs().get(smsKey).get("smsTermId");
             String _gsrsCvTerm = emaSmsFhirConfiguration.getCodeConfigs().get(smsKey).get("gsrsCvTerm");
             String display = EmaSmsFhrUtils.findCodeByCodeSystem(_gsrsCvTerm, gsrsSubstance);
             String system = emaSmsFhirConfiguration.getCodeConfigs().get(smsKey).get("smsUrl");
             if(display.isEmpty()) {
                return null;
             } else {
                 component = new SubstanceDefinition.SubstanceDefinitionCodeComponent();
                 component.setCode(new CodeableConcept()
                     .addCoding(new Coding()
                         .setCode((code == null) ? "" : code)
                         .setSystem((system == null) ? "" : system)
                         .setDisplay((display == null) ? "" : display)
                     )
                 );
                 return component;
             }
        }
    }

    private SubstanceDefinition.SubstanceDefinitionNameComponent makeSubstanceDefinitionNameComponent(Name name) {
       return new SubstanceDefinition.SubstanceDefinitionNameComponent()
           .setName(name.getName())
           .setLanguage(
               name.languages.stream().map(kw-> {
                   return new CodeableConcept()
                       .addCoding(new Coding()
                           .setCode(kw.term)
                           .setSystem(emaSmsFhirConfiguration.getMiscDefaultConfigs().get("name_language_coding").get("system"))
                           // Should we get from CV?
                           .setDisplay(Locale.forLanguageTag(kw.term).getDisplayLanguage())
                       );
               }).collect(Collectors.toList())
           )
           // Should we use name.preferred instead?
           .setPreferred(name.isDisplayName())
           .setStatus(new CodeableConcept()
               .addCoding(new Coding()
                   .setCode(emaSmsFhirConfiguration.getMiscDefaultConfigs().get("name_status_coding").get("code"))
                   .setSystem(emaSmsFhirConfiguration.getMiscDefaultConfigs().get("name_status_coding").get("system"))
                   .setDisplay(emaSmsFhirConfiguration.getMiscDefaultConfigs().get("name_status_coding").get("display"))
            )
       );
   }

}
