package shared.card_based;

public class Card {
    
    public enum Suit {HEARTS, DIAMOND, CLUBS, SPADES}

    public enum Rank {TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING, ACE}

    private final Suit suit;
    private final Rank rank;

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public Suit get_suit() {
        return this.suit;
    }

    public Rank get_rank() {
        return this.rank;
    }

    @Override
    public String toString() {
        return rank + " of " + suit;
    }

}