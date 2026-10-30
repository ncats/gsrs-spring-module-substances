package gsrs.module.substance.autoconfigure;

import ix.core.chem.StructureProcessorConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.ContextRefreshedEvent;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MolwitchLoaderTest {

    @Mock
    private StructureProcessorConfiguration processorConfiguration;

    @Test
    void onApplicationEventSkipsMolwitchWhenFeatureDisabled() throws Exception {
        MolwitchLoader loader = new MolwitchLoader();
        inject(loader, "processorConfiguration", processorConfiguration);
        inject(loader, "enabled", false);
        ContextRefreshedEvent event = org.mockito.Mockito.mock(ContextRefreshedEvent.class);

        assertDoesNotThrow(() -> loader.onApplicationEvent(event));
        verify(processorConfiguration, never()).getMolwitch();
    }

    @Test
    void onApplicationEventInvokesMolwitchWhenFeatureEnabled() throws Exception {
        MolwitchLoader loader = new MolwitchLoader();
        inject(loader, "processorConfiguration", processorConfiguration);
        inject(loader, "enabled", true);
        Map<String, Object> params = new HashMap<>();
        params.put("factory", "default");
        when(processorConfiguration.getMolwitch()).thenReturn(params);

        ContextRefreshedEvent event = org.mockito.Mockito.mock(ContextRefreshedEvent.class);

        assertDoesNotThrow(() -> loader.onApplicationEvent(event));
        verify(processorConfiguration, times(1)).getMolwitch();
    }

    @Test
    void onApplicationEventHandlesMultipleInvocationsFast() throws Exception {
        MolwitchLoader loader = new MolwitchLoader();
        inject(loader, "processorConfiguration", processorConfiguration);
        inject(loader, "enabled", true);
        when(processorConfiguration.getMolwitch()).thenReturn(Collections.singletonMap("k", "v"));
        ContextRefreshedEvent event = org.mockito.Mockito.mock(ContextRefreshedEvent.class);

        assertDoesNotThrow(() -> loader.onApplicationEvent(event));
        assertDoesNotThrow(() -> loader.onApplicationEvent(event));
        verify(processorConfiguration, times(2)).getMolwitch();
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

