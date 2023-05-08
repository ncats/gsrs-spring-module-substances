package ix.ginas.utils.validation.strategy;

import gsrs.services.GroupService;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class AcceptAndApplyAllWarningsProcessingStrategy extends AbstractProcessingStrategy {
    @Autowired
    public AcceptAndApplyAllWarningsProcessingStrategy(GroupService groupRepository,
            GsrsProcessingStrategyFactoryConfiguration srsProcessingStrategyFactoryConfiguration) {
        super(groupRepository, srsProcessingStrategyFactoryConfiguration);
    }

    @Override
    public void processMessage(GinasProcessingMessage gpm) {
        this.overrideMessage(gpm);
        if (gpm.messageType == GinasProcessingMessage.MESSAGE_TYPE.ERROR) {
            gpm.actionType = GinasProcessingMessage.ACTION_TYPE.FAIL;
        } else {
            if (gpm.suggestedChange){
                gpm.actionType = GinasProcessingMessage.ACTION_TYPE.APPLY_CHANGE;
            }else{
                gpm.actionType = GinasProcessingMessage.ACTION_TYPE.IGNORE;
            }
        }
    }

    //copy from AcceptApplyAllProcessingStrategy

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
