package ix.ncats.resolvers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ix.core.models.PubChemResolutionResult;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@Slf4j
public class PubChemNameListResolver implements Resolver<List<String>> {
    private static final  String PUG      = "https://pubchem.ncbi.nlm.nih.gov/rest/pug";

    @Override
    public Class<List<String>> getType() {
        return (Class<List<String>>) (Class<?>) List.class;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> resolve(String name) {
        //this name is really SMILES
        return List.of();
    }

    public PubChemResolutionResult getDataForChemical(String inchikey) throws IOException, InterruptedException {

        PubChemResolutionResult result = new PubChemResolutionResult();
        String url = String.format("%s/compound/inchikey/cids/JSON", PUG);
        String smilesForm = "inchikey=" + URLEncoder.encode(inchikey, StandardCharsets.UTF_8);
        String contentTypeForPubChem = "application/x-www-form-urlencoded";
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", contentTypeForPubChem)
                .POST(HttpRequest.BodyPublishers.ofString(smilesForm))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        if( status>= 200 && status < 300) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cidsNode = mapper.readTree(response.body())
                    .path("IdentifierList")
                    .path("CID");

            List<String> cids = StreamSupport.stream(cidsNode.spliterator(), false)
                    .map(JsonNode::asText)
                    .toList();

            if(cids.size()==1) {
                log.trace("cid: {}}", cids.get(0));
                result.setCid(cids.get(0));
                if( cids.get(0) != "0") {
                    result.setIupacName(getIupacNameForCid(cids.get(0)));
                    return result;
                }
            }
            log.warn("search for {} returned nothing useful", inchikey);
        }
        else {
            log.info("Error looking up {}: {}", inchikey, response.body());
        }
        return null;
    }

    public String getIupacNameForCid(String cid)  throws IOException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();
        String url = String.format("%s/compound/cid/%s/property/IUPACName,InChIKey/JSON", PUG, cid);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());
        if( response.statusCode() >= 200 && response.statusCode()< 300) {
            JsonNode dataNode = mapper.readTree(response.body());
            JsonNode nameNode =dataNode.findPath("IUPACName");
            if(nameNode!= null) {
                return  nameNode.asText();
            }
            return response.body();
        } else {
            log.warn("Error looking up CID {}; {}", cid, response.body());
        }
        return null;
    }

}
