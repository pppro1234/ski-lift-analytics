package skiliftanalytics.sequential;

/**
 * Record represents a single raw lift ride record read from the input CSV file.
 * Each record stores resort, day, skier, lift and the derived operational hour.
 */
public class Record {
    private String resort;
    private String day;
    private String skierId;
    private String liftId;
    private String operationalHour;

    /**
     * Constructs a new {@code Record} with all fields initialized.
     *
     * @param resort          the resort identifier
     * @param day             the day identifier
     * @param skierId         the skier identifier
     * @param liftId          the lift identifier
     * @param operationalHour the derived operational hour for this ride
     */
    public Record(String resort, String day, String skierId, String liftId, String operationalHour) {
        this.resort = resort;
        this.day = day;
        this.skierId = skierId;
        this.liftId = liftId;
        this.operationalHour = operationalHour;
    }

    /**
     * Returns the resort identifier for this record.
     *
     * @return the resort identifier
     */
    public String getResort() {
        return resort;
    }

    /**
     * Updates the resort identifier for this record.
     *
     * @param resort the new resort identifier
     */
    public void setResort(String resort) {
        this.resort = resort;
    }

    /**
     * Returns the day associated with this record.
     *
     * @return the day
     */
    public String getDay() {
        return day;
    }

    /**
     * Updates the day associated with this record.
     *
     * @param day the new day value
     */
    public void setDay(String day) {
        this.day = day;
    }

    /**
     * Returns the skier identifier.
     *
     * @return the skier identifier
     */
    public String getSkierId() {
        return skierId;
    }

    /**
     * Updates the skier identifier.
     *
     * @param skierId the new skier identifier
     */
    public void setSkierId(String skierId) {
        this.skierId = skierId;
    }

    /**
     * Returns the lift identifier.
     *
     * @return the lift identifier
     */
    public String getLiftId() {
        return liftId;
    }

    /**
     * Updates the lift identifier.
     *
     * @param liftId the new lift identifier
     */
    public void setLiftId(String liftId) {
        this.liftId = liftId;
    }

    /**
     * Returns the operational hour associated with this record.
     *
     * @return the operational hour
     */
    public String getOperationalHour() {
        return operationalHour;
    }

    /**
     * Updates the operational hour value.
     *
     * @param operationalHour the new operational hour
     */
    public void setOperationalHour(String operationalHour) {
        this.operationalHour = operationalHour;
    }
}
