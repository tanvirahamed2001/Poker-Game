package shared.communication_objects; // <--- add this line
import java.io.Serializable;
import java.util.ArrayList;

import shared.Player;
import shared.card_based.*;

public class GameState implements Serializable {
    private int gameId, pot, currentTurn, currentPlayer, lastActive, numPlayers, currentBet, lamportTimestamp;
    private boolean inprogress, activePlayers[], roundCompleted;
    private ArrayList<Player> players;
    private ArrayList<Integer> currentBets;
    private ArrayList<Card> tableCards;
    private Deck saveDeck;

    public GameState(int gameId, int pot, int currentTurn, int currentPlayer, int lastActive, int numPlayers, int currentBet, int ts,
                    boolean inprogress, boolean activePlayers[], boolean roundCompleted,
                    ArrayList<Player> players,
                    ArrayList<Integer> currentBets,
                    ArrayList<Card> tableCards,
                    Deck deck) {
        this.gameId = gameId;
        this.pot = pot;
        this.currentTurn = currentTurn;
        this.lastActive = lastActive;
        this.numPlayers = numPlayers;
        this.currentBet = currentBet;
        this.lamportTimestamp = ts;
        this.inprogress = inprogress;
        this.activePlayers = activePlayers;
        this.roundCompleted = roundCompleted;
        this.players = players;
        this.currentBets = currentBets;
        this.tableCards = tableCards;
        this.saveDeck = deck;
        this.currentPlayer = currentPlayer;
    }

    public int getLastActive() {
        return lastActive;
    }

    public int getNumPlayers() {
        return numPlayers;
    }

    public int getCurrentBet() {
        return currentBet;
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

    public boolean[] getActivePlayers() {
        return activePlayers;
    }

    public boolean getRoundCompleted() {
        return roundCompleted;
    }

    public ArrayList<Integer> getCurrentBets() {
        return currentBets;
    }

    public void updateGameState(int gameId, int pot, int currentTurn, int currentPlayer, int lastActive, int numPlayers, int currentBet,
                            boolean inprogress, boolean activePlayers[], boolean roundCompleted,
                            ArrayList<Player> players,
                            ArrayList<Integer> currentBets,
                            ArrayList<Card> tableCards,
                            Deck deck) {
            this.gameId = gameId;
            this.pot = pot;
            this.currentTurn = currentTurn;
            this.lastActive = lastActive;
            this.numPlayers = numPlayers;
            this.currentBet = currentBet;
            this.inprogress = inprogress;
            this.activePlayers = activePlayers;
            this.roundCompleted = roundCompleted;
            this.players = players;
            this.currentBets = currentBets;
            this.tableCards = tableCards;
            this.saveDeck = deck;
            this.currentPlayer = currentPlayer;
    }

    @Override
    public String toString() {
        return this.gameId + " " + this.players + " " + this.pot + " " + this.currentTurn + " " + this.tableCards + " " + this.inprogress + " " + this.currentPlayer + " " + this.saveDeck;
    }

    public void setLamportTS(int ts) {
        this.lamportTimestamp = ts;
    }

    public int getLamportTS() {
        return this.lamportTimestamp;
    }
}
