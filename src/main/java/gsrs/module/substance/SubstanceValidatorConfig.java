package gsrs.module.substance;

import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.ginas.models.v1.Substance;
import lombok.Data;

@Data
public class SubstanceValidatorConfig extends DefaultValidatorConfig {

    private Substance.SubstanceClass substanceClass;

    private Substance.SubstanceDefinitionType type;


    @Override
    protected <T> boolean meetsFilterCriteria(T obj) {
        if(!(obj instanceof Substance)) {
            return false;
        }
        Substance s = (Substance)obj;
        if(substanceClass !=null && substanceClass != s.substanceClass){
            return false;
        }
        if(type !=null && type != s.definitionType){
            return false;
        }
        return true;
    }
}
