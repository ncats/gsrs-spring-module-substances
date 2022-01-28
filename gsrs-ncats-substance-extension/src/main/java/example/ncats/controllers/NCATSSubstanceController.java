package example.ncats.controllers;

import gsrs.controller.GetGsrsRestApiMapping;
import gsrs.controller.GsrsRestApiController;
import gsrs.controller.IdHelpers;
import gsrs.controller.PostGsrsRestApiMapping;
import gsrs.module.substance.SubstanceEntityServiceImpl;
import gsrs.module.substance.utils.NCATSFileUtils;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Slf4j
@ExposesResourceFor(Substance.class)
@GsrsRestApiController(context = SubstanceEntityServiceImpl.CONTEXT)
public class NCATSSubstanceController {

    @PostGsrsRestApiMapping(path="/getFieldsForSDFile")
    public Object fieldsForSDF(@NotNull @RequestBody MultipartFile file,
                                          @RequestParam Map<String, String> processingParameters) throws IOException {
        log.trace("starting in fieldsForSDF");
        String fileName = file.getName();
        log.debug("using fileName: " + fileName);
        File tempSdFile= multipartToFile( file, fileName);
        Set<String> fields =NCATSFileUtils.getSdFileFields(tempSdFile.getPath());
        log.trace("total fields: " + fields.size());
        return fields;
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
}
