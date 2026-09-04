package ix.core;

import ix.core.stats.Estimate;
import ix.core.stats.Statistics;
import ix.core.util.ModelUtils;
import ix.core.util.SemaphoreCounter;
import ix.ginas.models.v1.DisulfideLink;
import ix.ginas.models.v1.Site;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CoreStatsAndCollectionsTest {

    @Test
    void estimateShouldStoreAndExposeCountAndType() {
        Estimate estimate = new Estimate(42, Estimate.TYPE.APPROXIMATE);

        assertEquals(42, estimate.getCount());
        assertEquals(Estimate.TYPE.APPROXIMATE, estimate.getType());

        estimate.setCount(7);
        estimate.setType(Estimate.TYPE.EXACT);

        assertEquals(7, estimate.getCount());
        assertEquals(Estimate.TYPE.EXACT, estimate.getType());
    }

    @Test
    void statisticsShouldApplyChangesTrackCompletionAndComputeDerivedValues() throws Exception {
        Statistics statistics = new Statistics();
        setLongField(statistics, "start", 1_000L);

        statistics.applyChange(Statistics.CHANGE.ADD_EX_GOOD);
        statistics.applyChange(Statistics.CHANGE.ADD_PR_GOOD);
        statistics.applyChange(Statistics.CHANGE.ADD_PE_GOOD);
        statistics.applyChange(Statistics.CHANGE.ADD_PE_BAD);

        assertEquals(1, statistics.recordsExtractedSuccess.get());
        assertEquals(1, statistics.recordsProcessedSuccess.get());
        assertEquals(1, statistics.recordsPersistedSuccess.get());
        assertEquals(2, statistics.totalFailedAndPersisted());
        assertFalse(statistics._isDone());

        statistics.totalRecords = new Estimate(2, Estimate.TYPE.EXACT);
        assertTrue(statistics._isDone());

        assertEquals(1000.0, statistics.getAverageTimeToPersistMS(2_000L), 0.0001);
        assertTrue(statistics.getEstimatedTimeLeft() >= 0);
        assertTrue(statistics.toString().contains("Extracted: 1 (0 failed)"));
        assertTrue(statistics.toString().contains("Persisted: 1 (1 failed)"));
    }

    @Test
    void statisticsShouldCopyLastChangeAndSupportCancellation() {
        Statistics source = new Statistics();
        Statistics target = new Statistics();

        source.applyChange(Statistics.CHANGE.CANCEL);
        target.applyChange(source);

        assertTrue(source.cancelled);
        assertTrue(source._isDone());
        assertTrue(target.cancelled);
        assertTrue(target._isDone());
        assertTrue(source.isNewer(new Statistics()));
    }

    @Test
    void semaphoreCounterShouldTrackDepthAndRunCleanupInLifoOrder() {
        SemaphoreCounter<String> counter = new SemaphoreCounter<>();
        List<String> calls = new ArrayList<>();

        assertTrue(counter.add("alpha", () -> calls.add("first"), false));
        assertFalse(counter.add("alpha", () -> calls.add("second"), false));
        assertTrue(counter.contains("alpha"));

        assertTrue(counter.remove("alpha"));
        assertEquals(Collections.singletonList("second"), calls);
        assertTrue(counter.remove("alpha"));
        assertEquals(Arrays.asList("second", "first"), calls);
        assertFalse(counter.contains("alpha"));
        assertFalse(counter.remove("alpha"));
    }

    @Test
    void semaphoreCounterShouldSuppressDuplicateCleanupWhenOnlyRunIfNewIsTrue() {
        SemaphoreCounter<String> counter = new SemaphoreCounter<>();
        List<String> calls = new ArrayList<>();

        assertTrue(counter.add("beta", () -> calls.add("first"), true));
        assertFalse(counter.add("beta", () -> calls.add("second"), true));

        assertTrue(counter.removeCompletely("beta"));
        assertEquals(Collections.singletonList("first"), calls);
        assertFalse(counter.contains("beta"));
        assertFalse(counter.removeCompletely("beta"));
    }

    @Test
    void modelUtilsShouldCreateAndParseShorthandNotation() {
        List<Site> sites = Arrays.asList(new Site(2, 4), new Site(2, 5), new Site(2, 7));

        assertEquals("2_4-2_5;2_7", ModelUtils.shorthandNotationFor(sites));
        assertEquals(new Site(2, 4), ModelUtils.parseShorthandLinkage("2_4"));
        assertEquals(Arrays.asList(new Site(1, 2), new Site(1, 4)), ModelUtils.parseShorthandLinkages("1_2;1_4"));
        assertEquals(Arrays.asList(new Site(2, 1), new Site(2, 2), new Site(2, 3)), ModelUtils.parseShorthandRanges("2_1-2_3"));
    }

    @Test
    void modelUtilsShouldExpandRangesAndFallbackToProvidedSubunit() {
        List<Site> sites = ModelUtils.parseShorthandAtSubunit("1_2-1_4,5", "9");

        assertEquals(Arrays.asList(
                new Site(1, 2),
                new Site(1, 3),
                new Site(1, 4),
                new Site(9, 5)
        ), sites);
    }

    @Test
    void modelUtilsShouldRejectInvalidRanges() {
        assertThrows(IllegalStateException.class, () -> ModelUtils.parseShorthandAtSubunit("1_2-2_4", "1"));
        assertThrows(IllegalStateException.class, () -> ModelUtils.parseShorthandAtSubunit("1_4-1_2", "1"));
    }

    @Test
    void modelUtilsShouldRenderDisulfideLinkShorthandWithArrows() {
        DisulfideLink link = new DisulfideLink();
        link.setSitesShorthand("1_2;1_4");

        assertEquals("1_2->1_4", link.getLinksShorthand());
        assertEquals("1_2->1_4", ModelUtils.shorthandNotationForLinks(Collections.singletonList(link)));
    }

    private static void setLongField(Object target, String fieldName, long value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setLong(target, value);
    }
}

