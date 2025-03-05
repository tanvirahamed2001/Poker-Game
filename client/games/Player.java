package games;

public class Player {

    private String name;
    private int funds;

    public Player(String name, int funds) {
        this.name = name;
        this.funds = funds;
    }

    public String get_name() {
        return this.name;
    }

    public int view_funds() {
        return this.funds;
    }

    public void deposit_funds(int amount) {
        this.funds += amount;
    }

    public int bet_amount(int bet) {
        if(bet <= this.funds && bet != 0) {
            this.funds = bet;
            return bet;
        }
        return 0;
        //TODO: Error out with something
    }

}
