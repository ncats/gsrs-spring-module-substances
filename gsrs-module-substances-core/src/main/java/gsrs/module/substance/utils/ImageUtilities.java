package gsrs.module.substance.utils;

import gsrs.repository.PayloadRepository;
import gsrs.service.PayloadService;
import ix.core.models.Payload;
import ix.ginas.models.v1.Code;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.batik.gvt.ImageNode;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;

@Slf4j
public class ImageUtilities {

    public static final String SUBSTANCE_IMAGE_REFERENCE_TYPE = "IMAGE REFERENCE";
    public static final String PLANTS_OF_THE_WORLD = "POWO";
    public static final String KEW_PARSING_EXPRESSION ="div[class=c-gallery__image-container first-image] img";;

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
    public ImageInfo getSubstanceImage(Substance substance, Integer imageNumber){
        log.trace("starting in getSubstanceImage");
        List<ImageInfo> images =getSubstanceImageInfos(substance);
        if( !images.isEmpty() ) {
            if( images.size()==1 || imageNumber==0)  {
                return images.get(0);
            }
            int numberToLookUp = imageNumber % images.size();
            if( numberToLookUp >= 0 && numberToLookUp < images.size()) {
                return images.get(numberToLookUp);
            }
        }
        if(substance.substanceClass == Substance.SubstanceClass.structurallyDiverse
                && substance.codes.stream().anyMatch(c->c.codeSystem.equalsIgnoreCase(PLANTS_OF_THE_WORLD))) {
            log.trace("str div with POWO");
            Optional<Code> powoCode = substance.codes.stream().filter(c->c.codeSystem.equalsIgnoreCase(PLANTS_OF_THE_WORLD)).findFirst();
            if( powoCode.isPresent() ) {
                log.trace("going to retrieve and return POWO image");
                String data=TautomerUtils.getFullResponse(powoCode.get().url);
                return new ImageInfo(true, data.getBytes(), "jpg");
            }
        }
        return new ImageInfo(false,null,null);
    }

    public List<ImageInfo> getSubstanceImageInfos(Substance substance){
        log.trace("starting in getSubstanceImageInfos");
        List<ImageInfo> images =
                substance.references.stream()
                        .filter(r->isImageReference(r))
                        .map(r->getPayloadIdFromUrl(r.uploadedFile))
                        .filter(p->p != null && p.length() >0)
                        .map(id->payloadRepository.findById(UUID.fromString(id)))
                        .filter(Optional::isPresent)
                        .map( p->p.get())
                        .map(p->{
                            try {
                                Optional<byte[]> fileData = getBytesFromPayloadId(p.id, Math.toIntExact(p.size));
                                return new ImageInfo(true, fileData.get(), p.mimeType);
                            } catch (IOException e) {
                                log.error("Error retrieving payload/image: {}", e.getMessage());
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(Collectors.toList());
        return images;
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

    public static String extractImageElementText(String rawDocument, String expression){
        org.jsoup.nodes.Document doc = Jsoup.parse(rawDocument);
        Elements elements= doc.body().select(expression);
        if(elements.size() == 1) {
            if( elements.get(0).nodeName().equalsIgnoreCase("img")) {
                return elements.get(0).toString();
            }
            return elements.get(0).select("img").toString();
        }
        return "multiple";
    }
}
