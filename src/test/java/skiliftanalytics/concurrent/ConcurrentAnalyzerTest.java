package skiliftanalytics.concurrent;

import skiliftanalytics.sequential.Record;
import skiliftanalytics.sequential.Analyzer;
import skiliftanalytics.sequential.ResortProcessor;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ConcurrentAnalyzer}.
 *
 * These tests cover:
 * - aggregation correctness on a small synthetic dataset;
 * - behavior on empty input;
 * - the "top-10 per hour" trimming logic.
 */
public class ConcurrentAnalyzerTest {

    /**
     * Helper to convert [skierId, totalVertical] / [liftId, rides] 2D arrays into a Map.
     */
    private static Map<String, Integer> toMap(Object[][] rows) {
        Map<String, Integer> result = new HashMap<>();
        for (Object[] row : rows) {
            String key = (String) row[0];
            Integer value = (Integer) row[1];
            result.put(key, value);
        }
        return result;
    }

    /**
     * Basic happy-path test: verifies that the concurrent pipeline produces
     * the expected skier verticals, lift ride counts and hourly stats on a
     * small synthetic dataset.
     */
    @Test
    public void testProcessAggregatesCorrectly() throws InterruptedException {
        ArrayList<Record> records = new ArrayList<>();

        // hour "0"
        records.add(new Record("resortA", "1", "1", "1",  "0"));  // s1 +200
        records.add(new Record("resortA", "1", "1", "11", "0"));  // s1 +300  => 500

        // hour "1"
        records.add(new Record("resortA", "1", "2", "1",  "1"));  // s2 +200
        records.add(new Record("resortA", "1", "2", "21", "1"));  // s2 +400  => 600

        // hour "0"
        records.add(new Record("resortA", "1", "3", "31", "0"));  // s3 +500

        ConcurrentAnalyzer analyzer = new ConcurrentAnalyzer(2, 2, 2);
        analyzer.process(records);

        // --- skier CSV: totals + sorting (2 > 1,3) ---
        Object[][] skierCsv = analyzer.getSkierCsv();
        Map<String, Integer> skierMap = toMap(skierCsv);

        assertEquals(500, skierMap.get("1"));
        assertEquals(600, skierMap.get("2"));
        assertEquals(500, skierMap.get("3"));

        // highest vertical should be skier "2"
        assertEquals("2", skierCsv[0][0]);

        // --- lift CSV: total rides per lift ---
        Object[][] liftCsv = analyzer.getLiftCsv();
        Map<String, Integer> liftMap = toMap(liftCsv);

        assertEquals(2, liftMap.get("1"));   // used twice (s1 + s2)
        assertEquals(1, liftMap.get("11"));  // once
        assertEquals(1, liftMap.get("21"));  // once
        assertEquals(1, liftMap.get("31"));  // once

        // --- hours CSV: rebuild (hour, liftId) -> rides map and verify ---
        Object[][] hoursCsv = analyzer.getHoursCsv();
        Map<String, Map<String, Integer>> byHour = new HashMap<>();
        for (Object[] row : hoursCsv) {
            String hour = (String) row[0];
            String liftId = (String) row[1];
            Integer rides = (Integer) row[2];
            byHour.computeIfAbsent(hour, h -> new HashMap<>()).put(liftId, rides);
        }

        // hour "0": lifts 1, 11, 31
        Map<String, Integer> h0 = byHour.get("0");
        assertEquals(1, h0.get("1"));
        assertEquals(1, h0.get("11"));
        assertEquals(1, h0.get("31"));

        // hour "1": lifts 1, 21
        Map<String, Integer> h1 = byHour.get("1");
        assertEquals(1, h1.get("1"));
        assertEquals(1, h1.get("21"));
    }

    /**
     * Verifies that processing an empty input list completes without error
     * and produces empty CSV datasets.
     */
    @Test
    public void testProcessWithEmptyInput() throws InterruptedException {
        ArrayList<Record> empty = new ArrayList<>();
        ConcurrentAnalyzer analyzer = new ConcurrentAnalyzer(1, 1, 1);

        analyzer.process(empty);

        assertEquals(0, analyzer.getSkierCsv().length);
        assertEquals(0, analyzer.getLiftCsv().length);
        assertEquals(0, analyzer.getHoursCsv().length);
    }

    /**
     * Verifies that {@link ConcurrentAnalyzer#getHoursCsv()} trims each hour
     * to at most 10 lifts when more than 10 distinct lifts exist in a single hour.
     */
    @Test
    public void testHoursCsvTopTenLimit() throws InterruptedException {
        ArrayList<Record> records = new ArrayList<>();

        // For hour "0", create 11 different lifts, with lift "10" having 2 rides.
        for (int i = 1; i <= 11; i++) {
            String liftId = Integer.toString(i);
            records.add(new Record("resortA", "1", "1", liftId, "0"));
        }
        // extra ride on lift "10" so it is clearly the busiest
        records.add(new Record("resortA", "1", "1", "10", "0"));

        ConcurrentAnalyzer analyzer = new ConcurrentAnalyzer(2, 2, 2);
        analyzer.process(records);

        Object[][] hoursCsv = analyzer.getHoursCsv();

        // Collect all rows for hour "0"
        List<Object[]> hour0Rows = new ArrayList<>();
        for (Object[] row : hoursCsv) {
            if ("0".equals(row[0])) {
                hour0Rows.add(row);
            }
        }

        // Must be trimmed to at most 10 lifts.
        assertEquals(10, hour0Rows.size());

        // And the busiest lift "10" must be included with ride count 2.
        boolean hasLift10 = false;
        for (Object[] row : hour0Rows) {
            String liftId = (String) row[1];
            int rides = (Integer) row[2];
            if ("10".equals(liftId)) {
                hasLift10 = true;
                assertEquals(2, rides);
            }
        }
        assertTrue(hasLift10, "Expected hour 0 top-10 to include lift 10");
    }

    @Test
    public void testInputBeyondQueueCapacityProducesCompleteResults() {
        Record ride = new Record("resortA", "1", "skier", "1", "0");
        ArrayList<Record> records = new ArrayList<>(Collections.nCopies(20_000, ride));
        ConcurrentAnalyzer analyzer = new ConcurrentAnalyzer(1, 1, 1);

        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> analyzer.process(records));
        assertArrayEquals(new Object[][]{{"skier", 4_000_000}}, analyzer.getSkierCsv());
        assertArrayEquals(new Object[][]{{"1", 20_000}}, analyzer.getLiftCsv());
        assertArrayEquals(new Object[][]{{"0", "1", 20_000}}, analyzer.getHoursCsv());
    }

    @Test
    public void testWorkerFailureDoesNotDeadlockOrExposePartialResults() {
        Record valid = new Record("resortA", "1", "skier", "1", "0");
        ArrayList<Record> records = new ArrayList<>(Collections.nCopies(20_000, valid));
        records.set(0, new Record("resortA", "1", "skier", "41", "0"));
        ConcurrentAnalyzer analyzer = new ConcurrentAnalyzer(1, 1, 1);

        IllegalStateException failure = assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> assertThrows(IllegalStateException.class, () -> analyzer.process(records)));
        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
        assertThrows(IllegalStateException.class, analyzer::getSkierCsv);
        assertThrows(IllegalStateException.class, analyzer::getLiftCsv);
    }

    @Test
    public void testInterruptStopsProducerAndWorkers() throws Exception {
        CountDownLatch enteredGetter = new CountDownLatch(1);
        CountDownLatch releaseGetter = new CountDownLatch(1);
        Record waitingRide = new Record("resortA", "1", "skier", "1", "0") {
            @Override public String getSkierId() {
                enteredGetter.countDown();
                try {
                    releaseGetter.await();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                return super.getSkierId();
            }
        };
        ConcurrentAnalyzer analyzer = new ConcurrentAnalyzer(1, 1, 1);
        AtomicReference<Throwable> outcome = new AtomicReference<>();
        Thread producer = new Thread(() -> {
            try {
                analyzer.process(new ArrayList<>(List.of(waitingRide)));
            } catch (Throwable ex) {
                outcome.set(ex);
            }
        });

        producer.start();
        try {
            assertTrue(enteredGetter.await(2, TimeUnit.SECONDS));
            producer.interrupt();
            producer.join(5_000);
            assertFalse(producer.isAlive(), "Interrupted analysis did not exit");
            assertInstanceOf(InterruptedException.class, outcome.get());
            assertThrows(IllegalStateException.class, analyzer::getHoursCsv);
        } finally {
            releaseGetter.countDown();
            producer.interrupt();
            producer.join(5_000);
        }
    }

    @Test
    public void testTiesTopTenAndModesAgree() throws InterruptedException {
        ArrayList<Record> records = new ArrayList<>();
        for (int lift = 1; lift <= 11; lift++) {
            records.add(new Record("resortA", "1", "skier" + lift,
                    Integer.toString(lift), "0"));
        }

        Analyzer sequential = new Analyzer(ResortProcessor.createResorts(records));
        ConcurrentAnalyzer concurrent = new ConcurrentAnalyzer(2, 2, 2);
        concurrent.process(records);

        assertArrayEquals(sequential.getSkierCsv(), concurrent.getSkierCsv());
        assertArrayEquals(sequential.getLiftCsv(), concurrent.getLiftCsv());
        assertArrayEquals(sequential.getHoursCsv(), concurrent.getHoursCsv());
        assertEquals(10, concurrent.getHoursCsv().length);
        assertArrayEquals(new Object[]{"0", "1", 1}, concurrent.getHoursCsv()[0]);
        assertArrayEquals(new Object[]{"0", "8", 1}, concurrent.getHoursCsv()[9]);
    }
}
