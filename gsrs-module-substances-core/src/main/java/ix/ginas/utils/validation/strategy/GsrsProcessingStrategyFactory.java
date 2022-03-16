package ix.ginas.utils.validation.strategy;

import gsrs.services.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Factory class that creates new {@link GsrsProcessingStrategy}
 * instances.
 */
@Service
public class GsrsProcessingStrategyFactory {

    private final GroupService groupService;

    @Autowired
    public GsrsProcessingStrategyFactory(GroupService groupService) {
        this.groupService = groupService;
    }
    public GsrsProcessingStrategy createNewDefaultStrategy(){
        return new AcceptApplyAllProcessingStrategy(groupService);
    }

    /**
     * Create a new {@link GsrsProcessingStrategy} based on the GSRS 2.x strategy names
     * (ACCEPT_APPLY_ALL, ACCEPT_APPLY_ALL_WARNINGS_MARK_FAILED etc).
     * @param strategyName the strategy name, can not be null.
     * @return a new GsrsProcessingStrategy instance.
     * @throws IllegalArgumentException if unknown strategy.
     */
    public GsrsProcessingStrategy createNewStrategy(String strategyName){
        switch(strategyName.toUpperCase()){
            case "ACCEPT_APPLY_ALL":
                return new AcceptApplyAllProcessingStrategy(groupService);
            case "ACCEPT_APPLY_ALL_WARNINGS":
                return new AcceptAndApplyAllWarningsProcessingStrategy(groupService);
            case "ACCEPT_APPLY_ALL_WARNINGS_MARK_FAILED":
                return new AcceptAndApplyAllWarningsProcessingStrategy(groupService)
                            .markFailed();
            case "ACCEPT_APPLY_ALL_MARK_FAILED":
                return new AcceptApplyAllProcessingStrategy(groupService)
                            .markFailed();
            case "ACCEPT_APPLY_ALL_NOTE_FAILED":
                return new AcceptApplyAllProcessingStrategy(groupService)
                            .noteFailed();
            default:
                throw new IllegalArgumentException("No strategy known with name:\"" + strategyName + "\"");
        }
    }
}
