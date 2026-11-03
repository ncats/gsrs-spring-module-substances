package example.imports;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.dataexchange.SubstanceStagingAreaEntityService;
import gsrs.springUtils.AutowireHelper;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationResponse;
import ix.core.validator.Validator;
import ix.core.validator.ValidatorCallback;
import ix.core.validator.ValidatorCategory;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.ValidatorFactory;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategy;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactory;
import ix.ginas.utils.validation.validators.AutoGenerateUuidIfNeeded;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubstanceStagingAreaUuidTest {
    private SubstanceStagingAreaEntityService service;
    private ValidatorFactory factory;
    private Validator<Substance> validator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new SubstanceStagingAreaEntityService();
        factory = mock(ValidatorFactory.class);
        validator = mock(Validator.class);
        ReflectionTestUtils.setField(service, "validatorFactory", CachedSupplier.of(() -> factory));
        ReflectionTestUtils.setField(service, "initializedValidator", true);
        when(factory.createValidatorFor(any(Substance.class), isNull(),
                eq(ValidatorConfig.METHOD_TYPE.BATCH), any(ValidatorCategory.class))).thenReturn(validator);
        GsrsProcessingStrategyFactory strategies = mock(GsrsProcessingStrategyFactory.class);
        when(strategies.createNewStrategy(anyString())).thenReturn(mock(GsrsProcessingStrategy.class));
        ReflectionTestUtils.setField(service, "gsrsProcessingStrategyFactory", strategies);
    }

    @Test
    void successfulValidationPreservesIncomingUuidAndHidesTemporaryGenerationMessage() {
        Substance substance = new Substance();
        UUID incomingUuid = UUID.randomUUID();
        substance.uuid = incomingUuid;
        generateTemporaryUuid(substance, null);

        try (MockedStatic<AutowireHelper> helpers = mockStatic(AutowireHelper.class)) {
            AutowireHelper helper = mock(AutowireHelper.class);
            helpers.when(AutowireHelper::getInstance).thenReturn(helper);
            doAnswer(invocation -> {
                assertEquals(incomingUuid, substance.uuid);
                return null;
            }).when(helper).autowire(any());

            ValidationResponse<Substance> response = service.validate(substance);

            assertSame(substance, response.getNewObject());
            assertEquals(incomingUuid, response.getNewObject().uuid);
            assertTrue(response.getValidationMessages().stream().noneMatch(message ->
                    message.getMessage().startsWith("Substance has no UUID, will generate uuid")));
        }
    }

    @Test
    void failureAfterUuidGenerationRestoresIncomingUuidAndPropagatesFailure() {
        Substance substance = new Substance();
        UUID incomingUuid = UUID.randomUUID();
        substance.uuid = incomingUuid;
        IllegalStateException failure = new IllegalStateException("Validation failed");
        generateTemporaryUuid(substance, failure);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.validate(substance)));
        assertEquals(incomingUuid, substance.uuid);
    }

    @Test
    void validatorCreationFailureRestoresIncomingUuidAndPropagatesFailure() {
        Substance substance = new Substance();
        UUID incomingUuid = UUID.randomUUID();
        substance.uuid = incomingUuid;
        IllegalStateException failure = new IllegalStateException("Validator creation failed");
        when(factory.createValidatorFor(any(Substance.class), isNull(),
                eq(ValidatorConfig.METHOD_TYPE.BATCH), any(ValidatorCategory.class))).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.validate(substance)));
        assertEquals(incomingUuid, substance.uuid);
    }

    @Test
    void validationWithoutIncomingUuidRetainsExistingNullUuidBehavior() {
        Substance substance = new Substance();
        generateTemporaryUuid(substance, null);

        try (MockedStatic<AutowireHelper> helpers = mockStatic(AutowireHelper.class)) {
            helpers.when(AutowireHelper::getInstance).thenReturn(mock(AutowireHelper.class));
            ValidationResponse<Substance> response = service.validate(substance);

            assertNull(response.getNewObject().uuid);
            assertTrue(response.getValidationMessages().stream().anyMatch(message ->
                    message.getMessage().startsWith("Substance has no UUID, will generate uuid")));
        }
    }

    private void generateTemporaryUuid(Substance substance, RuntimeException failure) {
        doAnswer(invocation -> {
            assertNull(substance.uuid);
            ValidatorCallback callback = invocation.getArgument(2);
            new AutoGenerateUuidIfNeeded().validate(substance, null, callback);
            assertNotNull(substance.uuid);
            if (failure != null) {
                throw failure;
            }
            return null;
        }).when(validator).validate(same(substance), isNull(), any(ValidatorCallback.class));
    }
}
