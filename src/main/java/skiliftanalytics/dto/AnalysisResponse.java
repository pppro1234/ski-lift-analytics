package skiliftanalytics.dto;

import java.util.List;

public record AnalysisResponse(
        AnalysisSummaryDto summary,
        List<SkierStatDto> topSkiers,
        List<LiftStatDto> liftUsage,
        List<HourlyTopLiftDto> hourlyTopLifts
) {
}
