package shared.communication_objects; // <--- add this line

import java.io.Serializable;
import java.util.ArrayList;

import shared.Player;
import shared.card_based.*;

/**
 * Utility class for initiating a game state replication. Houses all the
 * necessary data from a table
 * needed for replication to the backups.
 */
public class GameState implements Serializable {
    private int gameId, pot, currentTurn, currentPlayer, lastActive, numPlayers, currentBet, lamportTimestamp;
    private boolean inprogress, activePlayers[], roundCompleted;
    private ArrayList<Player> players;
    private ArrayList<Integer> currentBets;
    private ArrayList<Card> tableCards;
    private Deck saveDeck;

    /**
     * GameState constructor
     * 
     * @param gameId         int Game ID
     * @param pot            int Pot
     * @param currentTurn    int Current Turn
     * @param currentPlayer  int Current Player
     * @param lastActive     int Last Active Player
     * @param numPlayers     int Number of Players
     * @param currentBet     int Current Bet
     * @param ts             int TimeStamp
     * @param inprogress     boolean In Progress
     * @param activePlayers  boolean ActivePlayers
     * @param roundCompleted boolean Round Completed
     * @param players        ArrayList<Player> Players
     * @param currentBets    ArrayList<Integer> Current Bets
     * @param tableCards     ArrayList<Card> Table Cards
     * @param deck           Deck The Tables Deck
     */
    public GameState(int gameId, int pot, int currentTurn, int currentPlayer, int lastActive, int numPlayers,
            int currentBet, int ts,
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

    /**
     * Gets last active player
     * 
     * @return int Last Active
     */
    public int getLastActive() {
        return lastActive;
    }

    /**
     * Gets the number of players
     * 
     * @return int num players
     */
    public int getNumPlayers() {
        return numPlayers;
    }

    /**
     * Get the current bet
     * 
     * @return int current bet
     */
    public int getCurrentBet() {
        return currentBet;
    }

    /**
     * Get the Game ID
     * 
     * @return int game id
     */
    public int getGameId() {
        return gameId;
    }

    /**
     * Get the tables deck
     * 
     * @return deck tables deck
     */
    public Deck getSavedDeck() {
        return this.saveDeck;
    }

    /**
     * Get the current player
     * 
     * @return int representing the current player
     */
    public int getCurrentPlayer() {
        return this.currentPlayer;
    }

    /**
     * Returns the list of players
     * 
     * @return ArrayList<Player>
     */
    public ArrayList<Player> getPlayers() {
        return players;
    }

    /**
     * Get the current pot
     * 
     * @return int pot
     */
    public int getPot() {
        return pot;
    }

    /**
     * Get the current turn 1-5
     * 
     * @return int for the current turn
     */
    public int getCurrentTurn() {
        return currentTurn;
    }

    /**
     * Get the list of cards on the table. The Flop, River, etc...
     * 
     * @return ArrayList<Card>
     */
    public ArrayList<Card> getTableCards() {
        return tableCards;
    }

    /**
     * Gets the in progress status of the table
     * 
     * @return boolean inProgress
     */
    public boolean getProgress() {
        return inprogress;
    }

    /**
     * Get the list of active Players
     * 
     * @return boolean[] representing active players
     */
    public boolean[] getActivePlayers() {
        return activePlayers;
    }

    /**
     * Gets the boolean for round completed
     * 
     * @return boolean for round completion
     */
    public boolean getRoundCompleted() {
        return roundCompleted;
    }

    /**
     * Get the current bets
     * 
     * @return ArrayList<Integer> of current bets
     */
    public ArrayList<Integer> getCurrentBets() {
        return currentBets;
    }

    /**
     * toString override for printing relevant information about the game state
     */
    @Override
    public String toString() {
        return this.gameId + " " + this.players + " " + this.pot + " " + this.currentTurn + " " + this.tableCards + " "
                + this.inprogress + " " + this.currentPlayer + " " + this.saveDeck;
    }

    /**
     * Sets the lamport timestamp for the game state
     * 
     * @param ts int
     */
    public void setLamportTS(int ts) {
        this.lamportTimestamp = ts;
    }

    /**
     * Gets the lamport timestamp for the game state
     * 
     * @return a int for the lamport timestamp
     */
    public int getLamportTS() {
        return this.lamportTimestamp;
    }
}
