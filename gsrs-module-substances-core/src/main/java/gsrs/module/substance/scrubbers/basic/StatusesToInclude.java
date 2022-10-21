package gsrs.module.substance.scrubbers.basic;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@Generated("jsonschema2pojo")
public enum StatusesToInclude {

    APPROVED("approved"),
    ALTERNATIVE("alternative"),
    APPROVED_SUBCONCEPT("approved subconcept"),
    PENDING_SUBCONCEPT("pending subconcept"),
    PENDING("pending"),
    FAILED("failed"),
    CONCEPT("concept");
    private final String value;
    private final static Map<String, StatusesToInclude> CONSTANTS = new HashMap<String, StatusesToInclude>();

    static {
        for (StatusesToInclude c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    StatusesToInclude(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    @JsonValue
    public String value() {
        return this.value;
    }

    @JsonCreator
    public static StatusesToInclude fromValue(String value) {
        StatusesToInclude constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}
