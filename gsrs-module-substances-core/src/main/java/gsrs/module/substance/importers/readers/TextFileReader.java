package gsrs.module.substance.importers.readers;

import gsrs.module.substance.importers.model.DefaultPropertyBasedRecordContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextFileReader {

    public Stream<DefaultPropertyBasedRecordContext> readFile(InputStream inputStream, String delim) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        List<String> fields = Arrays.stream(br.readLine().split(delim)).collect(Collectors.toList());
        String line;

        Stream.Builder<DefaultPropertyBasedRecordContext> builder=Stream.builder();
        while ((line = br.readLine()) != null) {
            List<String> values = Arrays.stream(line.split(delim)).collect(Collectors.toList());
            DefaultPropertyBasedRecordContext record = new DefaultPropertyBasedRecordContext();
            Map<String, String> lineValues = new HashMap<>();
            for(int i =0; i< values.size(); i++) {
                lineValues.put(fields.get(i), values.get(i));
            }
            record.setAllProperties(lineValues);
            builder.add(record);
        }
        br.close();
        return builder.build();
    }
}
