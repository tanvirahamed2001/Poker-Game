package games;
import java.io.Serializable;

public class Player implements Serializable {

    private String name;
    private double funds;

    /**
     * Creates a new Player object from client information.
     * @param name
     * @param funds
     */
    public Player(String name, double funds) {
        this.name = name;
        this.funds = funds;
    }

    /**
     * Gets Player name
     * @return
     */
    public String get_name() {
        return this.name;
    }

    /**
     * Get Player funds
     * @return
     */
    public double view_funds() {
        return this.funds;
    }

    /**
     * Player deposits more funds
     * @param amount
     */
    public void deposit_funds(double amount) {
        this.funds += amount;
    }

    /**
     * Bets a specific amount and takes it out of the Players amount
     * @param bet
     * @return
     */
    public double bet_amount(double bet) {
        if(bet <= this.funds && bet != 0) {
            this.funds = bet;
            return bet;
        }
        return 0;
        //TODO: Error out with something
    }

}
