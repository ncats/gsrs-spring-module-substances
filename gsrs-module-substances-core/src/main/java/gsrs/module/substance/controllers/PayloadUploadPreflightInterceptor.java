package gsrs.module.substance.controllers;

import gsrs.module.substance.services.BulkUploadPreflight;
import gsrs.payload.PayloadController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Optional;

/**
 * Also protects the shared payload controller (provided by GSRS core) and legacy upload aliases.
 */
@Slf4j
public class PayloadUploadPreflightInterceptor implements HandlerInterceptor {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private final BulkUploadPreflight preflight;

    public PayloadUploadPreflightInterceptor(BulkUploadPreflight preflight) {
        this.preflight = preflight;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod method)
                || !(request instanceof MultipartHttpServletRequest multipart)
                || !"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        boolean payloadUpload = PayloadController.class.isAssignableFrom(method.getBeanType())
                && "handleFileUpload".equals(method.getMethod().getName());
        boolean legacyUpload = LegacyGinasAppController.class.isAssignableFrom(method.getBeanType())
                && "uploadPayload".equals(method.getMethod().getName());
        if (!payloadUpload && !legacyUpload) {
            return true;
        }
        var file = multipart.getFile("file-name");
        if (file == null || file.isEmpty()) {
            return true; // Leave missing-file handling to the controller and argument resolver.
        }
        Optional<String> rejection = preflight.rejectionFor(file.getSize());
        if (rejection.isEmpty()) {
            return true;
        }
        log.warn("Payload upload rejected: {}", rejection.get());
        response.setStatus(413);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(MAPPER.writeValueAsString(Map.of("message", rejection.get())));
        return false;
    }
}
