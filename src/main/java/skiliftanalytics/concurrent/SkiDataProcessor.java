package skiliftanalytics.concurrent;

import skiliftanalytics.sequential.DataStreamer;
import skiliftanalytics.sequential.Record;
import skiliftanalytics.sequential.Resort;
import skiliftanalytics.sequential.ResortProcessor;
import skiliftanalytics.sequential.Analyzer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Entry point for the ski data processing program.
 *
 * <p>Reads the bundled {@code ski_lift_data.csv} by default and writes
 * {@code Skiers.csv}, {@code Lifts.csv} and {@code Hours.csv} under {@code out/}.</p>
 *
 * <p>Mode selection:</p>
 * <ul>
 *   <li>No args or first arg {@code "concurrent"} &rarr; run the concurrent pipeline.</li>
 *   <li>First arg {@code "sequential"} &rarr; run the sequential pipeline.</li>
 * </ul>
 * <p>Optional second and third arguments select an input CSV path and output
 * directory.</p>
 */
public class SkiDataProcessor {
    /**
     * Entry point. See the usage in the class Javadoc.
     *
     * <p><b>Requires:</b> the bundled CSV or a readable input path.<br>
     * <b>Effects:</b> runs either the concurrent or sequential pipeline and writes three CSV files.
     *
     * @param args CLI arguments as documented
     */
    public static void main(String[] args) {
        if (args.length > 3) {
            throw new IllegalArgumentException(
                    "Usage: SkiDataProcessor [concurrent|sequential] [input.csv] [output-dir]");
        }
        String mode = (args.length > 0) ? args[0].toLowerCase(Locale.ROOT) : "concurrent";
        if (!"concurrent".equals(mode) && !"sequential".equals(mode)) {
            throw new IllegalArgumentException(
                    "Unsupported mode: " + mode + ". Use 'concurrent' or 'sequential'.");
        }
        Path outputDir = (args.length > 2) ? Path.of(args[2]) : Path.of("out");

        long t0 = System.nanoTime();

        try {
            Files.createDirectories(outputDir);
            ArrayList<Record> allRecords;
            if (args.length > 1) {
                try (InputStream input = Files.newInputStream(Path.of(args[1]))) {
                    allRecords = DataStreamer.readRecords(input);
                }
            } else {
                allRecords = DataStreamer.readRecords("ski_lift_data.csv");
            }

            Object[][] skiers;
            Object[][] lifts;
            Object[][] hours;
            if ("sequential".equals(mode)) {
                ArrayList<Resort> resorts = ResortProcessor.createResorts(allRecords);
                Analyzer analyzer = new Analyzer(resorts);
                skiers = analyzer.getSkierCsv();
                lifts = analyzer.getLiftCsv();
                hours = analyzer.getHoursCsv();
            } else {
                int cores = Runtime.getRuntime().availableProcessors();
                int skierThreads = Math.max(1, cores / 2);
                int liftThreads  = Math.max(1, cores / 4);
                int hourThreads  = Math.max(1, cores / 4);
                ConcurrentAnalyzer analyzer = new ConcurrentAnalyzer(skierThreads, liftThreads, hourThreads);
                analyzer.process(allRecords);
                skiers = analyzer.getSkierCsv();
                lifts = analyzer.getLiftCsv();
                hours = analyzer.getHoursCsv();
            }

            DataStreamer.writeSkiersCsv(skiers, outputDir.resolve("Skiers.csv").toString());
            DataStreamer.writeLiftsCsv(lifts, outputDir.resolve("Lifts.csv").toString());
            DataStreamer.writeHoursCsv(hours, outputDir.resolve("Hours.csv").toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read input or create output directory.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent analysis was interrupted.", e);
        }

        long t1 = System.nanoTime();
        System.out.printf("Total execution time: %.3f s%n", (t1 - t0) / 1_000_000_000.0);
    }
}
