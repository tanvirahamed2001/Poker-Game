package shared;

import java.io.Serializable;
import java.util.ArrayList;
import shared.card_based.Card;

// Player class no longer contains a Socket, making it serializable for network replication.
public class Player implements Serializable {

    private String name;
    private int funds;
    private ArrayList<Card> hand;
    private int client_id;
    private int seat;

    /**
     * Creates a new Player object from client information.
     * @param name The name of the player.
     * @param funds The initial funds of the player.
     */
    public Player(String name, int funds, int id) {
        this.name = name;
        this.funds = funds;
        this.hand = new ArrayList<>();
        this.client_id = id;
    }

    /**
     * Gets the player's name.
     * @return The name of the player.
     */
    public String get_name() {
        return this.name;
    }

    public void set_seat(int i) {
        this.seat = i;
    }

    public int get_seat() {
        return this.seat;
    }

    /**
     * Adds a new card to the player's hand.
     * @param card The card to add.
     */
    public void new_card(Card card) {
        hand.add(card);
    }

    /**
     * Returns a string representation of the player's hand.
     * @return A string containing all cards in the player's hand.
     */
    public String view_cards() {
        StringBuilder string = new StringBuilder();
        for (Card c : hand) {
            string.append(c.toString()).append(" ");
        }
        return string.toString();
    }

    /**
     * Returns the player's entire hand of cards.
     * @return An ArrayList of cards in the player's hand.
     */
    public ArrayList<Card> show_all_cards() {
        return hand;
    }

    /**
     * Removes all cards from the player's hand.
     */
    public void clear_hand() {
        hand.clear();
    }

    /**
     * Gets the player's current funds.
     * @return The player's funds.
     */
    public int view_funds() {
        return this.funds;
    }

    /**
     * Deposits funds into the player's account.
     * @param amount The amount to deposit.
     */
    public void deposit_funds(int amount) {
        this.funds += amount;
    }

    /**
     * Bets a specific amount and deducts it from the player's funds.
     * @param bet The amount to bet.
     * @return The amount bet if successful, otherwise 0.
     */
    public int bet_amount(int bet) {
        if (bet <= this.funds && bet != 0) {
            this.funds -= bet; // Deduct the bet amount from funds
            return bet;
        }
        return 0; // Indicates an invalid bet
    }

    @Override
    public String toString() {
        return "Player Name: " + this.name + " Funds: " + this.funds; 
    }
}