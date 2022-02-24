package ix.ginas.utils;

import gsrs.services.GroupService;
import ix.core.validator.GinasProcessingMessage;
import org.springframework.beans.factory.annotation.Autowired;

public class BatchProcessingStrategy extends AcceptApplyAllProcessingStrategy{
    @Autowired
    public BatchProcessingStrategy(GroupService groupRepository) {

        super(groupRepository);
        this.markFailed();
    }

    @Override
    public void processMessage(GinasProcessingMessage gpm) {
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
}
