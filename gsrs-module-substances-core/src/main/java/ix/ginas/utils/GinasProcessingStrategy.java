package ix.ginas.utils;

import gsrs.services.GroupService;
import ix.core.models.Group;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategy;

import java.util.*;
import java.util.stream.Collectors;

public abstract class GinasProcessingStrategy implements GsrsProcessingStrategy {
	public static final String FAILED = "FAILED";
	public static final String WARNING = "WARNING";
	public static final String FAIL_REASON = "FAIL_REASON";

	private final GroupService groupRepository;

	public GinasProcessingStrategy(GroupService groupRepository){
		this.groupRepository = Objects.requireNonNull(groupRepository);
	}
	//TODO: add messages directly here
	public List<GinasProcessingMessage> _localMessages = new ArrayList<GinasProcessingMessage>();
	@Override
	public abstract void processMessage(GinasProcessingMessage gpm);
	@Override
	public boolean test(GinasProcessingMessage gpm){
		processMessage(gpm);
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

	public static enum HANDLING_TYPE {
		MARK, FAIL, FORCE_IGNORE, NOTE
	};

	public HANDLING_TYPE failType = HANDLING_TYPE.MARK;
	public HANDLING_TYPE warningHandle = HANDLING_TYPE.MARK;

	//TODO katzelda Feb 2021: moved implementations to their own classes so we can inject depdendencies
//	private static GinasProcessingStrategy _ACCEPT_APPLY_ALL = new GinasProcessingStrategy() {
//		@Override
//		public void processMessage(GinasProcessingMessage gpm) {
//			if (gpm.suggestedChange){
//				gpm.actionType = GinasProcessingMessage.ACTION_TYPE.APPLY_CHANGE;
//			}else{
//				if(gpm.isError()){
//					gpm.actionType= GinasProcessingMessage.ACTION_TYPE.FAIL;
//				}else{
//					gpm.actionType = GinasProcessingMessage.ACTION_TYPE.IGNORE;
//				}
//			}
//		}
//	};

//	private static GinasProcessingStrategy _ACCEPT_APPLY_ALL_WARNINGS = new GinasProcessingStrategy() {
//		@Override
//		public void processMessage(GinasProcessingMessage gpm) {
//			if (gpm.messageType == GinasProcessingMessage.MESSAGE_TYPE.ERROR) {
//				gpm.actionType = GinasProcessingMessage.ACTION_TYPE.FAIL;
//			} else {
//				if (gpm.suggestedChange){
//					gpm.actionType = GinasProcessingMessage.ACTION_TYPE.APPLY_CHANGE;
//				}else{
//					gpm.actionType = GinasProcessingMessage.ACTION_TYPE.IGNORE;
//				}
//			}
//		}
//	};
	
//	public static GinasProcessingStrategy fromValue(String value){
//		switch(value.toUpperCase()){
//			case "ACCEPT_APPLY_ALL":
//				return ACCEPT_APPLY_ALL();
//			case "ACCEPT_APPLY_ALL_WARNINGS":
//				return ACCEPT_APPLY_ALL_WARNINGS();
//			case "ACCEPT_APPLY_ALL_WARNINGS_MARK_FAILED":
//				return ACCEPT_APPLY_ALL_WARNINGS_MARK_FAILED();
//			case "ACCEPT_APPLY_ALL_MARK_FAILED":
//				return ACCEPT_APPLY_ALL_MARK_FAILED();
//			case "ACCEPT_APPLY_ALL_NOTE_FAILED":
//				return ACCEPT_APPLY_ALL_NOTE_FAILED();
//			basic:
//				throw new IllegalArgumentException("No strategy known with name:\"" + value + "\"");
//		}
//
//	}


//	public static GinasProcessingStrategy ACCEPT_APPLY_ALL() {
//		return _ACCEPT_APPLY_ALL;
//	}
//
//	public static GinasProcessingStrategy ACCEPT_APPLY_ALL_WARNINGS() {
//		return _ACCEPT_APPLY_ALL_WARNINGS;
//	}
//
//
//	public static GinasProcessingStrategy ACCEPT_APPLY_ALL_WARNINGS_MARK_FAILED() {
//		return ACCEPT_APPLY_ALL_WARNINGS().markFailed();
//	}
//
//	public static GinasProcessingStrategy ACCEPT_APPLY_ALL_WARNINGS_NOTE_FAILED() {
//		return ACCEPT_APPLY_ALL_WARNINGS().markFailed();
//	}
//
//	public static GinasProcessingStrategy ACCEPT_APPLY_ALL_MARK_FAILED() {
//		return ACCEPT_APPLY_ALL().markFailed();
//	}
//	public static GinasProcessingStrategy ACCEPT_APPLY_ALL_NOTE_FAILED() {
//		return ACCEPT_APPLY_ALL().noteFailed();
//	}
//

	public GinasProcessingStrategy markFailed() {
		this.failType = HANDLING_TYPE.MARK;
		return this;
	}

	public GinasProcessingStrategy noteFailed() {
		this.failType = HANDLING_TYPE.NOTE;
		return this;
	}

	public GinasProcessingStrategy failFailed() {
		this.failType = HANDLING_TYPE.FAIL;
		return this;
	}

	public GinasProcessingStrategy forceIgnoreFailed() {
		this.failType = HANDLING_TYPE.FORCE_IGNORE;
		return this;
	}

	@Override
	public boolean handleMessages(Substance cs, List<GinasProcessingMessage> list) {
		boolean allow=true;
		final String noteFailed="Imported record has some validation issues and should not be considered authoratiative at this time";
		Map<String, Group> cache = new HashMap<>();
		for (GinasProcessingMessage gpm : list) {
			//TODO katzelda May 2021 : why was this here?  was this just a combination of different strategies?
//			if(gpm.isError() && gpm.appliedChange){
//				gpm.messageType= GinasProcessingMessage.MESSAGE_TYPE.WARNING;
//			}
			
			if (gpm.actionType == GinasProcessingMessage.ACTION_TYPE.FAIL || gpm.isError()) {

				if (failType == HANDLING_TYPE.FAIL) {
					throw new IllegalStateException(gpm.message);

				} else if (failType == HANDLING_TYPE.MARK) {
					cs.status = GinasProcessingStrategy.FAILED;
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

	private Group getGroupByName(String groupName){
		return groupRepository.registerIfAbsent(groupName);
	}
	@Override
	public void addProblems(Substance cs, List<GinasProcessingMessage> list) {
		if (warningHandle == HANDLING_TYPE.MARK) {
			List<GinasProcessingMessage> problems = list.stream()
				.filter(f->f.isProblem())
				.collect(Collectors.toList());
			if(!problems.isEmpty()){
				Map<String, Group> cache = new HashMap<>();
				cs.setValidationMessages(problems, n-> getGroupByName(n));
			}			
		}
	}

}
