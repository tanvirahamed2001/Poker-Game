import shared.communication_objects.LamportClock;

/**
 * The ServerLamportClock class provides a globally accessible instance
 * of a LamportClock for use across the server-side.
 */
public class ServerLamportClock {

    /** The single shared Lamport clock instance used across the server */
    private static final LamportClock clock = new LamportClock();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ServerLamportClock() {}

    /**
     * Returns the shared Lamport clock instance.
     *
     * @return the singleton {@link LamportClock} instance
     */
    public static LamportClock getInstance() {
        return clock;
    }
}
