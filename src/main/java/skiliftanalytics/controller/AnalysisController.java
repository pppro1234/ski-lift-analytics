package skiliftanalytics.controller;

import skiliftanalytics.dto.AnalysisRequest;
import skiliftanalytics.dto.AnalysisResponse;
import skiliftanalytics.service.AnalysisService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalysisController {
    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/analysis/run")
    public AnalysisResponse runAnalysis(@RequestBody(required = false) AnalysisRequest request) {
        String mode = request == null ? null : request.getMode();
        return analysisService.runDefaultAnalysis(mode);
    }

    @PostMapping(value = "/analysis/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisResponse uploadAndAnalyze(@RequestPart("file") MultipartFile file,
                                             @RequestParam(value = "mode", required = false) String mode)
            throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded CSV must not be empty.");
        }
        return analysisService.runUploadedAnalysis(file.getInputStream(), mode);
    }
}
