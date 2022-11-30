package gsrs.module.substance.importers.readers;

import gsrs.module.substance.importers.model.DefaultPropertyBasedRecordContext;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TextFileReader {

    public Stream<DefaultPropertyBasedRecordContext> readFile(InputStream inputStream, String delim, boolean trimQuotes) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        List<String> fields = Arrays.stream(br.readLine().split(delim)).map(s->trimQuotes ? s.substring(1, s.length()-1): s).collect(Collectors.toList());
        String line;

        Stream.Builder<DefaultPropertyBasedRecordContext> builder=Stream.builder();
        while ((line = br.readLine()) != null) {
            List<String> values = Arrays.stream(line.split(delim)).collect(Collectors.toList());
            DefaultPropertyBasedRecordContext record = new DefaultPropertyBasedRecordContext();
            Map<String, String> lineValues = new HashMap<>();
            for(int i =0; i< fields.size(); i++) {
                try {
                    String currentValue=values.get(i);
                    if( currentValue!=null && trimQuotes && currentValue.length()>2) {
                        currentValue = currentValue.substring(1, currentValue.length()-1);
                    }
                    lineValues.put(fields.get(i), currentValue);
                }
                catch (IndexOutOfBoundsException e) {
                    log.trace("error reading field '{}' from line '{}'", fields.get(i), line);
                }
            }
            record.setAllProperties(lineValues);
            builder.add(record);
        }
        br.close();
        return builder.build();
    }
}
