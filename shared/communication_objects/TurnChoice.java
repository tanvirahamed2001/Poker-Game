/**
 * object to house choices for players during a game of poker
 * i.e fold, check, bet etc...
 */

package shared.communication_objects;

import java.io.Serializable;

public class TurnChoice implements Serializable{

    public enum Choice {CHECK, CALL, BET, FOLD, FUNDS, CARD}

    private Choice choice;

    private int bet;

    public TurnChoice(Choice choice) {
        this.choice = choice;
    }
    
    public void betAmount(int amt) {
        this.bet = amt;
    }

    public int getBet() {
        return this.bet;
    }

    public Choice getChoice() {
        return this.choice;
    }
    
}
