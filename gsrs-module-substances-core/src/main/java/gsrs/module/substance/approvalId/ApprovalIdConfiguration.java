package gsrs.module.substance.approvalId;

import com.fasterxml.jackson.databind.ObjectMapper;
import ix.ginas.utils.DefaultApprovalIDGenerator;
import ix.ginas.utils.SubstanceApprovalIdGenerator;
import ix.ginas.utils.UniiLikeGenerator;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Map;

@Configuration
//this prefix is in "canonical form" even though the property is approvalIDGenerator Spring will figure it out
@ConfigurationProperties("ix.ginas.approval-id-generator")
@Data
public class ApprovalIdConfiguration {

    private Class generatorClass;
    private Map<String, Object> parameters;

    @Bean
    public SubstanceApprovalIdGenerator substanceApprovalIdGenerator(){
        if(generatorClass ==null){
            //no generator specified use default
            return new DefaultApprovalIDGenerator("GID", 8,true, "GID-");
        }
        ObjectMapper mapper = new ObjectMapper();

        if(parameters ==null){
            return (SubstanceApprovalIdGenerator) mapper.convertValue(Collections.emptyMap(), generatorClass);
        }
        return (SubstanceApprovalIdGenerator) mapper.convertValue(parameters, generatorClass);
    }
}
