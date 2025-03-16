package shared.communication_objects;

import java.io.Serializable;

public class Command implements Serializable {

    public enum Type {GAMES_LIST, PLAYER_INFO, TURN_CHOICE, GAME_CHOICE, MESSAGE, GAME_OVER}

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