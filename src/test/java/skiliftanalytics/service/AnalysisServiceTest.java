package skiliftanalytics.service;

import skiliftanalytics.dto.AnalysisResponse;
import skiliftanalytics.mapper.AnalysisResultMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AnalysisServiceTest {
    private final AnalysisService analysisService = new AnalysisService(new AnalysisResultMapper());

    @Test
    public void testRunUploadedAnalysisSequential() {
        String csv = """
                Resort,Day,SkierID,LiftID,Time
                resortA,1,100,1,30
                resortA,1,100,11,60
                resortA,1,200,31,75
                """;

        AnalysisResponse response = analysisService.runUploadedAnalysis(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                "sequential"
        );

        assertEquals("sequential", response.summary().mode());
        assertEquals(3, response.summary().recordCount());
        assertEquals("100", response.topSkiers().get(0).skierId());
        assertEquals(500, response.topSkiers().get(0).vertical());
        assertEquals(3, response.liftUsage().size());
        assertEquals(3, response.hourlyTopLifts().size());
    }

    @Test
    public void testTopHundredTieBoundaryMatchesBothModes() {
        StringBuilder csv = new StringBuilder("resort,day,skier,lift,time\n");
        for (int id = 1; id <= 101; id++) {
            csv.append("resortA,1,skier").append(String.format("%03d", id))
                    .append(",1,0\n");
        }

        AnalysisResponse sequential = analysisService.runUploadedAnalysis(
                new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8)), "sequential");
        AnalysisResponse concurrent = analysisService.runUploadedAnalysis(
                new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8)), "concurrent");

        assertEquals(100, sequential.topSkiers().size());
        assertEquals("skier001", sequential.topSkiers().get(0).skierId());
        assertEquals("skier100", sequential.topSkiers().get(99).skierId());
        assertEquals(sequential.topSkiers(), concurrent.topSkiers());
        assertEquals(sequential.liftUsage(), concurrent.liftUsage());
        assertEquals(sequential.hourlyTopLifts(), concurrent.hourlyTopLifts());
    }
}
