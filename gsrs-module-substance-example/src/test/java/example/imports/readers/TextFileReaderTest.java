package example.imports.readers;

import gsrs.module.substance.importers.model.DefaultPropertyBasedRecordContext;
import gsrs.module.substance.importers.readers.TextFileReader;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TextFileReaderTest {

    @Test
    public void testRead1() throws IOException {
        String fileName = "testText/export-INN_MIXTURES_PLUS.txt";
        File textFile = (new ClassPathResource(fileName)).getFile();
        Assertions.assertTrue(textFile.exists());
        FileInputStream fileInputStream = new FileInputStream(textFile);
        TextFileReader reader = new TextFileReader();
        Stream<DefaultPropertyBasedRecordContext> dataRecordContextStream =reader.readFile(fileInputStream, "\t");
        fileInputStream.close();
        long expectedRecordCount =35;
        List<DefaultPropertyBasedRecordContext> data = dataRecordContextStream.collect(Collectors.toList());
        long actual = data.size();
        Assertions.assertEquals(expectedRecordCount, actual);
        String uuid="84d0336c-d9a6-4394-8d42-c2afdbcd93b5";
        String expectedApprovalId="7HMD7M29RI";
        DefaultPropertyBasedRecordContext selectedDataItem = data.stream().filter(d->d.getProperty("UUID").equals(uuid)).findFirst().get();
        Assertions.assertEquals(expectedApprovalId, selectedDataItem.getProperty("APPROVAL_ID"));
    }
}
