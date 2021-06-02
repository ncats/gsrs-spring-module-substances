package gsrs.module.substance.controllers.restValidation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = StructureFormatValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidStructureFormat {
    String message() default "Invalid Structure format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
