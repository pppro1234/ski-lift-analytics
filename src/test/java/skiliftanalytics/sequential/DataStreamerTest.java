package skiliftanalytics.sequential;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class DataStreamerTest {
    private static final String HEADER = "resort,day,skier,lift,time\n";

    @Test
    public void testReadRecordsFromResource() {

        ArrayList<Record> records = DataStreamer.readRecords("ski_lift_data.csv");
        assertNotNull(records);
        assertFalse(records.isEmpty());
    }

    @Test
    public void testReadRecordsFromInputStream() {
        String csv = """
                Resort,Day,SkierID,LiftID,Time
                resortA,1,100,1,30
                resortA,1,100,11,60
                """;

        ArrayList<Record> records = DataStreamer.readRecords(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(2, records.size());
        assertEquals("100", records.get(0).getSkierId());
        assertEquals("0", records.get(0).getOperationalHour());
        assertEquals("1", records.get(1).getOperationalHour());
    }

    @Test
    public void testHeaderOnlyAndTimeBoundaries() {
        assertTrue(read(HEADER).isEmpty());
        ArrayList<Record> records = read(HEADER + "0,2,31,1,0\n0,2,31,40,360\n");
        assertEquals(2, records.size());
        assertEquals("0", records.get(0).getOperationalHour());
        assertEquals("5", records.get(1).getOperationalHour());
    }

    @Test
    public void testRejectsEmptyFileAndInvalidHeader() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> read(""))
                .getMessage().contains("line 1"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> read("resort,day,skier,time,lift\n0,2,31,1,60\n"))
                .getMessage().contains("line 1"));
    }

    @Test
    public void testRejectsMalformedRowsWithLineAndReason() {
        assertInvalidRow("0,2,31,1", "expected 5 fields");
        assertInvalidRow("0,2,31,1,60,extra", "expected 5 fields");
        assertInvalidRow("0,2,,1,60", "skier is required");
        assertInvalidRow("0,2,31,1,", "time is required");
        assertInvalidRow("0,2,31,01,60", "canonical integer");
        assertInvalidRow("0,2,31,41,60", "1 to 40");
        assertInvalidRow("0,2,31,one,60", "lift must be an integer");
        assertInvalidRow("0,2,31,1,-1", "0 to 360");
        assertInvalidRow("0,2,31,1,361", "0 to 360");
        assertInvalidRow("0,2,31,1,noon", "time must be an integer");
    }

    @Test
    public void testReadFailureIsNotReportedAsBadCsv() {
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("read failed");
            }
        };
        assertThrows(UncheckedIOException.class, () -> DataStreamer.readRecords(broken));
    }

    @Test
    public void testResortAndDayWithHyphensRemainDistinct() {
        ArrayList<Record> records = read(HEADER
                + "resort-A,1,31,1,60\n"
                + "resort,A-1,31,1,60\n");
        assertEquals(2, ResortProcessor.createResorts(records).size());
    }

    @Test
    public void testWriteFailurePropagates(@TempDir Path outputDirectory) {
        String directory = outputDirectory.toString();
        assertThrows(UncheckedIOException.class,
                () -> DataStreamer.writeSkiersCsv(new Object[0][0], directory));
        assertThrows(UncheckedIOException.class,
                () -> DataStreamer.writeLiftsCsv(new Object[0][0], directory));
        assertThrows(UncheckedIOException.class,
                () -> DataStreamer.writeHoursCsv(new Object[0][0], directory));
    }

    private static ArrayList<Record> read(String csv) {
        return DataStreamer.readRecords(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    private static void assertInvalidRow(String row, String reason) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> read(HEADER + "0,2,31,1,60\n" + row + "\n"));
        assertTrue(exception.getMessage().contains("line 3"), exception.getMessage());
        assertTrue(exception.getMessage().contains(reason), exception.getMessage());
    }

    @Test
    public void testWriteSkiersCsv() throws Exception {
        Object[][] data = new Object[][]{
                {"S1", 400},
                {"S2", 200}
        };

        File tmp = File.createTempFile("SkiersTest", ".csv");
        tmp.deleteOnExit();

        DataStreamer.writeSkiersCsv(data, tmp.getAbsolutePath());

        try (BufferedReader reader = new BufferedReader(new FileReader(tmp))) {
            String header = reader.readLine();
            String line1 = reader.readLine();
            String line2 = reader.readLine();
            String extra = reader.readLine();

            assertEquals("SkierID,Vertical", header);
            assertEquals("S1,400", line1);
            assertEquals("S2,200", line2);
            assertNull(extra);
        }
    }

    @Test
    public void testWriteLiftsCsv() throws Exception {
        Object[][] data = new Object[][]{
                {"1", 2},
                {"2", 1}
        };

        File tmp = File.createTempFile("LiftsTest", ".csv");
        tmp.deleteOnExit();

        DataStreamer.writeLiftsCsv(data, tmp.getAbsolutePath());

        try (BufferedReader reader = new BufferedReader(new FileReader(tmp))) {
            String header = reader.readLine();
            String line1 = reader.readLine();
            String line2 = reader.readLine();
            String extra = reader.readLine();

            assertEquals("LiftID,Number of Rides", header);
            assertEquals("1,2", line1);
            assertEquals("2,1", line2);
            assertNull(extra);
        }
    }

    @Test
    public void testWriteHoursCsv() throws Exception {
        // hour, liftId, rides
        Object[][] data = new Object[][]{
                {"0", "1", 2},
                {"1", "2", 1}
        };

        File tmp = File.createTempFile("HoursTest", ".csv");
        tmp.deleteOnExit();

        DataStreamer.writeHoursCsv(data, tmp.getAbsolutePath());

        try (BufferedReader reader = new BufferedReader(new FileReader(tmp))) {
            String line1 = reader.readLine(); // Hour #0
            String line2 = reader.readLine(); // header
            String line3 = reader.readLine(); // 1,2
            String line4 = reader.readLine(); // blank line (maybe)
            String line5 = reader.readLine(); // Hour #1
            String line6 = reader.readLine(); // header
            String line7 = reader.readLine(); // 2,1

            assertEquals("Hour #0", line1);
            assertEquals("LiftID,Number of Rides", line2);
            assertEquals("1,2", line3);

            if (line4 != null) {
                assertTrue(line4.isEmpty());
            }

            assertEquals("Hour #1", line5);
            assertEquals("LiftID,Number of Rides", line6);
            assertEquals("2,1", line7);
        }
    }
}
