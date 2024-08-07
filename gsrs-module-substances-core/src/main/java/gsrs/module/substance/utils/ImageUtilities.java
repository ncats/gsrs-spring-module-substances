package gsrs.module.substance.utils;

import gsrs.repository.PayloadRepository;
import gsrs.service.PayloadService;
import ix.core.models.Payload;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;

@Slf4j
public class ImageUtilities {

    public static final String SUBSTANCE_IMAGE_REFERENCE_TYPE = "IMAGE REFERENCE";

    @Autowired
    private PayloadRepository payloadRepository;

    @Autowired
    public PayloadService payloadService;

    private final static String patternSource="http.*payload\\(([0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12})\\)\\?format=raw";
    private final static Pattern guidInPayloadUrlPattern = Pattern.compile(patternSource);
    /*
    Look for a reference with the indicated tag and an uploaded file URL.
    Read the data from the URL and return it
     */
    public ImageInfo getSubstanceImage(Substance substance){
        log.trace("starting in getSubstanceImage");
        for (Reference ref : substance.references) {
            if(isImageReference(ref)) {
                log.trace("reference found with image tag.  uploadedFile: {}", ref.uploadedFile);
                String payloadId = getPayloadIdFromUrl(ref.uploadedFile);
                if( payloadId ==null || payloadId.length()==0) {
                    log.warn("found null/empty payload id from {}", ref.uploadedFile);
                    continue;
                }
                Optional<Payload> payload= payloadRepository.findById(UUID.fromString( payloadId));
                if(!payload.isPresent()) {
                    log.warn("found null/empty payload from {}", payloadId);
                    continue;
                }
                Optional<byte[]> fileData;
                try {
                    fileData = getBytesFromPayloadId(UUID.fromString(payloadId), Math.toIntExact(payload.get().size));
                    if(fileData.isPresent()) {
                        log.trace("going to call getMimeTypeFromUrl with ref.uploadedFile: {}", ref.uploadedFile);
                        return new ImageInfo(true, fileData.get(), payload.get().mimeType);
                    }
                } catch (IOException e) {
                    log.error("Error reading image data from {}", ref.uploadedFile);
                    throw new RuntimeException(e);
                }
            }
        }
        return new ImageInfo(false,null,null);
    }

    private String getPayloadIdFromUrl(String url) {
        log.trace("in getPayloadIdFromUrl with url: {}", url);
        Matcher m= guidInPayloadUrlPattern.matcher(url);
        if( m.matches()){
            String id= m.group(1);
            log.trace("found id {} within URL {}", id, url);
            return id;
        } else{
            log.trace("no match!");
        }
        return "";
    }

    private Optional<byte[]> getBytesFromPayloadId(UUID id, int payloadSize) throws IOException {
        Optional<InputStream> stream= payloadService.getPayloadAsInputStream(id);
        byte[] fileData = new byte[Math.toIntExact(payloadSize)];
        try(BufferedInputStream bufferedInputStream= new BufferedInputStream(stream.get())) {
            bufferedInputStream.read(fileData);
            return Optional.of( fileData);
        }
    }

    public static byte[] resizeImage(byte[] original, int newWidth, int newHeight, String format) {
        log.trace("starting resizeImage");
        if( format.equalsIgnoreCase("SVG")){
            return  handleResizeForSvg(original, newWidth, newHeight);
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(original);
        try {
            BufferedImage originalImage = ImageIO.read(inputStream);
            if(originalImage==null){
                log.warn("null image detected");
                return new byte[0];
            }
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

    /*
    Based on ideas from Perplexity and Baeldung
     */
    public static byte[] handleResizeForSvg(byte[] inputImageData, int newW, int newH){
        log.trace("starting handleResizeForSvg");
        try {
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
            Document svgDocument = factory.createDocument("http://www.w3.org/2000/svg", new ByteArrayInputStream(inputImageData));
            svgDocument.getDocumentElement().setAttribute("width", Integer.toString(newW));
            svgDocument.getDocumentElement().setAttribute("height", Integer.toString(newH));
            DOMSource source = new DOMSource(svgDocument);
            TransformerFactory factory1 = TransformerFactory.newInstance();
            Transformer transformer = factory1.newTransformer();
            StringWriter writer = new StringWriter();
            StreamResult result  = new StreamResult(writer);
            transformer.transform(source, result);
            byte[] retReady = writer.toString().getBytes(StandardCharsets.UTF_8);
            log.trace("completed resize");
            return retReady;
        }catch (IOException | TransformerConfigurationException ex) {
            log.error("Error resizing SVG {}", ex);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
        //in case of error, return the input
        return inputImageData;
    }

    public static boolean isImageReference(Reference ref){
        return ( ref.uploadedFile != null && ref.uploadedFile.length()>0  && ref.docType.equalsIgnoreCase(SUBSTANCE_IMAGE_REFERENCE_TYPE));
    }

}
