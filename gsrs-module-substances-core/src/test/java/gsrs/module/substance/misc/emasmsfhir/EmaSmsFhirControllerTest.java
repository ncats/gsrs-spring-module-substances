package gsrs.module.substance.misc.emasmsfhir;

import gsrs.module.substance.SubstanceEntityService;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmaSmsFhirControllerTest {

    private SubstanceEntityService substanceEntityService;
    private EmaSmsFhirController controller;
    private EmaSmsSubstanceDefinitionFhirMapper emaSmsSubstanceDefinitionFhirMapper;

    private Substance testSubstance;
    private String testSubstanceId;

    @BeforeEach
    public void setUp() {
        substanceEntityService = mock(SubstanceEntityService.class);
        emaSmsSubstanceDefinitionFhirMapper = mock(EmaSmsSubstanceDefinitionFhirMapper.class);
        controller = new EmaSmsFhirController();
        setField(controller, "substanceEntityService", substanceEntityService);
        setField(controller, "emaSmsSimpleRecordFhirMapper", new EmaSmsSimpleRecordFhirMapper());
        setField(controller, "emaSmsSubstanceDefinitionFhirMapper", emaSmsSubstanceDefinitionFhirMapper);

        testSubstanceId = "306d24b9-a6b8-4091-8024-02f9ec24b705";
        testSubstance = new Substance();
        testSubstance.setUuid(UUID.fromString(testSubstanceId));
        testSubstance.substanceClass = Substance.SubstanceClass.chemical;

        Name displayName = new Name();
        displayName.name = "Sodium Chloride";
        displayName.displayName = true;
        displayName.addLanguage("en");
        testSubstance.names = new ArrayList<>();
        testSubstance.names.add(displayName);

        testSubstance.codes = new ArrayList<>();
    }

    @Test
    @DisplayName("Simple record endpoint returns JSON when substance is found")
    public void testMakeSimpleEmaSmsRecordSuccess() {
        when(substanceEntityService.flexLookup(testSubstanceId))
                .thenReturn(Optional.of(testSubstance));

        ResponseEntity<?> response = controller.makeSimpleEmaSmsRecord(testSubstanceId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        String responseBody = (String) response.getBody();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("resourceType"));
    }

    @Test
    @DisplayName("Simple record endpoint returns 404 when not found")
    public void testMakeSimpleEmaSmsRecordNotFound() {
        when(substanceEntityService.flexLookup(testSubstanceId))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.makeSimpleEmaSmsRecord(testSubstanceId);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        String responseBody = (String) response.getBody();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("not found"));
    }

    @Test
    @DisplayName("Simple record endpoint returns 500 on service exception")
    public void testMakeSimpleEmaSmsRecordInternalError() {
        when(substanceEntityService.flexLookup(testSubstanceId))
                .thenThrow(new RuntimeException("Database connection error"));

        ResponseEntity<?> response = controller.makeSimpleEmaSmsRecord(testSubstanceId);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        String responseBody = (String) response.getBody();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("Internal error"));
    }

    @Test
    @DisplayName("SubstanceDefinition endpoint returns mapped resource")
    public void testMakeEmaSmsSubstanceDefinitionSuccess() {
        when(substanceEntityService.flexLookup(testSubstanceId))
                .thenReturn(Optional.of(testSubstance));
        org.hl7.fhir.r5.model.SubstanceDefinition substanceDefinition = new org.hl7.fhir.r5.model.SubstanceDefinition();
        substanceDefinition.setId("example");
        when(emaSmsSubstanceDefinitionFhirMapper.generateEmaSmsSubstanceDefinitionFromSubstance(testSubstance))
                .thenReturn(substanceDefinition);

        ResponseEntity<?> response = controller.makeEmaSmsSubstanceDefinition(testSubstanceId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String responseBody = (String) response.getBody();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("SubstanceDefinition"));
        verify(emaSmsSubstanceDefinitionFhirMapper, times(1))
                .generateEmaSmsSubstanceDefinitionFromSubstance(testSubstance);
    }

    @Test
    @DisplayName("SubstanceDefinition endpoint returns 404 when not found")
    public void testMakeEmaSmsSubstanceDefinitionNotFound() {
        when(substanceEntityService.flexLookup(testSubstanceId))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.makeEmaSmsSubstanceDefinition(testSubstanceId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(emaSmsSubstanceDefinitionFhirMapper, never())
                .generateEmaSmsSubstanceDefinitionFromSubstance(any());
    }

    @Test
    @DisplayName("SubstanceDefinition endpoint returns 500 on mapper exception")
    public void testMakeEmaSmsSubstanceDefinitionInternalError() {
        when(substanceEntityService.flexLookup(testSubstanceId))
                .thenReturn(Optional.of(testSubstance));
        when(emaSmsSubstanceDefinitionFhirMapper.generateEmaSmsSubstanceDefinitionFromSubstance(testSubstance))
                .thenThrow(new IllegalStateException("Mapper error"));

        ResponseEntity<?> response = controller.makeEmaSmsSubstanceDefinition(testSubstanceId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        String responseBody = (String) response.getBody();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("Internal error generating FHIR resource"));
    }

    @Test
    @DisplayName("Controller uses service lookup id exactly once")
    public void testFlexLookupCalledWithCorrectId() {
        when(substanceEntityService.flexLookup(testSubstanceId))
                .thenReturn(Optional.of(testSubstance));

        controller.makeSimpleEmaSmsRecord(testSubstanceId);

        verify(substanceEntityService, times(1)).flexLookup(testSubstanceId);
    }

    private static void setField(Object target, String fieldName, Object value) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                java.lang.reflect.Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalArgumentException("Could not find field: " + fieldName);
    }
}

