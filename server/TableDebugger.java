import java.util.ArrayList;

import shared.card_based.Card;
import shared.card_based.Card.Rank;
import shared.card_based.Card.Suit;

public class TableDebugger {
	public static void main(String[] args) {
		ServerTable table = new ServerTable(0, new ArrayList<PlayerConnection>());
		ArrayList<Card> checker = new ArrayList<>();
		checker.add(new Card(Suit.HEARTS, Rank.TWO));
		checker.add(new Card(Suit.HEARTS, Rank.THREE));
		checker.add(new Card(Suit.DIAMOND, Rank.FOUR));
		checker.add(new Card(Suit.DIAMOND, Rank.FIVE));
		checker.add(new Card(Suit.DIAMOND, Rank.SIX));
		checker.add(new Card(Suit.DIAMOND, Rank.JACK));
		checker.add(new Card(Suit.DIAMOND, Rank.ACE));
		
		
	}
}
