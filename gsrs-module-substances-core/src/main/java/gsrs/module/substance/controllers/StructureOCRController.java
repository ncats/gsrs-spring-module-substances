package gsrs.module.substance.controllers;

import gov.nih.ncats.common.io.IOUtil;
import gov.nih.ncats.molvec.Molvec;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.controller.GsrsRestApiController;
import gsrs.controller.PostGsrsRestApiMapping;
import gsrs.module.substance.SubstanceEntityServiceImpl;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This is the Controller methods that handle image to structure conversion.
 *
 */
@GsrsRestApiController(context = SubstanceEntityServiceImpl.CONTEXT)
public class StructureOCRController {

    @Autowired
    private StructureProcessor structureProcessor;

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    /**
     * Convert an image encoded in base64 following the pattern
     * used by GSRS 2.x UI.
     * @param raw encoded image data formatted as follows: data:image/$format;base64,$encodedData
     * @param queryParameters any URL query parameters that might be used to error routing.
     * @return a {@link Structure} object of that processed image.
     */
    @PostGsrsRestApiMapping("/ocrStructure")
    public ResponseEntity<Object> ocr(@NotNull @RequestBody String raw,
                                          @RequestParam Map<String, String> queryParameters) {
        //the format from the GSRS UI looks like
        //data:image/jpeg;base64,/9j/4AA...
        //with the data coming after the comma
        String base64Encoded = raw.substring(raw.indexOf(',')+1);

//        System.out.println("base64 =" +base64Encoded);
        byte[] data = Base64.getDecoder().decode(base64Encoded);
        return ocrFromBytes(queryParameters, data);
    }
    /**
     * Convert an image file upload.
     * @param file the image to process
     * @param queryParameters any URL query parameters that might be used to error routing.
     * @return a {@link Structure} object of that processed image.
     * @throws IOException if there was a problem reading the uploaded file.
     */
    @PostGsrsRestApiMapping("/ocrStructureFile")
    public ResponseEntity<Object> ocrFile(@NotNull @RequestBody MultipartFile file,
                                      @RequestParam Map<String, String> queryParameters) throws IOException {
        byte[] data = IOUtil.toByteArray(file.getInputStream());

        return ocrFromBytes(queryParameters, data);
    }

    private ResponseEntity<Object> ocrFromBytes(Map<String, String> queryParameters, byte[] data) {
        String mol;
        CompletableFuture<String> chemicalCompletableFuture = Molvec.ocrAsync(data);

        try {
            mol = chemicalCompletableFuture.get(10, TimeUnit.SECONDS);

            Structure struct = structureProcessor.instrument(mol);

            return new ResponseEntity<>(struct, HttpStatus.OK);
        }catch(TimeoutException toe){
            //timeout!!
            chemicalCompletableFuture.cancel(true);
            ResponseEntity<Object> responseEntity= gsrsControllerConfiguration.handleNotFound(queryParameters, "timedout");

            return responseEntity;
        }catch(Exception e) {
            return gsrsControllerConfiguration.handleError(e, queryParameters);
        }
    }
}
