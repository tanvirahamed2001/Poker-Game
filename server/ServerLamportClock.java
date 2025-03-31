import shared.communication_objects.LamportClock;

public class ServerLamportClock {
    
    private static final LamportClock clock = new LamportClock();

    private ServerLamportClock() {}
    
    public static LamportClock getInstance() {
        return clock;
    }
    
}
