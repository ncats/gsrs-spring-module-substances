package gsrs.module.substance.utils;

import gov.nih.ncats.common.sneak.Sneak;
import gsrs.service.PayloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class GSRSSpecialtyFileUtils {

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
