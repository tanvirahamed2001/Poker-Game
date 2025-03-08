// Basic information for a "Player"
// Might need to expand upon in the future
// Will work for what we need currently

package games;

public class Player {

    private String name;
    private double funds;

    public Player(String name, double funds) {
        this.name = name;
        this.funds = funds;
    }

    public String get_name() {
        return this.name;
    }

    public int view_funds() {
        return this.funds;
    }

    public void deposit_funds(double amount) {
        this.funds += amount;
    }

    public int bet_amount(double bet) {
        if(bet <= this.funds && bet != 0) {
            this.funds = bet;
            return bet;
        }
        return 0;
        //TODO: Error out with something
    }

}
