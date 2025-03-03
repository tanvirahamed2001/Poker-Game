package games.card_based;
import java.util.*;

public class Deck {

    private final List<Card> cards;

    public Deck() {
        cards = new ArrayList<>();

        for(Card.Suit suit : Card.Suit.values()) {
            for(Card.Rank rank : Card.Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }

        shuffle()
    }

    public void shuffle() {
        Collections.shuffle(card);
    }

    public Card deal_card() {
        if(cards.isEmpty()) {
            throw new IllegalStateException("No Cards Left In Deck!");
        }
        return cards.remove(cards.size() - 1);
    }
}