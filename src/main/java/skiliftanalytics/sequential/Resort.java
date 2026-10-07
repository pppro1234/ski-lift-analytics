package skiliftanalytics.sequential;

import java.util.ArrayList;
import java.util.HashMap;
/**
 * Resort represents a resort-day aggregation of all lift ride records,
 * including records grouped by hour, by skier and by lift.
 */
public class Resort {
    private HashMap<String, Skier> todaySkier; // pair <SkierId, skiliftanalytics.sequential.Skier>
    private HashMap<String, Lift> lifts; // pair <LiftId, skiliftanalytics.sequential.Lift>
    private String resortId;
    private String day;
    private ArrayList<Record> records;
    private HashMap<String, ArrayList<Record>> recordByHour;

    /**
     * Constructs a new {@code Resort} with all fields initialized.
     *
     * @param resortId     the resort identifier
     * @param day          the day identifier
     * @param records      all records for this resort-day
     * @param recordByHour map from hour to records in that hour
     * @param todaySkier   map from skier id to skier object
     * @param lifts        map from lift id to lift object
     */
    public Resort(String resortId, String day, ArrayList<Record> records,
                  HashMap<String, ArrayList<Record>> recordByHour,
                  HashMap<String, Skier> todaySkier,
                  HashMap<String, Lift> lifts) {
        this.resortId = resortId;
        this.day = day;
        this.records = records;
        this.recordByHour = recordByHour;
        this.todaySkier = todaySkier;
        this.lifts = lifts;
    }

    /**
     * Returns the mapping from skier id to skier object for this resort-day.
     *
     * @return the skier map
     */
    public HashMap<String, Skier> getTodaySkier() {
        return todaySkier;
    }

    /**
     * Replaces the mapping from skier id to skier object.
     *
     * @param todaySkier the new skier map
     */
    public void setTodaySkier(HashMap<String, Skier> todaySkier) {
        this.todaySkier = todaySkier;
    }

    /**
     * Returns the mapping from lift id to lift object for this resort-day.
     *
     * @return the lift map
     */
    public HashMap<String, Lift> getLifts() {
        return lifts;
    }

    /**
     * Replaces the mapping from lift id to lift object.
     *
     * @param lifts the new lift map
     */
    public void setLifts(HashMap<String, Lift> lifts) {
        this.lifts = lifts;
    }

    /**
     * Returns the resort identifier.
     *
     * @return the resort identifier
     */
    public String getResortId() {
        return resortId;
    }

    /**
     * Updates the resort identifier.
     *
     * @param resortId the new resort identifier
     */
    public void setResortId(String resortId) {
        this.resortId = resortId;
    }

    /**
     * Returns the day identifier.
     *
     * @return the day identifier
     */
    public String getDay() {
        return day;
    }

    /**
     * Updates the day identifier.
     *
     * @param day the new day identifier
     */
    public void setDay(String day) {
        this.day = day;
    }

    /**
     * Returns all records for this resort-day.
     *
     * @return the list of records
     */
    public ArrayList<Record> getRecords() {
        return records;
    }

    /**
     * Replaces the list of records for this resort-day.
     *
     * @param records the new list of records
     */
    public void setRecords(ArrayList<Record> records) {
        this.records = records;
    }

    /**
     * Returns the mapping from hour to list of records for that hour.
     *
     * @return the map from hour to records
     */
    public HashMap<String, ArrayList<Record>> getRecordByHour() {
        return recordByHour;
    }

    /**
     * Replaces the mapping from hour to list of records.
     *
     * @param recordByHour the new hour-to-records map
     */
    public void setRecordByHour(HashMap<String, ArrayList<Record>> recordByHour) {
        this.recordByHour = recordByHour;
    }
}
