package skiliftanalytics.sequential;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * DataStreamer handles reading raw records from the input CSV file and
 * writing various analysis results to output CSV files.
 */
public class DataStreamer {
    /**
     * Reads all records from the given CSV resource on the classpath.
     * The last column (time in minutes) is converted into an operational
     * hour in the range 0–5.
     *
     * @param filePath the resource name of the input CSV file
     * @return a list of {@link Record} objects read from the file
     */
    public static ArrayList<Record> readRecords(String filePath) {
        InputStream inputStream = DataStreamer.class.getClassLoader().getResourceAsStream(filePath);
        return readRecords(inputStream, filePath);
    }

    /**
     * Reads all records from a caller-provided CSV stream.
     *
     * @param inputStream CSV input stream
     * @return a list of {@link Record} objects read from the stream
     */
    public static ArrayList<Record> readRecords(InputStream inputStream) {
        return readRecords(inputStream, "input stream");
    }

    private static ArrayList<Record> readRecords(InputStream inputStream, String sourceName) {
        if (inputStream == null) {
            throw new IllegalArgumentException("CSV source not found: " + sourceName);
        }

        ArrayList<Record> records = new ArrayList<>();

        try (BufferedReader bufferedReader =
                     new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line = bufferedReader.readLine();
            if (line == null) {
                throw new IllegalArgumentException("CSV line 1: missing header.");
            }
            validateHeader(line);

            int lineNumber = 1;
            while ((line = bufferedReader.readLine()) != null) {
                lineNumber++;
                String[] values = line.split(",", -1);
                if (values.length != 5) {
                    throw new IllegalArgumentException("CSV line " + lineNumber
                            + ": expected 5 fields, found " + values.length + ".");
                }

                String resortId = requiredValue(values[0], "resort", lineNumber);
                String day = requiredValue(values[1], "day", lineNumber);
                String skierId = requiredValue(values[2], "skier", lineNumber);
                String liftId = requiredValue(values[3], "lift", lineNumber);
                String time = requiredValue(values[4], "time", lineNumber);

                int lift;
                try {
                    lift = Integer.parseInt(liftId);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("CSV line " + lineNumber
                            + ": lift must be an integer from 1 to 40.", e);
                }
                if (lift < 1 || lift > 40 || !Integer.toString(lift).equals(liftId)) {
                    throw new IllegalArgumentException("CSV line " + lineNumber
                            + ": lift must be a canonical integer from 1 to 40.");
                }

                int minutes;
                try {
                    minutes = Integer.parseInt(time);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("CSV line " + lineNumber
                            + ": time must be an integer from 0 to 360 minutes.", e);
                }
                if (minutes < 0 || minutes > 360) {
                    throw new IllegalArgumentException("CSV line " + lineNumber
                            + ": time must be from 0 to 360 minutes.");
                }

                String operationalHour = Integer.toString(Math.min(minutes / 60, 5));
                records.add(new Record(resortId, day, skierId, liftId, operationalHour));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read CSV source: " + sourceName, e);
        }
        return records;
    }

    private static void validateHeader(String header) {
        String[] columns = header.replace("\uFEFF", "").split(",", -1);
        boolean commonColumns = columns.length == 5
                && columns[0].trim().equalsIgnoreCase("resort")
                && columns[1].trim().equalsIgnoreCase("day")
                && columns[4].trim().equalsIgnoreCase("time");
        boolean canonical = commonColumns
                && columns[2].trim().equalsIgnoreCase("skier")
                && columns[3].trim().equalsIgnoreCase("lift");
        boolean alternateHeader = commonColumns
                && columns[2].trim().equalsIgnoreCase("skierId")
                && columns[3].trim().equalsIgnoreCase("liftId");
        if (!canonical && !alternateHeader) {
            throw new IllegalArgumentException("CSV line 1: expected header "
                    + "resort,day,skier,lift,time (or Resort,Day,SkierID,LiftID,Time).");
        }
    }

    private static String requiredValue(String value, String name, int lineNumber) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("CSV line " + lineNumber
                    + ": " + name + " is required.");
        }
        return trimmed;
    }


    /**
     * Write skier statistics to Skiers.csv
     * Top 100 skiers by vertical distance in descending order
     *
     * @param data     result from analyzer.getSkierCsv()
     * @param fileName output file name
     */
    public static void writeSkiersCsv(Object[][] data, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            // Write header
            writer.write("SkierID,Vertical");
            writer.newLine();

            // Write top 100 skiers (or less if fewer skiers exist)
            int limit = Math.min(100, data.length);
            for (int i = 0; i < limit; i++) {
                writer.write(data[i][0] + "," + data[i][1]);
                writer.newLine();
            }

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write " + fileName, e);
        }
        System.out.println("Successfully wrote " + fileName);
    }

    /**
     * Write lift statistics to Lifts.csv
     * All lifts with number of rides in ascending order by LiftID
     *
     * @param data     result from analyzer.getLiftCsv()
     * @param fileName output file name
     */
    public static void writeLiftsCsv(Object[][] data, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            // Write header
            writer.write("LiftID,Number of Rides");
            writer.newLine();

            // Write all lifts
            for (Object[] row : data) {
                writer.write(row[0] + "," + row[1]);
                writer.newLine();
            }

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write " + fileName, e);
        }
        System.out.println("Successfully wrote " + fileName);
    }

    /**
     * Write hourly lift statistics to Hours.csv
     * 6 sections (hours 0-5), one for each hour, with top 10 busiest lifts per hour
     * @param data result from analyzer.getHoursCsv()
     * @param fileName output file name
     */
    public static void writeHoursCsv(Object[][] data, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {

            // Group data by hour
            String currentHour = null;
            int countInCurrentHour = 0;

            for (Object[] row : data) {
                String hour = (String) row[0];
                String liftId = (String) row[1];
                Object rides = row[2];

                // Check if we're starting a new hour section
                if (!hour.equals(currentHour)) {
                    // Write section header for new hour
                    if (currentHour != null) {
                        writer.newLine(); // Add blank line between sections
                    }
                    writer.write("Hour #" + hour);
                    writer.newLine();
                    writer.write("LiftID,Number of Rides");
                    writer.newLine();

                    currentHour = hour;
                    countInCurrentHour = 0;
                }

                // Write data (only top 10 per hour)
                if (countInCurrentHour < 10) {
                    writer.write(liftId + "," + rides);
                    writer.newLine();
                    countInCurrentHour++;
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write " + fileName, e);
        }
        System.out.println("Successfully wrote " + fileName);
    }
//    public static void main(String[] args) {
//        String filePath = "ski_lift_data.csv";
//        List<Record> records = readRecords(filePath);
//        int i = 0;
//        for(Record record : records) {
//            System.out.println("Resort ID: " + record.getResort() +
//                    ", Day: " + record.getDay() +
//                    ", Skier ID: " + record.getSkierId() +
//                    ", Lift ID: " + record.getLiftId() +
//                    ", Operational Hour: " + record.getOperationalHour());
//            i++;
//            if (i == 5) {
//                break;
//            }
//        }
//        System.out.println("Total records read: " + records.size());
//    }
}
