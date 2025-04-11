package shared.communication_objects;

import java.io.Serializable;

/**
 * object to house choices for players during a game of poker
 * i.e fold, check, bet etc...
 */
public class TurnChoice implements Serializable {

    public enum Choice {
        CHECK, CALL, BET, FOLD, FUNDS, CARD
    }

    private Choice choice;

    private int bet;

    /**
     * Constructor for Turn Choice
     * 
     * @param choice TurnChoice.Choice
     */
    public TurnChoice(Choice choice) {
        this.choice = choice;
    }

    /**
     * If the player has chosen a BET, we can set bet amount here
     * 
     * @param amt the bet amount as int
     */
    public void betAmount(int amt) {
        this.bet = amt;
    }

    /**
     * If the player bets, we can get the bet
     * 
     * @return a int representation of the bet
     */
    public int getBet() {
        return this.bet;
    }

    /**
     * Gets the choice of the player
     * 
     * @return TurnChoice.Choice
     */
    public Choice getChoice() {
        return this.choice;
    }

}
