package gsrs.module.substance;

import java.lang.annotation.*;

@Documented
@Retention(value= RetentionPolicy.RUNTIME)
@Inherited
@Target(value={ElementType.FIELD})
public @interface SubstanceOwnerReference {
}
