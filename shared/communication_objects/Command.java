package shared.communication_objects;

import java.io.Serializable;

/**
 * The main command object for housing our sub commands. This is what will be
 * sent over the sockets
 * and the TYPE will be deduced from this class to handle any logic needed
 */
public class Command implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        GAMES_LIST, PLAYER_INFO, TURN_CHOICE, GAME_CHOICE, MESSAGE, GAME_OVER, TURN_TOKEN, ELECTION,
        HEARTBEAT, SERVER_CLIENT_ID, TABLE_INFO, RECONNECT, INITIAL_CONN,
        CLIENT_UPDATE_PLAYER, REPLICATION_ACK, SEAT, NEW, DISCONNECT, REFUND
    }

    private Type type;
    private Object payload;
    private int lamportTimestamp;

    /**
     * Constructor for Command Objects
     * 
     * @param type    the enum type of the command
     * @param payload the object payload of the command
     */
    public Command(Type type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    /**
     * Returns the type of the given command
     * 
     * @return Command.Type
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Returns the payload of the given command
     * 
     * @return Object
     */
    public Object getPayload() {
        return this.payload;
    }

    /**
     * Sets the lamport timestamp for the command
     * 
     * @param ts Integer Timestamp
     */
    public void setLamportTS(int ts) {
        this.lamportTimestamp = ts;
    }

    /**
     * Gets the lamport timestamp for the given command
     * 
     * @return Integer timestamp
     */
    public int getLamportTS() {
        return this.lamportTimestamp;
    }
}