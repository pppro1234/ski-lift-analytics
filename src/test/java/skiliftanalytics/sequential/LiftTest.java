package skiliftanalytics.sequential;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class LiftTest {

    @Test
    public void testElevationRangesByConstructor() {
        ArrayList<Record> records = new ArrayList<>();

        Lift lift1 = new Lift("1", records);
        Lift lift10 = new Lift("10", records);
        Lift lift15 = new Lift("15", records);
        Lift lift25 = new Lift("25", records);
        Lift lift35 = new Lift("35", records);

        assertEquals(200, lift1.getElevation());
        assertEquals(200, lift10.getElevation());
        assertEquals(300, lift15.getElevation());
        assertEquals(400, lift25.getElevation());
        assertEquals(500, lift35.getElevation());
    }

    @Test
    public void testInvalidLiftIdTooSmall() {
        assertThrows(IllegalArgumentException.class,
                () -> new Lift("0", new ArrayList<skiliftanalytics.sequential.Record>()));
    }

    @Test
    public void testInvalidLiftIdTooLarge() {
        assertThrows(IllegalArgumentException.class,
                () -> new Lift("41", new ArrayList<skiliftanalytics.sequential.Record>()));
    }

    @Test
    public void testGettersAndSetters() {
        ArrayList<skiliftanalytics.sequential.Record> records = new ArrayList<>();
        Lift lift = new Lift("5", records);

        ArrayList<skiliftanalytics.sequential.Record> newRecords = new ArrayList<>();
        newRecords.add(new skiliftanalytics.sequential.Record("1", "1", "S1", "5", "0"));

        lift.setLiftId("6");
        lift.setRecord(newRecords);
        lift.setElevation(999);

        assertEquals("6", lift.getLiftId());
        assertEquals(newRecords, lift.getRecord());
        assertEquals(999, lift.getElevation());
    }

    @Test
    public void testEqualsAndHashCode() {
        ArrayList<skiliftanalytics.sequential.Record> records = new ArrayList<>();
        records.add(new skiliftanalytics.sequential.Record("1", "1", "S1", "5", "0"));


        Lift liftA = new Lift("5", records);
        Lift liftB = new Lift("5", records);
        Lift liftC = new Lift("6", records);

        assertEquals(liftA, liftB);
        assertEquals(liftA.hashCode(), liftB.hashCode());
        assertNotEquals(liftA, liftC);
    }

}
