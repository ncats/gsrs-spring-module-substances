package gsrs.module.substance.controllers;

import gsrs.module.substance.services.BulkUploadPreflight;
import gsrs.payload.PayloadController;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.web.method.HandlerMethod;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayloadUploadPreflightInterceptorTest {
    @Test
    void sharedPayloadUploadAndLegacyAliasesReturnJson413BeforeCallingTheController() throws Exception {
        for (Object controller : new Object[]{new PayloadController(), new LegacyGinasAppController()}) {
            BulkUploadPreflight preflight = mock(BulkUploadPreflight.class);
            when(preflight.rejectionFor(4)).thenReturn(Optional.of("Database limit \"16MB\"\nRaise packet size"));
            PayloadUploadPreflightInterceptor interceptor = new PayloadUploadPreflightInterceptor(preflight);
            String method = controller instanceof PayloadController ? "handleFileUpload" : "uploadPayload";
            HandlerMethod handler = new HandlerMethod(controller, controller.getClass().getMethod(
                    method, org.springframework.web.multipart.MultipartFile.class, Map.class));
            MockHttpServletResponse response = new MockHttpServletResponse();
            assertFalse(interceptor.preHandle(upload(), response, handler));
            assertEquals(413, response.getStatus());
            assertEquals("application/json", response.getContentType().split(";")[0]);
            assertTrue(response.getContentAsString().contains("\\\"16MB\\\"\\n"));
            verify(preflight).rejectionFor(4);
        }
    }

    @Test
    void allowedUploadsProceedAndUnrelatedHandlersAreUnaffected() throws Exception {
        BulkUploadPreflight preflight = mock(BulkUploadPreflight.class);
        PayloadUploadPreflightInterceptor interceptor = new PayloadUploadPreflightInterceptor(preflight);
        PayloadController controller = new PayloadController();
        HandlerMethod handler = new HandlerMethod(controller, PayloadController.class.getMethod(
                "handleFileUpload", org.springframework.web.multipart.MultipartFile.class, Map.class));
        assertTrue(interceptor.preHandle(upload(), new MockHttpServletResponse(), handler));
        verify(preflight).rejectionFor(4);
        clearInvocations(preflight);
        assertTrue(interceptor.preHandle(upload(), new MockHttpServletResponse(), new Object()));
        MockMultipartHttpServletRequest empty = new MockMultipartHttpServletRequest();
        empty.setMethod("POST");
        assertTrue(interceptor.preHandle(empty, new MockHttpServletResponse(), handler));
        verifyNoInteractions(preflight);
    }

    private static MockMultipartHttpServletRequest upload() {
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.addFile(new MockMultipartFile("file-name", "test.ginas", "application/octet-stream", new byte[4]));
        return request;
    }
}
