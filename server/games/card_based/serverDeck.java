package games.card_based;
import java.util.*;

public class serverDeck {

    private final List<serverCard> cards;

    public serverDeck() {
        cards = new ArrayList<serverCard>();

        for(serverCard.Suit suit : serverCard.Suit.values()) {
            for(serverCard.Rank rank : serverCard.Rank.values()) {
                cards.add(new serverCard(suit, rank));
            }
        }

        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public serverCard deal_card() {
        if(cards.isEmpty()) {
            throw new IllegalStateException("No Cards Left In Deck!");
        }
        return cards.remove(cards.size() - 1);
    }
}