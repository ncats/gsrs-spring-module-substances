package gsrs.module.substance.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.repository.PrincipalRepository;
import ix.ginas.utils.DefaultApprovalIDGenerator;
import ix.ginas.utils.SubstanceApprovalIdGenerator;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
//this prefix is in "canonical form" even though the property is approvalIDGenerator Spring will figure it out
@ConfigurationProperties("ix.ginas.approval-id-generator")
@Data
public class ApprovalIdConfiguration {

    private Class generatorClass;
    private Map<String, Object> parameters;
    private Map<String, Boolean> validators;

    @Bean
    @ConditionalOnMissingBean
    public ApprovalService defaultApprovalService(SubstanceApprovalIdGenerator approvalIdGenerator,
                                                  SubstanceRepository substanceRepository,
                                                  PrincipalRepository principalRepository){
        if (validators == null) {
            validators = new HashMap<String, Boolean>();
        }
        return new DefaultApprovalService(approvalIdGenerator, substanceRepository, principalRepository, validators);

    }
    @Bean
    @ConditionalOnMissingBean
    public SubstanceApprovalIdGenerator substanceApprovalIdGenerator(){
        if(generatorClass ==null){
            //no generator specified use basic
            return new DefaultApprovalIDGenerator("GID", 8,true, "GID-");
        }
        ObjectMapper mapper = new ObjectMapper();

        if(parameters ==null){
            return (SubstanceApprovalIdGenerator) mapper.convertValue(Collections.emptyMap(), generatorClass);
        }
        return (SubstanceApprovalIdGenerator) mapper.convertValue(parameters, generatorClass);
    }
}
