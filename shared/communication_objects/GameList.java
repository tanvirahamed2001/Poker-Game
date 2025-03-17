/**
 * the object that allows passing the current table list on our server
 * houses an array list of game ids that can be sent across the socket
 */

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
