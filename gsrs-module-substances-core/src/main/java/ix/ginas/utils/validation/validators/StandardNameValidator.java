package ix.ginas.utils.validation.validators;

import java.util.HashMap;
import java.util.Map;

import gsrs.module.substance.standardizer.NameStandardizer;
import gsrs.module.substance.standardizer.ReplacementResult;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author mitch
 */
@Slf4j
public class StandardNameValidator extends AbstractValidatorPlugin<Substance> {

    @Autowired
    private NameStandardizer nameStandardizer;

    @Autowired
    private NameStandardizer stdNameStandardizer;

    public enum InvalidStdNameBehavior {
        warn,
        error
    }

    private String regenerateNameValue = "";
    private boolean warningOnMismatch = true;

    private InvalidStdNameBehavior invalidStdNameBehavior= InvalidStdNameBehavior.error;

    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {
        log.debug("nameStandardizer class=" + nameStandardizer.getClass().getName());
        if(nameStandardizer!=null)validateInPlace(objnew, objold, callback);
        log.debug("stdNameStandardizer class=" + stdNameStandardizer.getClass().getName());
        if(stdNameStandardizer!=null)validateFull(objnew, objold, callback);
    }

    // validateFull does a more comprehensive set of standardizations for the standard name field.
    // validateInPlace does a minimal replacement of 'awful' characters - for  the main 'name' field.


    public void validateFull(Substance objnew, Substance objold, ValidatorCallback callback) {
        log.trace("starting in validate");
        Map<String, Name> oldNames = new HashMap<>();

        //
        // The options here are:
        //  1. There is a NULL provided stdName, and the OLD stdName was non-null
        //       in which case, the stdName is set to be the old stdName, wasNull flag set to true
        //  2. There is a NULL provided stdName, and there is no OLD stdName (or it's a new name)
        //       in which case, the stdName remains null
        //  3. There is a provided non-null stdName that equals regenerateNameValue
        //       in which case, the stdName is set to null
        //  4. There is some other non-null provided stdName
        //       in which case, the stdName remains as provided
        //
        //  Then, on standardization:
        //  A. If there is a null stdname: 
        //         generate an stdName.
        //  B. If there is a non-null stdName, it is the SAME stdName as the old
        //      stdName, AND standardizing the OLD name would yield the old
        //      stdName: 
        //         regenerate the stdName and DO NOT warn.
        //  C. If there is a non-null stdName, it is the SAME stdName as the old
        //      stdName, AND standardizing the OLD name would NOT yield the old
        //      stdName AND the new regular name is DIFFERENT than the old
        //      regular name AND the provided stdName WAS ORIGINALLY null: 
        //         regenerate the stdName but DO warn that it's being
        //         regenerated.
        //  D. If there is a non-null stdName, it is the SAME stdName as the old
        //      stdName, AND standardizing the OLD name would NOT yield the old
        //      stdName AND the new regular name is DIFFERENT than the old
        //      regular name AND the provided stdName WAS originally NON-NULL: 
        //         keep provided stdName, but warn of discrepency and
        //         tell user to explicitly remove stdName to regenerate
        //  E. If there is a non-null stdName, it is the SAME stdName as the old
        //      stdName, AND standardizing the OLD name would NOT yield the old
        //      stdName AND the new regular name is the SAME as the old
        //      regular name: 
        //         keep the stdName provided and do not warn
        //  F. If there is a non-null stdName and it is NOT the same as
        //     the old stdName (or there is no old stdName):
        //          warn the user of the mismatch, KEEP PROVIDED stdName
        //        
        if (objold != null) {
            objold.names.forEach(n -> {
                oldNames.put(n.uuid.toString(), n);
            });
        }
        objnew.names.forEach((Name name) -> {
            log.trace("in StandardNameValidator, Name " + name.name);

            boolean wasNull = false;
            Name oldName = oldNames.get(name.getOrGenerateUUID().toString());
            String oldStdNameGiven = null;
            String oldStdNameCalc = null;
            String oldRegularName = null;
            boolean warnedAboutThisNameStandardization = false;

            if (oldName != null) {
                oldStdNameCalc = stdNameStandardizer.standardize(oldName.name).getResult();
                oldStdNameGiven = oldName.stdName;
                oldRegularName = oldName.name;
            }

            if (name.stdName == null) {
                wasNull = true;
                name.stdName = oldStdNameGiven;
            }

            if (name.stdName != null && name.stdName.equals(regenerateNameValue)) {
                name.stdName = null;
            }

            if (name.stdName == null) {
                name.stdName = stdNameStandardizer.standardize(name.name).getResult();
                log.debug("set (previously null) stdName to " + name.stdName);
            }
            else {
                log.trace("stdName: " + name.stdName);
                if (!stdNameStandardizer.isStandardized(name.stdName)) {
                    warnedAboutThisNameStandardization =true;
                    String message = String.format("Standardized name does not meet standards.  This name may contain one or more non-allowed character: '%s'",
                            name.stdName);
                    if( invalidStdNameBehavior== InvalidStdNameBehavior.error) {
                        callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(message));
                    }else {
                        callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(message));
                    }
                }
                log.trace("warningOnMismatch: " + warningOnMismatch);
                String newlyStandardizedName = stdNameStandardizer.standardize(name.name).getResult();

                if (!newlyStandardizedName.equals(name.stdName)) {
                    if (name.stdName.equals(oldStdNameGiven)) {
                        if (oldStdNameGiven.equals(oldStdNameCalc)) {
                            //The old name was basic standardized, so the new name should be too
                            //no need to warn.
                            name.stdName = newlyStandardizedName;
                            log.debug("set (previously standardized) stdName to " + name.stdName);
                        }
                        else {
                            // If the stdName WAS out-of sync, and the name has changed,
                            // the standardized name should probably be regenerated. Unless
                            // the user explicitly included a the standardized name in the
                            // submission
                            if (!name.name.equals(oldRegularName)) {
                                if (wasNull) {
                                    if (warningOnMismatch) {
                                        String message = String.format("Previous standardized name '%s' does not agree with newly generated standardized name '%s'. Newly generated standardized name will be used.",
                                                name.stdName, newlyStandardizedName);
                                        callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(message).appliableChange(true)
                                        );
                                        name.stdName = newlyStandardizedName;
                                    }
                                    else {
                                        name.stdName = newlyStandardizedName;
                                    }
                                }
                                else {
                                    if (warningOnMismatch) {
                                        String message = String.format("Previous standardized name '%s' does not agree with newly generated standardized name '%s'. Keeping previous standardized name. Remove standardized name to regenerate instead.",
                                                name.stdName, newlyStandardizedName);
                                        callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(message));
                                    }
                                }
                            }
                            else {
                                // We COULD show warnings here, but don't have to
                                // these warnings would be for the case where the name has NOT been edited
                                // and those kinds of validation warnings tend to be a bit excessive
                            }
                        }
                    }
                    else {
                        if (warningOnMismatch && !warnedAboutThisNameStandardization) {
                            String message = String.format("Provided standardized name '%s' does not agree with newly standardized name '%s'. Provided standardized name will be used.",
                                    name.stdName, newlyStandardizedName);
                            callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(message));
                        }
                    }
                }

            }
        });
    }

    
    public void validateInPlace(Substance s, Substance objold, ValidatorCallback callback) {
        log.trace("starting in validate");
        if (s == null) {
            log.warn("Substance is null");
            return;
        }
        if (s.names == null || s.names.isEmpty()) {
            //do not expect this to happen -- substance will be tested for no names
            log.warn("Substance has no names!");
        }

        s.names.forEach(n -> {

            ReplacementResult minimallyStandardizedName = nameStandardizer.standardize(n.name);
            String debugMessage = String.format("name: %s; minimallyStandardizedName: %s", n.name,
                    minimallyStandardizedName.getResult());
            log.trace(debugMessage);

            if (!minimallyStandardizedName.getResult().equals(n.name) || minimallyStandardizedName.getReplacementNotes().size() > 0) {
                GinasProcessingMessage mes = GinasProcessingMessage.WARNING_MESSAGE(String.format("Name %s minimally standardized to %s",
                        n.name, minimallyStandardizedName.getResult()));
                mes.appliableChange(true);
                callback.addMessage(mes, () -> {
                    n.name = minimallyStandardizedName.getResult();
                });
            }
        });
    }
    public boolean isWarningOnMismatch() {
        return warningOnMismatch;
    }

    public void setWarningOnMismatch(boolean warningOnMismatch) {
        this.warningOnMismatch = warningOnMismatch;
    }

    public String getRegenerateNameValue() {
        return regenerateNameValue;
    }

    public void setRegenerateNameValue(String regenerateNameValue) {
        this.regenerateNameValue = regenerateNameValue;
    }

    public InvalidStdNameBehavior isErrorOnInvalidStdName() {
        return invalidStdNameBehavior;
    }

    public void setBehaviorOnInvalidStdName(String behavior) {
        this.invalidStdNameBehavior = InvalidStdNameBehavior.valueOf(behavior);
    }

}
