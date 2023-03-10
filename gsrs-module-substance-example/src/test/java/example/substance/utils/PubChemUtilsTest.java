package example.substance.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import gsrs.module.substance.utils.PubChemUtils;
import ix.core.chem.PubChemResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PubChemUtilsTest {

    @Test
    public void deserializePubChemResultTest() throws JsonProcessingException {
        String input1=null;
        List<PubChemResult> results = PubChemUtils.deserializePubChemResult(input1);
        Assertions.assertTrue(results.isEmpty());
    }

    @Test
    public void deserializePubChemResult2Test() throws JsonProcessingException {
        String input1="{\n" +
                "    \"PropertyTable\": {\n" +
                "        \"Properties\": [\n" +
                "            {\n" +
                "                \"CID\": 229021,\n" +
                "                \"InChIKey\": \"WRWBCPJQPDHXTJ-DTMQFJJTSA-N\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"CID\": 101269,\n" +
                "                \"InChIKey\": \"IZHVBANLECCAGF-UHFFFAOYSA-N\"\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";
        List<PubChemResult> results = PubChemUtils.deserializePubChemResult(input1);
        Assertions.assertEquals(2, results.size());
        results.forEach(p-> System.out.printf("CID: %d - InChIKey: %s\n", p.CID, p.InChIKey));
    }

    @Test
    public void lookupInChiKeysTest() {
        List<String> queryData = Arrays.asList("WRWBCPJQPDHXTJ-DTMQFJJTSA-N","IZHVBANLECCAGF-UHFFFAOYSA-N","JKINPMFPGULFQY-UHFFFAOYSA-N","IRPSJVWFSWAZSZ-OIUSMDOTSA-L","LLJOASAHEBQANS-UHFFFAOYSA-N","DPMXBHNNOATSAP-UHFFFAOYSA-N","JTHVJAYASQZXKB-UHFFFAOYSA-M","NKUWPVYXJNSCQL-UHFFFAOYSA-L","LFQSCWFLJHTTHZ-UHFFFAOYSA-N","INQSMEFCAIHTJG-UHFFFAOYSA-N","QIQXTHQIDYTFRH-UHFFFAOYSA-N","QJYNZEYHSMRWBK-NIKIMHBISA-N","ISTBXSFGFOYLTM-NZEDGPFZSA-N","WPYMKLBDIGXBTP-UHFFFAOYSA-N");
        //List<String> queryData = Arrays.asList("WRWBCPJQPDHXTJ-DTMQFJJTSA-N");
        //String pubChemPostUrl="https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/property/inchikey/JSON";
        List<PubChemResult>results= PubChemUtils.lookupInChiKeys(queryData);
        Assertions.assertTrue(queryData.stream().allMatch(q->results.stream().anyMatch(r->r.InChIKey.equals(q))));
    }

    @Test
    public void lookupInChiKeyDoesNotExistsTest() {
        List<String> queryData = Collections.singletonList("SVQYPFKTOLHBOY-UHFFFAOYSA-N");
        List<PubChemResult>results= PubChemUtils.lookupInChiKeys(queryData);
        Assertions.assertTrue(queryData.stream().noneMatch(i->results.stream().anyMatch(i2->i2.getInChIKey().equals(i))));
    }

    @Test
    public void postTest() throws JsonProcessingException {
        List<String> queryData = Arrays.asList("WRWBCPJQPDHXTJ-DTMQFJJTSA-N","IZHVBANLECCAGF-UHFFFAOYSA-N","JKINPMFPGULFQY-UHFFFAOYSA-N","IRPSJVWFSWAZSZ-OIUSMDOTSA-L","LLJOASAHEBQANS-UHFFFAOYSA-N","DPMXBHNNOATSAP-UHFFFAOYSA-N","JTHVJAYASQZXKB-UHFFFAOYSA-M","NKUWPVYXJNSCQL-UHFFFAOYSA-L","LFQSCWFLJHTTHZ-UHFFFAOYSA-N","INQSMEFCAIHTJG-UHFFFAOYSA-N","QIQXTHQIDYTFRH-UHFFFAOYSA-N","QJYNZEYHSMRWBK-NIKIMHBISA-N","ISTBXSFGFOYLTM-NZEDGPFZSA-N","WPYMKLBDIGXBTP-UHFFFAOYSA-N");
        //List<String> queryData = Arrays.asList("WRWBCPJQPDHXTJ-DTMQFJJTSA-N");
        String pubChemPostUrl="https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/property/inchikey/JSON";
        String json= PubChemUtils.performPostUsingClient(pubChemPostUrl, String.join(",", queryData));
        List<PubChemResult>results= PubChemUtils.deserializePubChemResult(json);
        Assertions.assertTrue(queryData.stream().allMatch(q->results.stream().anyMatch(r->r.InChIKey.equals(q))));
    }

}
