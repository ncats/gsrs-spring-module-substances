package gsrs.module.substance.utils;

import lombok.extern.slf4j.Slf4j;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.io.IOException;

import lombok.Data;

/*
Routines that process files for some general purpose
 */
@Slf4j
public class NCATSFileUtils {

    private static final int MAX_READS=10;
    
    private static final Pattern sdFileFieldPattern = Pattern.compile("> +<(.*)>");
    private static final Pattern endRecordPattern = Pattern.compile("\\$\\$\\$\\$");

    @Data
    public static class InputFieldStatistics {
        private final int maxExamples;
        private String field;
        private List<String> examples= new ArrayList<>();        
        
        public InputFieldStatistics(String f){
            this.field=f;
            this.maxExamples=MAX_READS;
        }
        public InputFieldStatistics(String f, int max){
            this.field=f;
            this.maxExamples=max;
        }
        public InputFieldStatistics add(String val){
            if(examples.size()<maxExamples){
                examples.add(val);
            }
            return this;
        }
/* Some ideas to help suggest best imports
TODO: consider other data types like:
1. Numeric
2. Numeric + Units (e.g. properties)
3. INCHI
4. INCHI-KEY
5. SMILES
6. CAS number
...

        private int minLines;
        private int maxLines;
        private int minLength;
        private int maxLength;
       
        private boolean allNumeric=true;
        private boolean allIntegers=true;
*/
        
       
    }

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

    /*
    Read all lines of an SD file and compile a list of unique field names.
    Each field name occurs only once in the output.
     */
    public static Set<String> getSdFileFields(InputStream istream) throws IOException {
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(istream, StandardCharsets.UTF_8))) {
            while (br.ready()) {
                String line = br.readLine();
                Matcher matcher = sdFileFieldPattern.matcher(line);
                if(matcher.find()) { 
                    String fieldName = matcher.group(1);
                    log.trace("processing field name: " + fieldName);
                    fields.add(fieldName);
                }
            }
        }
        return fields;
    }

   /*
    Read an SD file from an input stream and retrieve some basic statistics about the fields.
     */
    public static Map<String, InputFieldStatistics> getSDFieldStatistics(InputStream istream) throws IOException {
        return getSDFieldStatistics(istream,MAX_READS);
    }
    
    /*
    Read an SD file from an input stream and retrieve some basic statistics about the fields.
     */
    public static Map<String, InputFieldStatistics> getSDFieldStatistics(InputStream istream , int maxExamples) throws IOException {
        Map<String, InputFieldStatistics> retMap = new LinkedHashMap<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(istream, "UTF-8"))) {
            String fieldName=null;
            String value="";
            boolean inValue=false;
            while (br.ready()) {
                String line = br.readLine();
                Matcher matcher = sdFileFieldPattern.matcher(line);
                Matcher endmatcher = endRecordPattern.matcher(line);
                if(matcher.find()) { 
                    if(inValue){
                        InputFieldStatistics fs=retMap.computeIfAbsent(fieldName, k-> new InputFieldStatistics(k, maxExamples));
                        fs.add(value.trim());
                    }
                    fieldName = matcher.group(1);
                    log.trace("processing field name: " + fieldName);
                    inValue=true;
                    value="";
                }else if(endmatcher.find()) { 
                    if(inValue){
                        InputFieldStatistics fs=retMap.computeIfAbsent(fieldName, k-> new InputFieldStatistics(k, maxExamples));
                        fs.add(value.trim());
                    }
                    inValue=false;
                }else{
                    if(inValue){
                        value=value + line + "\n";
                    }
                }
            }
        }
        return retMap;
    }
}
