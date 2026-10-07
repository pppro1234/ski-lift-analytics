package skiliftanalytics.sequential;

import java.util.*;
import java.util.stream.Collectors;

public class Analyzer {
    private ArrayList<Resort> resorts;

    public Analyzer(ArrayList<Resort> resorts) {
        this.resorts = resorts;
    }

    public Object[][] getSkierCsv() {
        HashMap<String, Integer> stat = new HashMap<>();
        for (Resort resort : resorts) {
            HashMap<String, Lift> lifts = resort.getLifts();
            HashMap<String, Skier> skiers = resort.getTodaySkier();
            for (Skier skier : skiers.values()) {
                ArrayList<Record> records = skier.getRecord();
                String skierId = skier.getSkierId();
                for (Record record : records) {
                    String liftId = record.getLiftId();
                    int elevation = lifts.get(liftId).getElevation();
                    stat.put(skierId, stat.getOrDefault(skierId, 0) + elevation);
                }
            }
        }
        LinkedHashMap<String, Integer> sorted = sortHashMapDescending(stat);
        return mapToArray(sorted);
    }

    public Object[][] getLiftCsv() {
        HashMap<String, Integer> stat = getLiftStat();
        Map<String, Integer> sorted = new TreeMap<>(stat);
        return mapToArray(sorted);
    }

    public Object[][] getHoursCsv() {
        // Get lift statistics for each hour
        HashMap<String, HashMap<String, Integer>> hourlyLiftStats = getHourlyLiftStats();

        // Sort and get top 10 for each hour
        HashMap<String, Object[][]> sortedByHour = new HashMap<>();
        int totalRows = 0;

        for (Map.Entry<String, HashMap<String, Integer>> entry : hourlyLiftStats.entrySet()) {
            String hour = entry.getKey();
            HashMap<String, Integer> liftStats = entry.getValue();

            // Sort lifts by ride count (descending)
            LinkedHashMap<String, Integer> sorted = sortHashMapDescending(liftStats);
            Object[][] sortedArray = mapToArray(sorted);

            // Take top 10
            int limit = Math.min(10, sortedArray.length);
            Object[][] top10 = new Object[limit][2];
            System.arraycopy(sortedArray, 0, top10, 0, limit);

            sortedByHour.put(hour, top10);
            totalRows += limit;
        }

        // Build result array
        Object[][] result = new Object[totalRows][3];
        int index = 0;

        // Sort hours numerically
        List<String> sortedHours = new ArrayList<>(sortedByHour.keySet());
        sortedHours.sort((h1, h2) -> Integer.compare(Integer.parseInt(h1), Integer.parseInt(h2)));

        for (String hour : sortedHours) {
            Object[][] lifts = sortedByHour.get(hour);
            for (Object[] lift : lifts) {
                result[index][0] = hour;
                result[index][1] = lift[0];  // liftId
                result[index][2] = lift[1];  // numberOfRides
                index++;
            }
        }
        return result;
    }

    private HashMap<String, HashMap<String, Integer>> getHourlyLiftStats() {
        HashMap<String, HashMap<String, Integer>> hourlyStats = new HashMap<>();

        for (Resort resort : resorts) {
            HashMap<String, ArrayList<Record>> recordByHour = resort.getRecordByHour();

            for (Map.Entry<String, ArrayList<Record>> entry : recordByHour.entrySet()) {
                String hour = entry.getKey();
                ArrayList<Record> records = entry.getValue();

                hourlyStats.putIfAbsent(hour, new HashMap<>());
                HashMap<String, Integer> liftStats = hourlyStats.get(hour);

                for (Record record : records) {
                    String liftId = record.getLiftId();
                    liftStats.put(liftId, liftStats.getOrDefault(liftId, 0) + 1);
                }
            }
        }

        return hourlyStats;
    }
    private HashMap<String, Integer> getLiftStat() {
        HashMap<String, Integer> stat = new HashMap<>();
        for (Resort resort : resorts) {
            HashMap<String, Lift> lifts = resort.getLifts();
            for (Lift lift : lifts.values()) {
                String liftId = lift.getLiftId();
                ArrayList<Record> records = lift.getRecord();
                int rideCount = lift.getRecord().size();
                stat.put(liftId, stat.getOrDefault(liftId, 0) + rideCount);
            }
        }
        return stat;
    }

    private ArrayList<Record> getAllRecords() {
        ArrayList<Record> allRecords = new ArrayList<>();
        for (Resort resort : resorts) {
            ArrayList<Record> records = resort.getRecords();
            allRecords.addAll(records);
        }
        return allRecords;
    }

    private LinkedHashMap<String, Integer> sortHashMapDescending(HashMap<String, Integer> stat) {
        return stat.entrySet()
                .stream()
                .sorted((a, b) -> {
                    int byCount = Integer.compare(b.getValue(), a.getValue());
                    return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
                })
                .collect(Collectors.toMap(
                        HashMap.Entry::getKey,
                        HashMap.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private HashMap<String, Integer> sortHashMapAscending(HashMap<String, Integer> stat) {
        return stat.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private Object[][] mapToArray(Map<String, Integer> sorted) {
        Object[][] array = new Object[sorted.size()][2];
        int i = 0;
        for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
            array[i][0] = entry.getKey();
            array[i][1] = entry.getValue();
            i++;
        }
        return array;
    }

    public ArrayList<Resort> getResorts() {
        return resorts;
    }

    public void setResorts(ArrayList<Resort> resorts) {
        this.resorts = resorts;
    }
}
