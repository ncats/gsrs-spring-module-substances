package example.substance.processor;

import example.substance.support.TestTransactionManagers;
import gsrs.module.substance.repository.CodeRepository;
import gsrs.module.substance.services.CodeEntityService;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.CodeSequentialGenerator;
import ix.ginas.utils.LegacyCodeSequentialGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniqueCodeGeneratorDatabaseTest {

    @Mock
    private CodeRepository codeRepository;

    @Mock
    private CodeEntityService codeEntityService;

    private CodeSequentialGenerator generator;

    @BeforeEach
    void setUp() {
        generator = newGenerator(6, "AB", 9999L, true, "BDNUM");
    }

    @Test
    void constructorNormalizesLenWhenNegative() {
        CodeSequentialGenerator local = newGenerator(-5, null, 10L, true, "MYCS");
        assertEquals(2, local.getLen());
    }

    @Test
    void constructorRejectsLenShorterThanMaxAndSuffix() {
        Exception ex = assertThrows(Exception.class,
                () -> newGenerator(2, "XYZ", 10L, true, "MYCS"));
        assertTrue(ex.getMessage().contains("The len value should be greater than or equal"));
    }

    @Test
    void constructorDerivesMaxWhenNull() {
        CodeSequentialGenerator local = new CodeSequentialGenerator(
                "test", 5, "ZZ", true, null, "MYCS", null);
        assertEquals(999L, local.getMax());
        assertEquals("test", local.getName());
    }

    @ParameterizedTest
    @CsvSource({
            "3,4,true",
            "4,4,true",
            "5,4,false",
            "-1,4,false"
    })
    void checkNextNumberWithinRange(Long next, Long max, boolean expected) {
        assertEquals(expected, generator.checkNextNumberWithinRange(next, max));
    }

    @Test
    void checkNextNumberWithinRangeRejectsNulls() {
        NullPointerException nextNull = assertThrows(NullPointerException.class,
                () -> generator.checkNextNumberWithinRange(null, 4L));
        assertTrue(nextNull.getMessage().contains("nextNumber can not be null"));

        NullPointerException maxNull = assertThrows(NullPointerException.class,
                () -> generator.checkNextNumberWithinRange(1L, null));
        assertTrue(maxNull.getMessage().contains("maxNumber can not be null"));
    }

    @Test
    void checkCodeIdLengthCoversValidAndInvalidValues() {
        assertTrue(generator.checkCodeIdLength("0001AB"));
        assertFalse(generator.checkCodeIdLength("123456789AB"));
    }

    @Test
    void checkCodeIdLengthRejectsNullInputsAndNullMax() {
        NullPointerException codeNull = assertThrows(NullPointerException.class,
                () -> generator.checkCodeIdLength(null));
        assertTrue(codeNull.getMessage().contains("codeId can not be null"));

        generator.setMax(null);
        NullPointerException maxNull = assertThrows(NullPointerException.class,
                () -> generator.checkCodeIdLength("0001AB"));
        assertTrue(maxNull.getMessage().contains("max number can not be null"));
    }

    @Test
    void getNextNumberFallsBackToOneWhenRepositoryFails() {
        when(codeRepository.findMaxCodeByCodeSystemAndCodeLikeAndCodeLessThan(any(), any(), any()))
                .thenThrow(new RuntimeException("DB down"));
        assertEquals(1L, generator.getNextNumber());
    }

    @Test
    void getNextNumberFallsBackToOneWhenRepositoryReturnsNull() {
        when(codeRepository.findMaxCodeByCodeSystemAndCodeLikeAndCodeLessThan("BDNUM", "%AB", 9999L))
                .thenReturn(null);

        assertEquals(1L, generator.getNextNumber());
    }

    @Test
    void getNextNumberUsesRepositoryValue() {
        when(codeRepository.findMaxCodeByCodeSystemAndCodeLikeAndCodeLessThan("BDNUM", "%AB", 9999L))
                .thenReturn(41L);
        assertEquals(42L, generator.getNextNumber());
    }

    @Test
    void getNextNumberThrowsWhenOutOfRange() {
        CodeSequentialGenerator smallRange = newGenerator(3, "", 1L, false, "BDNUM");
        when(codeRepository.findMaxCodeByCodeSystemAndCodeLikeAndCodeLessThan("BDNUM", "%", 1L))
                .thenReturn(1L);

        Exception ex = assertThrows(Exception.class, smallRange::getNextNumber);
        assertTrue(ex.getMessage().contains("out of range"));
    }

    @Test
    void getCodeBuildsPrimaryCode() {
        when(codeRepository.findMaxCodeByCodeSystemAndCodeLikeAndCodeLessThan("BDNUM", "%AB", 9999L))
                .thenReturn(0L);

        Code code = generator.getCode();

        assertNotNull(code);
        assertEquals("BDNUM", code.codeSystem);
        assertEquals("PRIMARY", code.type);
        assertEquals("0001AB", code.code);
        verify(codeRepository).findMaxCodeByCodeSystemAndCodeLikeAndCodeLessThan("BDNUM", "%AB", 9999L);
    }

    @Test
    void generateIdOmitsLeftPaddingWhenConfiguredOff() {
        CodeSequentialGenerator unpadded = newGenerator(4, "ZX", 99L, false, "BDNUM");
        when(codeRepository.findMaxCodeByCodeSystemAndCodeLikeAndCodeLessThan("BDNUM", "%ZX", 99L))
                .thenReturn(7L);

        assertEquals("8ZX", unpadded.generateID());
    }

    @Test
    void getCodeWrapsExceptionWhenIdGenerationFails() {
        CodeSequentialGenerator smallRange = newGenerator(3, "", 1L, false, "BDNUM");
        when(codeRepository.findMaxCodeByCodeSystemAndCodeLikeAndCodeLessThan("BDNUM", "%", 1L))
                .thenReturn(1L);

        Exception ex = assertThrows(Exception.class, smallRange::getCode);
        assertTrue(ex.getMessage().contains("Exception getting code in CodeSequentialGenerator"));
    }

    @Test
    void addCodeDelegatesToCodeEntityService() {
        when(codeRepository.findMaxCodeByCodeSystemAndCodeLikeAndCodeLessThan("BDNUM", "%AB", 9999L))
                .thenReturn(0L);

        Substance substance = new Substance();
        substance.substanceClass = Substance.SubstanceClass.chemical;

        when(codeEntityService.createNewSystemCode(eq(substance), eq("BDNUM"), any(), isNull()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Function<Code, String> idGenerator = invocation.getArgument(2, Function.class);
                    Code c = new Code();
                    c.codeSystem = "BDNUM";
                    c.type = "PRIMARY";
                    c.code = idGenerator.apply(c);
                    return c;
                });

        Code created = generator.addCode(substance);
        assertEquals("BDNUM", created.codeSystem);
        assertEquals("PRIMARY", created.type);
        assertTrue(created.code.endsWith("AB"));
    }

    @Test
    void addCodePassesConfiguredGroupsToCodeEntityService() {
        CodeSequentialGenerator grouped = new CodeSequentialGenerator(
                "test", 6, "AB", true, 9999L, "BDNUM", Collections.singletonMap(0, "protected"));
        grouped.setCodeRepository(codeRepository);
        setField(grouped, "codeEntityService", codeEntityService);
        Substance substance = new Substance();
        substance.substanceClass = Substance.SubstanceClass.chemical;

        when(codeEntityService.createNewSystemCode(eq(substance), eq("BDNUM"), any(), eq("protected")))
                .thenReturn(new Code());

        grouped.addCode(substance);

        verify(codeEntityService).createNewSystemCode(eq(substance), eq("BDNUM"), any(), eq("protected"));
    }

    @Test
    void addCodeWrapsServiceException() {
        Substance substance = new Substance();
        substance.substanceClass = Substance.SubstanceClass.chemical;

        when(codeEntityService.createNewSystemCode(eq(substance), eq("BDNUM"), any(), isNull()))
                .thenThrow(new IllegalStateException("service failure"));

        Exception ex = assertThrows(Exception.class, () -> generator.addCode(substance));
        assertTrue(ex.getMessage().contains("Throwing exception in addCode in CodeSequentialGenerator"));
    }

    @Test
    void legacyGeneratorAllowsClassicConfiguration() {
        LegacyCodeSequentialGenerator legacy = new LegacyCodeSequentialGenerator(
                "BDNUM NAME", 9, "AB", true, "BDNUM");
        setField(legacy, "codeRepository", codeRepository);
        setField(legacy, "codeEntityService", codeEntityService);
        setField(legacy, "transactionManager", TestTransactionManagers.mockTransactionManager());

        when(codeRepository.findCodeByCodeSystemAndCodeLike("BDNUM", "%AB"))
            .thenReturn(Stream.of("0000002AB"));

        assertEquals("0000003AB", legacy.getCode().code);
    }

    @Test
    void legacyGeneratorCachesHighestCodeAndIncrementsInMemory() {
        LegacyCodeSequentialGenerator legacy = newLegacyGenerator();
        when(codeRepository.findCodeByCodeSystemAndCodeLike("BDNUM", "%AB"))
                .thenReturn(Stream.of("0000002AB"));

        assertEquals(3L, legacy.getNextNumber());
        assertEquals(4L, legacy.getNextNumber());
        verify(codeRepository).findCodeByCodeSystemAndCodeLike("BDNUM", "%AB");
    }

    @Test
    void legacyAddCodeDelegatesWithProtectedGroupAndGeneratedId() {
        LegacyCodeSequentialGenerator legacy = newLegacyGenerator();
        Substance substance = new Substance();
        substance.substanceClass = Substance.SubstanceClass.chemical;
        when(codeRepository.findCodeByCodeSystemAndCodeLike("BDNUM", "%AB"))
                .thenReturn(Stream.of("0000002AB"));
        when(codeEntityService.createNewSystemCode(eq(substance), eq("BDNUM"), any(), eq("protected")))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Function<Code, String> idGenerator = invocation.getArgument(2, Function.class);
                    Code code = new Code();
                    code.codeSystem = "BDNUM";
                    code.type = "PRIMARY";
                    code.code = idGenerator.apply(code);
                    return code;
                });

        Code created = legacy.addCode(substance);

        assertEquals("0000003AB", created.code);
        verify(codeEntityService).createNewSystemCode(eq(substance), eq("BDNUM"), any(), eq("protected"));
    }

    @Test
    void legacyAccessorsComparatorAndValidationRoundTrip() {
        TestableLegacyCodeSequentialGenerator legacy = new TestableLegacyCodeSequentialGenerator(
                "BDNUM NAME", 9, "AB", true, "BDNUM");
        legacy.setCodeRepository(codeRepository);
        legacy.setCodeSystem("ALT");
        Comparator<String> comparator = legacy.codeSystemComparator();

        assertEquals(codeRepository, legacy.getCodeRepository());
        assertEquals("ALT", legacy.getCodeSystem());
        assertEquals("BDNUM NAME", legacy.getName());
        assertTrue(legacy.isValidId("anything"));
        assertTrue(comparator.compare("0000002AB", "0000010AB") < 0);
    }

    @Test
    void isValidIdAlwaysReturnsTrue() {
        assertTrue(generator.isValidId("anything"));
    }

    @Test
    void accessorMethodsRoundTrip() {
        assertNotNull(generator.getCodeRepository());
        generator.setCodeSystem("ALT");
        assertEquals("ALT", generator.getCodeSystem());

        generator.setMax(500L);
        assertEquals(500L, generator.getMax());
    }

    private CodeSequentialGenerator newGenerator(int len,
                                                 String suffix,
                                                 Long max,
                                                 boolean padding,
                                                 String codeSystem) {
        CodeSequentialGenerator local = new CodeSequentialGenerator(
                "test", len, suffix, padding, max, codeSystem, null);
        local.setCodeRepository(codeRepository);
        setField(local, "codeEntityService", codeEntityService);
        return local;
    }

    private LegacyCodeSequentialGenerator newLegacyGenerator() {
        LegacyCodeSequentialGenerator legacy = new LegacyCodeSequentialGenerator(
                "BDNUM NAME", 9, "AB", true, "BDNUM");
        setField(legacy, "codeRepository", codeRepository);
        setField(legacy, "codeEntityService", codeEntityService);
        setField(legacy, "transactionManager", TestTransactionManagers.mockTransactionManager());
        return legacy;
    }

    private static void setField(Object target, String fieldName, Object value) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
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

    private static class TestableLegacyCodeSequentialGenerator extends LegacyCodeSequentialGenerator {
        private TestableLegacyCodeSequentialGenerator(String name,
                                                     int len,
                                                     String suffix,
                                                     boolean padding,
                                                     String codeSystem) {
            super(name, len, suffix, padding, codeSystem);
        }

        private Comparator<String> codeSystemComparator() {
            return getCodeSystemComparator();
        }
    }
}
