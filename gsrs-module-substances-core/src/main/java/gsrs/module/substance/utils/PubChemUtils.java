package gsrs.module.substance.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import ix.core.chem.PubChemResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PubChemUtils {

    private final static String PUBCHEM_LOOKUP_URL = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/property/inchikey/JSON";

    /*
    return CIDs for InChIKey input
     */
    public static List<PubChemResult> lookupInChiKeys(List<String> inchiKeys){

        String postData = String.join(",", inchiKeys);
        String json = performPostUsingClient(PUBCHEM_LOOKUP_URL, postData);
        try {
            return deserializePubChemResult(json);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing results", e);
            throw new RuntimeException(e);
        }
    }

    public static List<PubChemResult> deserializePubChemResult(String resultJson) throws JsonProcessingException {
        if( resultJson==null || resultJson.trim().length()==0) {
            log.warn("Empty input in deserializePubChemResult");
            return new ArrayList<>();
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode resultNode = mapper.readTree(resultJson);
        if( resultNode.at("/PropertyTable/Properties") instanceof ArrayNode) {
            ArrayNode properties = (ArrayNode) resultNode.at("/PropertyTable/Properties");
            return mapper.readValue(properties.toString(), new TypeReference<List<PubChemResult>>() {
            });
        }
        return new ArrayList<>();
    }

    public static String performPostUsingClient(String url, String data) {
        CloseableHttpClient client = null;
        try
        {
            HttpClientBuilder builder =HttpClientBuilder.create();
            //builder.setMaxConnPerRoute(50);
            //builder.setMaxConnTotal(100);
            client = builder.build();
        }
        catch(Exception ex) {
            System.err.println("Error creating HttpClient:");
            ex.printStackTrace();
        }
        HttpPost post = new HttpPost(url);
        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("inchikey", data);
            HttpEntity multipart = builder.build();
            post.addHeader("Accept", "application/json");
            post.setEntity(multipart);
            assert client != null;
            HttpResponse response = client.execute(post);
            log.info("result of post: {}",response.getStatusLine().getStatusCode());
            InputStream is = response.getEntity().getContent();
            StringBuilder textBuilder = new StringBuilder();
            try (Reader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                int c;
                while ((c = reader.read()) != -1) {
                    textBuilder.append((char) c);
                }
            }
            is.close();
            return textBuilder.toString();
        } catch (IOException ex) {
            log.error("Error in performPostUsingClient: ", ex);
        }
        return null;
    }

    /*
    Including this as a backup.  At first, POST method did not work.
     */
    private static String performGetUsingClient(String url, String data) {
        CloseableHttpClient client = null;
        try
        {
            HttpClientBuilder builder =HttpClientBuilder.create();
            client = builder.build();
        }
        catch(Exception ex) {
            System.err.println("Error creating HttpClient:");
            ex.printStackTrace();
        }
        try {
            if(!data.startsWith("inchikey=")) data="inchikey="+data;
            url = url+"?"+data;
            HttpGet get = new HttpGet(url);
            get.addHeader("Content-Type", "text/plain");
            get.addHeader("Accept", "application/json");

            assert client != null;
            HttpResponse response = client.execute(get);
            InputStream is = response.getEntity().getContent();
            StringBuilder textBuilder = new StringBuilder();
            try (Reader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                int c;
                while ((c = reader.read()) != -1) {
                    textBuilder.append((char) c);
                }
            }
            is.close();
            return textBuilder.toString();
        } catch (IOException ex) {
            log.error("Error in performGetUsingClient: ", ex);
        }
        return null;
    }

}
