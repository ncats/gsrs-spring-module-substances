package ix.ginas.utils.validation.strategy;

import gsrs.services.GroupService;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class AcceptApplyAllProcessingStrategy extends GinasProcessingStrategy {
    @Autowired
    public AcceptApplyAllProcessingStrategy(GroupService groupRepository, GsrsProcessingStrategyFactoryConfiguration srsProcessingStrategyFactoryConfiguration) {
        super(groupRepository, srsProcessingStrategyFactoryConfiguration);
    }

    @Override
    public void processMessage(GinasProcessingMessage gpm) {
        this.overrideMessage(gpm);
        if (gpm.suggestedChange){
            gpm.actionType = GinasProcessingMessage.ACTION_TYPE.APPLY_CHANGE;
        }else{
            if(gpm.isError()){
                gpm.actionType= GinasProcessingMessage.ACTION_TYPE.FAIL;
            }else{
                gpm.actionType = GinasProcessingMessage.ACTION_TYPE.IGNORE;
            }
        }
    }

    @Override
    public void setIfValid(ValidationResponse resp, List<GinasProcessingMessage> messages) {
        if (GinasProcessingMessage.ALL_VALID(messages)) {
            resp.addValidationMessage(GinasProcessingMessage.SUCCESS_MESSAGE("Substance is valid"));
            resp.setValid(true);
        }else{
            if(resp.hasError()){
                resp.setValid(false);
            }else {
                //might be warnings
                resp.setValid(true);
            }
        }
    }

}
