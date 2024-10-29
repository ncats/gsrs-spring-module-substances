package gsrs.module.substance.utils;

import gov.nih.ncats.molwitch.Chemical;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
public class TautomerUtils {

    private String RESOLVER_BASE = "https://opendata.ncats.nih.gov/resolver/";

    public List<String> getTautomerSmiles(Chemical chemical) throws IOException {
        String url = RESOLVER_BASE+ "tautomers?structure=" + URLEncoder.encode(chemical.toSmiles(), Charset.defaultCharset());
        String response= getFullResponse(url);
        if(response.contains("|")) {
            String trimmed  = response.split("\t")[1];
            List<String> tautomers = Arrays.asList( trimmed.split("\\|"));
            return tautomers;
        }
        return Collections.emptyList();
    }

    public static String getFullResponse(String urlString){
        StringBuilder sb = new StringBuilder();
        try {
            URL url = new URL(urlString);
            InputStream is = url.openStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String line;
            boolean first=true;
            while((line = br.readLine())!=null){
                //System.out.println(line);
                if(!first)
                    sb.append("\n");
                else
                    first=false;
                sb.append(line);
            }
            br.close();

        } catch (MalformedURLException e) {
            log.error("erroneous URL", e);
            e.printStackTrace();
        } catch (IOException e) {
            log.error("error in processing", e);
            e.printStackTrace();
        }
        return sb.toString();
    }

}
