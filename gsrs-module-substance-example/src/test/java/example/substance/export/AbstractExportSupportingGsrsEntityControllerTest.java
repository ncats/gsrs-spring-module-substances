package example.substance.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.AbstractExportSupportingGsrsEntityController;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.service.GsrsEntityService;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.GsrsEntityTestConfiguration;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.search.SearchResult;
import ix.ginas.exporters.SpecificExporterSettings;
import ix.ginas.exporters.ExporterSpecificExportSettings;
import ix.ginas.exporters.GeneralExportSettings;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.*;


@Slf4j
@ActiveProfiles("test")
@SpringBootTest(classes = { GsrsEntityTestConfiguration.class})
@WithMockUser(username = "admin", roles = "Admin")
public class AbstractExportSupportingGsrsEntityControllerTest extends AbstractGsrsJpaEntityJunit5Test {

    AbstractExportSupportingGsrsEntityController controller = new AbstractExportSupportingGsrsEntityController() {
        @Override
        protected LegacyGsrsSearchService getlegacyGsrsSearchService() {
            return null;
        }

        @Override
        protected Object createSearchResponse(List results, SearchResult result, HttpServletRequest request) {
            return null;
        }
      
        @SneakyThrows
        @Override
        protected GsrsEntityService getEntityService() {
            GsrsEntityService entityService = (GsrsEntityService) mock(GsrsEntityService.class);
            when(entityService.getEntityClass()).thenReturn(Substance.class);
            return entityService;
        }
    };
    private void setup(){

        controller = AutowireHelper.getInstance().autowireAndProxy(controller);
    }

    private boolean ranSetup =false;

    @Test
    public void testDoesExporterKeyExist() throws JsonProcessingException {

        if(!ranSetup) {
            setup();
            ranSetup=true;
        }
        //create 2 configurations with identical keys
        String madeUpKey = "Made Up Key" +UUID.randomUUID().toString();
        String config1 = createBogusConfig(madeUpKey);
        Map<String, String> params = new HashMap<>();
        ResponseEntity response = controller.handleExportConfigSave(config1, params);
        System.out.println(response.getBody());
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        ResponseEntity response2 = controller.handleExportConfigSave(config1, params);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response2.getStatusCode());
    }

    @Test
    public void testGetAll() throws NoSuchFieldException, IllegalAccessException {
        if(!ranSetup) {
            setup();
            ranSetup=true;
        }
        ResponseEntity response = controller.handleExportConfigsFetch(new HashMap<String, String>());
        System.out.println("response:");
        System.out.println(response.getBody().getClass().getName());
        Class cls = response.getBody().getClass();
        cls.cast( response.getBody());
        System.out.println("fields:");
        Arrays.stream(cls.getDeclaredFields()).forEach(f-> System.out.println(f.getName()));
        System.out.println("methods:");
        Arrays.stream(cls.getDeclaredMethods()).forEach(f-> System.out.println(f.getName()));
        /*Field fld= cls.getField("obj");
        fld.setAccessible(true);
        Object listOfValues= fld.get(response.getBody());
        List<SpecificExporterSettings> items= (List<SpecificExporterSettings>)  listOfValues;
        items.forEach(i-> System.out.println(String.format("config key: %s; id: %s", i.getConfigurationKey(), i.getConfigurationId())));*/

        System.out.println(response.getBody().toString());
        Assertions.assertTrue(response.toString().length()>0);
    }

    private String createBogusConfig(String expConfKey){
        ObjectMapper objectMapper = new ObjectMapper();

        ExporterSpecificExportSettings exporterSpecificExportSettings = ExporterSpecificExportSettings.builder()
                .columnNames(Arrays.asList("molfile", "UNII", "PT", "CAS"))
                .includeRepeatingDataOnEveryRow(false)
                .build();
        JsonNode exporterSettings = objectMapper.valueToTree(exporterSpecificExportSettings);
        SpecificExporterSettings config =  SpecificExporterSettings.builder()
                .exporterKey(expConfKey)
                .exporterSettings(exporterSettings)
                .configurationKey("Advanced SDFiles " + UUID.randomUUID().toString().substring(0, 10))
                .build();
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("Error creating test config", e);
        }
        return "";
    }
}
