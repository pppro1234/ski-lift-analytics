package skiliftanalytics.service;

import java.util.Locale;

public enum AnalysisMode {
    SEQUENTIAL,
    CONCURRENT;

    public static AnalysisMode from(String value) {
        if (value == null || value.isBlank()) {
            return CONCURRENT;
        }

        try {
            return AnalysisMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported mode: " + value
                    + ". Use 'sequential' or 'concurrent'.", ex);
        }
    }

    public String apiValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
