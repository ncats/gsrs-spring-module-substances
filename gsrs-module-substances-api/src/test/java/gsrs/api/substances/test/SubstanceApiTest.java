package gsrs.api.substances.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.api.substances.LazyFetchedCollection;
import gsrs.api.substances.SubstanceDTO;
import gsrs.api.substances.SubstanceRestApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

@RestClientTest(SubstanceRestApi.class)
public class SubstanceApiTest {

    @Autowired
    private MockRestServiceServer mockRestServiceServer;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;
    @Autowired
    private SubstanceRestApi api;


    @TestConfiguration
    static class Testconfig{
        @Bean
        public SubstanceRestApi substanceRestApi(RestTemplateBuilder restTemplateBuilder){

            return new SubstanceRestApi(restTemplateBuilder, "http://example.com", new ObjectMapper());
        }
    }
    @BeforeEach
    public void setup(){
        this.mockRestServiceServer.reset();
    }

    @AfterEach
    public void verify(){
        this.mockRestServiceServer.verify();
    }

    @Test
    public void count() throws IOException {
        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances/@count"))
                .andRespond(withSuccess("126161", MediaType.APPLICATION_JSON));

        assertEquals(126161L, api.count());
    }

    @Test
    public void singleStructureDiverseCompactRecord() throws IOException{
        String json = "{\"uuid\":\"00003571-8a34-49de-a980-267d6394cfa3\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"definitionType\":\"PRIMARY\",\"definitionLevel\":\"COMPLETE\",\"substanceClass\":\"structurallyDiverse\",\"status\":\"approved\",\"version\":\"1\",\"approvedBy\":\"FDA_SRS\",\"approvalID\":\"B71UA545DE\",\"structurallyDiverse\":{\"uuid\":\"672a9e8e-f5a9-4aec-ac12-79e1e4d24e6b\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"sourceMaterialClass\":\"ORGANISM\",\"sourceMaterialType\":\"PLANT\",\"part\":[\"LEAF\"],\"parentSubstance\":{\"uuid\":\"10d60422-223a-4ca5-88f1-c541ac1461e1\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"refPname\":\"CYNARA SCOLYMUS WHOLE\",\"refuuid\":\"20a5f29a-088d-4b16-93e1-2e1f536c50b7\",\"substanceClass\":\"reference\",\"approvalID\":\"9N3437ZUU0\",\"linkingID\":\"9N3437ZUU0\",\"name\":\"CYNARA SCOLYMUS WHOLE\",\"_nameHTML\":\"CYNARA SCOLYMUS WHOLE\",\"references\":[],\"access\":[]},\"references\":[\"792882b4-0c0f-4284-a8c5-95b891b276d4\",\"c03a4470-3f2c-4742-b1ce-b412df65b84c\"],\"access\":[]},\"_names\":{\"count\":11,\"href\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/names\"},\"_modifications\":{\"count\":0,\"href\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/modifications\"},\"_references\":{\"count\":38,\"href\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/references\"},\"_codes\":{\"count\":4,\"href\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/codes\"},\"_relationships\":{\"count\":12,\"href\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/relationships\"},\"_nameHTML\":\"CYNARA SCOLYMUS LEAF\",\"_approvalIDDisplay\":\"B71UA545DE\",\"_name\":\"CYNARA SCOLYMUS LEAF\",\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)?view=full\"}";

        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<SubstanceDTO> opt= api.findByResolvedId("00003571-8a34-49de-a980-267d6394cfa3");
        assertTrue(opt.isPresent());
        SubstanceDTO substanceDTO = opt.get();
        assertEquals(substanceDTO.getUuid(), UUID.fromString("00003571-8a34-49de-a980-267d6394cfa3"));
        assertEquals(SubstanceDTO.SubstanceClass.structurallyDiverse, substanceDTO.getSubstanceClass());

        assertEquals(substanceDTO.get_names(), new LazyFetchedCollection(11, "https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/names"));
        assertEquals(substanceDTO.get_codes(), new LazyFetchedCollection(4, "https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/codes"));
        assertEquals(substanceDTO.get_references(), new LazyFetchedCollection(38, "https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/references"));



    }

    @Test
    public void singleChemicalCompactRecord() throws IOException{
        String json = "{\"uuid\":\"00003c75-39d4-4005-9fde-f5eca9abd4f1\",\"created\":1628077823000,\"createdBy\":\"admin\",\"lastEdited\":1628077823000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"definitionType\":\"PRIMARY\",\"definitionLevel\":\"COMPLETE\",\"substanceClass\":\"chemical\",\"status\":\"approved\",\"version\":\"5\",\"approvedBy\":\"FDA_SRS\",\"approvalID\":\"5Y3NBK9IS7\",\"structure\":{\"id\":\"534525ee-706c-4aef-ba47-223db1148e2e\",\"created\":1628077823000,\"lastEdited\":1628077823000,\"deprecated\":false,\"digest\":\"66197313549521884e485965e846d6a5478ca535\",\"molfile\":\"\\n  Marvin  01132104202D          \\n\\n 15 18  0  0  0  0            999 V2000\\n   14.8609   -4.2730    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   15.6572   -4.0568    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   16.1056   -4.7601    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   16.9335   -4.7601    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   17.2594   -3.6489    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   16.6786   -3.0600    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   15.8814   -3.2716    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   15.3045   -2.6828    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   14.4915   -2.8950    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   14.2832   -3.6843    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   13.4948   -3.8918    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   13.2706   -4.6811    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   14.0835   -5.4585    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   14.8609   -5.0734    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   15.5673   -5.3874    0.0000 S   0  0  0  0  0  0  0  0  0  0  0  0\\n  1  2  1  0  0  0  0\\n  2  3  2  0  0  0  0\\n  3  4  1  0  0  0  0\\n  4  5  2  0  0  0  0\\n  6  5  1  0  0  0  0\\n  7  6  2  0  0  0  0\\n  2  7  1  0  0  0  0\\n  7  8  1  0  0  0  0\\n  9  8  2  0  0  0  0\\n 10  9  1  0  0  0  0\\n  1 10  1  0  0  0  0\\n 10 11  2  0  0  0  0\\n 11 12  1  0  0  0  0\\n 13 12  2  0  0  0  0\\n 14 13  1  0  0  0  0\\n  1 14  2  0  0  0  0\\n 14 15  1  0  0  0  0\\n  3 15  1  0  0  0  0\\nM  END\",\"smiles\":\"S1C2=C3C4=C1C=CC=C4C=CC3=CC=C2\",\"formula\":\"C14H8S\",\"opticalActivity\":\"NONE\",\"atropisomerism\":\"No\",\"stereoCenters\":0,\"definedStereo\":0,\"ezCenters\":0,\"charge\":0,\"mwt\":208.278,\"count\":1,\"createdBy\":\"admin\",\"lastEditedBy\":\"admin\",\"self\":\"https://ginas.ncats.nih.gov/app/api/v1/structures(534525ee-706c-4aef-ba47-223db1148e2e)?view=full\",\"stereochemistry\":\"ACHIRAL\",\"access\":[],\"references\":[\"44d3c199-f8df-4627-9cd5-81f586f27692\"],\"_formulaHTML\":\"C<sub>14</sub>H<sub>8</sub>S\",\"_properties\":{\"count\":7,\"href\":\"https://ginas.ncats.nih.gov/app/api/v1/structures(534525ee-706c-4aef-ba47-223db1148e2e)/properties\"},\"hash\":\"XGW7KULHL9GR\"},\"_moieties\":{\"count\":1,\"href\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003c75-39d4-4005-9fde-f5eca9abd4f1)/moieties\"},\"_names\":{\"count\":4,\"href\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003c75-39d4-4005-9fde-f5eca9abd4f1)/names\"},\"_references\":{\"count\":7,\"href\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003c75-39d4-4005-9fde-f5eca9abd4f1)/references\"},\"_codes\":{\"count\":5,\"href\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003c75-39d4-4005-9fde-f5eca9abd4f1)/codes\"},\"_nameHTML\":\"PHENANTHRO(4,5-BCD)THIOPHENE\",\"_approvalIDDisplay\":\"5Y3NBK9IS7\",\"_name\":\"PHENANTHRO(4,5-BCD)THIOPHENE\",\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003c75-39d4-4005-9fde-f5eca9abd4f1)?view=full\"}";

        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances(00003c75-39d4-4005-9fde-f5eca9abd4f1)"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<SubstanceDTO> opt= api.findByResolvedId("00003c75-39d4-4005-9fde-f5eca9abd4f1");
        assertTrue(opt.isPresent());
        SubstanceDTO substanceDTO = opt.get();
    }
}
