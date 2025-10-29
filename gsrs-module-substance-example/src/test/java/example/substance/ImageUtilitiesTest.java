package example.substance;

import gsrs.module.substance.utils.ImageInfo;
import gsrs.module.substance.utils.ImageUtilities;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;

@Slf4j
public class ImageUtilitiesTest {

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
        ImageInfo imageInfo = imageUtilities.getSubstanceImage(substance);
        Assertions.assertFalse(imageInfo.isHasData());
    }

    @Disabled
    @Test
    public void testSubstanceWithImage() {
        SubstanceBuilder builder = new SubstanceBuilder();
        Name plainName = new Name();
        plainName.name="Plain Substance";
        plainName.displayName=true;

        Reference reference = new Reference();
        reference.publicDomain= true;
        reference.docType="Image Reference";
        reference.citation="Descriptions of stuff, page 203";
        reference.uploadedFile="https://upload.wikimedia.org/wikipedia/commons/1/1d/Feldspar-Group-291254.jpg";
        plainName.addReference(reference);
        builder.addName(plainName);
        builder.addReference(reference);
        Substance substance = builder.build();
        ImageUtilities imageUtilities = new ImageUtilities();
        ImageInfo imageInfo= imageUtilities.getSubstanceImage(substance);
        Assertions.assertTrue(imageInfo.isHasData() && imageInfo.getImageData().length>0);
    }

    @Test
    public void resizeImageTest1() {
        String imageUrl ="https://upload.wikimedia.org/wikipedia/commons/1/1d/Feldspar-Group-291254.jpg";
        try {
            URL fileUrl = new URL(imageUrl);
            URLConnection connection = fileUrl.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0"); // Mimic a real browser
            InputStream is = connection.getInputStream();
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
            URLConnection connection = fileUrl.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0"); // Mimic a real browser
            InputStream is = connection.getInputStream();
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
}