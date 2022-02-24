package ix.ginas.utils;

import gsrs.services.GroupService;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class BatchProcessingStrategy extends AcceptApplyAllProcessingStrategy{
    @Autowired
    public BatchProcessingStrategy(GroupService groupRepository) {

        super(groupRepository);
        this.markFailed();
    }

    @Override
    public void setIfValid(ValidationResponse resp, List<GinasProcessingMessage> messages) {
        //mark everything valid?
        resp.setValid(true);
    }
}
