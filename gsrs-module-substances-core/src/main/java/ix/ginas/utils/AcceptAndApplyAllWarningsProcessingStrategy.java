package ix.ginas.utils;

import gsrs.repository.GroupRepository;
import ix.core.validator.GinasProcessingMessage;
import org.springframework.beans.factory.annotation.Autowired;

public class AcceptAndApplyAllWarningsProcessingStrategy extends GinasProcessingStrategy{
    @Autowired
    public AcceptAndApplyAllWarningsProcessingStrategy(GroupRepository groupRepository) {
        super(groupRepository);
    }

    @Override
    public void processMessage(GinasProcessingMessage gpm) {
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
}
