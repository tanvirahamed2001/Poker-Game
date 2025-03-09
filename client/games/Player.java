package games;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import games.card_based.Card;

public class Player implements Serializable {

    private String name;
    private int funds;
    private ArrayList<Card> hand;

    /**
     * Creates a new Player object from client information.
     * @param name
     * @param funds
     */
    public Player(String name, int funds) {
        this.name = name;
        this.funds = funds;
        hand = new ArrayList<>();
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
    private void view_cards() {
        for(Card c : hand) {
            c.toString();
        }
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
        //TODO: Error out with something
    }

}
