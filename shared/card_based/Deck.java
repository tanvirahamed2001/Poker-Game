package shared.card_based;

import java.io.Serializable;
import java.util.*;

/**
 * Deck object. Houses everything needed to build a deck of cards
 */
public class Deck implements Serializable {

    private List<Card> cards;

    /**
     * Deck constructor
     */
    public Deck() {
        cards = new ArrayList<Card>();

        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }

        shuffle();
    }

    /**
     * Function for shuffling the built in list of cards
     */
    public void shuffle() {
        Collections.shuffle(cards);
    }

    /**
     * Deals out a single card from the deck
     * 
     * @return a single card
     */
    public Card deal_card() {
        if (cards.isEmpty()) {
            Deck reshuffled = new Deck();
            cards = reshuffled.cards;
        }
        return cards.remove(cards.size() - 1);
    }
}