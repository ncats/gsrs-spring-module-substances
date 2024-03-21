package ix.ginas.utils;

import ix.core.FieldNameDecorator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubstanceFieldNameDecorator implements FieldNameDecorator {
    private static final Pattern DYNAMIC_CODE_SYSTEM = Pattern.compile("root_codes_(.*)");
    private static final SubstanceFieldNameDecoratorConfiguration displayNames = SubstanceFieldNameDecoratorConfiguration.INSTANCE();

    @Override
    public String getDisplayName(String field) {
        String fdisp=displayNames.get(field);
        if(fdisp==null){
            Matcher m=DYNAMIC_CODE_SYSTEM.matcher(field);
            if(m.find()){
                return m.group(1);
            }
        }
        return fdisp;
    }
}
