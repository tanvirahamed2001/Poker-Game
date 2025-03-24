package shared.communication_objects; // <--- add this line
import java.io.Serializable;
import java.util.ArrayList;

import shared.Player;
import shared.card_based.*;

public class GameState implements Serializable {
    private int gameId;
    private ArrayList<Player> players;
    private int pot;
    private int currentTurn;
    private ArrayList<Card> tableCards;
    private boolean inprogress;
    private int currentPlayer;
    private Deck saveDeck;

    public GameState(int gameId, ArrayList<Player> players, int pot, int currentTurn, ArrayList<Card> tableCards, boolean inprogress, int currentPlayer, Deck savedDeck) {
        this.gameId = gameId;
        this.players = players;
        this.pot = pot;
        this.currentTurn = currentTurn;
        this.tableCards = tableCards;
        this.inprogress = inprogress;
        this.currentPlayer = currentPlayer;
        this.saveDeck = savedDeck;
    }

    public int getGameId() {
        return gameId;
    }
    
    public Deck getSavedDeck() {
        return this.saveDeck;
    }

    public int getCurrentPlayer() {
        return this.currentPlayer;
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

    public boolean getProgress() {
        return inprogress;
    }

    public void updateGameState(int gameId,  ArrayList<Player> players, int pot, int currentTurn, ArrayList<Card> tablecards, boolean inprogress, int currentPlayer, Deck savedDeck) {
        this.gameId = gameId;
        this.players = players;
        this.pot = pot;
        this.currentTurn = currentTurn;
        this.tableCards = tablecards;
        this.inprogress = inprogress;
        this.currentPlayer = currentPlayer;
        this.saveDeck = savedDeck;
    }

    @Override
    public String toString() {
        return this.gameId + " " + this.players + " " + this.pot + " " + this.currentTurn + " " + this.tableCards + " " + this.inprogress + " " + this.currentPlayer + " " + this.saveDeck;
    }
}
