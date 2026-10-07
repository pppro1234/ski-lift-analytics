package skiliftanalytics.concurrent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link SkiDataProcessor}.
 */
public class SkiDataProcessorTest {
    @TempDir
    Path tempDir;

    @Test
    public void testBothModesWriteSameReports() throws IOException {
        Path input = tempDir.resolve("rides.csv");
        Files.writeString(input, """
                Resort,Day,SkierID,LiftID,Time
                resortA,1,100,1,30
                resortA,1,100,11,60
                resortA,1,200,1,30
                """);
        Path sequential = tempDir.resolve("sequential");
        Path concurrent = tempDir.resolve("concurrent");

        SkiDataProcessor.main(new String[]{"sequential", input.toString(), sequential.toString()});
        SkiDataProcessor.main(new String[]{"concurrent", input.toString(), concurrent.toString()});

        for (String report : List.of("Skiers.csv", "Lifts.csv", "Hours.csv")) {
            assertArrayEquals(Files.readAllBytes(sequential.resolve(report)),
                    Files.readAllBytes(concurrent.resolve(report)), report);
        }
        assertEquals(List.of("SkierID,Vertical", "100,500", "200,200"),
                Files.readAllLines(concurrent.resolve("Skiers.csv")));
        assertEquals(List.of("LiftID,Number of Rides", "1,2", "11,1"),
                Files.readAllLines(concurrent.resolve("Lifts.csv")));
    }

    @Test
    public void testInvalidModeAndMissingInputFail() {
        assertThrows(IllegalArgumentException.class,
                () -> SkiDataProcessor.main(new String[]{"unknown"}));
        assertThrows(UncheckedIOException.class,
                () -> SkiDataProcessor.main(new String[]{
                        "sequential", tempDir.resolve("missing.csv").toString(),
                        tempDir.resolve("out").toString()}));
    }
}
