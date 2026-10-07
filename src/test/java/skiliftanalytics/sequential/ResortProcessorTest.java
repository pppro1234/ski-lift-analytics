package skiliftanalytics.sequential;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class ResortProcessorTest {

    private ArrayList<skiliftanalytics.sequential.Record> buildSampleRecords() {
        ArrayList<skiliftanalytics.sequential.Record> records = new ArrayList<>();
        // Resort 1, day 1
        records.add(new skiliftanalytics.sequential.Record("1", "1", "S1", "1", "0"));
        records.add(new skiliftanalytics.sequential.Record("1", "1", "S1", "2", "1"));
        records.add(new Record("1", "1", "S2", "1", "0"));
        // Resort 2, day 2
        records.add(new skiliftanalytics.sequential.Record("2", "2", "S3", "1", "0"));
        return records;
    }

    private Resort findResort(ArrayList<Resort> resorts, String resortId, String day) {
        for (Resort resort : resorts) {
            if (resortId.equals(resort.getResortId()) && day.equals(resort.getDay())) {
                return resort;
            }
        }
        return null;
    }

    @Test
    public void testCreateResortsGroupsByResortAndDay() {
        ArrayList<skiliftanalytics.sequential.Record> allRecords = buildSampleRecords();
        ArrayList<Resort> resorts = ResortProcessor.createResorts(allRecords);

        assertEquals(2, resorts.size());

        Resort resort1 = findResort(resorts, "1", "1");
        Resort resort2 = findResort(resorts, "2", "2");

        assertNotNull(resort1);
        assertNotNull(resort2);

        assertEquals(3, resort1.getRecords().size());
        assertEquals(1, resort2.getRecords().size());

        // recordByHour for resort1: hour "0" has 2 records, hour "1" has 1 record
        HashMap<String, ArrayList<skiliftanalytics.sequential.Record>> recordByHour1 = resort1.getRecordByHour();
        assertEquals(2, recordByHour1.get("0").size());
        assertEquals(1, recordByHour1.get("1").size());

        // skier map
        assertEquals(2, resort1.getTodaySkier().size());
        assertTrue(resort1.getTodaySkier().containsKey("S1"));
        assertTrue(resort1.getTodaySkier().containsKey("S2"));

        // lift map
        assertEquals(2, resort1.getLifts().size());
        assertTrue(resort1.getLifts().containsKey("1"));
        assertTrue(resort1.getLifts().containsKey("2"));
    }
}
