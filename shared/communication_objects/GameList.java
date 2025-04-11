package shared.communication_objects;

import java.io.Serializable;

/**
 * the object that allows passing the current table list on our server
 * houses an array list of game ids that can be sent across the socket
 */
public class GameList implements Serializable {

    String gameList;

    /**
     * Constructor for GameList
     * 
     * @param games String of Games
     */
    public GameList(String games) {
        this.gameList = games;
    }

    /**
     * Gets the List of Games
     * 
     * @return String of Games
     */
    public String getGames() {
        return this.gameList;
    }

}
