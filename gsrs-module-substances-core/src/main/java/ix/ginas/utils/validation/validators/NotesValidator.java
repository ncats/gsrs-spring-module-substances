package ix.ginas.utils.validation.validators;

import gsrs.module.substance.repository.ReferenceRepository;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Note;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by katzelda on 5/14/18.
 */
public class NotesValidator extends AbstractValidatorPlugin<Substance> {
	
	private final String NotesValidatorNullWarning1 = "NotesValidatorNullWarning1";
	private final String NotesValidatorNullWarning2 = "NotesValidatorNullWarning2"; 

    @Autowired
    private ReferenceRepository referenceRepository;

    public ReferenceRepository getReferenceRepository() {
        return referenceRepository;
    }

    public void setReferenceRepository(ReferenceRepository referenceRepository) {
        this.referenceRepository = referenceRepository;
    }

    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {

        Iterator<Note> iter = s.notes.iterator();
        while(iter.hasNext()){
            Note n = iter.next();
            if (n == null) {
                GinasProcessingMessage mes = GinasProcessingMessage
                        .WARNING_MESSAGE(NotesValidatorNullWarning1, "Null note objects are not allowed")
                        .appliableChange(true);
                callback.addMessage(mes, () -> {
                    iter.remove();
                });
                continue;
            }
            if (n.note == null) {
                GinasProcessingMessage mes = GinasProcessingMessage
                        .WARNING_MESSAGE(NotesValidatorNullWarning2,"Note objects must have a populated note field. Setting to empty String")
                        .appliableChange(true);
                callback.addMessage(mes, () -> n.note= "");
                continue;
            }
            if (!ValidationUtils.validateReference(s,n, callback, ValidationUtils.ReferenceAction.ALLOW, referenceRepository)) {
                //TODO should we really return here and not check the others?
                break ;
            }
        }

        //don't know if we need this but this way a new reference is made so ebean knows we changed stuff
        s.notes = new ArrayList<>(s.notes);

    }
}
