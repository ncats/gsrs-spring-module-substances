package gsrs.module.substance.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonStringRequestBodyHttpMessageConverterTest {

    @Test
    void readsApplicationJsonAsRawString() throws Exception {
        GsrsSubstanceModuleAutoConfiguration.JsonStringRequestBodyHttpMessageConverter converter =
                new GsrsSubstanceModuleAutoConfiguration.JsonStringRequestBodyHttpMessageConverter();
        String json = "{\"stagingAreaRecords\":[{\"id\":\"1b0a8ec0-bb2d-4a0e-bcf1-dc29dd912c7c\"}],\"processingActions\":[{\"parameters\":{},\"processingActionName\":\"create\"}]}";

        assertTrue(converter.canRead(String.class, MediaType.APPLICATION_JSON));
        assertFalse(converter.canWrite(String.class, MediaType.APPLICATION_JSON));
        assertEquals(json, converter.read(String.class, new StringInputMessage(json, MediaType.APPLICATION_JSON)));
    }

    @Test
    void registersJsonStringConverterBeforeDefaultConverters() {
        HttpMessageConverters.ServerBuilder builder = HttpMessageConverters.forServer().registerDefaults();

        new GsrsSubstanceModuleAutoConfiguration()
                .gsrsJsonStringRequestBodyWebMvcConfigurer()
                .configureMessageConverters(builder);

        HttpMessageConverter<?> firstConverter = builder.build().iterator().next();
        assertTrue(firstConverter instanceof GsrsSubstanceModuleAutoConfiguration.JsonStringRequestBodyHttpMessageConverter);
    }

    private static final class StringInputMessage implements HttpInputMessage {
        private final byte[] body;
        private final HttpHeaders headers = new HttpHeaders();

        private StringInputMessage(String body, MediaType mediaType) {
            this.body = body.getBytes(StandardCharsets.UTF_8);
            this.headers.setContentType(mediaType);
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}