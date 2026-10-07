package skiliftanalytics.service;

import skiliftanalytics.concurrent.ConcurrentAnalyzer;
import skiliftanalytics.dto.AnalysisResponse;
import skiliftanalytics.exception.AnalysisInterruptedException;
import skiliftanalytics.mapper.AnalysisResultMapper;
import skiliftanalytics.sequential.Analyzer;
import skiliftanalytics.sequential.DataStreamer;
import skiliftanalytics.sequential.Record;
import skiliftanalytics.sequential.Resort;
import skiliftanalytics.sequential.ResortProcessor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@Service
public class AnalysisService {
    private static final String DEFAULT_CSV_RESOURCE = "ski_lift_data.csv";

    private final AnalysisResultMapper analysisResultMapper;

    public AnalysisService(AnalysisResultMapper analysisResultMapper) {
        this.analysisResultMapper = analysisResultMapper;
    }

    public AnalysisResponse runDefaultAnalysis(String modeValue) {
        AnalysisMode mode = AnalysisMode.from(modeValue);
        ArrayList<Record> records;
        try {
            records = DataStreamer.readRecords(DEFAULT_CSV_RESOURCE);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Default CSV resource is unavailable or invalid.", ex);
        }
        return analyze(records, mode);
    }

    public AnalysisResponse runUploadedAnalysis(InputStream inputStream, String modeValue) {
        AnalysisMode mode = AnalysisMode.from(modeValue);
        ArrayList<Record> records = DataStreamer.readRecords(inputStream);
        return analyze(records, mode);
    }

    private AnalysisResponse analyze(ArrayList<Record> records, AnalysisMode mode) {
        long start = System.nanoTime();
        try {
            AnalysisTables tables = (mode == AnalysisMode.SEQUENTIAL)
                    ? runSequential(records)
                    : runConcurrent(records);
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            return analysisResultMapper.toResponse(
                    mode.apiValue(),
                    records.size(),
                    durationMs,
                    tables.skierRows(),
                    tables.liftRows(),
                    tables.hourRows()
            );
        } catch (AnalysisInterruptedException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Analysis failed.", ex);
        }
    }

    private AnalysisTables runSequential(ArrayList<Record> records) {
        ArrayList<Resort> resorts = ResortProcessor.createResorts(records);
        Analyzer analyzer = new Analyzer(resorts);
        return new AnalysisTables(
                analyzer.getSkierCsv(),
                analyzer.getLiftCsv(),
                analyzer.getHoursCsv()
        );
    }

    private AnalysisTables runConcurrent(ArrayList<Record> records) {
        int cores = Runtime.getRuntime().availableProcessors();
        int skierThreads = Math.max(1, cores / 2);
        int liftThreads = Math.max(1, cores / 4);
        int hourThreads = Math.max(1, cores / 4);

        ConcurrentAnalyzer analyzer = new ConcurrentAnalyzer(skierThreads, liftThreads, hourThreads);
        try {
            analyzer.process(records);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AnalysisInterruptedException("Concurrent analysis was interrupted.", ex);
        }

        return new AnalysisTables(
                analyzer.getSkierCsv(),
                analyzer.getLiftCsv(),
                analyzer.getHoursCsv()
        );
    }

    private record AnalysisTables(Object[][] skierRows, Object[][] liftRows, Object[][] hourRows) {
    }
}
