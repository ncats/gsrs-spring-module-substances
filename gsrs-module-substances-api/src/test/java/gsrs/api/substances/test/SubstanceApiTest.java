package gsrs.api.substances.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.molwitch.Chemical;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate;
import gsrs.api.GsrsEntityRestTemplate;
import gsrs.api.substances.SubstanceRestApi;
import gsrs.assertions.GsrsMatchers;
import gsrs.substances.dto.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(SubstanceRestApi.class)
public class SubstanceApiTest {

    @Autowired
    private MockRestServiceServer mockRestServiceServer;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

   @Autowired
    private SubstanceRestApi api;


    @TestConfiguration
    static class Testconfig {
        @Bean
        public SubstanceRestApi substanceRestApi(RestTemplateBuilder restTemplateBuilder) {

            return new SubstanceRestApi(restTemplateBuilder, "http://example.com", new ObjectMapper());
        }
    }

    @BeforeEach
    public void setup() {
        this.mockRestServiceServer.reset();
    }

    @AfterEach
    public void verify() {
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
    public void singleStructureDiverseCompactRecord() throws IOException {
        String json = "{\"uuid\":\"00003571-8a34-49de-a980-267d6394cfa3\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"definitionType\":\"PRIMARY\",\"definitionLevel\":\"COMPLETE\",\"substanceClass\":\"structurallyDiverse\",\"status\":\"approved\",\"version\":\"1\",\"approvedBy\":\"FDA_SRS\",\"approvalID\":\"B71UA545DE\",\"structurallyDiverse\":{\"uuid\":\"672a9e8e-f5a9-4aec-ac12-79e1e4d24e6b\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"sourceMaterialClass\":\"ORGANISM\",\"sourceMaterialType\":\"PLANT\",\"part\":[\"LEAF\"],\"parentSubstance\":{\"uuid\":\"10d60422-223a-4ca5-88f1-c541ac1461e1\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"refPname\":\"CYNARA SCOLYMUS WHOLE\",\"refuuid\":\"20a5f29a-088d-4b16-93e1-2e1f536c50b7\",\"substanceClass\":\"reference\",\"approvalID\":\"9N3437ZUU0\",\"linkingID\":\"9N3437ZUU0\",\"name\":\"CYNARA SCOLYMUS WHOLE\",\"_nameHTML\":\"CYNARA SCOLYMUS WHOLE\",\"references\":[],\"access\":[]},\"references\":[\"792882b4-0c0f-4284-a8c5-95b891b276d4\",\"c03a4470-3f2c-4742-b1ce-b412df65b84c\"],\"access\":[]},\"_names\":{\"count\":11,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/names\"},\"_modifications\":{\"count\":0,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/modifications\"},\"_references\":{\"count\":38,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/references\"},\"_codes\":{\"count\":4,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/codes\"},\"_relationships\":{\"count\":12,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/relationships\"},\"_nameHTML\":\"CYNARA SCOLYMUS LEAF\",\"_approvalIDDisplay\":\"B71UA545DE\",\"_name\":\"CYNARA SCOLYMUS LEAF\",\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)?view=full\"}";

        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<SubstanceDTO> opt = api.findByResolvedId("00003571-8a34-49de-a980-267d6394cfa3");
        assertTrue(opt.isPresent());
        SubstanceDTO substanceDTO = opt.get();
        assertEquals(substanceDTO.getUuid(), UUID.fromString("00003571-8a34-49de-a980-267d6394cfa3"));
        assertEquals(SubstanceDTO.SubstanceClass.structurallyDiverse, substanceDTO.getSubstanceClass());

        assertEquals(substanceDTO.get_names(), new LazyFetchedCollection(11, "https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/names"));
        assertEquals(substanceDTO.get_codes(), new LazyFetchedCollection(4, "https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/codes"));
        assertEquals(substanceDTO.get_references(), new LazyFetchedCollection(38, "https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/references"));


    }

    @Test
    public void singleChemicalCompactRecord() throws IOException {
        String json = "{\"uuid\":\"00003c75-39d4-4005-9fde-f5eca9abd4f1\",\"created\":1628077823000,\"createdBy\":\"admin\",\"lastEdited\":1628077823000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"definitionType\":\"PRIMARY\",\"definitionLevel\":\"COMPLETE\",\"substanceClass\":\"chemical\",\"status\":\"approved\",\"version\":\"5\",\"approvedBy\":\"FDA_SRS\",\"approvalID\":\"5Y3NBK9IS7\",\"structure\":{\"id\":\"534525ee-706c-4aef-ba47-223db1148e2e\",\"created\":1628077823000,\"lastEdited\":1628077823000,\"deprecated\":false,\"digest\":\"66197313549521884e485965e846d6a5478ca535\",\"molfile\":\"\\n  Marvin  01132104202D          \\n\\n 15 18  0  0  0  0            999 V2000\\n   14.8609   -4.2730    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   15.6572   -4.0568    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   16.1056   -4.7601    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   16.9335   -4.7601    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   17.2594   -3.6489    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   16.6786   -3.0600    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   15.8814   -3.2716    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   15.3045   -2.6828    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   14.4915   -2.8950    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   14.2832   -3.6843    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   13.4948   -3.8918    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   13.2706   -4.6811    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   14.0835   -5.4585    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   14.8609   -5.0734    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   15.5673   -5.3874    0.0000 S   0  0  0  0  0  0  0  0  0  0  0  0\\n  1  2  1  0  0  0  0\\n  2  3  2  0  0  0  0\\n  3  4  1  0  0  0  0\\n  4  5  2  0  0  0  0\\n  6  5  1  0  0  0  0\\n  7  6  2  0  0  0  0\\n  2  7  1  0  0  0  0\\n  7  8  1  0  0  0  0\\n  9  8  2  0  0  0  0\\n 10  9  1  0  0  0  0\\n  1 10  1  0  0  0  0\\n 10 11  2  0  0  0  0\\n 11 12  1  0  0  0  0\\n 13 12  2  0  0  0  0\\n 14 13  1  0  0  0  0\\n  1 14  2  0  0  0  0\\n 14 15  1  0  0  0  0\\n  3 15  1  0  0  0  0\\nM  END\",\"smiles\":\"S1C2=C3C4=C1C=CC=C4C=CC3=CC=C2\",\"formula\":\"C14H8S\",\"opticalActivity\":\"NONE\",\"atropisomerism\":\"No\",\"stereoCenters\":0,\"definedStereo\":0,\"ezCenters\":0,\"charge\":0,\"mwt\":208.278,\"count\":1,\"createdBy\":\"admin\",\"lastEditedBy\":\"admin\",\"self\":\"https://ginas.ncats.nih.gov/app/api/v1/structures(534525ee-706c-4aef-ba47-223db1148e2e)?view=full\",\"stereochemistry\":\"ACHIRAL\",\"access\":[],\"references\":[\"44d3c199-f8df-4627-9cd5-81f586f27692\"],\"_formulaHTML\":\"C<sub>14</sub>H<sub>8</sub>S\",\"_properties\":{\"count\":7,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/structures(534525ee-706c-4aef-ba47-223db1148e2e)/properties\"},\"hash\":\"XGW7KULHL9GR\"},\"_moieties\":{\"count\":1,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003c75-39d4-4005-9fde-f5eca9abd4f1)/moieties\"},\"_names\":{\"count\":4,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003c75-39d4-4005-9fde-f5eca9abd4f1)/names\"},\"_references\":{\"count\":7,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003c75-39d4-4005-9fde-f5eca9abd4f1)/references\"},\"_codes\":{\"count\":5,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003c75-39d4-4005-9fde-f5eca9abd4f1)/codes\"},\"_nameHTML\":\"PHENANTHRO(4,5-BCD)THIOPHENE\",\"_approvalIDDisplay\":\"5Y3NBK9IS7\",\"_name\":\"PHENANTHRO(4,5-BCD)THIOPHENE\",\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003c75-39d4-4005-9fde-f5eca9abd4f1)?view=full\"}";

        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances(00003c75-39d4-4005-9fde-f5eca9abd4f1)"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<ChemicalSubstanceDTO> opt = api.findByResolvedId("00003c75-39d4-4005-9fde-f5eca9abd4f1");
        assertTrue(opt.isPresent());
        ChemicalSubstanceDTO substanceDTO = opt.get();

        assertEquals(SubstanceDTO.SubstanceClass.chemical, substanceDTO.getSubstanceClass());
        assertEquals(substanceDTO.get_names(), new LazyFetchedCollection(4, "https://ginas.ncats.nih.gov/app/api/v1/substances(00003c75-39d4-4005-9fde-f5eca9abd4f1)/names"));
        assertEquals(substanceDTO.get_codes(), new LazyFetchedCollection(5, "https://ginas.ncats.nih.gov/app/api/v1/substances(00003c75-39d4-4005-9fde-f5eca9abd4f1)/codes"));
        assertEquals(substanceDTO.get_references(), new LazyFetchedCollection(7, "https://ginas.ncats.nih.gov/app/api/v1/substances(00003c75-39d4-4005-9fde-f5eca9abd4f1)/references"));

        Chemical actualChemical = substanceDTO.getStructure().asChemical().get();
        assertEquals(15, actualChemical.getAtomCount());
    }

    @Test
    public void singleMixtureCompactRecord() throws IOException {
        String json = "{\"uuid\":\"4fb9df0c-6693-4dbb-baf3-6cba2c3d013e\",\"created\":1628189922000,\"createdBy\":\"admin\",\"lastEdited\":1628189922000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"definitionType\":\"PRIMARY\",\"definitionLevel\":\"COMPLETE\",\"substanceClass\":\"mixture\",\"status\":\"approved\",\"version\":\"1\",\"approvedBy\":\"FDA_SRS\",\"approvalID\":\"LR135I04CJ\",\"mixture\":{\"uuid\":\"471e79c8-72c6-4cdc-b942-1148d1447ea5\",\"created\":1628189922000,\"createdBy\":\"admin\",\"lastEdited\":1628189922000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"components\":[{\"uuid\":\"1fcd4d82-1179-4d70-84e8-3d653dd5f32b\",\"created\":1628189922000,\"createdBy\":\"admin\",\"lastEdited\":1628189922000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"type\":\"MUST_BE_PRESENT\",\"substance\":{\"uuid\":\"0c094892-0dd6-4171-96c9-70c3ba7cdc6b\",\"created\":1628189922000,\"createdBy\":\"admin\",\"lastEdited\":1628189922000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"refPname\":\"MANGANESE 2-GLYCEROPHOSPHATE\",\"refuuid\":\"563f3732-2e8f-4962-b97b-9c7ebf1d62cc\",\"substanceClass\":\"reference\",\"approvalID\":\"94215249TT\",\"linkingID\":\"94215249TT\",\"name\":\"MANGANESE 2-GLYCEROPHOSPHATE\",\"_nameHTML\":\"MANGANESE 2-GLYCEROPHOSPHATE\",\"references\":[],\"access\":[]},\"references\":[],\"access\":[]},{\"uuid\":\"f2674498-7ed5-401f-97f6-aca47ee8dd45\",\"created\":1628189922000,\"createdBy\":\"admin\",\"lastEdited\":1628189922000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"type\":\"MUST_BE_PRESENT\",\"substance\":{\"uuid\":\"370722ae-c239-4fb8-afe7-7881414d1e44\",\"created\":1628189922000,\"createdBy\":\"admin\",\"lastEdited\":1628189922000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"refPname\":\"MANGANESE RAC-GLYCERYL-1-PHOSPHATE\",\"refuuid\":\"11bd0803-9f22-4ca6-a1c1-ccd15c7580b4\",\"substanceClass\":\"reference\",\"approvalID\":\"O8CNB3NY0U\",\"linkingID\":\"O8CNB3NY0U\",\"name\":\"MANGANESE RAC-GLYCERYL-1-PHOSPHATE\",\"_nameHTML\":\"MANGANESE RAC-GLYCERYL-1-PHOSPHATE\",\"references\":[],\"access\":[]},\"references\":[],\"access\":[]}],\"references\":[\"ace06802-d644-4e26-afc2-ab26607c1fda\",\"ecc3e268-830d-4482-88a2-71441e1b76f1\"],\"access\":[]},\"_names\":{\"count\":6,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(4fb9df0c-6693-4dbb-baf3-6cba2c3d013e)/names\"},\"_references\":{\"count\":16,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(4fb9df0c-6693-4dbb-baf3-6cba2c3d013e)/references\"},\"_codes\":{\"count\":3,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(4fb9df0c-6693-4dbb-baf3-6cba2c3d013e)/codes\"},\"_nameHTML\":\"MANGANESE GLYCEROPHOSPHATE\",\"_approvalIDDisplay\":\"LR135I04CJ\",\"_name\":\"MANGANESE GLYCEROPHOSPHATE\",\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(4fb9df0c-6693-4dbb-baf3-6cba2c3d013e)?view=full\"}";

        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances(4fb9df0c-6693-4dbb-baf3-6cba2c3d013e)"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<MixtureSubstanceDTO> opt = api.findByResolvedId("4fb9df0c-6693-4dbb-baf3-6cba2c3d013e");
        assertTrue(opt.isPresent());

        MixtureSubstanceDTO mixtureSubstanceDTO = opt.get();
        assertEquals("4fb9df0c-6693-4dbb-baf3-6cba2c3d013e", mixtureSubstanceDTO.getUuid().toString());
        assertEquals("LR135I04CJ", mixtureSubstanceDTO.getApprovalID());
        assertEquals("FDA_SRS", mixtureSubstanceDTO.getApprovedBy());
        assertEquals(SubstanceDTO.SubstanceClass.mixture, mixtureSubstanceDTO.getSubstanceClass());

        MixtureDTO mixtureDTO = mixtureSubstanceDTO.getMixture();
        assertEquals("471e79c8-72c6-4cdc-b942-1148d1447ea5", mixtureDTO.getUuid().toString());
        assertEquals(2, mixtureDTO.getReferences().size());

        assertEquals(2, mixtureDTO.getComponents().size());

        assertEquals(Arrays.asList("MANGANESE 2-GLYCEROPHOSPHATE", "MANGANESE RAC-GLYCERYL-1-PHOSPHATE"),
                mixtureDTO.getComponents().stream().map(m -> m.getSubstance().getRefPname()).collect(Collectors.toList()));
    }

    @Test
    public void polymerCompactRecord() throws IOException {
        String json = "{\"uuid\":\"7acf5be1-360c-42ce-aea5-9c2969ee410c\",\"created\":1628152616000,\"createdBy\":\"admin\",\"lastEdited\":1628152616000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"definitionType\":\"PRIMARY\",\"definitionLevel\":\"INCOMPLETE\",\"substanceClass\":\"polymer\",\"status\":\"approved\",\"version\":\"3\",\"approvedBy\":\"FDA_SRS\",\"approvalID\":\"4GAS6381TX\",\"polymer\":{\"uuid\":\"acbd989b-91ae-49a5-889f-db62f0314799\",\"created\":1628152616000,\"createdBy\":\"admin\",\"lastEdited\":1628152616000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"classification\":{\"uuid\":\"de21fea5-70e9-4a7b-b48b-723b6dec104d\",\"created\":1628152616000,\"createdBy\":\"admin\",\"lastEdited\":1628152616000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"polymerClass\":\"HOMOPOLYMER\",\"polymerGeometry\":\"LINEAR\",\"polymerSubclass\":[],\"references\":[],\"access\":[]},\"displayStructure\":{\"id\":\"4a483119-9005-4c0e-9318-08bba9974dcf\",\"created\":1628152616000,\"lastEdited\":1628152616000,\"deprecated\":false,\"digest\":\"49b3575ec034401019a488fb6a4ee150d7a65b59\",\"molfile\":\"\\n  Marvin  01132104232D          \\n\\n  6  5  0  0  0  0            999 V2000\\n    8.0382   -6.5429    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.7095   -5.7862    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    8.8482   -5.3835    0.0000 *   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.0244   -5.3882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    6.1404   -5.7908    0.0000 *   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.2735   -6.5399    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n  2  1  1  0  0  0  0\\n  2  3  1  0  0  0  0\\n  4  2  1  0  0  0  0\\n  5  4  1  0  0  0  0\\n  2  6  1  0  0  0  0\\nM  STY  1   1 SRU\\nM  SCN  1   1 HT \\nM  SAL   1  4   1   2   4   6\\nM  SDI   1  4    6.6044   -6.9629    6.6044   -4.9682\\nM  SDI   1  4    8.4582   -4.9682    8.4582   -6.9629\\nM  END\",\"smiles\":\"[He]CC([He])(C)C\",\"formula\":\"C4H8\",\"opticalActivity\":\"NONE\",\"atropisomerism\":\"No\",\"stereoCenters\":0,\"definedStereo\":0,\"ezCenters\":0,\"charge\":0,\"mwt\":56.1063,\"count\":1,\"createdBy\":\"admin\",\"lastEditedBy\":\"admin\",\"self\":\"https://ginas.ncats.nih.gov/app/api/v1/structures(4a483119-9005-4c0e-9318-08bba9974dcf)?view=full\",\"access\":[],\"references\":[],\"_formulaHTML\":\"C<sub>4</sub>H<sub>8</sub>\"},\"idealizedStructure\":{\"id\":\"544e0b30-5a6f-4835-8dd2-20a6579fd37d\",\"created\":1628152616000,\"lastEdited\":1628152616000,\"deprecated\":false,\"digest\":\"49b3575ec034401019a488fb6a4ee150d7a65b59\",\"molfile\":\"\\n  Marvin  01132104232D          \\n\\n  6  5  0  0  0  0            999 V2000\\n    8.0382   -6.5429    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.7095   -5.7862    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    8.8482   -5.3835    0.0000 *   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.0244   -5.3882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    6.1404   -5.7908    0.0000 *   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.2735   -6.5399    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n  2  1  1  0  0  0  0\\n  2  3  1  0  0  0  0\\n  4  2  1  0  0  0  0\\n  5  4  1  0  0  0  0\\n  2  6  1  0  0  0  0\\nM  STY  1   1 SRU\\nM  SCN  1   1 HT \\nM  SAL   1  4   1   2   4   6\\nM  SDI   1  4    6.6044   -6.9629    6.6044   -4.9682\\nM  SDI   1  4    8.4582   -4.9682    8.4582   -6.9629\\nM  END\",\"smiles\":\"[He]CC([He])(C)C\",\"formula\":\"C4H8\",\"opticalActivity\":\"NONE\",\"atropisomerism\":\"No\",\"stereoCenters\":0,\"definedStereo\":0,\"ezCenters\":0,\"charge\":0,\"mwt\":56.1063,\"count\":1,\"createdBy\":\"admin\",\"lastEditedBy\":\"admin\",\"self\":\"https://ginas.ncats.nih.gov/app/api/v1/structures(544e0b30-5a6f-4835-8dd2-20a6579fd37d)?view=full\",\"access\":[],\"references\":[],\"_formulaHTML\":\"C<sub>4</sub>H<sub>8</sub>\",\"_properties\":{\"count\":1,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/structures(544e0b30-5a6f-4835-8dd2-20a6579fd37d)/properties\"}},\"monomers\":[{\"uuid\":\"c85ad8f8-63af-4e49-a821-db7e69d0aada\",\"created\":1628152616000,\"createdBy\":\"admin\",\"lastEdited\":1628152616000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"amount\":{\"uuid\":\"bcaa2de3-3a6d-46e3-b427-89a4c3cb43d4\",\"created\":1628152616000,\"createdBy\":\"admin\",\"lastEdited\":1628152616000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"type\":\"WEIGHT PERCENTAGE\",\"average\":100.0,\"references\":[],\"access\":[]},\"monomerSubstance\":{\"uuid\":\"39f072b9-1de0-4ba3-8617-d4e1053da452\",\"created\":1628152616000,\"createdBy\":\"admin\",\"lastEdited\":1628152616000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"refPname\":\"ISOBUTYLENE\",\"refuuid\":\"6789212e-f4a0-407b-8d3e-37e3876d4dfe\",\"substanceClass\":\"reference\",\"approvalID\":\"QA2LMR467H\",\"linkingID\":\"QA2LMR467H\",\"name\":\"ISOBUTYLENE\",\"_nameHTML\":\"ISOBUTYLENE\",\"references\":[],\"access\":[]},\"type\":\"MONOMER\",\"defining\":false,\"references\":[],\"access\":[]}],\"structuralUnits\":[],\"references\":[\"70bde78b-a0bd-4b81-8181-20b8c96310fe\",\"baeac6ef-1618-4124-bc88-dd6a224e550e\"],\"access\":[]},\"_properties\":{\"count\":1,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(7acf5be1-360c-42ce-aea5-9c2969ee410c)/properties\"},\"_names\":{\"count\":6,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(7acf5be1-360c-42ce-aea5-9c2969ee410c)/names\"},\"_modifications\":{\"count\":0,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(7acf5be1-360c-42ce-aea5-9c2969ee410c)/modifications\"},\"_references\":{\"count\":8,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(7acf5be1-360c-42ce-aea5-9c2969ee410c)/references\"},\"_codes\":{\"count\":2,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(7acf5be1-360c-42ce-aea5-9c2969ee410c)/codes\"},\"_nameHTML\":\"POLYISOBUTYLENE (2600000 MW)\",\"_approvalIDDisplay\":\"4GAS6381TX\",\"_name\":\"POLYISOBUTYLENE (2600000 MW)\",\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(7acf5be1-360c-42ce-aea5-9c2969ee410c)?view=full\"}";

        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances(7acf5be1-360c-42ce-aea5-9c2969ee410c)"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<PolymerSubstanceDTO> opt = api.findByResolvedId("7acf5be1-360c-42ce-aea5-9c2969ee410c");
        assertTrue(opt.isPresent());

        PolymerSubstanceDTO polymerSubstance = opt.get();
        assertEquals("7acf5be1-360c-42ce-aea5-9c2969ee410c", polymerSubstance.getUuid().toString());

        assertEquals(new LazyFetchedCollection(0, "https://ginas.ncats.nih.gov/app/api/v1/substances(7acf5be1-360c-42ce-aea5-9c2969ee410c)/modifications"),
                polymerSubstance.get_modifications());

        PolymerClassificationDTO expected = PolymerClassificationDTO.builder()
                .uuid(UUID.fromString("de21fea5-70e9-4a7b-b48b-723b6dec104d"))
                .polymerClass("HOMOPOLYMER")
                .polymerGeometry("LINEAR")
                .build();


        assertThat(polymerSubstance.getPolymer().getClassification(), GsrsMatchers.matchesExample(expected));

        assertEquals("C4H8", polymerSubstance.getPolymer().getDisplayStructure().getFormula());
        assertEquals("C4H8", polymerSubstance.getPolymer().getIdealizedStructure().getFormula());
    }

    @Test
    public void namesList() throws IOException {
        String json = "[{\"uuid\":\"bbc560bc-4d3f-4718-87ba-466599170172\",\"created\":1628152616000,\"createdBy\":\"admin\",\"lastEdited\":1628152616000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"name\":\"POLYISOBUTYLENE (2600000 MW)\",\"type\":\"cn\",\"domains\":[],\"languages\":[\"en\"],\"nameJurisdiction\":[],\"nameOrgs\":[],\"preferred\":false,\"displayName\":true,\"_name\":\"POLYISOBUTYLENE (2600000 MW)\",\"_nameHTML\":\"POLYISOBUTYLENE (2600000 MW)\",\"references\":[\"9d99629a-a0f3-4533-87c2-cd508892ab4a\"],\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/names(bbc560bc-4d3f-4718-87ba-466599170172)?view=full\"},{\"uuid\":\"0678d775-1274-4546-9f74-48bdd8582bbd\",\"created\":1628152616000,\"createdBy\":\"admin\",\"lastEdited\":1628152616000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"name\":\"POLYISOBUTYLENE 416-479\",\"type\":\"cn\",\"domains\":[],\"languages\":[\"en\"],\"nameJurisdiction\":[],\"nameOrgs\":[],\"preferred\":false,\"displayName\":false,\"_name\":\"POLYISOBUTYLENE 416-479\",\"_nameHTML\":\"POLYISOBUTYLENE 416-479\",\"references\":[\"9d99629a-a0f3-4533-87c2-cd508892ab4a\"],\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/names(0678d775-1274-4546-9f74-48bdd8582bbd)?view=full\"},{\"uuid\":\"5e23a748-b818-459e-9806-d625aac1193e\",\"created\":1628152616000,\"createdBy\":\"admin\",\"lastEdited\":1628152616000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"name\":\"POLYISOBUTYLENE (2600000 MW(SUB V))\",\"type\":\"cn\",\"domains\":[],\"languages\":[\"en\"],\"nameJurisdiction\":[],\"nameOrgs\":[],\"preferred\":false,\"displayName\":false,\"_name\":\"POLYISOBUTYLENE (2600000 MW(SUB V))\",\"_nameHTML\":\"POLYISOBUTYLENE (2600000 MW(SUB V))\",\"references\":[\"9d99629a-a0f3-4533-87c2-cd508892ab4a\"],\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/names(5e23a748-b818-459e-9806-d625aac1193e)?view=full\"},{\"uuid\":\"47de8127-e0ac-4395-9b00-540809dc3d52\",\"created\":1628152616000,\"createdBy\":\"admin\",\"lastEdited\":1628152616000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"name\":\"POLYISOBUTYLENE (2500000 MW)\",\"type\":\"cn\",\"domains\":[],\"languages\":[\"en\"],\"nameJurisdiction\":[],\"nameOrgs\":[],\"preferred\":false,\"displayName\":false,\"_name\":\"POLYISOBUTYLENE (2500000 MW)\",\"_nameHTML\":\"POLYISOBUTYLENE (2500000 MW)\",\"references\":[\"baeac6ef-1618-4124-bc88-dd6a224e550e\"],\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/names(47de8127-e0ac-4395-9b00-540809dc3d52)?view=full\"},{\"uuid\":\"4748ad90-8c77-4217-888b-12a308017fd8\",\"created\":1628152616000,\"createdBy\":\"admin\",\"lastEdited\":1628152616000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"name\":\"OPPANOL B-150\",\"type\":\"bn\",\"domains\":[],\"languages\":[\"en\"],\"nameJurisdiction\":[],\"nameOrgs\":[],\"preferred\":false,\"displayName\":false,\"_name\":\"OPPANOL B-150\",\"_nameHTML\":\"OPPANOL B-150\",\"references\":[\"baeac6ef-1618-4124-bc88-dd6a224e550e\"],\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/names(4748ad90-8c77-4217-888b-12a308017fd8)?view=full\"},{\"uuid\":\"581228b3-4c1a-42df-b030-9a4122ae58b6\",\"created\":1628152616000,\"createdBy\":\"admin\",\"lastEdited\":1628152616000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"name\":\"OPPANOL B 150\",\"type\":\"bn\",\"domains\":[],\"languages\":[\"en\"],\"nameJurisdiction\":[],\"nameOrgs\":[],\"preferred\":false,\"displayName\":false,\"_name\":\"OPPANOL B 150\",\"_nameHTML\":\"OPPANOL B 150\",\"references\":[\"baeac6ef-1618-4124-bc88-dd6a224e550e\"],\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/names(581228b3-4c1a-42df-b030-9a4122ae58b6)?view=full\"}]";

        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances(bbc560bc-4d3f-4718-87ba-466599170172)/names"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<List<NameDTO>> opt = api.getNamesOfSubstance("bbc560bc-4d3f-4718-87ba-466599170172");

        List<NameDTO> names = opt.get();

        assertEquals(6, names.size());
        assertEquals(Arrays.asList("POLYISOBUTYLENE (2600000 MW)",
                "POLYISOBUTYLENE 416-479",
                "POLYISOBUTYLENE (2600000 MW(SUB V))",
                "POLYISOBUTYLENE (2500000 MW)",
                "OPPANOL B-150",
                "OPPANOL B 150"
        ), names.stream().map(NameDTO::getName).collect(Collectors.toList()));

    }

    @Test
    public void textSearch() throws IOException {
        Resource json = new ClassPathResource("substanceSearchResponse.json");
        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances/search?top=10&skip=0&fdim=10"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        AbstractLegacySearchGsrsEntityRestTemplate.SearchResult result = api.search(AbstractLegacySearchGsrsEntityRestTemplate.SearchRequest.builder().build().sanitize());

        assertEquals(10, result.getCount());
        assertEquals(10, result.getTop());
        assertEquals(17, result.getTotal());
        Optional<AbstractLegacySearchGsrsEntityRestTemplate.Facet> facet = result.getFacet("Code System");
        assertEquals(13, facet.get().getFacetValue("CAS").get().getCount());

        List<? extends SubstanceDTO> substances = result.getContent();
        assertEquals(10, substances.size());

        assertTrue(substances.stream().map(Object::getClass).collect(Collectors.toSet()).size() > 1);

    }

    @Test
    public void protein() throws IOException {
        Resource json = new ClassPathResource("protein.json");
        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances(myProtein)"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<ProteinSubstanceDTO> proteinSubstanceDTO = api.findByResolvedId("myProtein");

        assertTrue(proteinSubstanceDTO.get().getProtein().getSubunits().get(0).getSequence()
                .startsWith("MERAPPDGPLNASGALAGEAAAAGGARGFSAAWTAVLAALMALLIVATVL"));

    }

    // alex begin

    @Test
    public void testfindByResolvedIdError() throws IOException {
        String json = "{\"uuid\":\"11113571-8a34-49de-a980-267d6394cfa3\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"definitionType\":\"PRIMARY\",\"definitionLevel\":\"COMPLETE\",\"substanceClass\":\"structurallyDiverse\",\"status\":\"approved\",\"version\":\"1\",\"approvedBy\":\"FDA_SRS\",\"approvalID\":\"B71UA545DE\",\"structurallyDiverse\":{\"uuid\":\"672a9e8e-f5a9-4aec-ac12-79e1e4d24e6b\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"sourceMaterialClass\":\"ORGANISM\",\"sourceMaterialType\":\"PLANT\",\"part\":[\"LEAF\"],\"parentSubstance\":{\"uuid\":\"10d60422-223a-4ca5-88f1-c541ac1461e1\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"refPname\":\"CYNARA SCOLYMUS WHOLE\",\"refuuid\":\"20a5f29a-088d-4b16-93e1-2e1f536c50b7\",\"substanceClass\":\"reference\",\"approvalID\":\"9N3437ZUU0\",\"linkingID\":\"9N3437ZUU0\",\"name\":\"CYNARA SCOLYMUS WHOLE\",\"_nameHTML\":\"CYNARA SCOLYMUS WHOLE\",\"references\":[],\"access\":[]},\"references\":[\"792882b4-0c0f-4284-a8c5-95b891b276d4\",\"c03a4470-3f2c-4742-b1ce-b412df65b84c\"],\"access\":[]},\"_names\":{\"count\":11,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/names\"},\"_modifications\":{\"count\":0,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/modifications\"},\"_references\":{\"count\":38,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/references\"},\"_codes\":{\"count\":4,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/codes\"},\"_relationships\":{\"count\":12,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/relationships\"},\"_nameHTML\":\"CYNARA SCOLYMUS LEAF\",\"_approvalIDDisplay\":\"B71UA545DE\",\"_name\":\"CYNARA SCOLYMUS LEAF\",\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(11113571-8a34-49de-a980-267d6394cfa3)?view=full\"}";
        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances(11113571-8a34-49de-a980-267d6394cfa3)"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        Optional<SubstanceDTO> opt = api.findByResolvedId("11113571-8a34-49de-a980-267d6394cfa3");
        assertFalse(opt.isPresent());
    }

    @Test
    public void testPageError() throws IOException {
        this.mockRestServiceServer
                // Why does test only pass with slash after substances/?
                .expect(requestTo("/api/v1/substances/?top=0&skip=10"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        long top = 0;
        long skip = 10;
        Optional<GsrsEntityRestTemplate.PagedResult<SubstanceDTO>> opt = api.page(top, skip);
        assertFalse(opt.isPresent());
    }

    @Test
    public void testFindByResolvedIdError() throws IOException {
        String json = "{\"uuid\":\"11113571-8a34-49de-a980-267d6394cfa3\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"definitionType\":\"PRIMARY\",\"definitionLevel\":\"COMPLETE\",\"substanceClass\":\"structurallyDiverse\",\"status\":\"approved\",\"version\":\"1\",\"approvedBy\":\"FDA_SRS\",\"approvalID\":\"B71UA545DE\",\"structurallyDiverse\":{\"uuid\":\"672a9e8e-f5a9-4aec-ac12-79e1e4d24e6b\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"sourceMaterialClass\":\"ORGANISM\",\"sourceMaterialType\":\"PLANT\",\"part\":[\"LEAF\"],\"parentSubstance\":{\"uuid\":\"10d60422-223a-4ca5-88f1-c541ac1461e1\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"refPname\":\"CYNARA SCOLYMUS WHOLE\",\"refuuid\":\"20a5f29a-088d-4b16-93e1-2e1f536c50b7\",\"substanceClass\":\"reference\",\"approvalID\":\"9N3437ZUU0\",\"linkingID\":\"9N3437ZUU0\",\"name\":\"CYNARA SCOLYMUS WHOLE\",\"_nameHTML\":\"CYNARA SCOLYMUS WHOLE\",\"references\":[],\"access\":[]},\"references\":[\"792882b4-0c0f-4284-a8c5-95b891b276d4\",\"c03a4470-3f2c-4742-b1ce-b412df65b84c\"],\"access\":[]},\"_names\":{\"count\":11,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/names\"},\"_modifications\":{\"count\":0,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/modifications\"},\"_references\":{\"count\":38,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/references\"},\"_codes\":{\"count\":4,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/codes\"},\"_relationships\":{\"count\":12,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/relationships\"},\"_nameHTML\":\"CYNARA SCOLYMUS LEAF\",\"_approvalIDDisplay\":\"B71UA545DE\",\"_name\":\"CYNARA SCOLYMUS LEAF\",\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(11113571-8a34-49de-a980-267d6394cfa3)?view=full\"}";
        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances(11113571-8a34-49de-a980-267d6394cfa3)"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        Optional<SubstanceDTO> opt = api.findByResolvedId("11113571-8a34-49de-a980-267d6394cfa3");
        assertFalse(opt.isPresent());
    }

    @Test
    public void testEntityExistsFound() throws IOException {
        String json = "{\"uuid\":\"11113571-8a34-49de-a980-267d6394cfa3\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"definitionType\":\"PRIMARY\",\"definitionLevel\":\"COMPLETE\",\"substanceClass\":\"structurallyDiverse\",\"status\":\"approved\",\"version\":\"1\",\"approvedBy\":\"FDA_SRS\",\"approvalID\":\"B71UA545DE\",\"structurallyDiverse\":{\"uuid\":\"672a9e8e-f5a9-4aec-ac12-79e1e4d24e6b\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"sourceMaterialClass\":\"ORGANISM\",\"sourceMaterialType\":\"PLANT\",\"part\":[\"LEAF\"],\"parentSubstance\":{\"uuid\":\"10d60422-223a-4ca5-88f1-c541ac1461e1\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"refPname\":\"CYNARA SCOLYMUS WHOLE\",\"refuuid\":\"20a5f29a-088d-4b16-93e1-2e1f536c50b7\",\"substanceClass\":\"reference\",\"approvalID\":\"9N3437ZUU0\",\"linkingID\":\"9N3437ZUU0\",\"name\":\"CYNARA SCOLYMUS WHOLE\",\"_nameHTML\":\"CYNARA SCOLYMUS WHOLE\",\"references\":[],\"access\":[]},\"references\":[\"792882b4-0c0f-4284-a8c5-95b891b276d4\",\"c03a4470-3f2c-4742-b1ce-b412df65b84c\"],\"access\":[]},\"_names\":{\"count\":11,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/names\"},\"_modifications\":{\"count\":0,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/modifications\"},\"_references\":{\"count\":38,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/references\"},\"_codes\":{\"count\":4,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/codes\"},\"_relationships\":{\"count\":12,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/relationships\"},\"_nameHTML\":\"CYNARA SCOLYMUS LEAF\",\"_approvalIDDisplay\":\"B71UA545DE\",\"_name\":\"CYNARA SCOLYMUS LEAF\",\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(11113571-8a34-49de-a980-267d6394cfa3)?view=full\"}";
        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances(11113571-8a34-49de-a980-267d6394cfa3)?view=key"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        boolean exists = api.existsById(UUID.fromString("11113571-8a34-49de-a980-267d6394cfa3"));
        assertEquals(true, (boolean) exists);
    }

    @Test
    public void testEntityExistsNotFound() throws IOException {
        String json = "{\"uuid\":\"11113571-8a34-49de-a980-267d6394cfa3\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"definitionType\":\"PRIMARY\",\"definitionLevel\":\"COMPLETE\",\"substanceClass\":\"structurallyDiverse\",\"status\":\"approved\",\"version\":\"1\",\"approvedBy\":\"FDA_SRS\",\"approvalID\":\"B71UA545DE\",\"structurallyDiverse\":{\"uuid\":\"672a9e8e-f5a9-4aec-ac12-79e1e4d24e6b\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"sourceMaterialClass\":\"ORGANISM\",\"sourceMaterialType\":\"PLANT\",\"part\":[\"LEAF\"],\"parentSubstance\":{\"uuid\":\"10d60422-223a-4ca5-88f1-c541ac1461e1\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"refPname\":\"CYNARA SCOLYMUS WHOLE\",\"refuuid\":\"20a5f29a-088d-4b16-93e1-2e1f536c50b7\",\"substanceClass\":\"reference\",\"approvalID\":\"9N3437ZUU0\",\"linkingID\":\"9N3437ZUU0\",\"name\":\"CYNARA SCOLYMUS WHOLE\",\"_nameHTML\":\"CYNARA SCOLYMUS WHOLE\",\"references\":[],\"access\":[]},\"references\":[\"792882b4-0c0f-4284-a8c5-95b891b276d4\",\"c03a4470-3f2c-4742-b1ce-b412df65b84c\"],\"access\":[]},\"_names\":{\"count\":11,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/names\"},\"_modifications\":{\"count\":0,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/modifications\"},\"_references\":{\"count\":38,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/references\"},\"_codes\":{\"count\":4,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/codes\"},\"_relationships\":{\"count\":12,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/relationships\"},\"_nameHTML\":\"CYNARA SCOLYMUS LEAF\",\"_approvalIDDisplay\":\"B71UA545DE\",\"_name\":\"CYNARA SCOLYMUS LEAF\",\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(11113571-8a34-49de-a980-267d6394cfa3)?view=full\"}";
        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances(11113571-8a34-49de-a980-267d6394cfa3)?view=key"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        boolean exists = api.existsById(UUID.fromString("11113571-8a34-49de-a980-267d6394cfa3"));
        assertEquals(false, exists);
    }

    @Test
    public void testEntityExistsError() throws IOException {
        String json = "{\"uuid\":\"11113571-8a34-49de-a980-267d6394cfa3\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"definitionType\":\"PRIMARY\",\"definitionLevel\":\"COMPLETE\",\"substanceClass\":\"structurallyDiverse\",\"status\":\"approved\",\"version\":\"1\",\"approvedBy\":\"FDA_SRS\",\"approvalID\":\"B71UA545DE\",\"structurallyDiverse\":{\"uuid\":\"672a9e8e-f5a9-4aec-ac12-79e1e4d24e6b\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"sourceMaterialClass\":\"ORGANISM\",\"sourceMaterialType\":\"PLANT\",\"part\":[\"LEAF\"],\"parentSubstance\":{\"uuid\":\"10d60422-223a-4ca5-88f1-c541ac1461e1\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"refPname\":\"CYNARA SCOLYMUS WHOLE\",\"refuuid\":\"20a5f29a-088d-4b16-93e1-2e1f536c50b7\",\"substanceClass\":\"reference\",\"approvalID\":\"9N3437ZUU0\",\"linkingID\":\"9N3437ZUU0\",\"name\":\"CYNARA SCOLYMUS WHOLE\",\"_nameHTML\":\"CYNARA SCOLYMUS WHOLE\",\"references\":[],\"access\":[]},\"references\":[\"792882b4-0c0f-4284-a8c5-95b891b276d4\",\"c03a4470-3f2c-4742-b1ce-b412df65b84c\"],\"access\":[]},\"_names\":{\"count\":11,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/names\"},\"_modifications\":{\"count\":0,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/modifications\"},\"_references\":{\"count\":38,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/references\"},\"_codes\":{\"count\":4,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/codes\"},\"_relationships\":{\"count\":12,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/relationships\"},\"_nameHTML\":\"CYNARA SCOLYMUS LEAF\",\"_approvalIDDisplay\":\"B71UA545DE\",\"_name\":\"CYNARA SCOLYMUS LEAF\",\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(11113571-8a34-49de-a980-267d6394cfa3)?view=full\"}";
        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances(11113571-8a34-49de-a980-267d6394cfa3)?view=key"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        boolean exists = api.existsById(UUID.fromString("11113571-8a34-49de-a980-267d6394cfa3"));
        assertEquals(false, exists);
    }

    @Test
    public void testCautiousEntityExistsFound() throws IOException {
        String json = "{\"uuid\":\"11113571-8a34-49de-a980-267d6394cfa3\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"definitionType\":\"PRIMARY\",\"definitionLevel\":\"COMPLETE\",\"substanceClass\":\"structurallyDiverse\",\"status\":\"approved\",\"version\":\"1\",\"approvedBy\":\"FDA_SRS\",\"approvalID\":\"B71UA545DE\",\"structurallyDiverse\":{\"uuid\":\"672a9e8e-f5a9-4aec-ac12-79e1e4d24e6b\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"sourceMaterialClass\":\"ORGANISM\",\"sourceMaterialType\":\"PLANT\",\"part\":[\"LEAF\"],\"parentSubstance\":{\"uuid\":\"10d60422-223a-4ca5-88f1-c541ac1461e1\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"refPname\":\"CYNARA SCOLYMUS WHOLE\",\"refuuid\":\"20a5f29a-088d-4b16-93e1-2e1f536c50b7\",\"substanceClass\":\"reference\",\"approvalID\":\"9N3437ZUU0\",\"linkingID\":\"9N3437ZUU0\",\"name\":\"CYNARA SCOLYMUS WHOLE\",\"_nameHTML\":\"CYNARA SCOLYMUS WHOLE\",\"references\":[],\"access\":[]},\"references\":[\"792882b4-0c0f-4284-a8c5-95b891b276d4\",\"c03a4470-3f2c-4742-b1ce-b412df65b84c\"],\"access\":[]},\"_names\":{\"count\":11,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/names\"},\"_modifications\":{\"count\":0,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/modifications\"},\"_references\":{\"count\":38,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/references\"},\"_codes\":{\"count\":4,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/codes\"},\"_relationships\":{\"count\":12,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/relationships\"},\"_nameHTML\":\"CYNARA SCOLYMUS LEAF\",\"_approvalIDDisplay\":\"B71UA545DE\",\"_name\":\"CYNARA SCOLYMUS LEAF\",\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(11113571-8a34-49de-a980-267d6394cfa3)?view=full\"}";
        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances(11113571-8a34-49de-a980-267d6394cfa3)?view=key"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        Boolean exists = api.cautiousExistsById("11113571-8a34-49de-a980-267d6394cfa3", "uuid");
        assertEquals(true, (boolean) exists);
    }

    @Test
    public void testCautiousEntityExistsNotFound() throws IOException {
        String json = "{\"uuid\":\"11113571-8a34-49de-a980-267d6394cfa3\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"definitionType\":\"PRIMARY\",\"definitionLevel\":\"COMPLETE\",\"substanceClass\":\"structurallyDiverse\",\"status\":\"approved\",\"version\":\"1\",\"approvedBy\":\"FDA_SRS\",\"approvalID\":\"B71UA545DE\",\"structurallyDiverse\":{\"uuid\":\"672a9e8e-f5a9-4aec-ac12-79e1e4d24e6b\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"sourceMaterialClass\":\"ORGANISM\",\"sourceMaterialType\":\"PLANT\",\"part\":[\"LEAF\"],\"parentSubstance\":{\"uuid\":\"10d60422-223a-4ca5-88f1-c541ac1461e1\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"refPname\":\"CYNARA SCOLYMUS WHOLE\",\"refuuid\":\"20a5f29a-088d-4b16-93e1-2e1f536c50b7\",\"substanceClass\":\"reference\",\"approvalID\":\"9N3437ZUU0\",\"linkingID\":\"9N3437ZUU0\",\"name\":\"CYNARA SCOLYMUS WHOLE\",\"_nameHTML\":\"CYNARA SCOLYMUS WHOLE\",\"references\":[],\"access\":[]},\"references\":[\"792882b4-0c0f-4284-a8c5-95b891b276d4\",\"c03a4470-3f2c-4742-b1ce-b412df65b84c\"],\"access\":[]},\"_names\":{\"count\":11,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/names\"},\"_modifications\":{\"count\":0,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/modifications\"},\"_references\":{\"count\":38,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/references\"},\"_codes\":{\"count\":4,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/codes\"},\"_relationships\":{\"count\":12,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/relationships\"},\"_nameHTML\":\"CYNARA SCOLYMUS LEAF\",\"_approvalIDDisplay\":\"B71UA545DE\",\"_name\":\"CYNARA SCOLYMUS LEAF\",\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(11113571-8a34-49de-a980-267d6394cfa3)?view=full\"}";
        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances(11113571-8a34-49de-a980-267d6394cfa3)?view=key"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        Boolean exists = api.cautiousExistsById("11113571-8a34-49de-a980-267d6394cfa3", "uuid");
        assertEquals(false, exists);
    }

    @Test
    public void testCautiousEntityExistsError() throws IOException {
        String json = "{\"uuid\":\"11113571-8a34-49de-a980-267d6394cfa3\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"definitionType\":\"PRIMARY\",\"definitionLevel\":\"COMPLETE\",\"substanceClass\":\"structurallyDiverse\",\"status\":\"approved\",\"version\":\"1\",\"approvedBy\":\"FDA_SRS\",\"approvalID\":\"B71UA545DE\",\"structurallyDiverse\":{\"uuid\":\"672a9e8e-f5a9-4aec-ac12-79e1e4d24e6b\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"sourceMaterialClass\":\"ORGANISM\",\"sourceMaterialType\":\"PLANT\",\"part\":[\"LEAF\"],\"parentSubstance\":{\"uuid\":\"10d60422-223a-4ca5-88f1-c541ac1461e1\",\"created\":1628185287000,\"createdBy\":\"admin\",\"lastEdited\":1628185287000,\"lastEditedBy\":\"admin\",\"deprecated\":false,\"refPname\":\"CYNARA SCOLYMUS WHOLE\",\"refuuid\":\"20a5f29a-088d-4b16-93e1-2e1f536c50b7\",\"substanceClass\":\"reference\",\"approvalID\":\"9N3437ZUU0\",\"linkingID\":\"9N3437ZUU0\",\"name\":\"CYNARA SCOLYMUS WHOLE\",\"_nameHTML\":\"CYNARA SCOLYMUS WHOLE\",\"references\":[],\"access\":[]},\"references\":[\"792882b4-0c0f-4284-a8c5-95b891b276d4\",\"c03a4470-3f2c-4742-b1ce-b412df65b84c\"],\"access\":[]},\"_names\":{\"count\":11,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/names\"},\"_modifications\":{\"count\":0,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/modifications\"},\"_references\":{\"count\":38,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/references\"},\"_codes\":{\"count\":4,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/codes\"},\"_relationships\":{\"count\":12,\"url\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(00003571-8a34-49de-a980-267d6394cfa3)/relationships\"},\"_nameHTML\":\"CYNARA SCOLYMUS LEAF\",\"_approvalIDDisplay\":\"B71UA545DE\",\"_name\":\"CYNARA SCOLYMUS LEAF\",\"access\":[],\"_self\":\"https://ginas.ncats.nih.gov/app/api/v1/substances(11113571-8a34-49de-a980-267d6394cfa3)?view=full\"}";
        this.mockRestServiceServer
                .expect(requestTo("/api/v1/substances(11113571-8a34-49de-a980-267d6394cfa3)?view=key"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        Boolean exists = api.cautiousExistsById("11113571-8a34-49de-a980-267d6394cfa3", "uuid");
        assertNull(exists);
    }


    // alex end

}
