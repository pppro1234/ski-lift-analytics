package skiliftanalytics.dto;

public class AnalysisRequest {
    private String mode;

    public AnalysisRequest() {
    }

    public AnalysisRequest(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
