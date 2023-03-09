package gsrs.module.substance.utils;

import gsrs.repository.PayloadRepository;
import ix.core.models.Payload;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class ImageUtilities {

    public static final String SUBSTANCE_IMAGE_TAG ="SUBSTANCE IMAGE";

    @Autowired
    private PayloadRepository payloadRepository;

    private final static String patternSource="http.*payload\\(([0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12})\\)\\?format=raw";
    private final static Pattern guidInPayloadUrlPattern = Pattern.compile(patternSource);
    /*
    Look for a reference with the indicated tag and an uploaded file URL.
    Read the data from the URL and return it
     */
    public ImageInfo getSubstanceImage(Substance substance){
        log.trace("starting in getSubstanceImage");
        for (Reference ref : substance.references) {
            if(ref.tags.stream().anyMatch(t->t.term.equals(SUBSTANCE_IMAGE_TAG)) && !ref.uploadedFile.isEmpty()) {
                log.trace("reference found with image tag.  uploadedFile: {}", ref.uploadedFile);
                InputStream is = null;
                try {
                    URL fileUrl = new URL(ref.uploadedFile);
                    is = fileUrl.openStream();
                    byte[] imageBytes = IOUtils.toByteArray(is);
                    log.trace("going to call getMimeTypeFromUrl with ref.uploadedFile: {}", ref.uploadedFile);
                    String format =getMimeTypeFromUrl(ref.uploadedFile);
                    return new ImageInfo(true, imageBytes, format);
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
        return new ImageInfo(false, null, null);
    }

    private String getMimeTypeFromUrl(String url) {
        log.trace("in getMimeTypeFromUrl with url: {}", url);
        Matcher m= guidInPayloadUrlPattern.matcher(url);
        if( m.matches()){
            String id= m.group(1);
            log.trace("found id {} within URL {}", id, url);
            assert payloadRepository!=null;
            Optional<Payload> payloadOptional= payloadRepository.findById(UUID.fromString(id));
            if(payloadOptional.isPresent()){
                log.trace("found payload id: {}, mime {}, url {}",  payloadOptional.get().id, payloadOptional.get().name,
                        payloadOptional.get().mimeType, payloadOptional.get().getUrl());
                return payloadOptional.get().mimeType;
            }
        } else{
            log.trace("no match!");
        }
        return "";
    }

    public static byte[] resizeImage(byte[] original, int newWidth, int newHeight, String format) {
        log.trace("starting resizeImage");
        ByteArrayInputStream inputStream = new ByteArrayInputStream(original);
        try {
            BufferedImage originalImage = ImageIO.read(inputStream);
            
            BufferedImage  resizedImage = handleResize(originalImage, newWidth, newHeight);
            if(resizedImage==null){
                log.error("Error! no resized image!");
                return null;
            }
            ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
            log.trace("created resized image with width: {}; height: {}, min X: {}, min Y: {}, height(2): {}, width(2): {}",
                    resizedImage.getWidth(), resizedImage.getHeight(),
                    resizedImage.getData().getMinX(), resizedImage.getData().getMinY(), resizedImage.getData().getHeight(),
                    resizedImage.getData().getWidth());
            ImageIO.write( resizedImage, format, outputStream );
            byte[] bytes=outputStream.toByteArray();
            log.trace("size of bytes: {}", bytes.length);
            outputStream.close();
            return bytes;
        } catch (Exception e) {
            log.error("Error resizing image", e);
            return original;
        }
    }
    public static BufferedImage handleResize(BufferedImage img, int newW, int newH) {
        BufferedImage resizedImage =null;
        try {
            resizedImage=Thumbnails.of(img).size(newW, newH).asBufferedImage();
        } catch (IOException ex){
            log.error("Error resizing", ex);
        }
        return resizedImage;
    }
}
