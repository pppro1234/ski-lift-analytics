package skiliftanalytics.mapper;

import skiliftanalytics.dto.AnalysisResponse;
import skiliftanalytics.dto.AnalysisSummaryDto;
import skiliftanalytics.dto.HourlyTopLiftDto;
import skiliftanalytics.dto.LiftStatDto;
import skiliftanalytics.dto.SkierStatDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AnalysisResultMapper {
    public AnalysisResponse toResponse(String mode,
                                       int recordCount,
                                       long durationMs,
                                       Object[][] skierRows,
                                       Object[][] liftRows,
                                       Object[][] hourRows) {
        return new AnalysisResponse(
                new AnalysisSummaryDto(mode, recordCount, durationMs),
                mapTopSkiers(skierRows),
                mapLiftUsage(liftRows),
                mapHourlyTopLifts(hourRows)
        );
    }

    private List<SkierStatDto> mapTopSkiers(Object[][] rows) {
        int limit = Math.min(100, rows.length);
        List<SkierStatDto> topSkiers = new ArrayList<>(limit);
        for (int index = 0; index < limit; index++) {
            topSkiers.add(new SkierStatDto((String) rows[index][0], (Integer) rows[index][1]));
        }
        return topSkiers;
    }

    private List<LiftStatDto> mapLiftUsage(Object[][] rows) {
        List<LiftStatDto> liftUsage = new ArrayList<>(rows.length);
        for (Object[] row : rows) {
            liftUsage.add(new LiftStatDto((String) row[0], (Integer) row[1]));
        }
        return liftUsage;
    }

    private List<HourlyTopLiftDto> mapHourlyTopLifts(Object[][] rows) {
        List<HourlyTopLiftDto> hourlyTopLifts = new ArrayList<>(rows.length);
        for (Object[] row : rows) {
            hourlyTopLifts.add(new HourlyTopLiftDto((String) row[0], (String) row[1], (Integer) row[2]));
        }
        return hourlyTopLifts;
    }
}
