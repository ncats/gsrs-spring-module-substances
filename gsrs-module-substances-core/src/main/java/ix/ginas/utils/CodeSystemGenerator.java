package ix.ginas.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import ix.ginas.models.v1.Substance;

public class CodeSystemGenerator implements NamedIdGenerator<Substance, String>,SubstanceApprovalIdGenerator {


    private final String name;
    private final String codeSystem;
    private final String suffix;

    @JsonCreator
    public CodeSystemGenerator( @JsonProperty("name") String name,
                                @JsonProperty("codeSystem") String codeSystem,
                                @JsonProperty("suffix") String suffix) {
        this.name = name;
        this.codeSystem = codeSystem;
        this.suffix = suffix;
    }

    private static String modulo10(String candidate) {
        String clean = candidate.trim().replace("-", "") + "0";
        char[] chars = clean.toCharArray();
        if (chars.length == 0) {
            return "";
        }
        for (char c : chars) {
            if (!Character.isDigit(c)) {
                return "";
            }
        }
        int sum = 0;
        for (int i = chars.length-1; i >0; i--) {
            int position = chars.length-i;
            sum += (position * Character.getNumericValue(chars[i-1]));
        }
        return String.valueOf(sum % 10);
    }

    private static String modulo11(String candidate) {
        String clean = candidate.trim().replace("-", "");
        char[] chars = clean.toCharArray();
        if (chars.length == 0) {
            return "";
        }
        for (char c : chars) {
            if (!Character.isDigit(c)) {
                return "";
            }
        }
        int sum = 0;
        for (int i = 0; i < chars.length; i++){
            sum += ((i + 2) * Character.getNumericValue(chars[i]));
        }
        return String.valueOf((sum % 11) % 10);
    }

    @Override
    public synchronized String generateId(Substance s) {
        String code = s.codes.stream().filter(c -> (codeSystem.equals(c.codeSystem) && "PRIMARY".equals(c.type))).findFirst()
            .map(c -> c.getCode()).orElse("");
        if (suffix != null) {
            code = code + suffix.replace("{{mod10}}", modulo10(code)).replace("{{mod11}}", modulo11(code));
        }
        return code;
    }


    @Override
    public boolean isValidId(String id) {
        if (id.endsWith(id)) {
            return true;
        }
        return false;
    }

    @Override
    public String getName() {
        return name;
    }
}
