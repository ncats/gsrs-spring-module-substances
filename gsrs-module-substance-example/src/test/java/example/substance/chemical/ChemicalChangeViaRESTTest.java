package example.substance.chemical;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.GsrsModuleSubstanceApplication;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Moiety;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.UUID;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles="Admin")
class ChemicalChangeViaRESTTest extends AbstractSubstanceJpaFullStackEntityTest{
    @Test
    void changeStructureTest() throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        String substanceid="4127dad7-8ef7-48c4-a4c7-a3867935f28f";
        String retrievalUrl="http://localhost:8080/api/v1/substances(" + substanceid +")?view=internal";
        ResponseEntity<ChemicalSubstance> substanceResponseEntity  = restTemplate.getForEntity(retrievalUrl, ChemicalSubstance.class);

        ChemicalSubstance actualSubstance= substanceResponseEntity.getBody();
        Assertions.assertEquals(substanceid, actualSubstance.getUuid().toString());
        Assertions.assertEquals(6, actualSubstance.names.size());

        String methylatedMolfile="\n" +
                "  ACCLDraw09062312382D\n" +
                "\n" +
                " 67 69  0  0  1  0  0  0  0  0999 V2000\n" +
                "   11.7184  -16.8543    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   12.8282  -16.4504    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
                "   13.0333  -15.2873    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   12.1285  -14.5283    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   10.9827  -14.8139    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   10.3568  -13.8124    0.0000 N   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "   11.1159  -12.9077    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   12.2109  -13.3501    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   13.7329  -17.2095    0.0000 N   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "   14.8427  -16.8056    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   15.7474  -17.5647    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
                "   16.8572  -17.1607    0.0000 N   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "   16.8571  -15.9797    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   17.8799  -15.3893    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
                "   18.9027  -15.9797    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   19.9254  -15.3893    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   19.9254  -14.2083    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   20.9482  -13.6177    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   21.9710  -14.2083    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   21.9710  -15.3893    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   20.9482  -15.9797    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   22.9937  -13.6177    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   17.8799  -14.2083    0.0000 N   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "   16.8571  -13.6177    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   15.8344  -14.2083    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   16.8571  -12.4368    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
                "   15.8344  -11.8463    0.0000 N   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "   14.8116  -12.4368    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   13.7887  -11.8463    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
                "   12.7660  -12.4368    0.0000 N   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "   11.7433  -11.8463    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   11.7433  -10.6653    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
                "   10.7206  -10.0748    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   10.7205   -8.8938    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    9.6977   -8.3034    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   11.7433   -8.3034    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   12.7660  -10.0748    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   10.7205  -12.4368    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   13.7887  -10.6653    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   14.8116  -10.0748    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   14.8116   -8.8938    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   13.7887   -8.3034    0.0000 N   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "   13.7887   -7.1224    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   14.8116   -6.5318    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   12.7660   -6.5318    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   14.8116  -13.6177    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   17.8799  -11.8463    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   18.9027  -12.4368    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   19.9254  -11.8463    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   19.9254  -10.6653    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   15.8344  -15.3893    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   15.5423  -18.7278    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
                "   16.4470  -19.4868    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   16.2420  -20.6499    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   14.4325  -19.1316    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   14.6376  -17.9686    0.0000 H   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   15.0478  -15.6426    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   10.8138  -16.0952    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   11.5133  -18.0173    0.0000 N   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "   10.4519  -18.5350    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
                "    9.4092  -17.9807    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    8.4075  -18.6064    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    9.3679  -16.8004    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   10.6162  -19.7045    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   11.7794  -19.9096    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   12.3338  -18.8669    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   22.9937  -12.4382    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  1  2  1  0  0  0  0\n" +
                "  2  3  1  0  0  0  0\n" +
                "  3  4  1  0  0  0  0\n" +
                "  4  5  2  0  0  0  0\n" +
                "  5  6  1  0  0  0  0\n" +
                "  6  7  1  0  0  0  0\n" +
                "  8  7  2  0  0  0  0\n" +
                "  4  8  1  0  0  0  0\n" +
                "  2  9  1  6  0  0  0\n" +
                "  9 10  1  0  0  0  0\n" +
                " 10 11  1  0  0  0  0\n" +
                " 11 12  1  0  0  0  0\n" +
                " 12 13  1  0  0  0  0\n" +
                " 13 14  1  0  0  0  0\n" +
                " 14 15  1  0  0  0  0\n" +
                " 15 16  1  0  0  0  0\n" +
                " 16 17  2  0  0  0  0\n" +
                " 17 18  1  0  0  0  0\n" +
                " 18 19  2  0  0  0  0\n" +
                " 20 19  1  0  0  0  0\n" +
                " 21 20  2  0  0  0  0\n" +
                " 16 21  1  0  0  0  0\n" +
                " 19 22  1  0  0  0  0\n" +
                " 14 23  1  1  0  0  0\n" +
                " 23 24  1  0  0  0  0\n" +
                " 24 25  2  0  0  0  0\n" +
                " 26 24  1  1  0  0  0\n" +
                " 26 27  1  0  0  0  0\n" +
                " 27 28  1  0  0  0  0\n" +
                " 28 29  1  0  0  0  0\n" +
                " 29 30  1  1  0  0  0\n" +
                " 30 31  1  0  0  0  0\n" +
                " 31 32  1  0  0  0  0\n" +
                " 32 33  1  0  0  0  0\n" +
                " 33 34  1  0  0  0  0\n" +
                " 34 35  1  0  0  0  0\n" +
                " 34 36  2  0  0  0  0\n" +
                " 32 37  1  6  0  0  0\n" +
                " 31 38  2  0  0  0  0\n" +
                " 29 39  1  0  0  0  0\n" +
                " 39 40  1  0  0  0  0\n" +
                " 40 41  1  0  0  0  0\n" +
                " 41 42  1  0  0  0  0\n" +
                " 42 43  1  0  0  0  0\n" +
                " 43 44  2  0  0  0  0\n" +
                " 43 45  1  0  0  0  0\n" +
                " 28 46  2  0  0  0  0\n" +
                " 26 47  1  0  0  0  0\n" +
                " 47 48  1  0  0  0  0\n" +
                " 48 49  1  0  0  0  0\n" +
                " 49 50  1  0  0  0  0\n" +
                " 13 51  2  0  0  0  0\n" +
                " 11 52  1  0  0  0  0\n" +
                " 52 53  1  0  0  0  0\n" +
                " 53 54  1  0  0  0  0\n" +
                " 52 55  1  6  0  0  0\n" +
                " 11 56  1  6  0  0  0\n" +
                " 10 57  2  0  0  0  0\n" +
                "  1 58  2  0  0  0  0\n" +
                "  1 59  1  0  0  0  0\n" +
                " 59 60  1  0  0  0  0\n" +
                " 60 61  1  1  0  0  0\n" +
                " 61 62  1  0  0  0  0\n" +
                " 61 63  2  0  0  0  0\n" +
                " 60 64  1  0  0  0  0\n" +
                " 64 65  1  0  0  0  0\n" +
                " 66 65  1  0  0  0  0\n" +
                " 59 66  1  0  0  0  0\n" +
                " 22 67  1  0  0  0  0\n" +
                "M  END\n";

        actualSubstance.getStructure().molfile= methylatedMolfile;
        Moiety moiety= actualSubstance.getMoieties().get(0);
        moiety.structure.id=UUID.randomUUID();
        RestTemplate restTemplate2 = new RestTemplate();
        String updateUrl="http://localhost:8080/api/v1/substances";
        HttpHeaders headers = new HttpHeaders();
        headers.add("auth-username", "admin");
        headers.add("auth-password", "admin");
        HttpEntity<ChemicalSubstance> request = new HttpEntity<>(actualSubstance, headers);
        //restTemplate2.put(updateUrl, request);
        ResponseEntity<Substance> result= restTemplate2.exchange(updateUrl, HttpMethod.PUT,  request, Substance.class);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());

    }
}
