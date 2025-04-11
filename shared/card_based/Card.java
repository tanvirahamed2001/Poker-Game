package shared.card_based;

import java.io.Serializable;

/**
 * Card object. Houses all the basic necessities of a card.
 */
public class Card implements Serializable {

    public enum Suit {
        HEARTS, DIAMOND, CLUBS, SPADES
    }

    public enum Rank {
        TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING, ACE
    }

    private final Suit suit;
    private final Rank rank;

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    /**
     * Returns the Suit of a given card
     * 
     * @return the suit
     */
    public Suit get_suit() {
        return this.suit;
    }

    /**
     * Returns the rank of a givencard
     * 
     * @return the rank
     */
    public Rank get_rank() {
        return this.rank;
    }

    /**
     * toString override to print our the rank and suit
     */
    @Override
    public String toString() {
        return rank + " of " + suit;
    }

}