package skiliftanalytics.concurrent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicReference;
import skiliftanalytics.sequential.Record;

/**
 * Concurrent analyzer for ski lift data.
 *
 * <p>Consumes records via three internal queues and aggregates:</p>
 * <ul>
 *   <li>total vertical per skier,</li>
 *   <li>total rides per lift,</li>
 *   <li>top-10 busiest lifts per hour.</li>
 * </ul>
 *
 * <p>All shared state is kept in concurrent collections. The public getters
 * return CSV-ready {@code Object[][]} views of the aggregated results.</p>
 */
public class ConcurrentAnalyzer {

    /** Task payload for the "skier vertical" channel. */
    private static final class SkierTask {
        final String skierId;
        final String liftId;
        final boolean poison;
        SkierTask(String skierId, String liftId, boolean poison) {
            this.skierId = skierId; this.liftId = liftId; this.poison = poison;
        }
        static SkierTask poison() { return new SkierTask(null, null, true); }
    }

    /** Task payload for the "lift rides" channel. */
    private static final class LiftTask {
        final String liftId; final boolean poison;
        LiftTask(String liftId, boolean poison) { this.liftId = liftId; this.poison = poison; }
        static LiftTask poison() { return new LiftTask(null, true); }
    }

    /** Task payload for the "hourly top-10" channel. */
    private static final class HourTask {
        final String hour; final String liftId; final boolean poison;
        HourTask(String hour, String liftId, boolean poison) {
            this.hour = hour; this.liftId = liftId; this.poison = poison;
        }
        static HourTask poison() { return new HourTask(null, null, true); }
    }


    private final BlockingQueue<SkierTask> skierQ;
    private final BlockingQueue<LiftTask>  liftQ;
    private final BlockingQueue<HourTask>  hourQ;


    private final ConcurrentHashMap<String, LongAdder> skierVertical = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> liftRides     = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, LongAdder>> hourLiftRides =
            new ConcurrentHashMap<>();


    private final int skierWorkers;
    private final int liftWorkers;
    private final int hourWorkers;
    private boolean started;
    private volatile boolean completed;

    /**
     * Construct an analyzer with the given numbers of consumer threads and bounded queues.
     *
     * <p><b>Requires:</b> {@code skierWorkers >= 1 && liftWorkers >= 1 && hourWorkers >= 1}.<br>
     * <b>Modifies:</b> this.<br>
     * <b>Effects:</b> initializes internal queues and concurrency parameters.
     *
     * @param skierWorkers number of consumers for the skier channel (>= 1)
     * @param liftWorkers  number of consumers for the lift channel (>= 1)
     * @param hourWorkers  number of consumers for the hour channel (>= 1)
     * @throws IllegalArgumentException if any worker count is < 1
     */
    public ConcurrentAnalyzer(int skierWorkers, int liftWorkers, int hourWorkers) {
        if (skierWorkers < 1 || liftWorkers < 1 || hourWorkers < 1) {
            throw new IllegalArgumentException("Each worker count must be at least 1");
        }
        this.skierWorkers = skierWorkers;
        this.liftWorkers  = liftWorkers;
        this.hourWorkers  = hourWorkers;
        // Bounded queues to avoid unbounded memory growth under producer/consumer imbalance.
        int cap = 8192;
        this.skierQ = new ArrayBlockingQueue<>(cap);
        this.liftQ  = new ArrayBlockingQueue<>(cap);
        this.hourQ  = new ArrayBlockingQueue<>(cap);
    }

    /**
     * Run the concurrent pipeline on the given records.
     *
     * @param allRecords all input records; must not be {@code null}
     * @throws InterruptedException if interrupted while putting or waiting
     */
    public synchronized void process(ArrayList<Record> allRecords) throws InterruptedException {
        Objects.requireNonNull(allRecords, "allRecords");
        if (started) {
            throw new IllegalStateException("This analyzer has already been started");
        }
        started = true;
        final int totalWorkers = skierWorkers + liftWorkers + hourWorkers;
        ExecutorService pool = Executors.newFixedThreadPool(totalWorkers);
        AtomicReference<Throwable> workerFailure = new AtomicReference<>();
        List<Future<?>> workers = new ArrayList<>(totalWorkers);
        boolean joined = false;
        try {
            for (int i = 0; i < skierWorkers; i++) workers.add(submitWorker(pool, new SkierWorker(), workerFailure));
            for (int i = 0; i < liftWorkers; i++) workers.add(submitWorker(pool, new LiftWorker(), workerFailure));
            for (int i = 0; i < hourWorkers; i++) workers.add(submitWorker(pool, new HourWorker(), workerFailure));

            // Timed offers let the producer notice a failed consumer even when its queue is full.
            for (Record r : allRecords) {
                enqueue(skierQ, new SkierTask(r.getSkierId(), r.getLiftId(), false), workerFailure);
                enqueue(liftQ, new LiftTask(r.getLiftId(), false), workerFailure);
                enqueue(hourQ, new HourTask(r.getOperationalHour(), r.getLiftId(), false), workerFailure);
            }
            for (int i = 0; i < skierWorkers; i++) enqueue(skierQ, SkierTask.poison(), workerFailure);
            for (int i = 0; i < liftWorkers; i++) enqueue(liftQ, LiftTask.poison(), workerFailure);
            for (int i = 0; i < hourWorkers; i++) enqueue(hourQ, HourTask.poison(), workerFailure);

            checkWorkerFailure(workerFailure);
            for (Future<?> worker : workers) {
                try {
                    worker.get();
                } catch (ExecutionException ex) {
                    throw new IllegalStateException("Concurrent analysis worker failed", ex.getCause());
                }
            }
            checkWorkerFailure(workerFailure);
            joined = true;
        } finally {
            if (joined) {
                pool.shutdown();
            } else {
                for (Future<?> worker : workers) worker.cancel(true);
                pool.shutdownNow();
            }
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                    throw new IllegalStateException("Concurrent analysis workers did not stop");
                }
            } catch (InterruptedException ex) {
                pool.shutdownNow();
                throw ex;
            }
        }
        completed = true;
    }

    private static Future<?> submitWorker(ExecutorService pool, Runnable worker,
                                           AtomicReference<Throwable> workerFailure) {
        return pool.submit(() -> {
            try {
                worker.run();
            } catch (RuntimeException | Error ex) {
                workerFailure.compareAndSet(null, ex);
                throw ex;
            }
        });
    }

    private static <T> void enqueue(BlockingQueue<T> queue, T task,
                                    AtomicReference<Throwable> workerFailure) throws InterruptedException {
        do {
            checkWorkerFailure(workerFailure);
        } while (!queue.offer(task, 100, TimeUnit.MILLISECONDS));
        checkWorkerFailure(workerFailure);
    }

    private static void checkWorkerFailure(AtomicReference<Throwable> workerFailure) {
        Throwable cause = workerFailure.get();
        if (cause != null) {
            throw new IllegalStateException("Concurrent analysis worker failed", cause);
        }
    }

    private void requireCompleted() {
        if (!completed) {
            throw new IllegalStateException("Concurrent analysis has not completed successfully");
        }
    }

    /** Consumer for the skier channel: accumulates total vertical per skier. */
    private final class SkierWorker implements Runnable {
        @Override public void run() {
            try {
                while (true) {
                    SkierTask t = skierQ.take();
                    if (t.poison) break;
                    int vertical = elevationForLift(t.liftId); //1–10:200, 11–20:300, 21–30:400, 31–40:500
                    skierVertical.computeIfAbsent(t.skierId, k -> new LongAdder()).add(vertical);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Skier worker interrupted", ie);
            }
        }
    }

    /** Consumer for the lift channel: counts total rides per lift. */
    private final class LiftWorker implements Runnable {
        @Override public void run() {
            try {
                while (true) {
                    LiftTask t = liftQ.take();
                    if (t.poison) break;
                    liftRides.computeIfAbsent(t.liftId, k -> new LongAdder()).increment();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Lift worker interrupted", ie);
            }
        }
    }

    /** Consumer for the hour channel: counts rides per (hour, liftId). */
    private final class HourWorker implements Runnable {
        @Override public void run() {
            try {
                while (true) {
                    HourTask t = hourQ.take();
                    if (t.poison) break;
                    hourLiftRides
                            .computeIfAbsent(t.hour, h -> new ConcurrentHashMap<>())
                            .computeIfAbsent(t.liftId, k -> new LongAdder())
                            .increment();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Hour worker interrupted", ie);
            }
        }
    }

    /**
     * Map a liftId to its vertical rise in meters using the fixed ranges:
     * [1..10]→200, [11..20]→300, [21..30]→400, [31..40]→500.
     *
     * <p><b>Requires:</b> {@code 1 <= Integer.parseInt(liftId) <= 40}.<br>
     * <b>Effects:</b> none (pure function).
     *
     * @param liftId string representation of an integer in [1, 40]
     * @return vertical meters for a single ride on this lift
     * @throws IllegalArgumentException if liftId is out of range
     */
    private static int elevationForLift(String liftId) {
        int id = Integer.parseInt(liftId);
        if (id < 1 || id > 40) throw new IllegalArgumentException("LiftId must be 1..40, got: " + liftId);
        if (id <= 10) return 200;
        if (id <= 20) return 300;
        if (id <= 30) return 400;
        return 500;
    }

    /**
     * Return the data for Skiers.csv in descending total vertical.
     *
     * <p><b>Requires:</b> {@link #process(ArrayList)} has been called.<br>
     * <b>Effects:</b> none; sorting is done on a copy.<br>
     *
     * @return {@code Object[][]} where each row is {@code [skierId, totalVertical]}
     */
    public Object[][] getSkierCsv() {
        requireCompleted();
        List<Map.Entry<String, LongAdder>> list = new ArrayList<>(skierVertical.entrySet());
        list.sort(Comparator.<Map.Entry<String, LongAdder>>comparingLong(e -> e.getValue().longValue())
                .reversed().thenComparing(Map.Entry::getKey));
        Object[][] arr = new Object[list.size()][2];
        int i = 0;
        for (Map.Entry<String, LongAdder> e : list) {
            arr[i][0] = e.getKey();
            arr[i][1] = (int) e.getValue().longValue();
            i++;
        }
        return arr;
    }

    /**
     * Return the data for Lifts.csv in ascending <em>string</em> order of liftId,
     * matching the sequential implementation that uses a TreeMap.
     *
     * <p><b>Requires:</b> {@link #process(ArrayList)} has been called.<br>
     * <b>Effects:</b> none; sorting is done on a copy.<br>
     *
     * @return {@code Object[][]} where each row is {@code [liftId, rides]}
     */
    public Object[][] getLiftCsv() {
        requireCompleted();
        TreeMap<String, LongAdder> sorted = new TreeMap<>(liftRides);
        Object[][] arr = new Object[sorted.size()][2];
        int i = 0;
        for (Map.Entry<String, LongAdder> e : sorted.entrySet()) {
            arr[i][0] = e.getKey();
            arr[i][1] = (int) e.getValue().longValue();
            i++;
        }
        return arr;
    }

    /**
     * Return the flattened data for Hours.csv:
     * hours in ascending numeric order (expected "0".."5"); within each hour, the
     * top-10 lifts in descending ride count.
     *
     * <p><b>Requires:</b> {@link #process(ArrayList)} has been called.<br>
     * <b>Effects:</b> none; sorting and slicing are done on copies.<br>
     *
     * @return {@code Object[][]} where each row is {@code [hour, liftId, rides]}
     */
    public Object[][] getHoursCsv() {
        requireCompleted();
        List<String> hours = new ArrayList<>(hourLiftRides.keySet());
        hours.sort(Comparator.comparingInt(Integer::parseInt));

        // Pre-compute row count and take top-10 per hour
        int totalRows = 0;
        Map<String, List<Map.Entry<String, LongAdder>>> topByHour = new LinkedHashMap<>();
        for (String h : hours) {
            List<Map.Entry<String, LongAdder>> entries = new ArrayList<>(hourLiftRides.get(h).entrySet());
            entries.sort(Comparator.<Map.Entry<String, LongAdder>>comparingLong(e -> e.getValue().longValue())
                    .reversed().thenComparing(Map.Entry::getKey));
            if (entries.size() > 10) entries = entries.subList(0, 10);
            topByHour.put(h, entries);
            totalRows += entries.size();
        }

        Object[][] res = new Object[totalRows][3];
        int idx = 0;
        for (Map.Entry<String, List<Map.Entry<String, LongAdder>>> e : topByHour.entrySet()) {
            String hour = e.getKey();
            for (Map.Entry<String, LongAdder> lift : e.getValue()) {
                res[idx][0] = hour;
                res[idx][1] = lift.getKey();
                res[idx][2] = (int) lift.getValue().longValue();
                idx++;
            }
        }
        return res;
    }
}
