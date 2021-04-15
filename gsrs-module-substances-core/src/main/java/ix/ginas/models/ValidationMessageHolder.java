package ix.ginas.models;

import ix.core.models.Group;
import ix.core.validator.GinasProcessingMessage;

import java.util.List;


public interface ValidationMessageHolder {
	void setValidationMessages(List<GinasProcessingMessage> messages, Callback callback);
	default List<GinasProcessingMessage> getValidationMessages(){
		throw new UnsupportedOperationException("Not implemented yet");
	}

	interface Callback{
		Group getGroupByName(String groupName);
	}
}
