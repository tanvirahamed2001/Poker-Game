package shared.communication_objects;

import java.io.Serializable;

/**
 * Utility object for initiation of a Election. Combined with the Command object
 */
public class Election implements Serializable {

    private int initiator_id;
    private int target_id;

    /**
     * Election constructor
     * 
     * @param initID   Initiator ID
     * @param targetID Target ID
     */
    public Election(int initID, int targetID) {
        this.initiator_id = initID;
        this.target_id = targetID;
    }

    /**
     * Returns the Initiator ID
     * 
     * @return Int ID of the Initiator
     */
    public int get_init_id() {
        return this.initiator_id;
    }

    /**
     * Returns the Target ID
     * 
     * @return Int ID of the Target
     */
    public int get_target_id() {
        return this.target_id;
    }

}
