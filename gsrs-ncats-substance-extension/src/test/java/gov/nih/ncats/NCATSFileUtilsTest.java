package gov.nih.ncats;

import gsrs.module.substance.utils.NCATSFileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NCATSFileUtilsTest {

    @Test
    public void testSdFileFields() throws IOException {
        String fullPath = "src/test/resources/test3csmall.sdf";
        Set<String> fields = NCATSFileUtils.getSdFileFields(fullPath);
        Set<String> expectedFields = new HashSet<>(Arrays.asList("CAS", "NAMES"));
        Assertions.assertEquals(expectedFields, fields);
    }

    @Test
    public void testSdFileFields2() throws IOException {
        String fullPath = "src/test/resources/nlm_5 first.sdf";
        Set<String> fields = NCATSFileUtils.getSdFileFields(fullPath);
        Set<String> expectedFields = new HashSet<>(Arrays.asList("main name", "CAS", "NAMES"));
        Assertions.assertEquals(expectedFields, fields);
    }
}
