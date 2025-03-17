/**
 * the main command object for housing our sub commands. This is what will be sent over the sockets
 * and the TYPE will be deduced from this class to handle any logic needed
 */

package shared.communication_objects;

import java.io.Serializable;

public class Command implements Serializable {

    public enum Type {GAMES_LIST, PLAYER_INFO, TURN_CHOICE, GAME_CHOICE, MESSAGE, GAME_OVER, TURN_TOKEN}

    private Type type;
    private Object payload;

    public Command(Type type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public Type getType() {
        return this.type;
    }

    public Object getPayload() {
        return this.payload;
    }
}