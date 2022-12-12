package gsrs.module.substance.importers.readers;

import gsrs.module.substance.importers.model.DefaultPropertyBasedRecordContext;
import gsrs.module.substance.utils.NCATSFileUtils;
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

    public Stream<DefaultPropertyBasedRecordContext> readFile(InputStream inputStream, String delim, boolean trimQuotes, List<String> inputFields) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        List<String> fields;
        if(inputFields !=null && inputFields.size() >0){
            fields=new ArrayList<>();
            fields.addAll(inputFields);
        }else {
            fields=Arrays.stream(br.readLine().split(delim)).map(s -> trimQuotes ? s.substring(1, s.length() - 1) : s).collect(Collectors.toList());
        }
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

    public List<String> getFileFields(InputStream inputStream, String delim, boolean trimQuotes) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        return Arrays.stream(br.readLine().split(delim)).map(s->trimQuotes ? s.substring(1, s.length()-1): s).collect(Collectors.toList());
    }

    public Map<String, NCATSFileUtils.InputFieldStatistics> getFileStatistics(InputStream inputStream, String delim, boolean trimQuotes, List<String> fieldNames,
                                                                              int maxRecords, int linesToSkip) throws IOException {
        log.trace("In getFileStatistics, delim: {}; trimQuotes: {}", delim, trimQuotes);
        Map<String, NCATSFileUtils.InputFieldStatistics> retMap = new LinkedHashMap<>();
        Scanner reader = new Scanner(inputStream);

        List<String> fields = new ArrayList<>();
        if(fieldNames != null && fieldNames.size()>0){
            log.trace("getFileStatistics received input fieldNames");
            fields.addAll(fieldNames);
        } else {
            log.trace("getFileStatistics reading fields from file");
            fields = Arrays.stream(reader.nextLine().split(delim)).map(s->trimQuotes ? s.substring(1, s.length()-1): s).collect(Collectors.toList());
            log.trace(" got {}", fields.size());
        }

        for(int lineToSkip=0; lineToSkip< linesToSkip; lineToSkip++) {
            reader.nextLine();
        }
        for(int lineToRead=0; (lineToRead<maxRecords && reader.hasNext()); lineToRead++) {
            List<String> values = Arrays.stream(reader.nextLine().split(delim)).collect(Collectors.toList());
            for(int f=0; f< fields.size(); f++) {
                String fieldName = fields.get(f);
                String value = values.get(f);
                NCATSFileUtils.InputFieldStatistics fs = retMap.computeIfAbsent(fieldName, k -> new NCATSFileUtils.InputFieldStatistics(k, maxRecords));
                fs.add(value.trim());
            }
        }
        return retMap;
    }

    public Stream<DefaultPropertyBasedRecordContext> readFile2(InputStream inputStream, String delim, boolean trimQuotes,
                                                               List<String> inputFields, int linesToSkip) throws IOException {
        Scanner reader = new Scanner(inputStream);
        reader.reset();

        List<String> fields;
        if(inputFields !=null && inputFields.size() >0){
            fields=new ArrayList<>();
            fields.addAll(inputFields);
        }else {
            fields=Arrays.stream(reader.nextLine().split(delim)).map(s -> trimQuotes ? s.substring(1, s.length() - 1) : s).collect(Collectors.toList());
        }
        for( int i =0; i<linesToSkip; i++) reader.nextLine();

        String line;
        Stream.Builder<DefaultPropertyBasedRecordContext> builder=Stream.builder();
        while (reader.hasNext()) {
            line = reader.nextLine();
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
        return builder.build();
    }
}
