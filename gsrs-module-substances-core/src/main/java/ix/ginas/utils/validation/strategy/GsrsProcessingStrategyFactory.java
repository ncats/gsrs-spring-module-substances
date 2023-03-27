package ix.ginas.utils.validation.strategy;

import gsrs.services.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Factory class that creates new {@link GsrsProcessingStrategy}
 * instances.
 */
@Service
public class GsrsProcessingStrategyFactory {

    private final GroupService groupService;
    private final GsrsProcessingStrategyFactoryConfiguration configuration;

    @Autowired
    public GsrsProcessingStrategyFactory(GroupService groupService,
            GsrsProcessingStrategyFactoryConfiguration configuration) {
        this.groupService = groupService;
        this.configuration = configuration;
    }

    public GsrsProcessingStrategy createNewDefaultStrategy(){
        return createNewStrategy(configuration.getDefaultStrategy());
    }

    /**
     * Create a new {@link GsrsProcessingStrategy} based on the GSRS 2.x strategy names
     * (ACCEPT_APPLY_ALL, ACCEPT_APPLY_ALL_WARNINGS_MARK_FAILED etc).
     * @param strategyName the strategy name, can not be null.
     * @return a new GsrsProcessingStrategy instance.
     * @throws IllegalArgumentException if unknown strategy.
     */
    public GsrsProcessingStrategy createNewStrategy(String strategyName){
        Objects.requireNonNull(strategyName, "GsrsProcessingStrategy strategyName is null; please configure the property gsrs.processing-strategy.");
        switch(strategyName.toUpperCase()){
            case "ACCEPT_APPLY_ALL":
                return (GsrsProcessingStrategy) new AcceptApplyAllProcessingStrategy(groupService, configuration);
            case "ACCEPT_APPLY_ALL_WARNINGS":
                return (GsrsProcessingStrategy) new AcceptAndApplyAllWarningsProcessingStrategy(groupService, configuration);
            case "ACCEPT_APPLY_ALL_WARNINGS_MARK_FAILED":
                return (GsrsProcessingStrategy) new AcceptAndApplyAllWarningsProcessingStrategy(groupService, configuration)
                            .markFailed();
            case "ACCEPT_APPLY_ALL_MARK_FAILED":
                return (GsrsProcessingStrategy) new AcceptApplyAllProcessingStrategy(groupService, configuration)
                            .markFailed();
            case "ACCEPT_APPLY_ALL_NOTE_FAILED":
                return (GsrsProcessingStrategy) new AcceptApplyAllProcessingStrategy(groupService, configuration)
                            .noteFailed();
            default:
                throw new IllegalArgumentException("No strategy known with name:\"" + strategyName + "\"");
        }
    }
}
