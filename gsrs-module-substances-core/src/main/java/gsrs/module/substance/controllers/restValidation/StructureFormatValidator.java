package gsrs.module.substance.controllers.restValidation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class StructureFormatValidator implements
        ConstraintValidator<ValidStructureFormat, String> {
    private static Set<String> FORMATS;
    static{
        FORMATS = new HashSet<>();
        FORMATS.add("mol");
        FORMATS.add("sdf");
        FORMATS.add("smi");
        FORMATS.add("smiles");

    }
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return FORMATS.contains(value.toLowerCase(Locale.ROOT));
    }
}
