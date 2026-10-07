package skiliftanalytics.sequential;

import java.util.ArrayList;
/**
 * Skier represents a skier and the list of lift ride records associated
 * with that skier on a given resort-day.
 */

public class Skier {
    private String skierId;
    private ArrayList<Record> record;

    /**
     * Constructs a new {@code Skier} with the given identifier and records.
     *
     * @param skierId the skier identifier
     * @param record  the list of records associated with this skier
     */

    public Skier(String skierId, ArrayList<Record> record) {
        this.skierId = skierId;
        this.record = record;
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
     * Returns the list of records for this skier.
     *
     * @return the list of records
     */
    public ArrayList<Record> getRecord() {
        return record;
    }


    /**
     * Replaces the list of records for this skier.
     *
     * @param record the new list of records
     */
    public void setRecord(ArrayList<Record> record) {
        this.record = record;
    }
}
