package gsrs.module.substance.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
Routines that process files for some general purpose
 */
@Slf4j
public class NCATSFileUtils {

    private static final Pattern sdFileFieldPattern = Pattern.compile("> +<(.*)>");

    /*
    Read all lines of an SD file and compile a list of unique field names.
    Each field name occurs only once in the output.
     */
    public static Set<String> getSdFileFields(String sdFilePath) throws IOException {
        Set<String> fields = new HashSet<>();
        for (String line : Files.readAllLines(Paths.get(sdFilePath))) {
            Matcher matcher = sdFileFieldPattern.matcher(line);
            if( matcher.find()) {
                String fieldName = matcher.group(1);
                log.trace("processing field name: " + fieldName);
                fields.add(fieldName);
            }
        }

        return fields;
    }
}
