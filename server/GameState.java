import java.io.Serializable;
import java.util.ArrayList;

import shared.Player;
import shared.card_based.Card;

public class GameState implements Serializable {
    private ArrayList<Player> players;
    private int pot;
    private int currentTurn;
    private ArrayList<Card> tableCards;

    public GameState(ArrayList<Player> players, int pot, int currentTurn, ArrayList<Card> tableCards) {
        this.players = players;
        this.pot = pot;
        this.currentTurn = currentTurn;
        this.tableCards = tableCards;
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public int getPot() {
        return pot;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public ArrayList<Card> getTableCards() {
        return tableCards;
    }
}
