package shared.communication_objects;

import java.io.Serializable;

public class LamportClock implements Serializable {
    
    private int lamportTime;

    public LamportClock() {
        lamportTime = 0;
    }

    public void tick() {
        this.lamportTime++;
    }

    public int sendEvent() {
        this.lamportTime++;
        return this.lamportTime;
    }

    public void receievedEvent(int ts) {
        this.lamportTime = Math.max(this.lamportTime, ts);
        this.lamportTime++;
    }

    public int getTime() {
        return this.lamportTime;
    }
}
