package ix.ginas.utils.validation.validators.fda;

import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.initializers.NameStandardizer;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.AbstractValidatorPlugin;
import play.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StandardNameValidator extends AbstractValidatorPlugin<Substance>{

	private String regenerateNameValue = "";
	
    
	private boolean warningOnMismatch = true;

	public StandardNameValidator(){
	}

	@Override
	public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {
		NameStandardizer standardizer = new NameStandardizer();
		Map<String, Name> oldNames=new HashMap<String, Name>();
		
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
		
		if(objold!=null){
			objold.names.forEach(n->{
				oldNames.put(n.uuid.toString(), n);
			});
		}
		objnew.names.forEach((name) -> {      
			Logger.debug("in StandardNameValidator, Name " + name.name);
			
			boolean wasNull=false;
			Name oldName = oldNames.get(name.getOrGenerateUUID().toString());
			String oldStdNameGiven=null;
			String oldStdNameCalc=null;
			String oldRegularName=null;
			
			if(oldName!=null){
				oldStdNameCalc=standardizer.standardize(oldName.name);
				oldStdNameGiven=oldName.stdName;
				oldRegularName=oldName.name;
			}
			
			if (name.stdName == null) {
				wasNull=true;
				name.stdName=oldStdNameGiven;
			}
			
			if (name.stdName !=null && name.stdName.equals(regenerateNameValue)){
				name.stdName = null;
			}
			
			if(name.stdName == null){
				name.stdName = standardizer.standardize(name.name);
				Logger.debug("set (previously null) stdName to " + name.stdName);
			}else {
				Logger.trace("stdName: " + name.stdName);
				if (nameHasUnacceptableChar(name.stdName)) {
					String message = String.format("Names must contain only uppercase, numbers and other ASCII characters and may not have leading or trailing spaces or braces or brackets.  This name contains one or more non-allowed character: '%s'",
							name.stdName);
					callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(message));
				}
				Logger.trace("warningOnMismatch: " + warningOnMismatch);
				String newlyStandardizedName = standardizer.standardize(name.name);
				
				if(!newlyStandardizedName.equals(name.stdName)){
					if(name.stdName.equals(oldStdNameGiven)){
						if(oldStdNameGiven.equals(oldStdNameCalc)){
							//The old name was default standardized, so the new name should be too
							//no need to warn.
							name.stdName = newlyStandardizedName;
							Logger.debug("set (previously standardized) stdName to " + name.stdName);
						}else{
							// If the stdName WAS out-of sync, and the name has changed,
							// the standardized name should probably be regenerated. Unless
							// the user explicitly included a the standardized name in the
							// submission
							if(!name.name.equals(oldRegularName)){
								if(wasNull){
									if(warningOnMismatch){
										String message = String.format("Previous standardized name '%s' does not agree with newly generated standardized name '%s'. Newly generated standardized name will be used.",
												name.stdName, newlyStandardizedName);
										callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(message).appliableChange(true)
												);
										name.stdName = newlyStandardizedName;
									}else{
										name.stdName = newlyStandardizedName;
									}
								}else{
									if(warningOnMismatch){
										String message = String.format("Previous standardized name '%s' does not agree with newly generated standardized name '%s'. Keeping previous standardized name. Remove standardized name to regenerate instead.",
												name.stdName, newlyStandardizedName);
										callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(message));
									}
								}
							}else{
								// We COULD show warnings here, but don't have to
								// these warnings would be for the case where the name has NOT been edited
								// and those kinds of validation warnings tend to be a bit excessive
							}
						}
					}else{
						if(warningOnMismatch) {
							String message = String.format("Provided standardized name '%s' does not agree with newly standardized name '%s'. Provided standardized name will be used.",
									name.stdName, newlyStandardizedName);
							callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(message));
						}
					}
				}
				
				
			}
		});
	}

	public boolean nameHasUnacceptableChar(String name) {
		Pattern asciiPattern = Pattern.compile("\\A\\p{ASCII}*\\z");
		Matcher asciiMatcher = asciiPattern.matcher(name);
		Pattern lowerCasePattern = Pattern.compile("[a-z]+");
		Matcher lowerCaseMatcher = lowerCasePattern.matcher(name);
		List<Character> dirtyChars = Arrays.asList('\t', '\r', '\n', '`', '{', '}');

		if (name == null || name.length() == 0) {
			return false;
		}

		if (lowerCaseMatcher.find()) {
			return true;
		}

		if ((!name.trim().equals(name))) {
			return true;
		}

		if (!asciiMatcher.find()) {
			return true;
		}
		for (char testChar : name.toCharArray()) {
			if (dirtyChars.contains(testChar)) {
				return true;
			}
		}

		//square brackets are OK when they provide a name qualification, as int 'NAME [ORGANIZATION]'
		// but not OK otherwise, as in '[1,4]DICHLOROBENZENE' or 'NAME[SOMETHING]' (without a space before '[')
		int initBracketPos = name.indexOf("[");
		int closeBracketPos = name.indexOf("]");
		return initBracketPos > -1
						&& ((name.charAt(initBracketPos - 1) != ' ')
						|| (closeBracketPos < (name.length() - 1)));
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

}
