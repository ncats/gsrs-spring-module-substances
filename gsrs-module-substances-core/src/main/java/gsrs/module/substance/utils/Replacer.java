package gsrs.module.substance.utils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Replacer {

    Pattern p;
    String replace;
    String message = "String \"$0\" matches forbidden pattern";
    String postFixMessage = "";

    public Replacer(String regex, String replace) {
        this.p = Pattern.compile(regex);
        this.replace = replace;
    }

    public boolean matches(String test) {
        if(test==null) {
            return false;
        }
        return this.p.matcher(test).find();
    }

    public String fix(String test, AtomicInteger firstMatch) {
        Matcher m = p.matcher(test);
        if (m.find() && firstMatch != null) {
            firstMatch.set(m.start());
        }
        postFixMessage = getMessage(m.group());
        return test.replaceAll(p.pattern(), replace);
    }

    public Replacer message(String msg) {
        this.message = msg;
        return this;
    }

    public String getMessage(String test) {
        return message.replace("$0", test);
    }

    public String getPostFixMessage() {
        return postFixMessage;
    }
}