package ix.ginas.models.converters;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.UUIDJavaType;

import java.util.UUID;

public class TrimmedUUIDJavaType extends UUIDJavaType {

    @Override
    public UUID fromString(CharSequence string) {
        return super.fromString(string == null ? null : string.toString().trim());
    }

    @Override
    public <X> UUID wrap(X value, WrapperOptions options) {
        if (value instanceof String stringValue) {
            return super.wrap(stringValue.trim(), options);
        }
        return super.wrap(value, options);
    }
}
