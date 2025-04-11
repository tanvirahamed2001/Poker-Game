package shared.communication_objects;

import java.io.Serializable;

/**
 * Logical Clock representation via Lamport Algorithms
 */
public class LamportClock implements Serializable {

    private int lamportTime;

    /**
     * Constructor to initate lamport time to 0
     */
    public LamportClock() {
        lamportTime = 0;
    }

    /**
     * Ticks for internal events
     */
    public void tick() {
        this.lamportTime++;
    }

    /**
     * Function for incrementing lamport time when sending an object
     * 
     * @return the new lamport timestamp
     */
    public int sendEvent() {
        this.lamportTime++;
        return this.lamportTime;
    }

    /**
     * Function for incrementing lamport time when receiving an object
     * 
     * @param ts from the object received
     */
    public void receievedEvent(int ts) {
        this.lamportTime = Math.max(this.lamportTime, ts);
        this.lamportTime++;
    }

    /**
     * Function to get the lamport time
     * 
     * @return the current lamport timestamp
     */
    public int getTime() {
        return this.lamportTime;
    }
}
