package skiliftanalytics.sequential;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class SkierTest {

    @Test
    public void testConstructorAndGetters() {
        ArrayList<skiliftanalytics.sequential.Record> records = new ArrayList<>();
        records.add(new Record("1", "1", "S1", "5", "0"));

        Skier skier = new Skier("S1", records);

        assertEquals("S1", skier.getSkierId());
        assertEquals(records, skier.getRecord());
    }

    @Test
    public void testSetters() {
        ArrayList<skiliftanalytics.sequential.Record> records = new ArrayList<>();
        Skier skier = new Skier("S1", records);

        ArrayList<skiliftanalytics.sequential.Record> newRecords = new ArrayList<>();
        newRecords.add(new skiliftanalytics.sequential.Record("1", "2", "S1", "6", "1"));

        skier.setSkierId("S2");
        skier.setRecord(newRecords);

        assertEquals("S2", skier.getSkierId());
        assertEquals(newRecords, skier.getRecord());
    }
}
