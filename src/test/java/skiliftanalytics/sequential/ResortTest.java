package skiliftanalytics.sequential;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class ResortTest {

    @Test
    public void testConstructorAndGetters() {
        ArrayList<skiliftanalytics.sequential.Record> records = new ArrayList<>();
        records.add(new skiliftanalytics.sequential.Record("1", "1", "S1", "5", "0"));

        HashMap<String, ArrayList<skiliftanalytics.sequential.Record>> recordByHour = new HashMap<>();
        recordByHour.put("0", new ArrayList<skiliftanalytics.sequential.Record>(records));

        HashMap<String, Skier> skiers = new HashMap<>();
        skiers.put("S1", new Skier("S1", records));

        HashMap<String, Lift> lifts = new HashMap<>();
        lifts.put("5", new Lift("5", new ArrayList<skiliftanalytics.sequential.Record>(records)));

        Resort resort = new Resort("R1", "1", records, recordByHour, skiers, lifts);

        assertEquals("R1", resort.getResortId());
        assertEquals("1", resort.getDay());
        assertEquals(records, resort.getRecords());
        assertEquals(recordByHour, resort.getRecordByHour());
        assertEquals(skiers, resort.getTodaySkier());
        assertEquals(lifts, resort.getLifts());
    }

    @Test
    public void testSetters() {
        Resort resort = new Resort("R1", "1",
                new ArrayList<Record>(),
                new HashMap<String, ArrayList<skiliftanalytics.sequential.Record>>(),
                new HashMap<String, Skier>(),
                new HashMap<String, Lift>());

        ArrayList<skiliftanalytics.sequential.Record> records = new ArrayList<>();
        records.add(new skiliftanalytics.sequential.Record("2", "2", "S2", "10", "1"));

        HashMap<String, ArrayList<skiliftanalytics.sequential.Record>> recordByHour = new HashMap<>();
        recordByHour.put("1", new ArrayList<skiliftanalytics.sequential.Record>(records));

        HashMap<String, Skier> skiers = new HashMap<>();
        skiers.put("S2", new Skier("S2", records));

        HashMap<String, Lift> lifts = new HashMap<>();
        lifts.put("10", new Lift("10", new ArrayList<skiliftanalytics.sequential.Record>(records)));

        resort.setResortId("R2");
        resort.setDay("2");
        resort.setRecords(records);
        resort.setRecordByHour(recordByHour);
        resort.setTodaySkier(skiers);
        resort.setLifts(lifts);

        assertEquals("R2", resort.getResortId());
        assertEquals("2", resort.getDay());
        assertEquals(records, resort.getRecords());
        assertEquals(recordByHour, resort.getRecordByHour());
        assertEquals(skiers, resort.getTodaySkier());
        assertEquals(lifts, resort.getLifts());
    }
}
