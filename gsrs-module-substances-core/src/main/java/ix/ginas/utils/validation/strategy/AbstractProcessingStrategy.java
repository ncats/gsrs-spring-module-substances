package ix.ginas.utils.validation.strategy;

import gsrs.security.GsrsSecurityUtils;
import gsrs.services.GroupService;
import ix.core.models.Group;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.models.v1.Substance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class AbstractProcessingStrategy implements GsrsProcessingStrategy {
    public static final String FAILED = "FAILED";
    public static final String WARNING = "WARNING";
    public static final String FAIL_REASON = "FAIL_REASON";
    public static enum HANDLING_TYPE {
        MARK, FAIL, FORCE_IGNORE, NOTE
    };
    public HANDLING_TYPE failType = HANDLING_TYPE.MARK;
    public HANDLING_TYPE warningHandle = HANDLING_TYPE.MARK;
    //TODO: add messages directly here
    public List<GinasProcessingMessage> _localMessages = new ArrayList<GinasProcessingMessage>();

    private GroupService groupRepository;
    private GsrsProcessingStrategyFactoryConfiguration configuration;

    public AbstractProcessingStrategy(GroupService groupRepository,
            GsrsProcessingStrategyFactoryConfiguration configuration){
        this.groupRepository = Objects.requireNonNull(groupRepository);
        this.configuration = Objects.requireNonNull(configuration);
    }

    @Override
    public abstract void processMessage(GinasProcessingMessage gpm);

    @Override
    public boolean test(GinasProcessingMessage gpm){
        this.processMessage(gpm);
        return gpm.actionType == GinasProcessingMessage.ACTION_TYPE.APPLY_CHANGE;
    }

    @Override
    public void addAndProcess(List<GinasProcessingMessage> source, List<GinasProcessingMessage> destination){
        for(GinasProcessingMessage gpm: source){
            this.processMessage(gpm);
            destination.add(gpm);
        }
    }

    @Override
    public abstract void setIfValid(ValidationResponse validationResponse,  List<GinasProcessingMessage> messages);

    @Override
    public boolean handleMessages(Substance cs, List<GinasProcessingMessage> list) {
        boolean allow=true;
        final String noteFailed="Imported record has some validation issues and should not be considered authoratiative at this time";
        Map<String, Group> cache = new HashMap<>();
        for (GinasProcessingMessage gpm : list) {
            //TODO katzelda May 2021 : why was this here?  was this just a combination of different strategies?
//            if(gpm.isError() && gpm.appliedChange){
//                gpm.messageType= GinasProcessingMessage.MESSAGE_TYPE.WARNING;
//            }

            if (gpm.actionType == GinasProcessingMessage.ACTION_TYPE.FAIL || gpm.isError()) {

                if (failType == HANDLING_TYPE.FAIL) {
                    throw new IllegalStateException(gpm.message);

                } else if (failType == HANDLING_TYPE.MARK) {
                    cs.status = AbstractProcessingStrategy.FAILED;
                    cs.addRestrictGroup(getGroupByName(Substance.GROUP_ADMIN));

                } else if (failType == HANDLING_TYPE.NOTE) {
                    boolean hasNoteYet=cs.getNotes().stream()
                                                    .filter(n->n.note.equals(noteFailed))
                                                    .findAny()
                                                    .isPresent();
                    if(!hasNoteYet){
                        cs.addNote(noteFailed);
                    }
                }
            }
        }
        return allow;
    }

    @Override
    public void addProblems(Substance cs, List<GinasProcessingMessage> list) {
        if (warningHandle == HANDLING_TYPE.MARK) {
            List<GinasProcessingMessage> problems = list.stream()
                .filter(f->(f.getMessageType().getPriority() < 2))
                .collect(Collectors.toList());
            if(!problems.isEmpty()){
                Map<String, Group> cache = new HashMap<>();
                cs.setValidationMessages(problems, n-> getGroupByName(n));
            }
        }
    }

    public AbstractProcessingStrategy markFailed() {
        this.failType = HANDLING_TYPE.MARK;
        return this;
    }

    public AbstractProcessingStrategy noteFailed() {
        this.failType = HANDLING_TYPE.NOTE;
        return this;
    }

    public AbstractProcessingStrategy failFailed() {
        this.failType = HANDLING_TYPE.FAIL;
        return this;
    }

    public AbstractProcessingStrategy forceIgnoreFailed() {
        this.failType = HANDLING_TYPE.FORCE_IGNORE;
        return this;
    }

    private Group getGroupByName(String groupName){
        return groupRepository.registerIfAbsent(groupName);
    }

    public void overrideMessage(GinasProcessingMessage gpm){
        Objects.requireNonNull(configuration.getOverrideRules(), "OverrideRules value is null; please configure the property gsrs.processing-strategy.");
        for (GsrsProcessingStrategyFactoryConfiguration.OverrideRule rule : configuration.getOverrideRules()) {
            if ((rule.getUserRoles() == null || GsrsSecurityUtils.hasAnyRoles(rule.getUserRoles())) && rule.getRegex().matcher(gpm.toString()).find()) {
                if (rule.getNewMessageType() != null) {
                    gpm.messageType = rule.getNewMessageType();
                }
                break;
            }
        }
    }
}
