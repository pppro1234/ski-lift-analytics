package skiliftanalytics.sequential;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MainTest {
    @TempDir
    Path tempDir;

    @Test
    public void testSequentialMainWritesReports() throws IOException {
        Path input = tempDir.resolve("rides.csv");
        Path output = tempDir.resolve("reports");
        Files.writeString(input, """
                Resort,Day,SkierID,LiftID,Time
                resortA,1,100,1,30
                resortA,1,100,11,60
                resortA,1,200,1,30
                """);

        Main.main(new String[]{input.toString(), output.toString()});

        assertEquals(List.of("SkierID,Vertical", "100,500", "200,200"),
                Files.readAllLines(output.resolve("Skiers.csv")));
        assertEquals(List.of("LiftID,Number of Rides", "1,2", "11,1"),
                Files.readAllLines(output.resolve("Lifts.csv")));
        assertEquals(List.of("Hour #0", "LiftID,Number of Rides", "1,2", "",
                        "Hour #1", "LiftID,Number of Rides", "11,1"),
                Files.readAllLines(output.resolve("Hours.csv")));
    }
}
