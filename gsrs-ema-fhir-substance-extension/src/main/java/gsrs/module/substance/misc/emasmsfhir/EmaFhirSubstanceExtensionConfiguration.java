package gsrs.module.substance.misc.emasmsfhir;

import gsrs.EnableGsrsJpaEntities;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;


@Configuration
@Import({
    EmaSmsFhirConfiguration.class,
    EmaSmsFhirController.class,
    EmaSmsSubstanceDefinitionFhirMapper.class,
    EmaSmsSimpleRecordFhirMapper.class
})
public class EmaFhirSubstanceExtensionConfiguration {

@PostConstruct
void hello() {
System.out.println("I have contructed ... EmaFhirSubstanceExtensionConfiguration");
}

    @Bean("EmaSmsFhirConfiguration")
    // @ConditionalOnMissingBean(EmaSmsFhirConfiguration.class)
    public EmaSmsFhirConfiguration getEmaSmsFhirConfiguration(){
        return new EmaSmsFhirConfiguration();
    }


    @Bean("EmaSmsFhirController")
    // @ConditionalOnMissingBean(EmaSmsFhirController.class)
    public EmaSmsFhirController getEmaSmsFhirController(){
        return new EmaSmsFhirController();
    }

    @Bean("EmaSmsSubstanceDefinitionFhirMapper")
    // @ConditionalOnMissingBean(EmaSmsSubstanceDefinitionFhirMapper.class)
    public EmaSmsSubstanceDefinitionFhirMapper getEmaSmsSubstanceDefinitionFhirMapper(){
        return new EmaSmsSubstanceDefinitionFhirMapper();
    }

    @Bean("EmaSmsSimpleRecordFhirMapper")
    // @ConditionalOnMissingBean(EmaSmsSimpleRecordFhirMapper.class)
    public EmaSmsSimpleRecordFhirMapper getEmaSmsSimpleRecordFhirMapper(){
        return new EmaSmsSimpleRecordFhirMapper();
    }

}
