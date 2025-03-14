package shared;

import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import shared.card_based.Card;

//TODO: Rewrite player so it does not house a socket so we can send it over a network to replicate data to the backup servers (PLEASE REMOVE THIS TODO ONCE FINISHED)

public class Player implements Serializable {

    private String name;
    private int funds;
    private ArrayList<Card> hand;
    public Socket socket;

    /**
     * Creates a new Player object from client information.
     * @param name
     * @param funds
     */
    public Player(String name, int funds, Socket socket) {
        this.name = name;
        this.funds = funds;
        hand = new ArrayList<>();
        this.socket = socket;
    }

    /**
     * Gets Player name
     * @return
     */
    public String get_name() {
        return this.name;
    }

    /**
     * Adds a new card to the players hand
     * @param card
     */
    public void new_card(Card card) {
        hand.add(card);
    }

    /**
     * Player side for viewing cards. Prints the toString()
     */
    public String view_cards() {
    	String string = "";
        for(Card c : hand) {
            string += c.toString() + " ";
        }
        return string;
    }

    /**
     * Shows all the current cards to who ever. Not very safe. Fix later?
     * @return
     */
    public ArrayList<Card> show_all_cards() {
        return hand;
    }
    /**
     * remove all cards from the players' hand
     */
    public void clear_hand() {
    	hand.clear();
    }

    /**
     * Get Player funds
     * @return
     */
    public int view_funds() {
        return this.funds;
    }

    /**
     * Player deposits more funds
     * @param amount
     */
    public void deposit_funds(int amount) {
        this.funds += amount;
    }

    /**
     * Bets a specific amount and takes it out of the Players amount
     * @param bet
     * @return
     */
    public int bet_amount(int bet) {
        if(bet <= this.funds && bet != 0) {
            this.funds = bet;
            return bet;
        }
        return 0;
        //TODO: Error out with something (PLEASE REMOVE THIS TODO ONCE FINISHED)
    }

}
