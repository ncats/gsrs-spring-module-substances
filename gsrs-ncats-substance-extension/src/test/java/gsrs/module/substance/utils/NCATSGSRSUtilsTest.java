package gsrs.module.substance.utils;

import gsrs.service.PayloadService;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTestSuperClass;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

@WithMockUser(username = "admin", roles = "Admin")
@Slf4j
public class NCATSGSRSUtilsTest extends AbstractSubstanceJpaFullStackEntityTest {

    private static NCATSGSRSUtils ncatsgsrsUtils = new NCATSGSRSUtils();

    @Autowired
    private PayloadService payloadService;

    private void setup() {
        //make sure dependencies are set within routine we intend to call
        try {
            Field transactionMgrField = ncatsgsrsUtils.getClass().getField("transactionManager");
            transactionMgrField.setAccessible(true);
            transactionMgrField.set(ncatsgsrsUtils, transactionManager);

            Field payloadServiceField = ncatsgsrsUtils.getClass().getField("payloadService");
            payloadServiceField.setAccessible(true);
            payloadServiceField.set(ncatsgsrsUtils, this.payloadService);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Error setting up dependencies for test");
            e.printStackTrace();
        }
        //nothing for the moment
    }

    @Test
    //@Transactional
    public void testSdFileSave() throws IOException {
        setup();
        File sdFile = new ClassPathResource("test3csmall.sdf").getFile();
        String fileName = sdFile.getName();
        String fileText = readAllFileText(sdFile);
        UUID fileID = ncatsgsrsUtils.saveSdFile(fileName, fileText);
        System.out.println("fileID: " + fileID);
        Assertions.assertNotNull(fileID);
    }

    private String readAllFileText(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        return String.join("\n", lines);
    }
}