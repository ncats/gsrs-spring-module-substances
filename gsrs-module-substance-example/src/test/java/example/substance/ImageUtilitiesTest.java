package example.substance;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.utils.ImageInfo;
import gsrs.module.substance.utils.ImageUtilities;
import gsrs.repository.PayloadRepository;
import gsrs.service.GsrsEntityService;
import gsrs.service.PayloadService;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import ix.core.models.Payload;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.UUID;

@Slf4j
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
public class ImageUtilitiesTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    protected SubstanceEntityService substanceEntityService;

    @Autowired
    private PayloadRepository payloadRepository;

    @Autowired
    public PayloadService payloadService;

    @Test
    public void testSubstanceWithoutImage(){
        SubstanceBuilder builder = new SubstanceBuilder();
        Name plainName = new Name();
        plainName.name="Plain Substance";
        plainName.displayName=true;

        Reference reference = new Reference();
        reference.publicDomain= true;
        reference.docType="book";
        reference.citation="Descriptions of stuff, page 203";
        reference.addTag("PUBLIC_DOMAIN_RELEASE");
        plainName.addReference(reference);
        builder.addName(plainName);
        builder.addReference(reference);
        Substance substance = builder.build();
        ImageUtilities imageUtilities = new ImageUtilities();
        ImageInfo imageInfo = imageUtilities.getSubstanceImage(substance, 0);
        Assertions.assertFalse(imageInfo.isHasData());
    }

    @Test
    public void testSubstanceWithImage() throws IOException {
        String imageUrl ="https://upload.wikimedia.org/wikipedia/commons/1/1d/Feldspar-Group-291254.jpg";
        SubstanceBuilder builder = new SubstanceBuilder();
        Name plainName = new Name();
        plainName.name="Plain Substance";
        plainName.displayName=true;
        Reference reference = new Reference();
        reference.publicDomain= true;
        reference.docType= ImageUtilities.SUBSTANCE_IMAGE_REFERENCE_TYPE;
        reference.citation="Descriptions of stuff, page 203";
        plainName.addReference(reference);
        builder.addName(plainName);
        builder.addReference(reference);
        UUID id= savePayload(imageUrl, "Feldspar-Group-291254.jpg");
        reference.uploadedFile="http://localhost:8081/api/v1/payload(" + id.toString() + ")?format=raw";
        Substance substance = builder.build();
        GsrsEntityService.CreationResult<Substance> result= substanceEntityService.createEntity(substance.toFullJsonNode());
        Assertions.assertTrue(result.isCreated());
        ImageUtilities imageUtilities = new ImageUtilities();
        AutowireHelper.getInstance().autowireAndProxy(imageUtilities);
        ImageInfo imageInfo= imageUtilities.getSubstanceImage(substance, 0);
        Assertions.assertTrue(imageInfo.isHasData() && imageInfo.getImageData().length>0);
    }

    @Test
    public void testSubstanceWithImage2() throws IOException {
        SubstanceBuilder builder = new SubstanceBuilder();
        Name plainName = new Name();
        String imageUrl ="https://upload.wikimedia.org/wikipedia/commons/1/1d/Feldspar-Group-291254.jpg";
        plainName.name="Plain Substance";
        plainName.displayName=true;
        Reference reference = new Reference();
        reference.publicDomain= true;
        reference.docType= ImageUtilities.SUBSTANCE_IMAGE_REFERENCE_TYPE;
        reference.citation="Descriptions of stuff, page 203";
        UUID id= savePayload(imageUrl, "Feldspar-Group-291254.jpg");
        reference.uploadedFile="http://localhost:8081/api/v1/payload(" + id.toString() + ")?format=raw";
        plainName.addReference(reference);
        builder.addName(plainName);
        builder.addReference(reference);

        String imageUrl2 = "https://foto.wuestenigel.com/wp-content/uploads/api/fresh-salad-with-a-mixture-of-different-lettuce-and-arugula-in-a-black-bowl.jpeg";
        Reference reference2 = new Reference();
        reference2.publicDomain= true;
        reference2.docType= ImageUtilities.SUBSTANCE_IMAGE_REFERENCE_TYPE;
        reference2.citation="Descriptions of stuff, page 206";
        UUID id2= savePayload(imageUrl2, "fresh-salad-with-a-mixture-of-different-lettuce-and-arugula-in-a-black-bowl.jpeg");
        reference2.uploadedFile="http://localhost:8081/api/v1/payload(" + id2.toString() + ")?format=raw";
        builder.addReference(reference2);
        Substance substance = builder.build();
        ImageUtilities imageUtilities = new ImageUtilities();
        AutowireHelper.getInstance().autowireAndProxy(imageUtilities);
        ImageInfo imageInfo= imageUtilities.getSubstanceImage(substance, 1);
        Assertions.assertTrue(imageInfo.isHasData() && imageInfo.getImageData().length>67000);
    }

    @Test
    public void testSubstanceWithImage3() throws IOException {
        SubstanceBuilder builder = new SubstanceBuilder();
        Name plainName = new Name();
        String imageUrl ="https://upload.wikimedia.org/wikipedia/commons/1/1d/Feldspar-Group-291254.jpg";
        plainName.name="Plain Substance";
        plainName.displayName=true;
        Reference reference = new Reference();
        reference.publicDomain= true;
        reference.docType= ImageUtilities.SUBSTANCE_IMAGE_REFERENCE_TYPE;
        reference.citation="Descriptions of stuff, page 203";
        UUID id= savePayload(imageUrl, "Feldspar-Group-291254.jpg");
        reference.uploadedFile="http://localhost:8081/api/v1/payload(" + id.toString() + ")?format=raw";
        plainName.addReference(reference);
        builder.addName(plainName);
        builder.addReference(reference);

        String imageUrl2 = "https://foto.wuestenigel.com/wp-content/uploads/api/fresh-salad-with-a-mixture-of-different-lettuce-and-arugula-in-a-black-bowl.jpeg";
        Reference reference2 = new Reference();
        reference2.publicDomain= true;
        reference2.docType= ImageUtilities.SUBSTANCE_IMAGE_REFERENCE_TYPE;
        reference2.citation="Descriptions of stuff, page 206";
        UUID id2= savePayload(imageUrl2, "fresh-salad-with-a-mixture-of-different-lettuce-and-arugula-in-a-black-bowl.jpeg");
        reference2.uploadedFile="http://localhost:8081/api/v1/payload(" + id2.toString() + ")?format=raw";
        builder.addReference(reference2);

        String imageUrl3 = "https://www.soil-net.com/album/Plants/Garden/slides/Flower%20Clematis%2001.jpg";
        Reference reference3 = new Reference();
        reference3.publicDomain= true;
        reference3.docType= ImageUtilities.SUBSTANCE_IMAGE_REFERENCE_TYPE;
        reference3.citation="Descriptions of stuff, page 208";
        UUID id3= savePayload(imageUrl3, "Flower Clematis 01.jpg");
        reference3.uploadedFile="http://localhost:8081/api/v1/payload(" + id3.toString() + ")?format=raw";
        builder.addReference(reference3);

        Substance substance = builder.build();
        ImageUtilities imageUtilities = new ImageUtilities();
        AutowireHelper.getInstance().autowireAndProxy(imageUtilities);
        ImageInfo imageInfo= imageUtilities.getSubstanceImage(substance, 2);
        Assertions.assertTrue(imageInfo.isHasData() && imageInfo.getImageData().length>77000);
    }


    @Test
    public void resizeImageTest1() {
        String imageUrl ="https://upload.wikimedia.org/wikipedia/commons/1/1d/Feldspar-Group-291254.jpg";
        try {
            URL fileUrl = new URL(imageUrl);
            InputStream is = fileUrl.openStream ();
            byte[] imageBytes = IOUtils.toByteArray(is);
            byte[] resizedBytes= ImageUtilities.resizeImage(imageBytes, 50, 50, "jpg");

            File basicFile = File.createTempFile ("del2Resized", "jpg");
            log.trace("basicFile in resizeImageTest1 {}", basicFile.getAbsoluteFile());
            assert resizedBytes != null;
            Files.write(basicFile.toPath(), resizedBytes);
            Assertions.assertTrue(resizedBytes.length>0);
        }
        catch (IOException e) {
            System.err.printf ("Failed while reading bytes from %s: %s", imageUrl, e.getMessage());
            e.printStackTrace ();
            Assertions.fail("error processing image fails test");
        }
    }

    @Test
    public void resizeImageTest2() {
        String imagePath ="testImage/puppy1.png";
        try {
            File imageFile = new ClassPathResource(imagePath).getFile();
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            byte[] resizedBytes= ImageUtilities.resizeImage(imageBytes, 200, 100, "png");
            File basicFile = File.createTempFile ("delResizedPuppy2", "png");
            log.trace("basicFile in resizeImageTest2 {}", basicFile.getAbsoluteFile());
            assert resizedBytes != null;
            Files.write(basicFile.toPath(), resizedBytes);
            Assertions.assertTrue(resizedBytes.length>0);
        }
        catch (IOException e) {
            System.err.printf ("Failed while reading bytes from %s: %s", imagePath, e.getMessage());
            e.printStackTrace ();
            Assertions.fail("error processing image fails test");
        }
    }

    @Test
    public void resizeImageTest3() {
        String imageUrl ="https://upload.wikimedia.org/wikipedia/commons/1/1d/Feldspar-Group-291254.jpg";
        try {
            URL fileUrl = new URL(imageUrl);
            InputStream is = fileUrl.openStream ();
            byte[] imageBytes = IOUtils.toByteArray(is);
            byte[] resizedBytes= ImageUtilities.resizeImage(imageBytes, 50, 50, "jpeg");
            File basicFile = File.createTempFile ("del2Resized", "jpg");
            assert resizedBytes != null;
            Files.write(basicFile.toPath(), resizedBytes);
            Assertions.assertTrue(resizedBytes.length>0);
        }
        catch (IOException e) {
            System.err.printf ("Failed while reading bytes from %s: %s", imageUrl, e.getMessage());
            e.printStackTrace ();
            Assertions.fail("error processing image fails test");
        }
    }

    @Test
    public void resizeImageTest4() {
        String imagePath ="testImage/pentagon.svg";
        try {
            File imageFile = new ClassPathResource(imagePath).getFile();
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            byte[] resizedBytes= ImageUtilities.resizeImage(imageBytes, 200, 100, "svg");
            File basicFile = File.createTempFile ("delResizedPentagon", ".svg");
            log.debug("writing image to file {}", basicFile.getAbsolutePath());
            assert resizedBytes != null;
            Files.write(basicFile.toPath(), resizedBytes);
            Assertions.assertTrue(resizedBytes.length>0);
            String resizedSvgText = new String(resizedBytes);
            String widthString = "width=\"200\"";
            Assertions.assertTrue(resizedSvgText.contains(widthString));
            String heightString = "height=\"100\"";
            Assertions.assertTrue(resizedSvgText.contains(heightString));
        }
        catch (IOException e) {
            System.err.printf ("Failed while reading bytes from %s: %s", imagePath, e.getMessage());
            e.printStackTrace ();
            Assertions.fail("error processing image fails test");
        }
    }

    @Test
    public void resizeImageTest5() {
        String imagePath ="testImage/simple_x.tiff";
        try {
            File imageFile = new ClassPathResource(imagePath).getFile();
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            byte[] resizedBytes= ImageUtilities.resizeImage(imageBytes, 200, 100, "tif");
            File basicFile = File.createTempFile ("delResizedSimpleX", "tiff");
            assert resizedBytes != null;
            Files.write(basicFile.toPath(), resizedBytes);
            Assertions.assertTrue(resizedBytes.length>0);
        }
        catch (IOException e) {
            System.err.printf ("Failed while reading bytes from %s: %s", imagePath, e.getMessage());
            e.printStackTrace ();
            Assertions.fail("error processing image fails test");
        }
    }

    private UUID savePayload(String urlSource, String resourceName) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Payload payload = tx.execute(status -> {
            try {
                URL url = new URL(urlSource);
                InputStream in = url.openStream();
                return payloadService.createPayload(resourceName, "ignore",
                        in, PayloadService.PayloadPersistType.TEMP);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return payload.id;
    }
}
