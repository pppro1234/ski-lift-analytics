package skiliftanalytics.sequential;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Lift represents a ski lift, including its identifier, elevation gain and
 * the list of records for rides on this lift.
 */
public class Lift {
    private String liftId;
    private ArrayList<Record> record;
    private int elevation;

    /**
     * Constructs a new {@code Lift} with the given identifier and record list.
     * The elevation gain is derived from the numeric lift id:
     * <ul>
     *     <li>1–10  → 200</li>
     *     <li>11–20 → 300</li>
     *     <li>21–30 → 400</li>
     *     <li>31–40 → 500</li>
     * </ul>
     *
     * @param liftId the lift identifier as a string
     * @param record the list of records for this lift
     * @throws IllegalArgumentException if the numeric id is not in [1,40]
     */
    public Lift(String liftId, ArrayList<Record> record) {
        this.liftId = liftId;
        this.record = record;
        int id = Integer.parseInt(liftId);
        if(id<1||id>40) throw new IllegalArgumentException("LiftId must be 1 to 40. ");
        this.elevation = (id <= 10) ? 200
                : ((id <= 20) ? 300
                : ((id <= 30) ? 400
                : 500));
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
     * Returns the list of records associated with this lift.
     *
     * @return the list of records
     */
    public ArrayList<Record> getRecord() {
        return record;
    }

    /**
     * Replaces the list of records for this lift.
     *
     * @param record the new list of records
     */
    public void setRecord(ArrayList<Record> record) {
        this.record = record;
    }

    /**
     * Returns the elevation gain contributed by one ride on this lift.
     *
     * @return the elevation gain
     */
    public int getElevation() {
        return elevation;
    }

    /**
     * Manually updates the elevation gain for this lift.
     *
     * @param elevation the new elevation gain
     */
    public void setElevation(int elevation) {
        this.elevation = elevation;
    }

    /**
     * Compares this lift to another object for equality. Two lifts are equal
     * if they have the same id, elevation and record list.
     *
     * @param o the object to compare with
     * @return {@code true} if the two lifts are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Lift lift = (Lift) o;
        return elevation == lift.elevation && Objects.equals(liftId, lift.liftId) && Objects.equals(record, lift.record);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code value for this lift
     */
    @Override
    public int hashCode() {
        return Objects.hash(liftId, record, elevation);
    }

}
