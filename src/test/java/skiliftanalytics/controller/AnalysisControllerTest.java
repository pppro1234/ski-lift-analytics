package skiliftanalytics.controller;

import skiliftanalytics.exception.AnalysisInterruptedException;
import skiliftanalytics.exception.GlobalExceptionHandler;
import skiliftanalytics.service.AnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AnalysisControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private static MockMultipartFile csvFile(String csv) {
        return new MockMultipartFile("file", "upload.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMvc mockMvcWithService(AnalysisService service) {
        return MockMvcBuilders.standaloneSetup(new AnalysisController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    public void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    public void testRunAnalysisEndpoint() throws Exception {
        mockMvc.perform(post("/api/analysis/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"sequential\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.mode").value("sequential"))
                .andExpect(jsonPath("$.summary.recordCount").isNumber())
                .andExpect(jsonPath("$.topSkiers").isArray())
                .andExpect(jsonPath("$.liftUsage").isArray())
                .andExpect(jsonPath("$.hourlyTopLifts").isArray());
    }

    @Test
    public void testUploadEndpoint() throws Exception {
        String csv = """
                Resort,Day,SkierID,LiftID,Time
                resortA,1,100,1,30
                resortA,1,100,11,60
                """;
        mockMvc.perform(multipart("/api/analysis/upload")
                        .file(csvFile(csv))
                        .param("mode", "concurrent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.mode").value("concurrent"))
                .andExpect(jsonPath("$.summary.recordCount").value(2))
                .andExpect(jsonPath("$.topSkiers[0].skierId").value("100"))
                .andExpect(jsonPath("$.topSkiers[0].vertical").value(500));
    }

    @Test
    public void testHeaderOnlyUploadReturnsEmptyResults() throws Exception {
        mockMvc.perform(multipart("/api/analysis/upload")
                        .file(csvFile("Resort,Day,SkierID,LiftID,Time\n"))
                        .param("mode", "concurrent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.recordCount").value(0))
                .andExpect(jsonPath("$.topSkiers").isEmpty())
                .andExpect(jsonPath("$.liftUsage").isEmpty())
                .andExpect(jsonPath("$.hourlyTopLifts").isEmpty());
    }

    @Test
    public void testMalformedUploadReturnsLineNumber() throws Exception {
        mockMvc.perform(multipart("/api/analysis/upload")
                        .file(csvFile("Resort,Day,SkierID,LiftID,Time\nresortA,1,100,1\n")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("line 2")));
    }

    @Test
    public void testInvalidLiftUploadReturnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/analysis/upload")
                        .file(csvFile("Resort,Day,SkierID,LiftID,Time\nresortA,1,100,41,30\n")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("line 2")));
    }

    @Test
    public void testUploadReadFailureReturnsServerError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "upload.csv", "text/csv", new byte[]{1}) {
            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("private read detail");
            }
        };
        mockMvcWithService(mock(AnalysisService.class))
                .perform(multipart("/api/analysis/upload").file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INPUT_READ_FAILED"))
                .andExpect(jsonPath("$.message").value("Failed to read CSV input."));
    }

    @Test
    public void testAnalysisFailureReturnsServerError() throws Exception {
        AnalysisService service = mock(AnalysisService.class);
        when(service.runUploadedAnalysis(any(InputStream.class), eq("concurrent")))
                .thenThrow(new IllegalStateException("private worker detail"));

        mockMvcWithService(service)
                .perform(multipart("/api/analysis/upload")
                        .file(csvFile("Resort,Day,SkierID,LiftID,Time\n"))
                        .param("mode", "concurrent"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("ANALYSIS_FAILED"))
                .andExpect(jsonPath("$.message").value("Analysis failed."));
    }

    @Test
    public void testInterruptedAnalysisReturnsUnavailable() throws Exception {
        AnalysisService service = mock(AnalysisService.class);
        when(service.runUploadedAnalysis(any(InputStream.class), eq("concurrent")))
                .thenThrow(new AnalysisInterruptedException("private interrupt detail", new InterruptedException()));

        mockMvcWithService(service)
                .perform(multipart("/api/analysis/upload")
                        .file(csvFile("Resort,Day,SkierID,LiftID,Time\n"))
                        .param("mode", "concurrent"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("ANALYSIS_INTERRUPTED"))
                .andExpect(jsonPath("$.message").value("Analysis was interrupted."));
    }
}
