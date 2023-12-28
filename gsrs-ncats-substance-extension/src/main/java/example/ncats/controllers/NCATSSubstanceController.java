package example.ncats.controllers;

import gsrs.*;
import gsrs.controller.GetGsrsRestApiMapping;
import gsrs.controller.GsrsRestApiController;
import gsrs.controller.PostGsrsRestApiMapping;
import gsrs.module.substance.SubstanceEntityServiceImpl;
import gsrs.module.substance.repository.StructureRepository;
import gsrs.module.substance.services.SubstanceSequenceSearchService;
import gsrs.module.substance.utils.GSRSSpecialtyFileUtils;
import gsrs.module.substance.utils.NCATSFileUtils;
import gsrs.service.PayloadService;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.DependsOn;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@SpringBootApplication
@EnableGsrsApi(indexValueMakerDetector = EnableGsrsApi.IndexValueMakerDetector.CONF)
@EnableGsrsLegacyCache
@EnableGsrsLegacySequenceSearch
@ExposesResourceFor(Substance.class)
@EnableGsrsLegacyStructureSearch
@EnableGsrsLegacyPayload
@DependsOn("SubstanceSequenceSearchService")
@GsrsRestApiController(context = SubstanceEntityServiceImpl.CONTEXT)
public class NCATSSubstanceController {

    @Autowired
    private SubstanceSequenceSearchService substanceSequenceSearchService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private PayloadService payloadService;

    @PostGsrsRestApiMapping(path="/getFieldsForSDFile")
    public ResponseEntity<Object> fieldsForSDF(@NotNull @RequestBody MultipartFile file,
                                          @RequestParam Map<String, String> processingParameters) throws IOException {
        log.trace("starting in fieldsForSDF");
        String fileName = file.getName();
        log.debug("using fileName: " + fileName);
        File tempSdFile= multipartToFile( file, fileName);
        Set<String> fields =NCATSFileUtils.getSdFileFields(tempSdFile.getPath());
        log.trace("total fields: " + fields.size());
        return ResponseEntity.ok(fields);
    }

    @PostGsrsRestApiMapping("/savesdfile")
    public ResponseEntity<String> saveSdFile(@NotNull @RequestBody MultipartFile file,
                                             @RequestParam Map<String, String> processingParameters) throws IOException {
        String fileName = file.getName();
        File sdFile = multipartToFile(file, fileName);
        GSRSSpecialtyFileUtils utils = new GSRSSpecialtyFileUtils();
        UUID savedFileId =utils.saveSdFile(sdFile);
        String  message = String.format("Result of saving your file: %s", savedFileId.toString());
        return ResponseEntity.ok(message);
    }
    /*
    Adding a simple GET as a test
     */
    @GetGsrsRestApiMapping("/getFieldsForSDFile")
    public String getFieldInfo() {
        return "Structure,ID";
    }

    public static File multipartToFile(MultipartFile multipart, String fileName) throws IllegalStateException, IOException {
        File convFile = new File(System.getProperty("java.io.tmpdir")+"/"+fileName);
        multipart.transferTo(convFile);
        return convFile;
    }

    public static File stringToFile(String data, String fileName) throws IllegalStateException, IOException {

        File convFile = new File(System.getProperty("java.io.tmpdir")+"/"+fileName);
        try(FileWriter writer = new FileWriter(convFile.getAbsoluteFile())) {
            writer.write(data);
        }
        return convFile;
    }

    public static void main(String[] args) {
        SpringApplication.run(NCATSSubstanceController.class, args);
    }
}
