package example;

import gsrs.*;
import gsrs.controller.GetGsrsRestApiMapping;
import gsrs.controller.PostGsrsRestApiMapping;
import gsrs.cv.EnableControlledVocabulary;
import gsrs.module.substance.indexers.DeprecatedIndexValueMaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import utils.NCATSFileUtils;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

//import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableGsrsApi(indexValueMakerDetector = EnableGsrsApi.IndexValueMakerDetector.CONF
//     additionalDatabaseSourceConfigs = {ApplicationsDataSourceConfig.class}
)
@EnableGsrsJpaEntities
@EnableGsrsLegacyAuthentication
@EnableGsrsLegacyCache
@EnableGsrsLegacyPayload
@EnableGsrsLegacySequenceSearch
@EnableGsrsLegacyStructureSearch
@EntityScan(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
//@EnableJpaRepositories(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
@EnableGsrsScheduler
@EnableGsrsBackup
@EnableControlledVocabulary
@Slf4j
public class GsrsModuleSubstanceApplication {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "PUT", "POST", "PATCH", "DELETE", "OPTIONS");

            }
        };
    }



    @Bean
    public DeprecatedIndexValueMaker deprecatedIndexValueMaker(){
        return new DeprecatedIndexValueMaker();
    }

    public static void main(String[] args) {
        SpringApplication.run(GsrsModuleSubstanceApplication.class, args);
    }

    @PostGsrsRestApiMapping(path="/getFieldsForSDFile")
    public ResponseEntity<Object> fieldsForSDF(@NotNull @RequestBody MultipartFile file,
                                               @RequestParam Map<String, String> processingParameters) throws IOException {
        log.trace("starting in fieldsForSDF");
        String fileName = file.getName();
        log.debug("using fileName: " + fileName);
        File tempSdFile= multipartToFile( file, fileName);
        Set<String> fields = NCATSFileUtils.getSdFileFields(tempSdFile.getPath());
        log.trace("total fields: " + fields.size());
        return ResponseEntity.ok(fields);
    }

    @GetGsrsRestApiMapping(path="/getFieldsForSDFile")
    public ResponseEntity<String> fieldsForSDF() throws IOException {
        log.trace("starting in fieldsForSDF (get)");
        String message ="please use the POST method";
        return ResponseEntity.ok(message);
    }


    public static File multipartToFile(MultipartFile multipart, String fileName) throws IllegalStateException, IOException {
        File convFile = new File(System.getProperty("java.io.tmpdir")+"/"+fileName);
        multipart.transferTo(convFile);
        return convFile;
    }

}
