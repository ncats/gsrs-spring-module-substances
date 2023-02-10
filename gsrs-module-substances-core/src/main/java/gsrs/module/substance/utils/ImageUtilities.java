package gsrs.module.substance.utils;

import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

@Slf4j
public class ImageUtilities {

    public static final String SUBSTANCE_IMAGE_TAG ="SUBSTANCE IMAGE";

    /*
    Look for a reference with the indicated tag and an uploaded file URL.
    Read the data from the URL and return it
     */
    public static Optional<byte[]> getSubstanceImage(Substance substance){
        log.trace("starting in getSubstanceImage");
        for (Reference ref : substance.references) {
            if(ref.tags.stream().anyMatch(t->t.term.equals(SUBSTANCE_IMAGE_TAG)) && !ref.uploadedFile.isEmpty()) {
                log.trace("reference found with image tag.  uploadedFile: {}", ref.uploadedFile);
                InputStream is = null;
                try {
                    URL fileUrl = new URL(ref.uploadedFile);
                    is = fileUrl.openStream ();
                    byte[] imageBytes = IOUtils.toByteArray(is);
                    return Optional.of(imageBytes);
                }
                catch (IOException e) {
                    System.err.printf ("Failed while reading bytes from %s: %s", ref.uploadedFile, e.getMessage());
                    e.printStackTrace ();
                    // Perform any other exception handling that's appropriate.
                }
                finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            log.warn("Error closing input stream (probably inconsequential)",e);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static byte[] resizeImage(byte[] original, int newWidth, int newHeight, String format) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(original);
        try {
            BufferedImage newImage = ImageIO.read(inputStream);
            Image scaledImage= newImage.getScaledInstance(newWidth, newHeight, BufferedImage.SCALE_SMOOTH);
            BufferedImage scaled2=(BufferedImage) scaledImage;
            ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
            ImageIO.write( scaled2, format, outputStream );
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Error resizing image", e);
            throw new RuntimeException(e);
        }
    }
}