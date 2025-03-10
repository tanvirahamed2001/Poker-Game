package shared.card_based;

import java.util.*;

public class Deck {

    private List<Card> cards;

    public Deck() {
        cards = new ArrayList<Card>();

        for(Card.Suit suit : Card.Suit.values()) {
            for(Card.Rank rank : Card.Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }

        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card deal_card() {
        if(cards.isEmpty()) {
            Deck reshuffled = new Deck();
            cards = reshuffled.cards;
        }
        return cards.remove(cards.size() - 1);
    }
}