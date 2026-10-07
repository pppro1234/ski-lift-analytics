package skiliftanalytics.sequential;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class AnalyzerNewTest {

    private ArrayList<skiliftanalytics.sequential.Record> buildSampleRecords() {
        ArrayList<skiliftanalytics.sequential.Record> records = new ArrayList<>();
        // Resort 1, day 1
        // S1: L1 (hour 0), L2 (hour 1)
        // S2: L1 (hour 0)
        records.add(new skiliftanalytics.sequential.Record("1", "1", "S1", "1", "0"));
        records.add(new skiliftanalytics.sequential.Record("1", "1", "S1", "2", "1"));
        records.add(new Record("1", "1", "S2", "1", "0"));
        return records;
    }

    private Analyzer buildAnalyzer() {
        ArrayList<skiliftanalytics.sequential.Record> allRecords = buildSampleRecords();
        ArrayList<Resort> resorts = ResortProcessor.createResorts(allRecords);
        return new Analyzer(resorts);
    }

    @Test
    public void testGetSkierCsv() {
        Analyzer analyzer = buildAnalyzer();
        Object[][] data = analyzer.getSkierCsv();

        // S1: two rides, elevation 200 + 200 = 400
        // S2: one ride, elevation 200
        assertEquals(2, data.length);

        assertEquals("S1", data[0][0]);
        assertEquals(400, ((Integer) data[0][1]).intValue());

        assertEquals("S2", data[1][0]);
        assertEquals(200, ((Integer) data[1][1]).intValue());
    }

    @Test
    public void testGetLiftCsv() {
        Analyzer analyzer = buildAnalyzer();
        Object[][] data = analyzer.getLiftCsv();

        // Lift 1: 2 rides; Lift 2: 1 ride
        assertEquals(2, data.length);

        assertEquals("1", data[0][0]);
        assertEquals(2, ((Integer) data[0][1]).intValue());

        assertEquals("2", data[1][0]);
        assertEquals(1, ((Integer) data[1][1]).intValue());
    }

    @Test
    public void testGetHoursCsv() {
        Analyzer analyzer = buildAnalyzer();
        Object[][] data = analyzer.getHoursCsv();

        // Hour "0": lift 1 with 2 rides
        // Hour "1": lift 2 with 1 ride
        assertEquals(2, data.length);

        // sorted by hour numerically: "0" then "1"
        assertEquals("0", data[0][0]);
        assertEquals("1", data[0][1]);
        assertEquals(2, ((Integer) data[0][2]).intValue());

        assertEquals("1", data[1][0]);
        assertEquals("2", data[1][1]);
        assertEquals(1, ((Integer) data[1][2]).intValue());
    }

    @Test
    public void testGettersAndSetters() {
        ArrayList<skiliftanalytics.sequential.Record> allRecords = buildSampleRecords();
        ArrayList<Resort> resorts = ResortProcessor.createResorts(allRecords);

        Analyzer analyzer = new Analyzer(resorts);
        assertEquals(resorts, analyzer.getResorts());

        ArrayList<Resort> newResorts = new ArrayList<>();
        analyzer.setResorts(newResorts);
        assertEquals(newResorts, analyzer.getResorts());
    }
}
