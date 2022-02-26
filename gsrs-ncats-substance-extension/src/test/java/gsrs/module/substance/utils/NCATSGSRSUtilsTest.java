package gsrs.module.substance.utils;

import example.GsrsModuleSubstanceApplication;
import gsrs.service.PayloadService;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

//identifies test as Spring Boot and specifies which Spring Boot Application to load
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
@Slf4j
public class NCATSGSRSUtilsTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private GSRSSpecialtyFileUtils gsrsSpecialtyFileUtils;

    @Autowired
    private PayloadService payloadService;

    private void setup() {
        //make sure dependencies are set within routine we intend to call
        AutowireHelper.getInstance().autowireAndProxy(gsrsSpecialtyFileUtils);
    }

    @Test
    //@Transactional
    public void testSdFileSave() throws IOException {
        setup();
        File sdFile = new ClassPathResource("test3csmall.sdf").getFile();
        String fileName = sdFile.getName();
        String fileText = readAllFileText(sdFile);
        UUID fileID = gsrsSpecialtyFileUtils.saveSdFile(fileName, fileText);
        System.out.println("fileID: " + fileID);
        Assertions.assertNotNull(fileID);
    }

    private String readAllFileText(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        return String.join("\n", lines);
    }
}