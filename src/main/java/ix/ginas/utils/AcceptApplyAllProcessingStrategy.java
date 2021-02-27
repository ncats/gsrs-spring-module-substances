package ix.ginas.utils;

import gsrs.repository.GroupRepository;
import ix.core.validator.GinasProcessingMessage;
import org.springframework.beans.factory.annotation.Autowired;

public class AcceptApplyAllProcessingStrategy extends GinasProcessingStrategy{
    @Autowired
    public AcceptApplyAllProcessingStrategy(GroupRepository groupRepository) {
        super(groupRepository);
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
