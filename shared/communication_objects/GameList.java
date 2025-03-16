package shared.communication_objects;

import java.io.Serializable;
import java.util.ArrayList;

public class GameList implements Serializable {

    ArrayList<String> gameList;

    public GameList(ArrayList<String> games) {
        this.gameList = games;
    }

    public ArrayList<String> getGames() {
        return this.gameList;
    }
    
}
