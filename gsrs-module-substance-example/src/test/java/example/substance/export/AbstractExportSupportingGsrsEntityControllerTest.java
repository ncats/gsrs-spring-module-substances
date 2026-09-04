package example.substance.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.AbstractExportSupportingGsrsEntityController;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.service.GsrsEntityService;
import gsrs.repository.TextRepository;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.models.Text;
import ix.core.search.SearchResult;
import ix.ginas.exporters.SpecificExporterSettings;
import ix.ginas.exporters.ExporterSpecificExportSettings;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Answers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class AbstractExportSupportingGsrsEntityControllerTest {

    private final Map<String, Text> savedExporterKeys = new HashMap<>();
    private final AtomicLong nextTextId = new AtomicLong(1L);

    private final TextRepository textRepository = mock(TextRepository.class, invocation -> {
        String methodName = invocation.getMethod().getName();
        Object[] args = invocation.getArguments();
        if ("findByLabel".equals(methodName)) {
            String label = (String) args[0];
            Text saved = savedExporterKeys.get(label);
            return saved != null ? Collections.singletonList(saved) : Collections.emptyList();
        }
        if (methodName.startsWith("save") || methodName.startsWith("insert") || methodName.startsWith("persist") || methodName.startsWith("store")) {
            if (args != null && args.length > 0 && args[0] instanceof Text text && text.label != null) {
                Text saved = new Text(text.label);
                copyTextValue(saved, text.getValue());
                saved.id = nextTextId.getAndIncrement();
                savedExporterKeys.put(text.label, saved);
                return saved;
            }
            return args != null && args.length > 0 ? args[0] : null;
        }
        if ("retrieveById".equals(methodName) || "getOne".equals(methodName) || "findById".equals(methodName)) {
            return Optional.empty();
        }
        Class<?> returnType = invocation.getMethod().getReturnType();
        if (returnType.equals(boolean.class)) return false;
        if (returnType.equals(byte.class)) return (byte) 0;
        if (returnType.equals(short.class)) return (short) 0;
        if (returnType.equals(int.class)) return 0;
        if (returnType.equals(long.class)) return 0L;
        if (returnType.equals(float.class)) return 0f;
        if (returnType.equals(double.class)) return 0d;
        if (returnType.equals(char.class)) return '\0';
        return Answers.RETURNS_DEFAULTS.answer(invocation);
    });

    private AbstractExportSupportingGsrsEntityController controller;

    @BeforeEach
    public void setup() {
        savedExporterKeys.clear();
        controller = createController();
        injectField(controller, "textRepository", textRepository);

        Class<?> exportConfigType = findGsrsExportConfigurationFieldType();
        if (exportConfigType != null) {
            Object exportConfigMock = mock(exportConfigType, invocation -> {
                if ("getHardcodedDefaultExportPresets".equals(invocation.getMethod().getName())) {
                    Class<?> returnType = invocation.getMethod().getReturnType();
                    if (Map.class.isAssignableFrom(returnType)) {
                        return Collections.emptyMap();
                    }
                    if (List.class.isAssignableFrom(returnType)) {
                        return Collections.emptyList();
                    }
                    if (Set.class.isAssignableFrom(returnType)) {
                        return Collections.emptySet();
                    }
                    if (Optional.class.isAssignableFrom(returnType)) {
                        return Optional.empty();
                    }
                    if (returnType.isArray()) {
                        return java.lang.reflect.Array.newInstance(returnType.getComponentType(), 0);
                    }
                }
                return Answers.RETURNS_DEFAULTS.answer(invocation);
            });
            setFieldIfPresent("gsrsExportConfiguration", exportConfigMock);
        }

        AbstractPlatformTransactionManager noOpTransactionManager = new AbstractPlatformTransactionManager() {
            @Override
            protected @NonNull Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(@NonNull Object transaction, @NonNull TransactionDefinition definition) {
                // no-op
            }

            @Override
            protected void doCommit(@NonNull DefaultTransactionStatus status) {
                // no-op
            }

            @Override
            protected void doRollback(@NonNull DefaultTransactionStatus status) {
                // no-op
            }
        };
        setFieldIfPresent("transactionManager", noOpTransactionManager);
        setFieldIfPresent("platformTransactionManager", noOpTransactionManager);
        setFieldIfPresent("transactionTemplate", new TransactionTemplate(noOpTransactionManager));

        setupStaticContextAccessor();
    }

    private void setupStaticContextAccessor() {
        try {
            Class<?> processorType = Class.forName("gsrs.controller.hateoas.GsrsUnwrappedEntityModelProcessor");
            Object processor = mock(processorType, invocation -> {
                if ("process".equals(invocation.getMethod().getName()) && invocation.getArguments().length > 0) {
                    return invocation.getArgument(0);
                }
                return Answers.RETURNS_DEFAULTS.answer(invocation);
            });

            org.springframework.context.ApplicationContext applicationContext = mock(org.springframework.context.ApplicationContext.class, invocation -> {
                if ("getBean".equals(invocation.getMethod().getName())
                        && invocation.getArguments().length == 1
                        && invocation.getArgument(0) instanceof Class<?> requestedType
                        && requestedType.equals(processorType)) {
                    return processor;
                }
                return Answers.RETURNS_DEFAULTS.answer(invocation);
            });

            Field instanceField = StaticContextAccessor.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            Object instance = instanceField.get(null);
            if (instance == null) {
                instance = new StaticContextAccessor();
                instanceField.set(null, instance);
            }
            injectField(instance, "applicationContext", applicationContext);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to initialize StaticContextAccessor for export controller test", e);
        }
    }

    private void setFieldIfPresent(String fieldName, Object value) {
        try {
            injectField(controller, fieldName, value);
        } catch (IllegalArgumentException ignored) {
            // field not present in this controller version
        }
    }

    private void injectField(Object target, String fieldName, Object value) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unable to set field '" + fieldName + "' on " + target.getClass().getName(), e);
            }
        }
        throw new IllegalArgumentException("Field '" + fieldName + "' not found on " + target.getClass().getName());
    }

    private void copyTextValue(Text target, String value) {
        try {
            try {
                target.getClass().getMethod("setValue", String.class).invoke(target, value);
                return;
            } catch (NoSuchMethodException ignored) {
                // fall through to field access
            }
            Field valueField = target.getClass().getDeclaredField("value");
            valueField.setAccessible(true);
            valueField.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to copy Text value in test stub", e);
        }
    }

    private AbstractExportSupportingGsrsEntityController createController() {
        return new AbstractExportSupportingGsrsEntityController() {
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
                GsrsEntityService entityService = mock(GsrsEntityService.class);
                when(entityService.getEntityClass()).thenReturn(Substance.class);
                return entityService;
            }
        };
    }

    private Class<?> findGsrsExportConfigurationFieldType() {
        Class<?> current = controller.getClass();
        while (current != null) {
            try {
                return current.getDeclaredField("gsrsExportConfiguration").getType();
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    @Test
    public void testDoesExporterKeyExist() throws JsonProcessingException {
        //create 2 configurations with identical keys
        String madeUpKey = "Made Up Key" + UUID.randomUUID();
        String config1 = createBogusConfig(madeUpKey);
        Map<String, String> params = new HashMap<>();
        ResponseEntity response = controller.handleExportConfigSave(config1, params);
        System.out.println(response.getBody());
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        ResponseEntity response2 = controller.handleExportConfigSave(config1, params);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response2.getStatusCode());
    }

    @Test
    public void testGetAll() {
        ResponseEntity response = controller.handleExportConfigsFetch(new HashMap<String, String>());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertFalse(response.toString().isEmpty());
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
