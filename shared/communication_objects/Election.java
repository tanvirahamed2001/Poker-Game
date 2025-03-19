package shared.communication_objects;

import java.io.Serializable;

public class Election implements Serializable{

    private int initiator_id;
    private int target_id;

    public Election(int initID, int targetID) {
        this.initiator_id = initID;
        this.target_id = targetID;
    }

    public int get_init_id() {
        return this.initiator_id;
    }

    public int get_target_id() {
        return this.target_id;
    }
    
}
