// Basic information for a "Player"
// Might need to expand upon in the future
// Will work for what we need currently

package games;

import java.io.Serializable;

public class Player implements Serializable {

    private String name;
    private double funds;

    public Player(String name, double funds) {
        this.name = name;
        this.funds = funds;
    }

    public String get_name() {
        return this.name;
    }

    public double view_funds() {
        return this.funds;
    }

    public void deposit_funds(double amount) {
        this.funds += amount;
    }

    public double bet_amount(double bet) {
        if(bet <= this.funds && bet != 0) {
            this.funds = bet;
            return bet;
        }
        return 0;
        //TODO: Error out with something
    }

}
