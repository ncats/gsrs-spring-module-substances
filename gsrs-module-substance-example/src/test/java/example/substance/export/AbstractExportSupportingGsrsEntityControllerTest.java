package example.substance.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.substance.support.TestTransactionManagers;
import gsrs.autoconfigure.GsrsExportConfiguration;
import gsrs.controller.AbstractExportSupportingGsrsEntityController;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModelProcessor;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.repository.TextRepository;
import gsrs.service.GsrsEntityService;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.models.Text;
import ix.core.search.SearchResult;
import ix.ginas.exporters.ExporterSpecificExportSettings;
import ix.ginas.exporters.SpecificExporterSettings;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AbstractExportSupportingGsrsEntityControllerTest {

    private static final String ENTITY_CONTEXT = "substances";
    private static final String ENTITY_LABEL =
            SpecificExporterSettings.getEntityKeyFromClass(Substance.class.getName());

    @Mock
    private TextRepository textRepository;
    @Mock
    private GsrsExportConfiguration gsrsExportConfiguration;
    @Mock
    private GsrsEntityService<Substance, UUID> entityService;

    private final List<Text> savedTexts = new ArrayList<>();
    private AbstractExportSupportingGsrsEntityController controller;
    private Object previousStaticContextAccessor;

    @BeforeEach
    void setUp() {
        previousStaticContextAccessor = ReflectionTestUtils.getField(StaticContextAccessor.class, "instance");
        installHateoasProcessor();

        when(entityService.getEntityClass()).thenReturn(Substance.class);
        when(textRepository.findByLabel(anyString())).thenAnswer(invocation -> findByLabel(invocation.getArgument(0)));

        controller = new TestExportSupportingController(entityService);
        ReflectionTestUtils.setField(controller, "textRepository", textRepository);
        ReflectionTestUtils.setField(controller, "gsrsExportConfiguration", gsrsExportConfiguration);
        ReflectionTestUtils.setField(controller, "transactionManager", TestTransactionManagers.mockTransactionManager());
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(StaticContextAccessor.class, "instance", previousStaticContextAccessor);
    }

    @Test
    void savingDuplicateExporterKeyReturnsBadRequest() throws JsonProcessingException {
        when(textRepository.saveAndFlush(any(Text.class))).thenAnswer(invocation -> save(invocation.getArgument(0)));
        String configJson = createExportConfigJson("SDF-" + UUID.randomUUID());

        ResponseEntity<Object> firstResponse = controller.handleExportConfigSave(configJson, new HashMap<>());
        ResponseEntity<Object> duplicateResponse = controller.handleExportConfigSave(configJson, new HashMap<>());

        Assertions.assertEquals(HttpStatus.OK, firstResponse.getStatusCode());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, duplicateResponse.getStatusCode());
        Assertions.assertEquals(1, savedTexts.size());
    }

    @Test
    void fetchingConfigurationsUsesStoredAndHardcodedPresets() throws JsonProcessingException {
        savedTexts.add(textConfig("database-exporter", "Database preset", 7L));
        when(entityService.getContext()).thenReturn(ENTITY_CONTEXT);
        when(gsrsExportConfiguration.getHardcodedDefaultExportPresets(ENTITY_CONTEXT))
                .thenReturn(Collections.singletonList(textConfig("hardcoded-exporter", "Hardcoded preset", 8L)));

        ResponseEntity<Object> response = controller.handleExportConfigsFetch(new HashMap<>());

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        verify(textRepository).findByLabel(ENTITY_LABEL);
        verify(gsrsExportConfiguration).getHardcodedDefaultExportPresets(ENTITY_CONTEXT);
    }

    private void installHateoasProcessor() {
        StaticContextAccessor staticContextAccessor = new StaticContextAccessor();
        ApplicationContext applicationContext = org.mockito.Mockito.mock(ApplicationContext.class);
        GsrsUnwrappedEntityModelProcessor processor = org.mockito.Mockito.mock(GsrsUnwrappedEntityModelProcessor.class);
        when(applicationContext.getBean(GsrsUnwrappedEntityModelProcessor.class)).thenReturn(processor);
        when(processor.process(any(GsrsUnwrappedEntityModel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(staticContextAccessor, "applicationContext", applicationContext);
        ReflectionTestUtils.setField(StaticContextAccessor.class, "instance", staticContextAccessor);
    }

    private List<Text> findByLabel(String label) {
        return savedTexts.stream()
                .filter(text -> Objects.equals(label, text.label))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Text save(Text text) {
        if (text.id == null) {
            text.id = (long) savedTexts.size() + 1;
        }
        savedTexts.removeIf(existing -> Objects.equals(existing.id, text.id));
        savedTexts.add(text);
        return text;
    }

    private Text textConfig(String exporterKey, String configurationKey, Long id) throws JsonProcessingException {
        Text text = exporterSettings(exporterKey, configurationKey)
                .asText();
        text.id = id;
        return text;
    }

    private String createExportConfigJson(String exporterKey) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(exporterSettings(exporterKey, "Advanced SDFiles"));
    }

    private SpecificExporterSettings exporterSettings(String exporterKey, String configurationKey) {
        ObjectMapper objectMapper = new ObjectMapper();
        ExporterSpecificExportSettings exporterSpecificExportSettings = ExporterSpecificExportSettings.builder()
                .columnNames(Arrays.asList("molfile", "UNII", "PT", "CAS"))
                .includeRepeatingDataOnEveryRow(false)
                .build();
        JsonNode exporterSettings = objectMapper.valueToTree(exporterSpecificExportSettings);
        return SpecificExporterSettings.builder()
                .exporterKey(exporterKey)
                .exporterSettings(exporterSettings)
                .configurationKey(configurationKey + " " + UUID.randomUUID())
                .entityClass(Substance.class.getName())
                .owner("test")
                .build();
    }

    private static class TestExportSupportingController extends AbstractExportSupportingGsrsEntityController {

        private final GsrsEntityService<Substance, UUID> entityService;

        private TestExportSupportingController(GsrsEntityService<Substance, UUID> entityService) {
            this.entityService = entityService;
        }

        @Override
        protected LegacyGsrsSearchService getlegacyGsrsSearchService() {
            return null;
        }

        @Override
        protected Object createSearchResponse(List results, SearchResult result, HttpServletRequest request) {
            return null;
        }

        @Override
        protected GsrsEntityService getEntityService() {
            return entityService;
        }
    }
}
