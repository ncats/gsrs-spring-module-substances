package gsrs.module.substance.utils;

import gov.nih.ncats.common.sneak.Sneak;
import gsrs.service.PayloadService;
import gsrs.springUtils.AutowireHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

@Slf4j
public class NCATSGSRSUtils {

    @Autowired
    private PayloadService payloadService;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    private static final String SDF_MIME_TYPE = "chemical/x-mdl-sdf";

    public UUID saveSdFile(String sdFileName, String sdFileData) throws IOException {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        UUID sdfId = transactionTemplate.execute(s -> {
            try {
                return payloadService.createPayload(sdFileName, SDF_MIME_TYPE,
                                sdFileData,
                        PayloadService.PayloadPersistType.PERM
                ).id;
            } catch (Throwable t) {
                return Sneak.sneakyThrow(t);
            }
        });
        return sdfId;
    }

    public UUID saveSdFile(File sdFile) throws IOException {
        List<String> fileLines= Files.readAllLines(sdFile.toPath());
        String fileText = String.join("\n", fileLines);
        return saveSdFile(sdFile.getName(), fileText);
    }


}
