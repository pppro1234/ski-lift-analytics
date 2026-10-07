package skiliftanalytics.sequential;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RecordTest {

    @Test
    public void testConstructorAndGetters() {
        skiliftanalytics.sequential.Record record = new Record("1", "2", "S1", "10", "3");

        assertEquals("1", record.getResort());
        assertEquals("2", record.getDay());
        assertEquals("S1", record.getSkierId());
        assertEquals("10", record.getLiftId());
        assertEquals("3", record.getOperationalHour());
    }

    @Test
    public void testSetters() {
        skiliftanalytics.sequential.Record record = new skiliftanalytics.sequential.Record("1", "2", "S1", "10", "3");

        record.setResort("9");
        record.setDay("5");
        record.setSkierId("S9");
        record.setLiftId("20");
        record.setOperationalHour("4");

        assertEquals("9", record.getResort());
        assertEquals("5", record.getDay());
        assertEquals("S9", record.getSkierId());
        assertEquals("20", record.getLiftId());
        assertEquals("4", record.getOperationalHour());
    }
}
