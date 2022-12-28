package gsrs.module.substance.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ix.core.chem.PubChemResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PubChemUtils {

    public List<PubChemResult> lookupInChiKeys(List<String> inchiKeys){

        String pubchemPostUrl = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/property/inchikey/JSON";
        String postData = String.join(",", inchiKeys);
        String json = performPostUsingClient(pubchemPostUrl, postData);
        try {
            return deserializePubChemResult(json);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing results", e);
            throw new RuntimeException(e);
        }
    }

    public List<PubChemResult> deserializePubChemResult(String resultJson) throws JsonProcessingException {
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

    private static String performPostUsingClient(String url, String data) {
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
            if(!data.startsWith("inchikey=")) data="inchikey="+data;

            StringEntity postData = new StringEntity(data);
            post.addHeader("Content-Type", "text/plain");
            post.addHeader("Accept", "application/json");
            List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            urlParameters.add(new BasicNameValuePair("inchikey", data));

            //post.setEntity(new UrlEncodedFormEntity(urlParameters));
            post.setEntity(postData);
            assert client != null;
            HttpResponse response = client.execute(post);
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

}
