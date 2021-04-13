package fda.gsrs.substance.validators;

import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FDAExtraNamesValidator extends AbstractValidatorPlugin<Substance> {
	final static String badPatternsMessage =
			"Non-conforming patterns were found in the name: [%s]. "
					+ "The bad-pattern identifiers and their position of first occurance "
					+ "are as follows: [%s].";
	final static LinkedHashMap<String, Pattern> badPatternsMap = new LinkedHashMap<String, Pattern>();
	static {
		badPatternsMap.put("DOUBLE QUOTE", Pattern.compile("\""));
		badPatternsMap.put("LINE FEED", Pattern.compile("\n"));
		badPatternsMap.put("CARRIAGE RETURN", Pattern.compile("\r"));
		badPatternsMap.put("TAB", Pattern.compile("\t"));
	};

	@Override
	public void validate(Substance s, Substance objold, ValidatorCallback callback) {

		// assume s.names.isEmpty() is OK because checking for this is done elsewhere.
		for(Name n : s.names) {
			if (n.name != null) {
				// System.out.println("The names is: "+n.name);
				LinkedHashMap<String, Integer> badPatternsResultMap = checkNameForBadPatterns(n.name);
				if(!badPatternsResultMap.isEmpty()) {
					GinasProcessingMessage mes = GinasProcessingMessage.ERROR_MESSAGE(formatBadPatternsMessage(n.getName(), badPatternsResultMap));
					callback.addMessage(mes);
				}
			}
		}
	}

	public static LinkedHashMap<String, Integer> checkNameForBadPatterns(String name) {
		LinkedHashMap<String, Integer> resultMap =  new LinkedHashMap<String,Integer>();
		for (Map.Entry<String, Pattern> entry : badPatternsMap.entrySet()) {
			Matcher matcher = entry.getValue().matcher(name);
			if(matcher.find()) {
				resultMap.put(entry.getKey(), matcher.start()+1);
			}
		}
		return resultMap;
	}

	public static String formatBadPatternsMessage(String name, LinkedHashMap<String, Integer> resultMap) {
		ArrayList<String> errors = new ArrayList<String>();
		for (Map.Entry<String, Integer> entry : resultMap.entrySet()) {
			errors.add(entry.getKey() + " (" + String.valueOf(entry.getValue()) + ")");
		}
		return  String.format(badPatternsMessage, name,  String.join("; ", errors));
	}
}