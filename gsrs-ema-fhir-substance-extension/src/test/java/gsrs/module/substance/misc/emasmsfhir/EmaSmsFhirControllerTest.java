package gsrs.module.substance.misc.emasmsfhir;
import gsrs.module.substance.SubstanceEntityService;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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
        EmaSmsFhirTestData.setField(controller, "substanceEntityService", substanceEntityService);
        EmaSmsFhirTestData.setField(controller, "emaSmsSimpleRecordFhirMapper", new EmaSmsSimpleRecordFhirMapper());
        EmaSmsFhirTestData.setField(controller, "emaSmsSubstanceDefinitionFhirMapper", emaSmsSubstanceDefinitionFhirMapper);

        testSubstanceId = "306d24b9-a6b8-4091-8024-02f9ec24b705";
        testSubstance = EmaSmsFhirTestData.chemicalSubstanceWithDisplayName("Sodium Chloride");
        testSubstance.setUuid(UUID.fromString(testSubstanceId));
    }

    @Test
    @DisplayName("Simple record endpoint returns JSON when substance is found")
    public void testMakeSimpleEmaSmsRecordSuccess() {
        when(substanceEntityService.flexLookup(testSubstanceId))
                .thenReturn(Optional.of(testSubstance));

        MockHttpServletResponse response = new MockHttpServletResponse();
        try {
            controller.makeSimpleEmaSmsRecord(testSubstanceId, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals(StandardCharsets.UTF_8.name(), response.getCharacterEncoding());
        try {
            assertNotNull(
                    response.getContentAsString()
            );
            assertTrue(
                    response.getContentAsString().contains("resourceType")
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

// For, Getaneh should this be used?
//        verify(emaSmsSimpleRecordFhirMapper, times(1))
//                .generateEmaSmsSimpleRecordFromSubstance(testSubstance);
    }

    @Test
    @DisplayName("Simple record endpoint returns 404 when not found")
    public void testMakeSimpleEmaSmsRecordNotFound() {
        when(substanceEntityService.flexLookup(testSubstanceId))
                .thenReturn(Optional.empty());
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            controller.makeSimpleEmaSmsRecord(testSubstanceId, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());

// For, Getaneh should this be used?
//        verify(emaSmsSimpleRecordFhirMapper, never())
//                .generateEmaSmsSimpleRecordFromSubstance(any());
    }

    @Test
    @DisplayName("Simple record endpoint returns 500 on service exception")
    public void testMakeSimpleEmaSmsRecordInternalError() {
        when(substanceEntityService.flexLookup(testSubstanceId))
                .thenThrow(new RuntimeException("Database connection error"));

        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            controller.makeSimpleEmaSmsRecord(testSubstanceId, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals(StandardCharsets.UTF_8.name(), response.getCharacterEncoding());
        try {
            assertEquals(
                    "Internal error generating FHIR resource.",
                    response.getContentAsString()
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

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

        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            controller.makeEmaSmsSubstanceDefinition(testSubstanceId, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals(StandardCharsets.UTF_8.name(), response.getCharacterEncoding());
        try {
            assertNotNull(
                response.getContentAsString()
            );
            assertTrue(
                response.getContentAsString().contains("SubstanceDefinition")
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        verify(emaSmsSubstanceDefinitionFhirMapper, times(1))
                .generateEmaSmsSubstanceDefinitionFromSubstance(testSubstance);
    }

    @Test
    @DisplayName("SubstanceDefinition endpoint returns 404 when not found")
    public void testMakeEmaSmsSubstanceDefinitionNotFound() {
        when(substanceEntityService.flexLookup(testSubstanceId))
                .thenReturn(Optional.empty());
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            controller.makeEmaSmsSubstanceDefinition(testSubstanceId, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());

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

        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            controller.makeEmaSmsSubstanceDefinition(testSubstanceId, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals(StandardCharsets.UTF_8.name(), response.getCharacterEncoding());
        try {
            assertEquals(
                    "Internal error generating FHIR resource.",
                    response.getContentAsString()
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Controller uses service lookup id exactly once")
    public void testFlexLookupCalledWithCorrectId() {
        when(substanceEntityService.flexLookup(testSubstanceId))
                .thenReturn(Optional.of(testSubstance));

        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            controller.makeSimpleEmaSmsRecord(testSubstanceId, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        verify(substanceEntityService, times(1))
        .flexLookup(testSubstanceId);
    }
}

