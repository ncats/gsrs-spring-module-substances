package ix.ncats.resolvers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Slf4j
public class PubChemNameListResolver implements Resolver<List<String>> {
    public static final String PUBCHEM_RESOLVER =
            "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name";
    public static final String PUBCHEM_RESOLVER_SID =
            "https://pubchem.ncbi.nlm.nih.gov/rest/pug/substance/name";

    private static final  String PUG      = "https://pubchem.ncbi.nlm.nih.gov/rest/pug";
    private static final String PUG_VIEW = "https://pubchem.ncbi.nlm.nih.gov/rest/pug_view";

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

    public String getNamesData(String smiles) throws IOException, InterruptedException {

        String url = String.format("%s/compound/smiles/cids/JSON", PUG);
        String smilesForm = "smiles=" + URLEncoder.encode(smiles, StandardCharsets.UTF_8);
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
            JsonNode o= mapper.readTree(response.body());
            JsonNode cidsNode =o.path("IdentifierList").path("CID");
            List<String> cids = new ArrayList<>();
            if (cidsNode.isArray()) {
                for (JsonNode cidNode : cidsNode) {
                    cids.add(cidNode.asText());
                }
            }
            if(cids== null ){
                log.warn("No CID found for SMILES {}", smiles);
            } else if(cids.size()==1) {
                System.out.printf("cid: %s%n", cids.get(0));
                return getIupacNameForCid(cids.get(0));
            }
            log.warn("search for {} returned nothing", smiles);
        }
        else {
            log.info("Error looking up {}: {}", smiles, response.body());
            System.out.printf("Error looking up %s: %s%n", smiles, response.body());
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
