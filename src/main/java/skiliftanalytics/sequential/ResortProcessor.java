package skiliftanalytics.sequential;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ResortProcessor {

    private record ResortDay(String resortId, String day) {
    }

    /**
     * Group all records by resort and day, then create Resort objects
     * @param allRecords all records from CSV
     * @return list of Resort objects
     */
    public static ArrayList<Resort> createResorts(ArrayList<Record> allRecords) {
        // 1. Group by resort and day without conflating identifiers containing '-'.
        HashMap<ResortDay, ArrayList<Record>> groupedRecords = groupByResortAndDay(allRecords);

        // 2. Create Resort object for each group
        ArrayList<Resort> resorts = new ArrayList<>();

        for (Map.Entry<ResortDay, ArrayList<Record>> entry : groupedRecords.entrySet()) {
            ResortDay key = entry.getKey();
            ArrayList<Record> records = entry.getValue();

            // Build Resort object
            Resort resort = buildResort(key.resortId(), key.day(), records);
            resorts.add(resort);
        }

        return resorts;
    }

    /**
     * Group records by resort and day
     */
    private static HashMap<ResortDay, ArrayList<Record>> groupByResortAndDay(
            ArrayList<Record> allRecords) {

        HashMap<ResortDay, ArrayList<Record>> grouped = new HashMap<>();

        for (Record record : allRecords) {
            ResortDay key = new ResortDay(record.getResort(), record.getDay());

            grouped.putIfAbsent(key, new ArrayList<>());
            grouped.get(key).add(record);
        }

        return grouped;
    }

    /**
     * Build a single Resort object from records
     */
    private static Resort buildResort(String resortId, String day,
                                      ArrayList<Record> records) {

        // 1. Group records by hour
        HashMap<String, ArrayList<Record>> recordByHour = buildRecordByHour(records);

        // 2. Build Skier map
        HashMap<String, Skier> todaySkier = buildSkierMap(records);

        // 3. Build Lift map
        HashMap<String, Lift> lifts = buildLiftMap(records);

        // 4. Create Resort object
        return new Resort(resortId, day, records, recordByHour, todaySkier, lifts);
    }

    /**
     * Group records by operational hour
     */
    private static HashMap<String, ArrayList<Record>> buildRecordByHour(
            ArrayList<Record> records) {

        HashMap<String, ArrayList<Record>> recordByHour = new HashMap<>();

        for (Record record : records) {
            String hour = record.getOperationalHour();

            recordByHour.putIfAbsent(hour, new ArrayList<>());
            recordByHour.get(hour).add(record);
        }

        return recordByHour;
    }

    /**
     * Build Skier map from records
     */
    private static HashMap<String, Skier> buildSkierMap(ArrayList<Record> records) {
        HashMap<String, Skier> skiers = new HashMap<>();

        for (Record record : records) {
            String skierId = record.getSkierId();

            // Create new Skier if not exists
            if (!skiers.containsKey(skierId)) {
                ArrayList<Record> skierRecords = new ArrayList<>();
                skiers.put(skierId, new Skier(skierId, skierRecords));
            }

            // Add record to corresponding skier
            skiers.get(skierId).getRecord().add(record);
        }

        return skiers;
    }

    /**
     * Build Lift map from records
     */
    private static HashMap<String, Lift> buildLiftMap(ArrayList<Record> records) {
        HashMap<String, Lift> lifts = new HashMap<>();

        for (Record record : records) {
            String liftId = record.getLiftId();

            // Create new Lift if not exists
            if (!lifts.containsKey(liftId)) {
                ArrayList<Record> liftRecords = new ArrayList<>();
                // Lift constructor automatically calculates elevation based on liftId
                lifts.put(liftId, new Lift(liftId, liftRecords));
            }

            // Add record to corresponding lift
            lifts.get(liftId).getRecord().add(record);
        }

        return lifts;
    }
//    public static void main(String[] args) {
//        // 1. Read CSV file
//        ArrayList<Record> allRecords = DataStreamer.readRecords("ski_lift_data.csv");
//        System.out.println("Loaded " + allRecords.size() + " records");
//
//        // 2. Create Resort objects
//        ArrayList<Resort> resorts = ResortProcessor.createResorts(allRecords);
//        System.out.println("Created " + resorts.size() + " Resort objects");
//
//        // 3. Verify data
//        for (Resort resort : resorts) {
//            System.out.println("===== Resort " + resort.getResortId() +
//                    " - Day " + resort.getDay() + " =====");
//            System.out.println("Total records: " + resort.getRecords().size());
//            System.out.println("Number of skiers: " + resort.getTodaySkier().size());
//            System.out.println("Number of lifts: " + resort.getLifts().size());
//            System.out.println("Hours tracked: " + resort.getRecordByHour().size());
//            System.out.println();
//        }
//
//        // 4. Analyze using Analyzer
//        Analyzer analyzer = new Analyzer(resorts);
//
//        // Generate skier statistics
//        Object[][] skierStats = analyzer.getSkierCsv();
//        System.out.println("===== Top Skiers by Vertical Distance =====");
//        for (int i = 0; i < Math.min(5, skierStats.length); i++) {
//            System.out.println("Skier " + skierStats[i][0] +
//                    ": " + skierStats[i][1] + " meters");
//        }
//
//        // Generate lift usage statistics
//        Object[][] liftStats = analyzer.getLiftCsv();
//        System.out.println("\n===== Lift Usage Statistics =====");
//        for (int i = 0; i < Math.min(5, liftStats.length); i++) {
//            System.out.println("Lift " + liftStats[i][0] +
//                    ": " + liftStats[i][1] + " rides");
//        }
//    }
}
